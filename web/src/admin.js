// CannaClub — webový admin pro obsluhu a vedoucí (iPhone i počítač)
// Umí totéž co admin v Android appce. Oprávnění hlídají pravidla Firestore/Storage
// (kolekce admins) a serverové funkce, web jen volá to samé co appka.
import { initFirebase } from "./fb.js";
import { signInWithEmailAndPassword, onAuthStateChanged, signOut } from "firebase/auth";
import {
  doc, getDoc, getDocs, setDoc, deleteDoc, addDoc, onSnapshot, collection, query, orderBy, where, limit,
  runTransaction, Timestamp,
} from "firebase/firestore";
import { getStorage, ref, uploadBytes, getDownloadURL, deleteObject } from "firebase/storage";
import { Html5Qrcode, Html5QrcodeSupportedFormats } from "html5-qrcode";
import {
  RANKS, esc, num, chunk4, initials, toMillis, rankFor, activePromo, czechUntil, errMsg, icon, toast, sheet,
  confirmDialog, bindActions, busy, barcodeSvg, productCard, installHint, dismissHint,
} from "./common.js";

const { app, auth, db, call } = initFirebase("admin");
const storage = getStorage(app);
const root = document.getElementById("app");

const REASONS = ["Vrácení obalů", "Nákup v obchodě", "Věrnostní bonus", "Oprava chyby", "Uplatnění odměny", "Jiný důvod"];
const STEPS = [-10, -5, 5, 10, 20];
const ALL_FORMS = ["1g", "3g", "volně", "5g", "drť", "preroll"];
const AUDIENCES = ["Všem", "Bronzový+", "Stříbrný+", "Zlatý+", "Rodina"];
const DURATIONS = [
  ["TODAY", "Do konce dne"], ["H24", "24 hodin"], ["SUNDAY", "Do neděle"], ["WEEK", "7 dní"],
];
const SORTS = [["name", "A–Z"], ["points", "Body"], ["newest", "Nejnovější"]];

const A = {
  view: "loading",            // loading | login | home | loyalty | products
  users: [],
  query: "",
  sort: "name",
  config: null,
  products: [],
};
let unsubs = [];

// ════════════════════════════════════════════════════════════════════
// Přihlášení obsluhy
// ════════════════════════════════════════════════════════════════════
onAuthStateChanged(auth, async (u) => {
  if (!u || u.isAnonymous) { stop(); A.view = "login"; return render(); }
  try {
    const ok = (await getDoc(doc(db, "admins", u.uid))).exists();
    if (!ok) { await signOut(auth); toast("Tento účet není obsluha.", "error"); return; }
    start();
    if (A.view === "loading" || A.view === "login") { A.view = "home"; render(); }
  } catch {
    await signOut(auth); toast("Tento účet není obsluha.", "error");
  }
});

function start() {
  if (unsubs.length) return;
  unsubs.push(onSnapshot(query(collection(db, "users"), orderBy("name")), (snap) => {
    A.users = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    if (A.view === "home") { updateStats(); updateList(); }
  }, (e) => toast(errMsg(e, "Zákazníky se nepodařilo načíst."), "error")));
  unsubs.push(onSnapshot(doc(db, "config", "loyalty"), (snap) => {
    A.config = snap.data() || null;
    if (A.view === "loyalty") updatePromoBox();
  }, () => {}));
  unsubs.push(onSnapshot(query(collection(db, "products"), orderBy("orderIndex")), (snap) => {
    A.products = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    if (A.view === "products") updateProducts();
  }, () => {}));
}
function stop() { unsubs.forEach((u) => u()); unsubs = []; A.users = []; }

// ════════════════════════════════════════════════════════════════════
// Vykreslení
// ════════════════════════════════════════════════════════════════════
function render() {
  root.className = `screen admin-${A.view}`;
  const v = A.view;
  if (v === "loading") root.innerHTML = `<div class="center-fill"><span class="spin big light"></span></div>`;
  else if (v === "login") root.innerHTML = loginHtml();
  else if (v === "home") { root.innerHTML = homeHtml(); updateStats(); updateList(); }
  else if (v === "loyalty") { root.innerHTML = loyaltyHtml(); updatePromoBox(); countRecipients(); }
  else if (v === "products") { root.innerHTML = productsHtml(); updateProducts(); }
  window.scrollTo(0, 0);
}

