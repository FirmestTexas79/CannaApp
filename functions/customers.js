/**
 * CannaApp — účty zákazníků přes server
 *
 * Zákazník se v appce přihlásí anonymně do Firebase Auth (neviditelné, bez hesla).
 * Ověření e-mailem + telefonem dělá server a anonymní účet "přiváže" k zákazníkovi
 * (users/{id}.authUids). Pravidla Firestore pak pustí zákazníka jen k jeho vlastním datům.
 *
 *  - customerLogin    (callable) → { userId }
 *  - customerRegister (callable) → { userId }
 *  - deleteMyAccount  (callable) → smaže zákazníka, který je přihlášený v appce
 *  - webDeleteAccount (callable, z webu) → smazání účtu bez appky (e-mail + telefon)
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");

const db = () => getFirestore();
const users = () => db().collection("users");

// ── Porovnání údajů (stejné jako dřív v appce) ───────────────────────
const phoneKey = (p) => {
  const d = String(p || "").replace(/\D/g, "").slice(-9);
  return d.length === 9 ? d : "";
};
const nameKey = (n) =>
  String(n || "").normalize("NFD").replace(/\p{Mn}+/gu, "").toLowerCase()
    .split(/\s+/).filter(Boolean).sort().join(" ");
const normEmail = (e) => String(e || "").trim().toLowerCase();

/**
 * Najde zákazníka: e-mail + telefon, nebo e-mail + jméno u převzatých
 * zákazníků bez telefonu (ti dostanou zadaný telefon uložený).
 */
async function findCustomer(email, phone, name, { allowNameOnly = true } = {}) {
  const snap = await users().where("email", "==", normEmail(email)).limit(5).get();
  if (snap.empty) return null;
  const p = phoneKey(phone);
  const byPhone = p && snap.docs.find((d) => phoneKey(d.get("phone")) === p);
  if (byPhone) return { doc: byPhone, setPhone: false };
  if (!allowNameOnly) return null;
  const n = nameKey(name);
  const byName = n && snap.docs.find((d) => !phoneKey(d.get("phone")) && nameKey(d.get("name")) === n);
  return byName ? { doc: byName, setPhone: Boolean(p) } : null;
}

function requireAuth(request) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Chybí přihlášení zařízení, restartuj appku.");
  return request.auth.uid;
}

// ════════════════════════════════════════════════════════════════════
exports.customerLogin = onCall(async (request) => {
  const uid = requireAuth(request);
  const { email, phone, name } = request.data || {};
  if (!email) throw new HttpsError("invalid-argument", "Vyplň e-mail.");

  const found = await findCustomer(email, phone, name);
  if (!found) throw new HttpsError("not-found", "Zákazník nenalezen");

  const update = { authUids: FieldValue.arrayUnion(uid) };
  if (found.setPhone) update.phone = String(phone).trim();
  await found.doc.ref.update(update);
  return { userId: found.doc.id };
});

// ════════════════════════════════════════════════════════════════════
exports.customerRegister = onCall(async (request) => {
  const uid = requireAuth(request);
  const { email, phone, name, consent } = request.data || {};
  const cleanName = String(name || "").trim().replace(/\s+/g, " ");
  const e = normEmail(email);

  if (cleanName.split(" ").length < 2) throw new HttpsError("invalid-argument", "Vyplň jméno i příjmení");
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e)) throw new HttpsError("invalid-argument", "Neplatný e-mail");
  if (!phoneKey(phone)) throw new HttpsError("invalid-argument", "Neplatné telefonní číslo");
  if (consent !== true) throw new HttpsError("invalid-argument", "Chybí souhlas se zpracováním údajů");

  const existing = await users().where("email", "==", e).limit(1).get();
  if (!existing.empty) throw new HttpsError("already-exists", "Tento e-mail už je registrovaný. Přihlas se.");

  const ref = users().doc();
  await ref.set({
    name: cleanName,
    email: e,
    phone: String(phone).trim(),
    points: 0,
    totalPoints: 0,
    dotykackaId: "",
    fcmToken: "",
    memberCode: "", // doplní onUserCreated
    selfRegistered: true,
    consentAt: Timestamp.now(),
    createdAt: Timestamp.now(),
    authUids: [uid],
  });
  return { userId: ref.id };
});

// ════════════════════════════════════════════════════════════════════
// Smazání účtu (požadavek Google Play: v appce i přes web)
// Smaže zákazníka i historii bodů. Záznam zákazníka v Dotykačce zůstává
// (účetní doklady prodejny), jen se z něj odebere členský kód.
// ════════════════════════════════════════════════════════════════════
async function deleteCustomer(ref, reason) {
  const snap = await ref.get();
  await db().recursiveDelete(ref);
  logger.info("Účet zákazníka smazán", { userId: ref.id, reason, dotykackaId: snap.get("dotykackaId") || null });
}

exports.deleteMyAccount = onCall(async (request) => {
  const uid = requireAuth(request);
  const snap = await users().where("authUids", "array-contains", uid).limit(1).get();
  if (snap.empty) throw new HttpsError("not-found", "Účet nenalezen.");
  await deleteCustomer(snap.docs[0].ref, "app");
  return { ok: true };
});

exports.webDeleteAccount = onCall(async (request) => {
  const { email, phone } = request.data || {};
  if (!email || !phoneKey(phone)) throw new HttpsError("invalid-argument", "Vyplň e-mail i telefon.");
  // Na webu jen e-mail + telefon (bez varianty se jménem)
  const found = await findCustomer(email, phone, null, { allowNameOnly: false });
  if (!found) throw new HttpsError("not-found", "Účet s tímto e-mailem a telefonem jsme nenašli.");
  await deleteCustomer(found.doc.ref, "web");
  return { ok: true };
});

// Pro test: node -e "require('./customers').selfTest()"
exports.selfTest = () => {
  const assert = require("node:assert");
  assert.strictEqual(phoneKey("+420 777 123 456"), "777123456");
  assert.strictEqual(phoneKey("123"), "");
  assert.strictEqual(nameKey("Nováková  Jana"), nameKey("jana novakova"));
  console.log("customers.js OK");
};
