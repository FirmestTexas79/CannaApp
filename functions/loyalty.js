/**
 * CannaApp — pravidla věrnostního programu, která obsluha mění z adminu
 *
 *  - bonus za rank      → vyšší rank = víc bodů za každý nákup, PEVNÝ a automatický (RANK_BONUS níže)
 *  - akce "více bodů"   → obsluha ji spustí z adminu na omezenou dobu: 2× (3×) body za nákup
 *  - hromadné zprávy    → push notifikace všem zákazníkům nebo jen od určitého ranku
 *
 * Nastavení je ve Firestore v dokumentu config/loyalty (zapisuje jen server,
 * appka ho čte a ukazuje zákazníkům: banner akce, výhody ranků).
 *
 *  - adminUpdateLoyalty (callable, jen obsluha) → spuštění či ukončení akce
 *  - adminBroadcast     (callable, jen obsluha) → hromadná notifikace (dryRun = jen spočítat příjemce)
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

const db = () => getFirestore();

// Ranky — klíče musí sedět s MemberRank v appce (model/User.kt).
// Hranice jsou výchozí; obsluha je mění v adminu → config/rewards.rankFrom (načte getConfig).
const DEFAULT_RANK_FROM = { BRONZOVY: 500, STRIBRNY: 1500, ZLATY: 4000, RODINA: 10000 };
const RANKS = [
  { key: "ZAKAZNIK", label: "Zákazník", from: 0 },
  { key: "BRONZOVY", label: "Bronzový", from: 500 },
  { key: "STRIBRNY", label: "Stříbrný", from: 1500 },
  { key: "ZLATY", label: "Zlatý", from: 4000 },
  { key: "RODINA", label: "Rodina", from: 10000 },
];

/** Hranice z configu (musí vzestupně stoupat, jinak výchozí). */
function applyRankScale(rankFrom) {
  const want = RANKS.map((r) => (r.key === "ZAKAZNIK" ? 0 : Math.round(Number(rankFrom && rankFrom[r.key]) || DEFAULT_RANK_FROM[r.key])));
  const ok = want.every((v, i) => i === 0 || v > want[i - 1]);
  RANKS.forEach((r, i) => { r.from = ok ? want[i] : (i ? DEFAULT_RANK_FROM[r.key] : 0); });
}

// Bonus za rank v % — musí sedět s LoyaltyConfig.DEFAULT_RANK_BONUS v appce
const RANK_BONUS = { ZAKAZNIK: 0, BRONZOVY: 5, STRIBRNY: 10, ZLATY: 15, RODINA: 20 };
const MAX_MULTIPLIER = 5;
const MAX_PROMO_DAYS = 31;

function rankIndex(totalPoints) {
  let i = 0;
  while (i + 1 < RANKS.length && totalPoints >= RANKS[i + 1].from) i++;
  return i;
}

function rankInfo(totalPoints) {
  const i = rankIndex(totalPoints);
  const next = RANKS[i + 1];
  return {
    key: RANKS[i].key,
    label: RANKS[i].label,
    detail: next ? `Do ranku ${next.label} chybí ${next.from - totalPoints} b` : "Nejvyšší rank, patříš do rodiny",
  };
}

// ── Nastavení (krátce cachované, scheduler běží každých 5 min) ───────
const configRef = () => db().collection("config").doc("loyalty");
let cache = { value: null, until: 0 };

async function getConfig() {
  if (cache.value && Date.now() < cache.until) return cache.value;
  const [snap, rewards] = await Promise.all([configRef().get(), db().collection("config").doc("rewards").get()]);
  const d = snap.exists ? snap.data() : {};
  applyRankScale(rewards.exists ? rewards.get("rankFrom") : null);
  const value = {
    rankBonus: RANK_BONUS,
    promo: d.promo || null, // { multiplier, label, startsAt, endsAt }
  };
  cache = { value, until: Date.now() + 60_000 };
  return value;
}

/** Je akce aktivní v daný okamžik (čas vytvoření účtu na pokladně)? */
function promoAt(config, millis) {
  const p = config.promo;
  if (!p || !p.endsAt || Number(p.multiplier) <= 1) return null;
  const start = p.startsAt ? p.startsAt.toMillis() : 0;
  if (millis < start || millis > p.endsAt.toMillis()) return null;
  return { multiplier: Number(p.multiplier), label: p.label || `${p.multiplier}× body` };
}

/**
 * Body za nákup:  základ = Kč / KC_PER_POINT
 *                 akce   = základ × (násobek − 1)
 *                 rank   = (základ + akce) × bonus ranku %
 */
function computePoints({ total, kcPerPoint, totalPointsBefore, config, orderMillis }) {
  const base = Math.floor(total / kcPerPoint);
  const promo = promoAt(config, orderMillis);
  const promoBonus = promo ? base * (promo.multiplier - 1) : 0;
  const rank = rankInfo(totalPointsBefore);
  const pct = Number(config.rankBonus[rank.key] || 0);
  const rankBonus = Math.floor(((base + promoBonus) * pct) / 100);
  return {
    points: base + promoBonus + rankBonus,
    base,
    promoBonus,
    promoLabel: promo ? promo.label : null,
    rankBonus,
    rankPct: pct,
    rankLabel: rank.label,
  };
}