function loginHtml() {
  return `<div class="auth-wrap">
    <img class="wordmark" src="/app/img/wordmark.png" alt="CannaClub">
    <div class="pill auth-card">
      <div class="eyebrow gold">Obsluha</div>
      <h1>Přihlášení</h1>
      <form id="adminLogin" novalidate>
        <label class="field"><span>E-mail</span><input name="email" type="email" autocomplete="username" inputmode="email" required></label>
        <label class="field"><span>Heslo</span><input name="password" type="password" autocomplete="current-password" required></label>
        <button class="btn gold wide" type="submit">Přihlásit se</button>
      </form>
    </div>
    ${installHint("cc_hint_a")}
  </div>`;
}

const back = (title, sub = "") => `
  <header class="admin-head"><button class="icon-btn" data-act="home" aria-label="Zpět">${icon("back")}</button>
    <div><div class="eyebrow gold">${esc(sub)}</div><h1>${esc(title)}</h1></div></header>`;

// ── Zákazníci ──
function homeHtml() {
  return `<div class="pill admin">
    <header class="admin-head">
      <div><div class="eyebrow gold">Obsluha</div><h1>Zákazníci</h1></div>
      <button class="icon-btn round" data-act="logout" aria-label="Odhlásit">${icon("logout")}</button>
    </header>
    ${installHint("cc_hint_a")}
    <div class="stats" id="stats"></div>
    <div class="nav-tiles">
      <button class="nav-tile gold" data-act="loyalty">${icon("sparkle")}<span><b>Akce a zprávy</b><small>Body a notifikace</small></span></button>
      <button class="nav-tile" data-act="products">${icon("leaf")}<span><b>Naše zeleň</b><small>Správa produktů</small></span></button>
    </div>
    <div class="search-row">
      <label class="search">${icon("search")}<input id="q" type="search" placeholder="Jméno, e-mail, telefon, kód" value="${esc(A.query)}" autocomplete="off"></label>
      <button class="scan-btn" data-act="scan" aria-label="Skenovat kartičku">${icon("scan")}</button>
    </div>
    <div class="sort-row">
      ${SORTS.map(([k, l]) => `<button class="chip ${A.sort === k ? "on" : ""}" data-act="sort" data-k="${k}">${l}</button>`).join("")}
      <span class="count" id="count"></span>
    </div>
    <div id="list" class="user-list"></div>
    <button class="btn link small center-block import-link" data-act="import">Převzít zákazníky z pokladny</button>
  </div>
  <button class="fab" data-act="addUser" aria-label="Přidat zákazníka">${icon("plus")}</button>`;
}

function updateStats() {
  const el = document.getElementById("stats"); if (!el) return;
  const weekAgo = Date.now() - 7 * 86_400_000;
  const open = A.users.reduce((s, u) => s + (u.points || 0), 0);
  const fresh = A.users.filter((u) => toMillis(u.createdAt) >= weekAgo).length;
  el.innerHTML = [
    ["users", num(A.users.length), "zákazníků"],
    ["tag", num(open), "bodů = Kč"],
    ["trend", `+${fresh}`, "za 7 dní"],
  ].map(([ic, v, l]) => `<div class="stat">${icon(ic)}<b>${v}</b><small>${l}</small></div>`).join("");
}

const fold = (s) => String(s || "").normalize("NFD").replace(/\p{Mn}/gu, "").toLowerCase();
const collator = new Intl.Collator("cs");

function filteredUsers() {
  const q = fold(A.query.trim()), qd = A.query.replace(/\s/g, "");
  let list = !q ? [...A.users] : A.users.filter((u) =>
    fold(u.name).includes(q) || fold(u.email).includes(q) ||
    (qd && String(u.phone || "").replace(/\s/g, "").includes(qd)) || (qd && String(u.memberCode || "").includes(qd)));
  if (A.sort === "name") list.sort((a, b) => collator.compare(a.name || "", b.name || ""));
  if (A.sort === "points") list.sort((a, b) => (b.points || 0) - (a.points || 0));
  if (A.sort === "newest") list.sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt));
  return list;
}

function updateList() {
  const el = document.getElementById("list"); if (!el) return;
  const list = filteredUsers();
  document.getElementById("count").textContent = list.length;
  el.innerHTML = list.slice(0, 300).map((u) => {
    const r = rankFor(u.totalPoints || 0);
    return `<button class="user-row" data-act="openUser" data-id="${esc(u.id)}">
      <span class="avatar ring" style="--rank:${r.color}">${esc(initials(u.name))}</span>
      <span class="user-text"><b>${esc(u.name)}</b><small>${esc(u.email)}</small></span>
      <span class="user-pts">${num(u.points)}<small>b</small></span></button>`;
  }).join("") || `<div class="empty">${icon("users")}<p>${A.users.length ? "Nikdo neodpovídá hledání." : "Načítám zákazníky…"}</p></div>`;
}

