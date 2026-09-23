/**
 * CannaApp — Cloud Functions
 *
 * Veškerá komunikace s Dotykačkou běží TADY na serveru, ne v aplikaci.
 * Refresh token dává přístup k celému cloudu pokladny (tržby, zákazníci,
 * vytváření a placení účtů), takže nesmí být v APK, které si stáhne každý zákazník.
 *
 * Funkce:
 *  - assignCustomerToOrder  (callable)  → záloha: obsluha naskenuje kód v admin appce a zákazník se připojí k účtu
 *    (hlavní cesta: prodavač naskenuje kód čtečkou přímo na pokladně, Dotykačka zákazníka načte sama)
 *  - syncDotykackaOrders    (každých 5 min) → dosynchronizuje zákazníky + zaplacené účty se zákazníkem = body
 *  - onUserCreated          (Firestore trigger) → nový zákazník dostane členský kód a účet v Dotykačce
 *  - notifyOnTransaction    (Firestore trigger) → push notifikace při každé změně bodů
 *  - importDotykackaCustomers (callable) → založí v appce účty stávajícím zákazníkům z Dotykačky
 *  - dotykackaStatus        (callable)  → diagnostika spojení z admin appky / při nastavování
 *
 * Nastavení: viz DOTYKACKA_NAVOD.md v kořeni repa.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const { defineSecret, defineString, defineInt } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const { randomInt } = require("node:crypto");
// firebase-admin 13+ má jen modulární API (admin.firestore() už neexistuje)
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

// Firestore běží v nam5 → funkce musí být v us-central1 (europe-west1 nefunguje)
setGlobalOptions({ region: "us-central1", maxInstances: 5 });

// ── Konfigurace ─────────────────────────────────────────────────────
const REFRESH_TOKEN = defineSecret("DOTYKACKA_REFRESH_TOKEN");
const CLOUD_ID = defineString("DOTYKACKA_CLOUD_ID");
const BRANCH_ID = defineString("DOTYKACKA_BRANCH_ID");
const KC_PER_POINT = defineInt("KC_PER_POINT", { default: 10 });

const API = "https://api.dotykacka.cz/v2";

// ── Chyby Dotykačky ─────────────────────────────────────────────────
class DotyError extends Error {
  constructor(status, body, path) {
    super(`Dotykačka ${status} na ${path}: ${String(body).slice(0, 500)}`);
    this.status = status;
  }
}

// ── Access token (platí ~1 h, obnovuje se sám) ──────────────────────
let cachedToken = null;
let cachedUntil = 0;

async function getAccessToken(force = false) {
  if (!force && cachedToken && Date.now() < cachedUntil) return cachedToken;

  const res = await fetch(`${API}/signin/token`, {
    method: "POST",
    headers: {
      "Authorization": `User ${REFRESH_TOKEN.value().trim()}`,
      "Content-Type": "application/json",
      "Accept": "application/json",
    },
    body: JSON.stringify({ _cloudId: CLOUD_ID.value().trim() }),
  });
  const text = await res.text();
  if (!res.ok) throw new DotyError(res.status, text, "/signin/token");

  const json = JSON.parse(text);
  if (!json.accessToken) throw new DotyError(res.status, text, "/signin/token (chybí accessToken)");
  cachedToken = json.accessToken;
  cachedUntil = Date.now() + 50 * 60 * 1000; // o 10 min dřív než vyprší
  return cachedToken;
}

/** Volání REST API v rámci cloudu. Při 401 jednou obnoví token a zkusí znovu. */
async function api(path, opts = {}, retried = false) {
  const { method = "GET", body, allow404 = false, headers = {}, withEtag = false } = opts;
  const token = await getAccessToken(retried);
  const res = await fetch(`${API}/clouds/${CLOUD_ID.value().trim()}${path}`, {
    method,
    headers: {
      "Authorization": `Bearer ${token}`,
      "Content-Type": "application/json",
      "Accept": "application/json",
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.status === 401 && !retried) {
    return api(path, opts, true);
  }
  // Dotykačka vrací u prázdného seznamu 404
  if (res.status === 404 && allow404) return null;

  const text = await res.text();
  if (!res.ok) throw new DotyError(res.status, text, path);
  const json = text ? JSON.parse(text) : null;
  return withEtag ? { json, etag: res.headers.get("etag") } : json;
}

/** Seznamy jsou stránkované: { data: [...] }. Prázdný seznam = 404. */
function listData(json) {
  if (!json) return [];
  if (Array.isArray(json)) return json;
  return Array.isArray(json.data) ? json.data : [];
}

/**
 * POS akce — příkaz přímo do pokladny na pobočce. Bez vlastního webhooku
 * čeká server na odpověď pokladny (max 21 s) a vrátí ji v těle odpovědi.
 * Pokladna musí být zapnutá a online.
 */
async function posAction(payload) {
  const path = `/branches/${BRANCH_ID.value().trim()}/pos-actions`;
  let json;
  try {
    json = await api(path, { method: "POST", body: payload });
  } catch (e) {
    if (e.status === 404) {
      throw new HttpsError("unavailable", "Pokladna neodpověděla. Je zapnutá a připojená k internetu?");
    }
    if (e.status === 429) {
      throw new HttpsError("resource-exhausted", "Pokladna právě zpracovává jiný požadavek, zkus to za pár sekund.");
    }
    throw e;
  }
  const code = json && typeof json.code === "number" ? json.code : 0;
  if (code !== 0) {
    throw new HttpsError("failed-precondition", posErrorMessage(code, json), { code });
  }
  return json;
}

function posErrorMessage(code, json) {
  const known = {
    1002: "Nastavení pokladny tuto akci nepovoluje.",
    1006: "Licence pokladny nepovoluje POS akce.",
    1010: "Zákazník ještě není v pokladně (synchronizace trvá chvilku). Zkus to znovu za ~30 s.",
    2001: "Účet je právě zamčený — dokonči na pokladně rozdělanou akci a zkus to znovu.",
    2002: "Účet nebyl nalezen (mezitím se zavřel?).",
    2004: "Účet už je zaplacený.",
    2005: "Účet už je vystavený, zákazníka už nejde přidat.",
    3001: "Pokladna je uzavřená — nejdřív otevři směnu.",
  };
  return known[code] || `Pokladna vrátila chybu ${code}: ${json?.message || "neznámá chyba"}`;
}

// ── Zákazníci ────────────────────────────────────────────────────────
function normEmail(e) {
  return String(e || "").trim().toLowerCase();
}

async function findCustomerByEmail(email) {
  const target = normEmail(email);
  if (!target) return null;
  // email podporuje jen operátor "like" (obsahuje) → přesnou shodu ověříme sami
  const filter = encodeURIComponent(`email|like|${target}`);
  const json = await api(`/customers?filter=${filter}&limit=100`, { allow404: true });
  return listData(json).find((c) => normEmail(c.email) === target && !c.deleted) || null;
}

async function findCustomerByExternalId(userId) {
  if (!userId) return null;
  const filter = encodeURIComponent(`externalId|eq|${userId}`);
  const json = await api(`/customers?filter=${filter}&limit=5`, { allow404: true });
  return listData(json).find((c) => !c.deleted) || null;
}

async function createCustomer(user, barcode) {
  const parts = String(user.name || "").trim().split(/\s+/).filter(Boolean);
  const firstName = parts[0] || "Zákazník";
  const lastName = parts.length > 1 ? parts.slice(1).join(" ") : "CannaClub";

  const customer = {
    _cloudId: Number(CLOUD_ID.value().trim()),
    firstName: firstName.slice(0, 180),
    lastName: lastName.slice(0, 180),
    companyName: "",
    email: normEmail(user.email).slice(0, 100),
    phone: String(user.phone || "").replace(/\s+/g, "").slice(0, 20),
    externalId: user.id,
    addressLine1: "",
    barcode: barcode || "",
    companyId: "",
    vatId: "",
    zip: "",
    headerPrint: "",
    hexColor: "#4A6741",
    internalNote: "Vytvořeno automaticky z CannaApp",
    points: 0,
    tags: ["CannaApp"],
    display: true,
    deleted: false,
    flags: 0, // povinné, i když nic nenastavujeme
  };

  const json = await api("/customers", { method: "POST", body: [customer] });
  const created = Array.isArray(json) ? json[0] : listData(json)[0] || json;
  if (!created || created.id == null) throw new Error(`Vytvoření zákazníka nevrátilo ID: ${JSON.stringify(json)}`);
  logger.info("Vytvořen zákazník v Dotykačce", { userId: user.id, dotykackaId: created.id });
  return created;
}

// ── Členský kód (čárový kód pro pokladnu) ─────────────────────────────
// Číselný, aby ho přečetl každý skener bez ohledu na rozložení klávesnice.
// Prefix 29 = interní rozsah, nekoliduje s EAN kódy zboží.
async function generateMemberCode() {
  for (let attempt = 0; attempt < 10; attempt++) {
    const code = "29" + String(randomInt(0, 10_000_000_000)).padStart(10, "0");
    const taken = await db.collection("users").where("memberCode", "==", code).limit(1).get();
    if (taken.empty) return code;
  }
  throw new Error("Nepodařilo se vygenerovat unikátní členský kód");
}

async function codeTakenByOther(code, userId) {
  const snap = await db.collection("users").where("memberCode", "==", code).limit(2).get();
  return snap.docs.some((d) => d.id !== userId);
}

/** Zapíše zákazníkovi v Dotykačce čárový kód (PATCH vyžaduje ETag). */
async function setCustomerBarcode(customerId, barcode) {
  const { json: current, etag } = await api(`/customers/${customerId}`, { withEtag: true });
  if (!etag) throw new Error("Dotykačka nevrátila ETag zákazníka");
  await api(`/customers/${customerId}`, {
    method: "PATCH",
    headers: { "If-Match": etag },
    body: { id: Number(customerId), _cloudId: Number(CLOUD_ID.value().trim()), barcode },
  });
  logger.info("Nastaven čárový kód zákazníka", { customerId, barcode, previous: current && current.barcode });
}

async function getCustomerById(id) {
  const c = await api(`/customers/${id}`, { allow404: true });
  return c && !c.deleted ? c : null;
}

/**
 * Propojí uživatele appky se zákazníkem v Dotykačce:
 *  - najde ho (dotykackaId → externalId → e-mail), jinak založí,
 *  - zajistí, že má v Dotykačce jako "Čárový kód" členský kód z appky.
 *    Pokud už tam čárový kód má (plastová karta), převezme ho appka,
 *    aby stará karta fungovala dál.
 * Vrací ID zákazníka v Dotykačce.
 */
async function syncUser(userId) {
  const ref = db.collection("users").doc(userId);
  const snap = await ref.get();
  if (!snap.exists) throw new Error(`Uživatel ${userId} neexistuje`);
  const user = { ...snap.data(), id: snap.id };

  let customer =
    (user.dotykackaId && (await getCustomerById(user.dotykackaId))) ||
    (await findCustomerByExternalId(user.id)) ||
    (await findCustomerByEmail(user.email));

  let code = String(user.memberCode || "").trim();

  if (customer) {
    const existing = String(customer.barcode || "").trim();
    if (existing && existing !== code) {
      if (await codeTakenByOther(existing, user.id)) {
        throw new Error(`Čárový kód ${existing} zákazníka ${customer.id} už má v appce jiný uživatel`);
      }
      code = existing; // převzít kód z karty
    } else if (!existing) {
      if (!code) code = await generateMemberCode();
      await setCustomerBarcode(customer.id, code);
    }
  } else {
    if (!code) code = await generateMemberCode();
    customer = await createCustomer(user, code);
  }

  await ref.update({
    dotykackaId: String(customer.id),
    memberCode: code,
    dotykackaSynced: true,
    dotykackaSyncError: FieldValue.delete(),
  });
  return String(customer.id);
}

async function syncUserSafe(userId) {
  try {
    await syncUser(userId);
    return true;
  } catch (e) {
    logger.error("Synchronizace zákazníka s Dotykačkou selhala", { userId, error: e.message });
    await db.collection("users").doc(userId)
      .update({ dotykackaSynced: false, dotykackaSyncError: String(e.message).slice(0, 300) })
      .catch(() => {});
    return false;
  }
}

// ── Přiřazení k otevřenému účtu ──────────────────────────────────────
function toMillis(v) {
  if (v == null) return 0;
  if (typeof v === "number") return v;
  const t = Date.parse(v);
  return Number.isNaN(t) ? 0 : t;
}

async function assignToOpenOrder(dotykackaId) {
  const customerId = Number(dotykackaId);
  const list = await posAction({ action: "order/list" });
  const open = (list.orders || [])
    .map((o) => o.order)
    .filter((o) => o && !o.paid && !o.completed && !o["canceled-date"]);

  // Už je připojený (dvojité naskenování) → nic nedělat
  const already = open.find((o) => Number(o["customer-id"]) === customerId);
  if (already) {
    return { orderId: already.id, orderTotal: already["price-total"] ?? null, openWithoutCustomer: 0, alreadyAssigned: true };
  }

  const candidates = open
    .filter((o) => o["customer-id"] == null)
    .sort((a, b) => toMillis(b.created) - toMillis(a.created));

  if (candidates.length === 0) {
    throw new HttpsError(
      "failed-precondition",
      "Na pokladně není otevřený účet bez zákazníka. Nejdřív namarkuj zboží, pak skenuj QR."
    );
  }

  // Nejnověji založený účet = ten, který obsluha právě markuje
  const target = candidates[0];
  await posAction({ "action": "order/update", "order-id": target.id, "customer-id": customerId });

  return {
    orderId: target.id,
    orderTotal: target["price-total"] ?? null,
    openWithoutCustomer: candidates.length,
    alreadyAssigned: false,
  };
}

async function requireAdmin(request) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Nejsi přihlášený jako obsluha.");
  const adminDoc = await db.collection("admins").doc(request.auth.uid).get();
  if (!adminDoc.exists) throw new HttpsError("permission-denied", "Tento účet není obsluha.");
}

function wrapError(e, fallback) {
  if (e instanceof HttpsError) return e;
  logger.error(fallback, e);
  if (e instanceof DotyError && e.status === 401) {
    return new HttpsError("unauthenticated", "Dotykačka odmítla přihlášení — zkontroluj refresh token a Cloud ID.");
  }
  if (e instanceof DotyError && e.status === 403) {
    return new HttpsError("permission-denied", "Aplikace nemá v Dotykačce oprávnění k této akci.");
  }
  return new HttpsError("internal", `${fallback}: ${e.message}`);
}

// ════════════════════════════════════════════════════════════════════
// 1) Obsluha naskenovala QR → připojit zákazníka k účtu na pokladně
// ════════════════════════════════════════════════════════════════════
exports.assignCustomerToOrder = onCall({ secrets: [REFRESH_TOKEN] }, async (request) => {
  await requireAdmin(request);

  const userId = request.data && request.data.userId;
  if (typeof userId !== "string" || !userId) {
    throw new HttpsError("invalid-argument", "Chybí userId.");
  }

  const snap = await db.collection("users").doc(userId).get();
  if (!snap.exists) throw new HttpsError("not-found", "Zákazník nenalezen.");

  try {
    const dotykackaId = await syncUser(userId);
    const result = await assignToOpenOrder(dotykackaId);
    logger.info("Zákazník připojen k účtu", { userId, dotykackaId, ...result });
    return { dotykackaId, ...result };
  } catch (e) {
    throw wrapError(e, "Připojení k účtu selhalo");
  }
});

// ════════════════════════════════════════════════════════════════════
// 1b) Import stávajících zákazníků z Dotykačky do appky
//     Spouští obsluha tlačítkem v admin appce. Založí účet v appce každému
//     zákazníkovi z Dotykačky, který má e-mail a v appce ještě není.
//     Členský kód = jeho stávající čárový kód z Dotykačky, jinak nový
//     (ten do Dotykačky zapíše trigger onUserCreated).
// ════════════════════════════════════════════════════════════════════
async function listAllCustomers() {
  const all = [];
  for (let page = 1; page <= 200; page++) {
    const json = await api(`/customers?limit=100&page=${page}`, { allow404: true });
    const data = listData(json);
    all.push(...data);
    if (data.length < 100) break;
  }
  return all.filter((c) => !c.deleted);
}

function customerName(c) {
  const name = [c.firstName, c.lastName].map((x) => String(x || "").trim()).filter(Boolean).join(" ");
  return name || String(c.companyName || "").trim();
}

exports.importDotykackaCustomers = onCall(
  { secrets: [REFRESH_TOKEN], timeoutSeconds: 540, memory: "512MiB" },
  async (request) => {
    await requireAdmin(request);
    const dryRun = request.data && request.data.dryRun === true;

    let customers;
    try {
      customers = await listAllCustomers();
    } catch (e) {
      throw wrapError(e, "Načtení zákazníků z Dotykačky selhalo");
    }

    const users = await db.collection("users").get();
    const byEmail = new Set();
    const byDotyId = new Set();
    const codes = new Set();
    users.docs.forEach((d) => {
      if (d.get("email")) byEmail.add(normEmail(d.get("email")));
      if (d.get("dotykackaId")) byDotyId.add(String(d.get("dotykackaId")));
      if (d.get("memberCode")) codes.add(String(d.get("memberCode")));
    });

    const result = { totalInDotykacka: customers.length, imported: 0, alreadyInApp: 0, noEmail: 0, duplicateEmail: 0 };
    const seenEmails = new Set();
    let batch = db.batch();
    let inBatch = 0;

    for (const c of customers) {
      const email = normEmail(c.email);
      if (!email || !email.includes("@")) { result.noEmail++; continue; }
      if (byEmail.has(email) || byDotyId.has(String(c.id))) { result.alreadyInApp++; continue; }
      if (seenEmails.has(email)) { result.duplicateEmail++; continue; }
      seenEmails.add(email);

      let code = String(c.barcode || "").trim();
      const hasCard = Boolean(code) && !codes.has(code);
      if (!hasCard) code = await generateMemberCode();
      codes.add(code);

      result.imported++;
      if (dryRun) continue;

      const ref = db.collection("users").doc();
      batch.set(ref, {
        id: "",
        name: customerName(c) || "Zákazník",
        email,
        phone: String(c.phone || "").replace(/\s+/g, ""),
        points: 0,
        totalPoints: 0,
        dotykackaId: String(c.id),
        memberCode: code,
        // má-li kartu, je hotovo; jinak kód do Dotykačky zapíše onUserCreated
        dotykackaSynced: hasCard,
        importedFromDotykacka: true,
        fcmToken: "",
        createdAt: Timestamp.now(),
      });
      if (++inBatch === 400) { await batch.commit(); batch = db.batch(); inBatch = 0; }
    }
    if (inBatch > 0 && !dryRun) await batch.commit();

    logger.info("Import zákazníků z Dotykačky", { dryRun, ...result });
    return { dryRun, ...result };
  }
);

// ════════════════════════════════════════════════════════════════════
// 2) Diagnostika — ověří token, cloud a pobočku a zda pokladna odpovídá
// ════════════════════════════════════════════════════════════════════
exports.dotykackaStatus = onCall({ secrets: [REFRESH_TOKEN] }, async (request) => {
  await requireAdmin(request);
  const out = { token: false, branch: null, pos: null };
  try {
    await getAccessToken(true);
    out.token = true;
    const branch = await api(`/branches/${BRANCH_ID.value().trim()}`);
    out.branch = branch && branch.name;
    const hello = await posAction({ action: "order/hello" });
    out.pos = { registerStatus: hello.registerStatus, version: hello.version && hello.version.name };
  } catch (e) {
    out.error = e.message;
  }
  return out;
});

// ════════════════════════════════════════════════════════════════════
// 3) Každých 5 minut: zaplacené účty se zákazníkem → body
//    Každý účet se započítá jen jednou (kolekce dotykacka_orders).
// ════════════════════════════════════════════════════════════════════
exports.syncDotykackaOrders = onSchedule(
  { schedule: "every 5 minutes", timeZone: "Europe/Prague", secrets: [REFRESH_TOKEN], timeoutSeconds: 300 },
  async () => {
    const stateRef = db.doc("integration/dotykacka");
    const state = await stateRef.get();
    const startedAt = state.exists && state.get("syncStartedAt");

    // První běh jen zapíše startovní čas — staré nákupy se zpětně nepočítají
    if (!startedAt) {
      await stateRef.set({ syncStartedAt: Timestamp.now() }, { merge: true });
      logger.info("Synchronizace Dotykačky zapnuta, body se počítají od teď.");
      return;
    }
    const since = startedAt.toMillis();

    await syncPendingCustomers(stateRef, state.get("customersBackfilled") === true);

    const filter = encodeURIComponent(
      `_branchId|eq|${BRANCH_ID.value().trim()};_customerId|eq|notnull;paid|eq|true`
    );
    const json = await api(`/orders?filter=${filter}&sort=-created&limit=100`, { allow404: true });
    const orders = listData(json);

    let awarded = 0;
    for (const order of orders) {
      if (toMillis(order.created) < since) continue;
      const flags = Number(order.flags || 0);
      const canceled = order.canceledDate || (flags & 0b110) !== 0; // CANCELED_FULL | CANCELLATION
      const total = Number(order.totalValueRounded || 0);
      if (canceled || total <= 0) continue;

      try {
        if (await awardOrder(order, total)) awarded++;
      } catch (e) {
        logger.error("Chyba při připisování bodů", { orderId: order.id, error: e.message });
      }
    }

    await stateRef.set({ lastSyncAt: Timestamp.now(), lastSyncAwarded: awarded }, { merge: true });
    if (awarded) logger.info(`Připsány body za ${awarded} účtů.`);
  }
);

/**
 * Zákazníci, kteří ještě nemají v Dotykačce členský kód.
 * Poprvé projde všechny existující uživatele, pak jen ty s dotykackaSynced == false.
 */
async function syncPendingCustomers(stateRef, backfilled) {
  const LIMIT = 100;
  let ids;
  if (!backfilled) {
    const all = await db.collection("users").get();
    ids = all.docs.filter((d) => d.get("dotykackaSynced") !== true).map((d) => d.id);
  } else {
    const pending = await db.collection("users").where("dotykackaSynced", "==", false).limit(LIMIT).get();
    ids = pending.docs.map((d) => d.id);
  }

  let ok = 0;
  for (const id of ids.slice(0, LIMIT)) {
    if (await syncUserSafe(id)) ok++;
  }
  if (!backfilled && ids.length <= LIMIT) {
    await stateRef.set({ customersBackfilled: true }, { merge: true });
  }
  if (ids.length) logger.info(`Zákazníci s Dotykačkou: ${ok}/${Math.min(ids.length, LIMIT)} synchronizováno.`);
}

async function awardOrder(order, total) {
  const orderRef = db.collection("dotykacka_orders").doc(String(order.id));
  const usersQuery = db.collection("users").where("dotykackaId", "==", String(order._customerId)).limit(1);
  const points = Math.floor(total / KC_PER_POINT.value());

  return db.runTransaction(async (tx) => {
    const processed = await tx.get(orderRef);
    if (processed.exists) return false;

    const users = await tx.get(usersQuery);
    const base = {
      dotykackaCustomerId: String(order._customerId),
      total,
      documentNumber: order.documentNumber || null,
      processedAt: FieldValue.serverTimestamp(),
    };

    if (users.empty) {
      // Zákazník z Dotykačky, který nemá účet v appce — jen si poznamenáme
      tx.set(orderRef, { ...base, status: "no_app_user", points: 0 });
      return false;
    }

    const userRef = users.docs[0].ref;
    tx.set(orderRef, { ...base, status: "awarded", userId: userRef.id, points });
    if (points <= 0) return false;

    tx.update(userRef, {
      points: FieldValue.increment(points),
      totalPoints: FieldValue.increment(points),
    });
    const txRef = userRef.collection("transactions").doc();
    tx.set(txRef, {
      id: txRef.id,
      type: "ADD",
      amount: points,
      reason: `Nákup ${Math.round(total)} Kč`,
      createdAt: FieldValue.serverTimestamp(),
    });
    return true;
  });
}

// ════════════════════════════════════════════════════════════════════
// 4) Nový zákazník v appce → hned členský kód + zákazník v Dotykačce
// ════════════════════════════════════════════════════════════════════
exports.onUserCreated = onDocumentCreated(
  { document: "users/{userId}", secrets: [REFRESH_TOKEN] },
  async (event) => {
    const userId = event.params.userId;
    const ref = db.collection("users").doc(userId);
    // Kód dostane hned, i kdyby Dotykačka zrovna nešla (dosynchronizuje scheduler)
    if (!event.data.get("memberCode")) {
      await ref.update({ memberCode: await generateMemberCode(), dotykackaSynced: false });
    }
    if (event.data.get("dotykackaSynced") === true) return; // import se stávající kartou
    await syncUserSafe(userId);
  }
);

// ════════════════════════════════════════════════════════════════════
// 5) Push notifikace při každém pohybu bodů
//    (nákup přes Dotykačku i ruční úprava obsluhou)
// ════════════════════════════════════════════════════════════════════
exports.notifyOnTransaction = onDocumentCreated("users/{userId}/transactions/{txId}", async (event) => {
  const tx = event.data && event.data.data();
  if (!tx || !tx.amount) return;

  const userRef = db.collection("users").doc(event.params.userId);
  const user = await userRef.get();
  const token = user.exists && user.get("fcmToken");
  if (!token) return;

  const isAdd = tx.type === "ADD";
  const balance = user.get("points");
  const title = isAdd ? `+${tx.amount} bodů 🌿` : `−${tx.amount} bodů`;
  const body = `${tx.reason || "Změna bodů"} · zůstatek ${balance} b`;

  try {
    await getMessaging().send({
      token,
      notification: { title, body },
      data: { title, body },
      android: {
        priority: "high",
        notification: { channelId: "canna_points", icon: "ic_notification", color: "#4A6741" },
      },
    });
  } catch (e) {
    const code = e.errorInfo && e.errorInfo.code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
      await userRef.update({ fcmToken: "" });
    } else {
      logger.error("Push se nepodařilo odeslat", e);
    }
  }
});
