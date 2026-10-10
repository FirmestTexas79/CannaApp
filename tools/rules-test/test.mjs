// Test pravidel Firestore v emulátoru. Spuštění (z této složky):
//   npm install
//   npm test
import { readFileSync } from "node:fs";
import { initializeTestEnvironment, assertSucceeds, assertFails } from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, updateDoc, getDocs, collection, query, where } from "firebase/firestore";

const env = await initializeTestEnvironment({
  projectId: "demo-cannaapp",
  firestore: { rules: readFileSync("../../firestore.rules", "utf8"), host: "127.0.0.1", port: 8085 },
});

await env.withSecurityRulesDisabled(async (ctx) => {
  const db = ctx.firestore();
  await setDoc(doc(db, "admins/adm"), { email: "obsluha@x.cz" });
  await setDoc(doc(db, "users/u1"), { name: "Jana", email: "jana@x.cz", points: 10, authUids: ["anonA"] });
  await setDoc(doc(db, "users/u2"), { name: "Petr", email: "petr@x.cz", points: 5, authUids: ["anonB"] });
  await setDoc(doc(db, "users/u1/transactions/t1"), { amount: 10 });
  await setDoc(doc(db, "users/u2/transactions/t1"), { amount: 5 });
  await setDoc(doc(db, "products/p1"), { name: "Lemon Haze" });
  await setDoc(doc(db, "config/loyalty"), { promo: null });
});

const nobody = env.unauthenticatedContext().firestore();
const jana = env.authenticatedContext("anonA").firestore();
const admin = env.authenticatedContext("adm").firestore();

const checks = [
  ["cizí bez přihlášení nečte zákazníka", assertFails(getDoc(doc(nobody, "users/u1")))],
  ["nikdo nestáhne seznam zákazníků", assertFails(getDocs(collection(nobody, "users")))],
  ["dohledání podle e-mailu nejde", assertFails(getDocs(query(collection(jana, "users"), where("email", "==", "petr@x.cz"))))],
  ["zákazník čte svůj účet", assertSucceeds(getDoc(doc(jana, "users/u1")))],
  ["zákazník nečte cizí účet", assertFails(getDoc(doc(jana, "users/u2")))],
  ["zákazník smí uložit FCM token", assertSucceeds(updateDoc(doc(jana, "users/u1"), { fcmToken: "t" }))],
  ["zákazník si nepřipíše body", assertFails(updateDoc(doc(jana, "users/u1"), { points: 9999 }))],
  ["zákazník nezaloží účet", assertFails(setDoc(doc(jana, "users/u3"), { name: "x", authUids: ["anonA"] }))],
  ["zákazník čte své pohyby", assertSucceeds(getDoc(doc(jana, "users/u1/transactions/t1")))],
  ["zákazník nečte cizí pohyby", assertFails(getDoc(doc(jana, "users/u2/transactions/t1")))],
  ["zákazník nepíše pohyby", assertFails(setDoc(doc(jana, "users/u1/transactions/t2"), { amount: 1 }))],
  ["obsluha čte všechny", assertSucceeds(getDocs(collection(admin, "users")))],
  ["obsluha upraví ceník odměn", assertSucceeds(setDoc(doc(admin, "config/rewards"), { items: [] }))],
  ["zákazník neupraví ceník odměn", assertFails(setDoc(doc(jana, "config/rewards"), { items: [] }))],
  ["obsluha nezmění akci (jen server)", assertFails(setDoc(doc(admin, "config/loyalty"), { promo: null }))],
  ["zákazník nečte žádosti o smazání", assertFails(getDocs(collection(jana, "deletionRequests")))],
  ["obsluha čte žádosti o smazání", assertSucceeds(getDocs(collection(admin, "deletionRequests")))],
  ["obsluha mění body", assertSucceeds(updateDoc(doc(admin, "users/u2"), { points: 7 }))],
  ["produkty čte kdokoli", assertSucceeds(getDoc(doc(nobody, "products/p1")))],
  ["produkty nemění zákazník", assertFails(setDoc(doc(jana, "products/p1"), { name: "x" }))],
  ["akci čte kdokoli", assertSucceeds(getDoc(doc(nobody, "config/loyalty")))],
  ["akci nezapíše ani obsluha (jen server)", assertFails(setDoc(doc(admin, "config/loyalty"), { promo: 1 }))],
  ["admins nejde přepsat", assertFails(setDoc(doc(jana, "admins/anonA"), {}))],
];

let failed = 0;
for (const [name, p] of checks) {
  try { await p; console.log("OK  ", name); } catch (e) { failed++; console.log("CHYBA", name, "-", e.message.split("\n")[0]); }
}
await env.cleanup();
console.log(failed ? `\n${failed} testů selhalo` : "\nVšechna pravidla sedí");
process.exit(failed ? 1 : 0);