// ── Karta zákazníka: body + připojení k účtu na pokladně ──
function openUser(user, { scanned = false } = {}) {
  let points = user.points || 0, reason = REASONS[0];
  let dk = scanned ? { s: "syncing" } : null;   // stav Dotykačky
  const r = rankFor(user.totalPoints || 0);

  const dkHtml = () => {
    if (!dk) return "";
    if (dk.s === "syncing") return `<div class="dk syncing"><span class="spin"></span>Připojuji k účtu na pokladně…</div>`;
    if (dk.s === "ok") return `<div class="dk ok">${icon("check")}${esc(dk.msg)}</div>`;
    return `<div class="dk err"><span>${esc(dk.msg)}</span><button class="btn small ghost" data-act="retryDk">Znovu</button></div>`;
  };
  const diffHtml = () => {
    const d = points - (user.points || 0);
    return d === 0 ? "&nbsp;" : `<span class="${d > 0 ? "pos" : "neg"}">${d > 0 ? "+" : ""}${d} bodů</span>`;
  };

  const s = sheet(`
    <div class="cust-head"><span class="avatar ring big" style="--rank:${r.color}">${esc(initials(user.name))}</span>
      <div><h2>${esc(user.name)}</h2><small class="muted">${esc(user.email)}</small><small class="muted">${esc(user.phone || "")}</small></div></div>
    <div class="cust-meta"><span class="rank-chip dark"><i style="background:${r.color}"></i>${esc(r.label)}</span>
      <span class="muted">Teď ${num(user.points)} b · celkem ${num(user.totalPoints)} b</span></div>
    ${user.memberCode ? `<div class="mini-code">${barcodeSvg(user.memberCode, { height: 46, width: 1.6 })}<small>${esc(chunk4(user.memberCode))}</small></div>` : ""}
    <div id="dk">${dkHtml()}</div>
    <div class="eyebrow">Nový zůstatek bodů</div>
    <div class="stepper">
      <button class="step-btn" data-act="dec" aria-label="Ubrat">−</button>
      <input id="pts" type="number" inputmode="numeric" min="0" value="${points}">
      <button class="step-btn" data-act="inc" aria-label="Přidat">+</button>
    </div>
    <div class="chips center">${STEPS.map((n) => `<button class="chip ${n > 0 ? "pos" : "neg"}" data-act="step" data-n="${n}">${n > 0 ? "+" : "−"}${Math.abs(n)}</button>`).join("")}</div>
    <div class="diff" id="diff">${diffHtml()}</div>
    <div class="eyebrow">Důvod</div>
    <div class="chips" id="reasons">${REASONS.map((x) => `<button class="chip ${x === reason ? "on" : ""}" data-act="reason" data-r="${esc(x)}">${esc(x)}</button>`).join("")}</div>
    <div class="dlg-actions"><button class="btn ghost" data-close>Zrušit</button><button class="btn primary" data-act="save">Uložit</button></div>
  `);
  const ptsInput = s.body.querySelector("#pts");
  const setPts = (v) => { points = Math.max(0, Math.round(Number(v) || 0)); ptsInput.value = points; s.body.querySelector("#diff").innerHTML = diffHtml(); };
  ptsInput.addEventListener("input", () => { points = Math.max(0, Math.round(Number(ptsInput.value) || 0)); s.body.querySelector("#diff").innerHTML = diffHtml(); });

  const assign = async () => {
    dk = { s: "syncing" }; s.body.querySelector("#dk").innerHTML = dkHtml();
    try {
      const res = await call("assignCustomerToOrder", { userId: user.id }, 60_000);
      const total = res.orderTotal != null ? ` (${Math.round(res.orderTotal)} Kč)` : "";
      dk = { s: "ok", msg: res.alreadyAssigned ? `Zákazník už je na účtu${total}`
        : res.openWithoutCustomer > 1 ? `Připojeno k nejnovějšímu účtu${total} — otevřených je ${res.openWithoutCustomer}, zkontroluj pokladnu`
        : `Připojeno k účtu na pokladně${total}` };
    } catch (e) { dk = { s: "err", msg: errMsg(e, "Dotykačka nedostupná") }; }
    if (s.el.isConnected) s.body.querySelector("#dk").innerHTML = dkHtml();
  };
  if (scanned) assign();

  bindActions(s.el, {
    dec: () => setPts(points - 1),
    inc: () => setPts(points + 1),
    step: (el) => setPts(points + Number(el.dataset.n)),
    reason: (el) => {
      reason = el.dataset.r;
      s.body.querySelectorAll("#reasons .chip").forEach((c) => c.classList.toggle("on", c.dataset.r === reason));
    },
    retryDk: assign,
    save: (btn) => busy(btn, async () => {
      try {
        const applied = await updatePoints(user.id, points - (user.points || 0), reason);
        toast(applied === null ? "Beze změny" : "Body uloženy");
        s.close();
      } catch (e) { toast(errMsg(e, "Nepodařilo se uložit body."), "error"); }
    }, "Ukládám…"),
  });
}

