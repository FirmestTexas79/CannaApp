/**
 * CannaApp — členská kartička v Google Peněžence (Google Wallet)
 *
 *  - walletPassJwt    (callable)  → založí/aktualizuje kartičku zákazníka a vrátí odkaz "Přidat do Peněženky"
 *  - syncWalletPass   (Firestore) → když se změní body / rank / jméno / kód, kartička v Peněžence se sama přepíše
 *  - walletAsset      (HTTP)      → veřejně dostupné obrázky kartičky (logo, banner) — Google si je stahuje
 *
 * Nastavení: DOTYKACKA_NAVOD.md → sekce "Google Peněženka".
 *   functions/.env         WALLET_ISSUER_ID=<číslo vydavatele z Google Pay & Wallet Console>
 *   Secret Manager         WALLET_SA_KEY = celý JSON klíč servisního účtu
 *
 * Bez knihoven navíc: přihlášení servisním účtem i podpis JWT dělá node:crypto.
 */

const path = require("node:path");
const fs = require("node:fs");
const crypto = require("node:crypto");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { defineSecret, defineString } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const { getFirestore } = require("firebase-admin/firestore");

const WALLET_SA_KEY = defineSecret("WALLET_SA_KEY");
const WALLET_ISSUER_ID = defineString("WALLET_ISSUER_ID", { default: "" });

const WALLET_API = "https://walletobjects.googleapis.com/walletobjects/v1";
const CLASS_SUFFIX = "cannaclub_member";
// Zvýšit, když se změní vzhled třídy (barva, obrázky, odkazy) → při dalším volání se přepíše
const CLASS_VERSION = 2;

const BRAND_GREEN = "#3E5E35";
const WOLT_URL = "https://wolt.com/cs/cze/hradec-kralove/venue/cannaclub-cannabis-cbd";

// Ranky a jejich popis jsou společné s výpočtem bodů
const { rankInfo, getConfig } = require("./loyalty");

const db = () => getFirestore();
const projectId = () => process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT;
const assetUrl = (file) => `https://us-central1-${projectId()}.cloudfunctions.net/walletAsset/${file}`;
const formatCode = (code) => String(code).replace(/(\d{4})(?=\d)/g, "$1 ");

// ── Servisní účet → access token + podpis ───────────────────────────
function serviceAccount() {
  const raw = WALLET_SA_KEY.value();
  if (!raw || !raw.trim().startsWith("{")) {
    throw new HttpsError("failed-precondition", "Google Peněženka ještě není nastavená (chybí klíč servisního účtu).");
  }
  return JSON.parse(raw);
}

function issuerId() {
  const id = (WALLET_ISSUER_ID.value() || "").trim();
  if (!id) throw new HttpsError("failed-precondition", "Google Peněženka ještě není nastavená (chybí WALLET_ISSUER_ID).");
  return id;
}

