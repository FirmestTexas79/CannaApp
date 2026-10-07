// Firebase pro web — stejný projekt jako Android appka.
// Zákaznická část a admin běží na stejné doméně, proto má každá vlastní
// pojmenovanou instanci: přihlášení obsluhy nepřepíše přihlášení zákazníka.
import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getFunctions, httpsCallable } from "firebase/functions";

const CONFIG = {
  apiKey: "AIzaSyAoLXr4BmtkFG9eLihC6k0pxSRzwPmvZIU",
  authDomain: "cannaapp-e4e8b.firebaseapp.com",
  projectId: "cannaapp-e4e8b",
  storageBucket: "cannaapp-e4e8b.firebasestorage.app",
  messagingSenderId: "348771838600",
  appId: "1:348771838600:web:e7e671d3c580cd3ee3cd3d",
};

export function initFirebase(name) {
  const app = initializeApp(CONFIG, name);
  const functions = getFunctions(app, "us-central1");
  return {
    app,
    auth: getAuth(app),
    db: getFirestore(app),
    /** Zavolá Cloud Function a vrátí data. Chyba nese českou hlášku ze serveru. */
    call: async (fn, data = {}, timeout = 70_000) => (await httpsCallable(functions, fn, { timeout })(data)).data || {},
  };
}