/**
 * Obsluha nastaví nový zůstatek, zapíše se ROZDÍL (stejně jako v Android appce).
 * Transakce čte aktuální stav, takže body připsané pokladnou mezitím nepřepíše.
 * totalPoints se nikdy nesnižuje — je to základ pro rank.
 */
async function updatePoints(userId, diff, reason) {
  if (!diff) return null;
  return runTransaction(db, async (tx) => {
    const userRef = doc(db, "users", userId);
    const snap = await tx.get(userRef);
    const current = snap.get("points") || 0, total = snap.get("totalPoints") || 0;
    const updated = Math.max(0, current + diff), applied = updated - current;
    tx.update(userRef, { points: updated, totalPoints: applied > 0 ? total + applied : total });
    const txRef = doc(collection(db, "users", userId, "transactions"));
    tx.set(txRef, {
      id: txRef.id, type: applied >= 0 ? "ADD" : "SUBTRACT", amount: Math.abs(applied), reason,
      createdAt: Timestamp.now(), basePoints: 0, promoBonus: 0, promoLabel: null, rankBonus: 0, rankLabel: null,
    });
    return applied;
  });
}

// ── Skenování kartičky ──
async function scan() {
  let scanner = null, done = false;
  const s = sheet(`
    <div class="sheet-head"><h2>Naskenuj kartičku</h2><button class="icon-btn" data-close aria-label="Zavřít">${icon("close")}</button></div>
    <div id="reader" class="reader"><div class="center-fill"><span class="spin"></span></div></div>
    <p class="muted center small" id="scanMsg">Namiř kameru na čárový kód v appce zákazníka.</p>
    <form id="manual" class="manual"><input name="code" inputmode="numeric" placeholder="Nebo opiš číslo kartičky" autocomplete="off">
      <button class="btn primary" type="submit">Najít</button></form>
  `, { onClose: () => { done = true; scanner?.stop().catch(() => {}).finally(() => scanner?.clear()); } });

  const found = (raw) => {
    if (done) return;
    const code = String(raw || "").replace(/\s/g, "");
    const user = A.users.find((u) => u.memberCode === code) || (!code.includes("/") && A.users.find((u) => u.id === code));
    if (!user) { toast("Zákazník nenalezen", "error"); return; }
    done = true; s.close(); openUser(user, { scanned: true });
  };
  s.body.querySelector("#manual").addEventListener("submit", (e) => { e.preventDefault(); found(e.target.code.value); });

  try {
    scanner = new Html5Qrcode("reader", {
      formatsToSupport: [Html5QrcodeSupportedFormats.CODE_128, Html5QrcodeSupportedFormats.QR_CODE], verbose: false,
    });
    await scanner.start({ facingMode: "environment" },
      { fps: 12, qrbox: (w, h) => ({ width: Math.round(w * 0.9), height: Math.round(Math.min(h * 0.5, w * 0.45)) }) },
      (text) => found(text), () => {});
    if (done) scanner.stop().catch(() => {});
  } catch {
    s.body.querySelector("#reader").innerHTML = `<div class="empty">${icon("camera")}<p>Kamera není dostupná. Povol ji v nastavení prohlížeče, nebo opiš číslo kartičky.</p></div>`;
  }
}