const b64url = (buf) => Buffer.from(buf).toString("base64").replace(/=+$/, "").replace(/\+/g, "-").replace(/\//g, "_");

function signJwt(payload, sa) {
  const header = { alg: "RS256", typ: "JWT", kid: sa.private_key_id };
  const unsigned = `${b64url(JSON.stringify(header))}.${b64url(JSON.stringify(payload))}`;
  const signature = crypto.createSign("RSA-SHA256").update(unsigned).sign(sa.private_key);
  return `${unsigned}.${b64url(signature)}`;
}

let tokenCache = { token: null, until: 0 };

async function accessToken(sa) {
  if (tokenCache.token && Date.now() < tokenCache.until) return tokenCache.token;
  const now = Math.floor(Date.now() / 1000);
  const assertion = signJwt({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/wallet_object.issuer",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }, sa);
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(`Google odmítl přihlášení servisního účtu: ${JSON.stringify(json).slice(0, 300)}`);
  tokenCache = { token: json.access_token, until: Date.now() + (json.expires_in - 120) * 1000 };
  return tokenCache.token;
}

async function walletApi(sa, method, route, body) {
  const res = await fetch(`${WALLET_API}${route}`, {
    method,
    headers: { Authorization: `Bearer ${await accessToken(sa)}`, "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  if (res.status === 404) return null;
  if (!res.ok) {
    const hint = res.status === 403
      ? " (servisní účet nemá v Google Pay & Wallet Console práva, nebo není zapnuté Google Wallet API)"
      : "";
    throw new Error(`Google Wallet ${res.status} na ${route}${hint}: ${text.slice(0, 400)}`);
  }
  return text ? JSON.parse(text) : {};
}

// ── Třída (vzhled kartičky, společný pro všechny) ───────────────────
const lang = (value) => ({ defaultValue: { language: "cs-CZ", value } });

function classBody(issuer) {
  return {
    id: `${issuer}.${CLASS_SUFFIX}`,
    issuerName: "CannaClub",
    programName: "Věrnostní program",
    programLogo: { sourceUri: { uri: assetUrl("logo.png") }, contentDescription: lang("CannaClub") },
    wideProgramLogo: { sourceUri: { uri: assetUrl("wide-logo.png") }, contentDescription: lang("CannaClub") },
    heroImage: { sourceUri: { uri: assetUrl("hero.png") }, contentDescription: lang("CannaClub Hradec Králové") },
    hexBackgroundColor: BRAND_GREEN,
    countryCode: "CZ",
    reviewStatus: "UNDER_REVIEW",
    multipleDevicesAndHoldersAllowedStatus: "ONE_USER_ALL_DEVICES",
    linksModuleData: {
      uris: [{ id: "wolt", uri: WOLT_URL, description: "Rozvoz přes Wolt" }],
    },
  };
}

async function ensureClass(sa, issuer) {
  const metaRef = db().collection("integration").doc("wallet");
  const meta = await metaRef.get();
  if (meta.exists && meta.get("classVersion") === CLASS_VERSION && meta.get("issuerId") === issuer) return;

  const id = `${issuer}.${CLASS_SUFFIX}`;
  const existing = await walletApi(sa, "GET", `/loyaltyClass/${id}`);
  if (existing) await walletApi(sa, "PUT", `/loyaltyClass/${id}`, classBody(issuer));
  else await walletApi(sa, "POST", "/loyaltyClass", classBody(issuer));

  await metaRef.set({ classVersion: CLASS_VERSION, issuerId: issuer }, { merge: true });
  logger.info("Třída kartičky v Google Peněžence připravena", { id, version: CLASS_VERSION });
}

// ── Objekt (kartička jednoho zákazníka) ─────────────────────────────
const objectId = (issuer, memberCode) => `${issuer}.m${memberCode}`;

function objectBody(issuer, user) {
  const points = Number(user.points || 0);
  const rank = rankInfo(Number(user.totalPoints || 0));
  return {
    id: objectId(issuer, user.memberCode),
    classId: `${issuer}.${CLASS_SUFFIX}`,
    state: "ACTIVE",
    accountId: formatCode(user.memberCode),
    accountName: user.name || "Člen klubu",
    barcode: { type: "CODE_128", value: String(user.memberCode), alternateText: formatCode(user.memberCode) },
    loyaltyPoints: { label: "Body", balance: { int: points } },
    textModulesData: [
      { id: "rank", header: `Rank ${rank.label}`, body: rank.detail },
      { id: "how", header: "Jak uplatnit", body: "Ukaž kartičku u pokladny. Body vyměníš za odměny z nabídky prodejny." },
    ],
  };
}

async function upsertObject(sa, issuer, user) {
  const body = objectBody(issuer, user);
  const existing = await walletApi(sa, "GET", `/loyaltyObject/${body.id}`);
  if (existing) await walletApi(sa, "PUT", `/loyaltyObject/${body.id}`, body);
  else await walletApi(sa, "POST", "/loyaltyObject", body);
  return body.id;
}

// ════════════════════════════════════════════════════════════════════
// Zákazník klepne na "Přidat do Peněženky Google"
// ════════════════════════════════════════════════════════════════════
exports.walletPassJwt = onCall({ secrets: [WALLET_SA_KEY] }, async (request) => {
  const { userId, memberCode } = request.data || {};
  if (!userId || !memberCode) throw new HttpsError("invalid-argument", "Chybí zákazník.");

  const ref = db().collection("users").doc(String(userId));
  const snap = await ref.get();
  // Kartičku dostane jen zákazník přihlášený v appce na tomhle zařízení
  const linked = (snap.get("authUids") || []).includes(request.auth && request.auth.uid);
  if (!snap.exists || !linked || snap.get("memberCode") !== memberCode) {
    throw new HttpsError("permission-denied", "Kartičku se nepodařilo ověřit.");
  }
  if (snap.get("dotykackaSynced") !== true) {
    throw new HttpsError("failed-precondition", "Kartička se ještě připravuje, zkus to za pár minut.");
  }

  const sa = serviceAccount();
  const issuer = issuerId();
  try {
    await ensureClass(sa, issuer);
    await getConfig();   // aktuální hranice ranků
    const id = await upsertObject(sa, issuer, { ...snap.data(), id: snap.id });
    await ref.update({ walletObjectId: id });

    const jwt = signJwt({
      iss: sa.client_email,
      aud: "google",
      typ: "savetowallet",
      iat: Math.floor(Date.now() / 1000),
      origins: [],
      payload: { loyaltyObjects: [{ id }] },
    }, sa);
    return { jwt, saveUrl: `https://pay.google.com/gp/v/save/${jwt}` };
  } catch (e) {
    if (e instanceof HttpsError) throw e;
    logger.error("Kartičku do Peněženky se nepodařilo připravit", { userId, error: e.message });
    throw new HttpsError("internal", "Kartičku do Peněženky se nepodařilo připravit, zkus to později.");
  }
});

// ════════════════════════════════════════════════════════════════════
// Změna bodů / ranku / jména / kódu → přepsat kartičku v Peněžence
// ════════════════════════════════════════════════════════════════════
exports.syncWalletPass = onDocumentUpdated({ document: "users/{userId}", secrets: [WALLET_SA_KEY] }, async (event) => {
  const before = event.data.before.data() || {};
  const after = event.data.after.data() || {};
  if (!after.walletObjectId || !after.memberCode) return;

  const changed = ["points", "totalPoints", "name", "memberCode"].some((k) => before[k] !== after[k]);
  if (!changed) return;
  if (!(WALLET_ISSUER_ID.value() || "").trim()) return;   // Peněženka ještě není nastavená

  try {
    const sa = serviceAccount();
    const issuer = issuerId();
    // Nový kód (převzatý z Dotykačky) = nový objekt; starý zůstane, dokud si ho zákazník neodebere
    await getConfig();   // aktuální hranice ranků
    const id = await upsertObject(sa, issuer, after);
    if (id !== after.walletObjectId) await event.data.after.ref.update({ walletObjectId: id });
  } catch (e) {
    logger.error("Kartičku v Peněžence se nepodařilo aktualizovat", { userId: event.params.userId, error: e.message });
  }
});

// ════════════════════════════════════════════════════════════════════
// Obrázky kartičky (musí být veřejně na HTTPS, Google si je stahuje sám)
// ════════════════════════════════════════════════════════════════════
const ASSETS = new Set(["logo.png", "wide-logo.png", "hero.png"]);

exports.walletAsset = onRequest({ memory: "128MiB", maxInstances: 2 }, (req, res) => {
  const file = path.basename(req.path || "");
  if (!ASSETS.has(file)) {
    res.status(404).send("Not found");
    return;
  }
  res.set("Cache-Control", "public, max-age=86400");
  res.type("png").send(fs.readFileSync(path.join(__dirname, "wallet-assets", file)));
});