// ── Obsluha: kontrola přihlášení ─────────────────────────────────────
async function requireAdmin(request) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Nejsi přihlášený jako obsluha.");
  const adminDoc = await db().collection("admins").doc(request.auth.uid).get();
  if (!adminDoc.exists) throw new HttpsError("permission-denied", "Tento účet není obsluha.");
}

// ════════════════════════════════════════════════════════════════════
// Akce "více bodů"
//   { promo: { multiplier: 2, hours: 24 | endsAtMillis, label, notify } }
//   { promo: null }  → ukončit akci
// ════════════════════════════════════════════════════════════════════
exports.adminUpdateLoyalty = onCall(async (request) => {
  await requireAdmin(request);
  const data = request.data || {};
  const update = { updatedAt: FieldValue.serverTimestamp() };
  let notify = null;

  if (data.promo === null) {
    update.promo = null;
  } else if (data.promo && typeof data.promo === "object") {
    const m = Math.round(Number(data.promo.multiplier || 2));
    if (m < 2 || m > MAX_MULTIPLIER) throw new HttpsError("invalid-argument", `Násobek musí být 2–${MAX_MULTIPLIER}×.`);
    const endsAt = Number(data.promo.endsAtMillis);
    if (!Number.isFinite(endsAt) || endsAt <= Date.now() || endsAt > Date.now() + MAX_PROMO_DAYS * 86_400_000) {
      throw new HttpsError("invalid-argument", "Konec akce musí být v budoucnu (nejdéle za měsíc).");
    }
    const label = String(data.promo.label || `${m}× body`).slice(0, 40);
    update.promo = {
      multiplier: m,
      label,
      startsAt: Timestamp.now(),
      endsAt: Timestamp.fromMillis(endsAt),
    };
    if (data.promo.notify) {
      notify = {
        title: `${label} 🌿`,
        body: `Teď za každý nákup ${m}× víc bodů. Akce platí do: ${formatEnd(endsAt)}.`,
      };
    }
  }

  await configRef().set(update, { merge: true });
  cache = { value: null, until: 0 };
  logger.info("Nastavení věrnostního programu změněno", { by: request.auth.uid, update: JSON.stringify(update) });

  let sent = 0;
  if (notify) sent = (await broadcast(notify.title, notify.body, 0)).sent;
  return { ok: true, notified: sent };
});

function formatEnd(millis) {
  const d = new Date(millis);
  const fmt = new Intl.DateTimeFormat("cs-CZ", {
    timeZone: "Europe/Prague", weekday: "long", day: "numeric", month: "numeric", hour: "2-digit", minute: "2-digit",
  });
  return fmt.format(d);
}

// ════════════════════════════════════════════════════════════════════
// Hromadná notifikace
//   { title, body, minRank: 0..4, dryRun }  → { recipients, sent }
// ════════════════════════════════════════════════════════════════════
async function recipients(minRank) {
  await getConfig();
  const from = RANKS[Math.max(0, Math.min(RANKS.length - 1, minRank || 0))].from;
  const col = db().collection("users");
  // Všichni = bez filtru (i starší účty bez pole totalPoints)
  const snap = from > 0 ? await col.where("totalPoints", ">=", from).get() : await col.get();
  return snap.docs.filter((d) => d.get("fcmToken"));
}

async function broadcast(title, body, minRank) {
  const docs = await recipients(minRank);
  let sent = 0;
  for (let i = 0; i < docs.length; i += 500) {
    const chunk = docs.slice(i, i + 500);
    const res = await getMessaging().sendEach(chunk.map((d) => ({
      token: d.get("fcmToken"),
      notification: { title, body },
      data: { title, body, kind: "broadcast" },
      android: {
        priority: "high",
        notification: { channelId: "canna_points", icon: "ic_notification", color: "#4A6741" },
      },
    })));
    sent += res.successCount;
    // Neplatné tokeny (odinstalovaná appka) uklidit
    const dead = [];
    res.responses.forEach((r, j) => {
      const code = r.error && r.error.code;
      if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
        dead.push(chunk[j].ref.update({ fcmToken: "" }));
      }
    });
    await Promise.all(dead);
  }
  return { recipients: docs.length, sent };
}

exports.adminBroadcast = onCall({ timeoutSeconds: 120 }, async (request) => {
  await requireAdmin(request);
  const { title, body, minRank = 0, dryRun = false } = request.data || {};
  const t = String(title || "").trim().slice(0, 65);
  const b = String(body || "").trim().slice(0, 240);
  if (!dryRun && (!t || !b)) throw new HttpsError("invalid-argument", "Vyplň nadpis i text zprávy.");

  if (dryRun) return { recipients: (await recipients(Number(minRank))).length, sent: 0 };

  const result = await broadcast(t, b, Number(minRank));
  await db().collection("broadcasts").add({
    title: t, body: b, minRank: Number(minRank), ...result,
    by: request.auth.uid, createdAt: FieldValue.serverTimestamp(),
  });
  logger.info("Hromadná zpráva odeslána", result);
  return result;
});

module.exports.RANKS = RANKS;
module.exports.rankInfo = rankInfo;
module.exports.getConfig = getConfig;
module.exports.computePoints = computePoints;