// ── Nový zákazník ──
function addUser() {
  const s = sheet(`
    <h2>Nový zákazník</h2><p class="muted">Vyplň registrační údaje</p>
    <form id="addForm" novalidate>
      <label class="field"><span>Jméno a příjmení</span><input name="name" placeholder="Jan Novák" autocomplete="off"></label>
      <label class="field"><span>E-mail</span><input name="email" type="email" inputmode="email" placeholder="jan@email.cz" autocomplete="off"></label>
      <label class="field"><span>Telefon</span><input name="phone" type="tel" inputmode="tel" placeholder="+420 777 000 000" autocomplete="off"></label>
      <label class="field"><span>Počáteční body</span><input name="points" type="number" inputmode="numeric" min="0" placeholder="0"></label>
      <div class="dlg-actions"><button class="btn ghost" type="button" data-close>Zrušit</button><button class="btn primary" type="submit">Přidat zákazníka</button></div>
    </form>`);
  s.body.querySelector("#addForm").addEventListener("submit", (e) => {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.target));
    const name = d.name.trim(), email = d.email.trim().toLowerCase(), phone = d.phone.trim(), pts = Math.max(0, parseInt(d.points, 10) || 0);
    if (!name || !email || !phone) return toast("Vyplň jméno, e-mail a telefon", "error");
    if (!email.includes("@")) return toast("Neplatný e-mail", "error");
    busy(e.target.querySelector("[type=submit]"), async () => {
      try {
        const dup = await getDocs(query(collection(db, "users"), where("email", "==", email), limit(1)));
        if (!dup.empty) return toast("Zákazník s tímto e-mailem už existuje", "error");
        // Členský kód a zákazníka v Dotykačce doplní server (onUserCreated)
        await addDoc(collection(db, "users"), {
          name, email, phone, points: pts, totalPoints: pts, dotykackaId: "", fcmToken: "", memberCode: "", createdAt: Timestamp.now(),
        });
        toast("Zákazník přidán"); s.close();
      } catch (err) { toast(errMsg(err, "Nepodařilo se přidat zákazníka"), "error"); }
    }, "Přidávám…");
  });
}

// ── Převzetí zákazníků z Dotykačky ──
function importCustomers() {
  const s = sheet(`<h3 class="dlg-title">Převzít zákazníky z pokladny</h3><div id="imp"><div class="center-fill pad"><span class="spin"></span></div>
    <p class="muted center">Zjišťuji, kolik zákazníků je v Dotykačce…</p></div>`, { dialog: true });
  const box = () => s.body.querySelector("#imp");
  const summary = (r) => `<ul class="imp-list">
    <li><b>${num(r.totalInDotykacka)}</b> zákazníků v Dotykačce</li>
    <li><b>${num(r.alreadyInApp)}</b> už je v appce</li>
    <li><b>${num(r.noEmail)}</b> nemá e-mail (nepřevezmou se)</li>
    <li><b>${num(r.imported)}</b> ${r.dryRun ? "se převezme" : "převzato"}</li></ul>`;
  call("importDotykackaCustomers", { dryRun: true }, 540_000).then((r) => {
    if (!s.el.isConnected) return;
    box().innerHTML = summary(r) + (r.imported > 0
      ? `<div class="dlg-actions"><button class="btn ghost" data-close>Zrušit</button><button class="btn gold" data-act="go">Převzít ${num(r.imported)}</button></div>`
      : `<p class="muted">Není koho převzít.</p><div class="dlg-actions"><button class="btn ghost" data-close>Zavřít</button></div>`);
  }).catch((e) => { if (s.el.isConnected) box().innerHTML = `<p class="err-text">${esc(errMsg(e, "Import selhal"))}</p><div class="dlg-actions"><button class="btn ghost" data-close>Zavřít</button></div>`; });

  bindActions(s.el, {
    go: async (btn) => busy(btn, async () => {
      try {
        const r = await call("importDotykackaCustomers", { dryRun: false }, 540_000);
        box().innerHTML = summary(r) + `<div class="dlg-actions"><button class="btn primary" data-close>Hotovo</button></div>`;
      } catch (e) { toast(errMsg(e, "Import selhal"), "error"); }
    }, "Převádím…"),
  });
}

// ════════════════════════════════════════════════════════════════════
// Akce a zprávy
// ════════════════════════════════════════════════════════════════════
const L = { multiplier: 2, duration: "TODAY", minRank: 0, recipients: null };

function loyaltyHtml() {
  return `<div class="pill admin">
    ${back("Akce a zprávy", "Obsluha")}
    <section class="panel">
      <h3>${icon("sparkle")} Akce „více bodů“</h3>
      <div id="promoBox"></div>
      <p class="muted">Zákazníci dostanou za nákup násobek bodů. V appce uvidí banner s akcí.</p>
      <div class="eyebrow">Násobek</div>
      <div class="chips">${[2, 3].map((m) => `<button class="chip ${L.multiplier === m ? "on" : ""}" data-act="mult" data-m="${m}">${m}× body</button>`).join("")}</div>
      <div class="eyebrow">Jak dlouho</div>
      <div class="chips">${DURATIONS.map(([k, l]) => `<button class="chip ${L.duration === k ? "on" : ""}" data-act="dur" data-k="${k}">${l}</button>`).join("")}</div>
      <label class="field"><span>Název akce</span><input id="promoLabel" maxlength="40" placeholder="${L.multiplier === 2 ? "Dvojité body" : "Trojité body"}"></label>
      <label class="switch"><input type="checkbox" id="promoNotify" checked><span>Poslat zákazníkům upozornění</span></label>
      <button class="btn gold wide" data-act="startPromo">Spustit akci</button>
    </section>
    <section class="panel">
      <h3>${icon("bell")} Hromadná zpráva</h3>
      <label class="field"><span>Nadpis</span><input id="bTitle" maxlength="65" placeholder="Dorazila nová odrůda"></label>
      <label class="field"><span>Text</span><textarea id="bBody" maxlength="240" rows="3" placeholder="Lemon Haze je zpátky skladem. Stav se!"></textarea></label>
      <div class="eyebrow">Komu</div>
      <div class="chips">${AUDIENCES.map((a, i) => `<button class="chip ${L.minRank === i ? "on" : ""}" data-act="aud" data-i="${i}">${a}</button>`).join("")}</div>
      <p class="muted small" id="recipients"></p>
      <button class="btn primary wide" data-act="sendBroadcast">Odeslat zprávu</button>
      <p class="muted small">Zprávu dostanou zákazníci s Android appkou a zapnutými notifikacemi.</p>
    </section>
  </div>`;
}

function updatePromoBox() {
  const el = document.getElementById("promoBox"); if (!el) return;
  const p = activePromo(A.config);
  el.innerHTML = p ? `<div class="promo running">${icon("sparkle")}<div><b>Běží: ${esc(p.label)} (${p.multiplier}×)</b><span>${esc(czechUntil(p.endsAt))}</span></div>
    <button class="btn small danger" data-act="stopPromo">Ukončit</button></div>` : "";
}

function endOf(k) {
  const d = new Date();
  const eod = () => d.setHours(23, 59, 59, 0);
  if (k === "TODAY") eod();
  if (k === "H24") d.setTime(d.getTime() + 24 * 3_600_000);
  if (k === "SUNDAY") { while (d.getDay() !== 0) d.setDate(d.getDate() + 1); eod(); }
  if (k === "WEEK") { d.setDate(d.getDate() + 6); eod(); }
  return d.getTime();
}

let countSeq = 0;
async function countRecipients() {
  const el = document.getElementById("recipients"); if (!el) return;
  const seq = ++countSeq;
  el.textContent = "Počítám příjemce…";
  try {
    const r = await call("adminBroadcast", { title: "", body: "", minRank: L.minRank, dryRun: true });
    if (seq === countSeq) { L.recipients = r.recipients; el.textContent = `Zprávu dostane ${num(r.recipients)} zákazníků.`; }
  } catch { if (seq === countSeq) { L.recipients = null; el.textContent = "Počet příjemců se nepodařilo zjistit."; } }
}

const chipsSelect = (el, attr) => el.parentElement.querySelectorAll(".chip").forEach((c) => c.classList.toggle("on", c === el));

const loyaltyActions = {
  mult: (el) => {
    L.multiplier = Number(el.dataset.m); chipsSelect(el);
    document.getElementById("promoLabel").placeholder = L.multiplier === 2 ? "Dvojité body" : "Trojité body";
  },
  dur: (el) => { L.duration = el.dataset.k; chipsSelect(el); },
  aud: (el) => { L.minRank = Number(el.dataset.i); chipsSelect(el); countRecipients(); },
  startPromo: async (btn) => {
    const label = document.getElementById("promoLabel").value.trim() || (L.multiplier === 2 ? "Dvojité body" : `${L.multiplier}× body`);
    const notify = document.getElementById("promoNotify").checked;
    const durLabel = DURATIONS.find(([k]) => k === L.duration)[1].toLowerCase();
    if (!(await confirmDialog({ title: `Spustit „${label}“?`, text: `${L.multiplier}× body za každý nákup, ${durLabel}.${notify ? " Zákazníci dostanou upozornění." : ""}`, ok: "Spustit" }))) return;
    busy(btn, async () => {
      try {
        const r = await call("adminUpdateLoyalty", { promo: { multiplier: L.multiplier, endsAtMillis: endOf(L.duration), label, notify } });
        toast(notify ? `Akce běží, upozornění dostalo ${num(r.notified)} zákazníků` : "Akce běží");
      } catch (e) { toast(errMsg(e), "error"); }
    }, "Spouštím…");
  },
  stopPromo: async (btn) => {
    if (!(await confirmDialog({ title: "Ukončit akci?", text: "Nákupy od teď dostanou zase normální počet bodů.", ok: "Ukončit", danger: true }))) return;
    busy(btn, async () => {
      try { await call("adminUpdateLoyalty", { promo: null }); toast("Akce ukončena"); } catch (e) { toast(errMsg(e), "error"); }
    }, "…");
  },
  sendBroadcast: async (btn) => {
    const title = document.getElementById("bTitle").value.trim(), body = document.getElementById("bBody").value.trim();
    if (!title || !body) return toast("Vyplň nadpis i text zprávy.", "error");
    if (!(await confirmDialog({ title: "Odeslat zprávu?", text: `„${title}“ dostane ${num(L.recipients ?? 0)} zákazníků. Odeslanou zprávu už nejde vzít zpět.`, ok: "Odeslat" }))) return;
    busy(btn, async () => {
      try {
        const r = await call("adminBroadcast", { title, body, minRank: L.minRank, dryRun: false }, 120_000);
        toast(`Zpráva odeslána ${num(r.sent)} zákazníkům`);
        document.getElementById("bTitle").value = ""; document.getElementById("bBody").value = "";
      } catch (e) { toast(errMsg(e), "error"); }
    }, "Odesílám…");
  },
};

// ════════════════════════════════════════════════════════════════════
// Naše zeleň
// ════════════════════════════════════════════════════════════════════
function productsHtml() {
  return `<div class="pill admin">
    ${back("Naše zeleň", "Správa produktů")}
    <button class="btn gold wide" data-act="newProduct">${icon("plus")} Přidat odrůdu</button>
    <div id="products" class="products"></div>
  </div>`;
}
function updateProducts() {
  const el = document.getElementById("products"); if (!el) return;
  el.innerHTML = A.products.map((p) => productCard(p, { admin: true })).join("") || `<p class="muted center">Zatím žádné produkty.</p>`;
}

/** Zmenší fotku na max 1200 px a převede na JPEG (rychlé nahrání i z mobilu). */
async function toJpeg(file, max = 1200) {
  const bmp = await createImageBitmap(file).catch(() => null);
  const img = bmp || await new Promise((res, rej) => { const i = new Image(); i.onload = () => res(i); i.onerror = rej; i.src = URL.createObjectURL(file); });
  const scale = Math.min(1, max / Math.max(img.width, img.height));
  const c = document.createElement("canvas");
  c.width = Math.round(img.width * scale); c.height = Math.round(img.height * scale);
  c.getContext("2d").drawImage(img, 0, 0, c.width, c.height);
  return new Promise((res) => c.toBlob(res, "image/jpeg", 0.85));
}

function editProduct(p) {
  const isNew = !p;
  p = p || { id: "", name: "", description: "", forms: ["1g", "3g", "volně"], imageUrl: "", imageName: "", orderIndex: 0 };
  const forms = new Set(p.forms || []);
  let file = null;
  const img = p.imageUrl || (p.imageName ? `/app/img/${p.imageName}.png` : "");
  const s = sheet(`
    <h2>${isNew ? "Nová odrůda" : "Upravit odrůdu"}</h2>
    <div class="eyebrow">Obrázek</div>
    <label class="img-pick"><span id="imgPrev">${img ? `<img src="${esc(img)}" alt="">` : icon("camera")}</span>
      <input type="file" accept="image/*" id="imgFile" hidden><b>Vybrat fotku</b></label>
    <label class="field"><span>Název odrůdy</span><input id="pName" value="${esc(p.name)}"></label>
    <label class="field"><span>Popis odrůdy</span><textarea id="pDesc" rows="3">${esc(p.description)}</textarea></label>
    <div class="eyebrow">Dostupné formy</div>
    <div class="chips" id="forms">${ALL_FORMS.map((f) => `<button class="chip ${forms.has(f) ? "on" : ""}" data-act="form" data-f="${esc(f)}">${esc(f)}</button>`).join("")}</div>
    <div class="dlg-actions">
      ${isNew ? "" : `<button class="btn ghost danger-text" data-act="del">${icon("trash")} Smazat</button>`}
      <button class="btn ghost" data-close>Zrušit</button><button class="btn primary" data-act="saveProduct">Uložit</button></div>`);
  s.body.querySelector("#imgFile").addEventListener("change", (e) => {
    file = e.target.files[0] || null;
    if (file) s.body.querySelector("#imgPrev").innerHTML = `<img src="${URL.createObjectURL(file)}" alt="">`;
  });
  bindActions(s.el, {
    form: (el) => { const f = el.dataset.f; forms.has(f) ? forms.delete(f) : forms.add(f); el.classList.toggle("on", forms.has(f)); },
    del: async (btn) => {
      if (!(await confirmDialog({ title: `Smazat „${p.name}“?`, text: "Tato akce je nevratná.", ok: "Smazat", danger: true }))) return;
      busy(btn, async () => {
        try {
          await deleteDoc(doc(db, "products", p.id));
          deleteObject(ref(storage, `products/${p.id}.jpg`)).catch(() => {});
          toast("Odrůda smazána"); s.close();
        } catch (e) { toast(errMsg(e), "error"); }
      });
    },
    saveProduct: (btn) => {
      const name = s.body.querySelector("#pName").value.trim();
      if (!name) return toast("Vyplň název odrůdy.", "error");
      busy(btn, async () => {
        try {
          const id = p.id || doc(collection(db, "products")).id;
          let imageUrl = p.imageUrl || "";
          if (file) {
            const r = ref(storage, `products/${id}.jpg`);
            await uploadBytes(r, await toJpeg(file), { contentType: "image/jpeg", cacheControl: "public, max-age=86400" });
            imageUrl = await getDownloadURL(r);
          }
          const orderIndex = isNew ? A.products.reduce((m, x) => Math.max(m, x.orderIndex || 0), -1) + 1 : p.orderIndex || 0;
          await setDoc(doc(db, "products", id), {
            id, name, description: s.body.querySelector("#pDesc").value.trim(),
            forms: ALL_FORMS.filter((f) => forms.has(f)), imageUrl, imageName: p.imageName || "", orderIndex,
          });
          toast("Uloženo"); s.close();
        } catch (e) { toast(errMsg(e, "Uložení se nepovedlo."), "error"); }
      }, "Ukládám…");
    },
  });
}

// ════════════════════════════════════════════════════════════════════
// Ovládání
// ════════════════════════════════════════════════════════════════════
root.addEventListener("input", (e) => {
  if (e.target.id === "q") { A.query = e.target.value; updateList(); }
});

root.addEventListener("submit", (e) => {
  if (e.target.id !== "adminLogin") return;
  e.preventDefault();
  const d = Object.fromEntries(new FormData(e.target));
  if (!d.email || !d.password) return toast("Vyplň všechna pole", "error");
  busy(e.target.querySelector("[type=submit]"), async () => {
    try { await signInWithEmailAndPassword(auth, d.email.trim(), d.password); }
    catch (err) { toast(/network/i.test(err?.code || "") ? "Nejsi připojený k internetu." : "Nesprávné přihlašovací údaje", "error"); }
  }, "Přihlašuji…");
});

bindActions(root, {
  ...loyaltyActions,
  home: () => { A.view = "home"; render(); },
  loyalty: () => { A.view = "loyalty"; render(); },
  products: () => { A.view = "products"; render(); },
  sort: (el) => { A.sort = el.dataset.k; document.querySelectorAll(".sort-row .chip").forEach((c) => c.classList.toggle("on", c === el)); updateList(); },
  openUser: (el) => { const u = A.users.find((x) => x.id === el.dataset.id); if (u) openUser(u); },
  scan,
  addUser,
  import: importCustomers,
  newProduct: () => editProduct(null),
  editProduct: (el) => editProduct(A.products.find((x) => x.id === el.dataset.id)),
  dismissHint,
  logout: async () => {
    if (await confirmDialog({ title: "Odhlásit se?", text: "Pro další práci se budeš muset znovu přihlásit.", ok: "Odhlásit", danger: true })) signOut(auth);
  },
});

render();
