// CannaClub — webová appka pro zákazníky (hlavně iPhone)
// Funguje stejně jako Android: anonymní přihlášení zařízení + ověření
// e-mailem a telefonem na serveru (functions/customers.js).
import { initFirebase } from "./fb.js";
import { signInAnonymously, onAuthStateChanged, signOut } from "firebase/auth";
import { doc, onSnapshot, collection, query, orderBy, limit } from "firebase/firestore";
import {
  RANKS, SHOP_ITEMS, WOLT_URL, PRIVACY_URL, EARN_KC_PER_POINT,
  esc, num, chunk4, initials, firstName, toMillis, dayLabel, timeLabel, czechUntil,
  rankFor, activePromo, errMsg, icon, toast, sheet, confirmDialog, bindActions, busy,
  barcodeSvg, productsByCategory, productDetailHtml, prefetchProductImages, installHint, dismissHint, keepAwake,
} from "./common.js";

const { auth, db, call } = initFirebase("customer");
const KEY = "cc_user_id";
const root = document.getElementById("app");

const S = {
  view: "loading",          // loading | login | register | home
  userId: null,
  user: null,
  txs: [],
  config: null,
  products: [],
  rewards: null,            // ceník odměn z config/rewards (null = výchozí)
  cat: null,                // vybraná kategorie v nabídce
  draft: {},                // rozepsaný formulář při přepínání login/registrace
};
let unsubs = [];
let productsSheet = null;

const store = {
  get: () => { try { return localStorage.getItem(KEY); } catch { return null; } },
  set: (v) => { try { v ? localStorage.setItem(KEY, v) : localStorage.removeItem(KEY); } catch { /* soukromý režim */ } },
};

// ════════════════════════════════════════════════════════════════════
// Data
// ════════════════════════════════════════════════════════════════════
function listen(userId) {
  stopListening();
  S.userId = userId;
  unsubs.push(onSnapshot(doc(db, "users", userId), (snap) => {
    if (!snap.exists()) return logoutLocal("Účet už neexistuje.");
    S.user = { id: snap.id, ...snap.data() };
    if (S.view !== "home") S.view = "home";
    render();
  }, () => logoutLocal("Přihlášení vypršelo, přihlas se znovu.")));

  unsubs.push(onSnapshot(
    query(collection(db, "users", userId, "transactions"), orderBy("createdAt", "desc"), limit(20)),
    (snap) => { S.txs = snap.docs.map((d) => ({ id: d.id, ...d.data() })); render(); },
    () => {}
  ));
  unsubs.push(onSnapshot(doc(db, "config", "loyalty"), (snap) => { S.config = snap.data() || null; render(); }, () => {}));
  unsubs.push(onSnapshot(doc(db, "config", "rewards"), (snap) => { S.rewards = snap.data()?.items || null; }, () => {}));
  unsubs.push(onSnapshot(query(collection(db, "products"), orderBy("orderIndex")), (snap) => {
    S.products = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    prefetchProductImages(S.products);
    productsSheet?.update(productsHtml());
  }, () => {}));
}

function stopListening() { unsubs.forEach((u) => u()); unsubs = []; }

async function ensureDevice() {
  if (!auth.currentUser) await signInAnonymously(auth);
}

async function logoutLocal(msg) {
  stopListening();
  store.set(null);
  Object.assign(S, { userId: null, user: null, txs: [], view: "login" });
  if (auth.currentUser?.isAnonymous) await signOut(auth).catch(() => {});
  render();
  if (msg) toast(msg);
}

onAuthStateChanged(auth, (u) => {
  const saved = store.get();
  if (u && saved) { if (S.userId !== saved) listen(saved); return; }
  if (!u && saved) store.set(null);   // zařízení ztratilo přihlášení → znovu ověřit
  if (S.view === "loading") { S.view = "login"; render(); }
});

// ════════════════════════════════════════════════════════════════════
// Obrazovky
// ════════════════════════════════════════════════════════════════════
function render() {
  const v = S.view;
  root.className = `screen screen-${v}`;
  if (v === "loading" || (v === "home" && !S.user)) root.innerHTML = `<div class="center-fill"><span class="spin big"></span></div>`;
  else if (v === "login") root.innerHTML = loginHtml();
  else if (v === "register") root.innerHTML = registerHtml();
  else root.innerHTML = homeHtml();
}

const field = (name, label, type, placeholder, extra = "") => `
  <label class="field"><span>${label}</span>
    <input name="${name}" type="${type}" placeholder="${esc(placeholder)}" value="${esc(S.draft[name] || "")}" ${extra}>
  </label>`;

function loginHtml() {
  return `<div class="auth-wrap">
    <img class="wordmark" src="/app/img/wordmark.png" alt="CannaClub">
    <div class="pill auth-card">
      <h1>Vítej zpět</h1>
      <p class="muted">Přihlas se ke svým bodům</p>
      <form id="loginForm" novalidate>
        ${field("email", "E-mail", "email", "vas@email.cz", 'autocomplete="email" inputmode="email" required')}
        ${field("phone", "Telefon", "tel", "+420 777 000 000", 'autocomplete="tel" inputmode="tel"')}
        ${field("name", "Jméno a příjmení <i>(když telefon nemáme)</i>", "text", "Marry Jane", 'autocomplete="name"')}
        <button class="btn primary wide" type="submit">Přihlásit se</button>
      </form>
      <button class="btn outline wide" data-act="toRegister">Nemám účet · Registrace</button>
    </div>
    ${installHint("cc_hint_c")}
  </div>`;
}

function registerHtml() {
  return `<div class="auth-wrap">
    <img class="wordmark" src="/app/img/wordmark.png" alt="CannaClub">
    <div class="pill auth-card">
      <h1>Registrace</h1>
      <p class="muted">Po registraci dostaneš členskou kartičku. Ukaž ji u pokladny a za každý nákup ti naskočí body.</p>
      <form id="registerForm" novalidate>
        ${field("name", "Jméno a příjmení", "text", "Marry Jane", 'autocomplete="name" required')}
        ${field("email", "E-mail", "email", "vas@email.cz", 'autocomplete="email" inputmode="email" required')}
        ${field("phone", "Telefon", "tel", "+420 777 000 000", 'autocomplete="tel" inputmode="tel" required')}
        <label class="consent"><input type="checkbox" name="consent">
          <span>Je mi 18 let a souhlasím se zpracováním jména, e-mailu a telefonu pro věrnostní program CannaClub.
          <a href="${PRIVACY_URL}" target="_blank" rel="noopener">Zásady ochrany osobních údajů</a></span>
        </label>
        <button class="btn primary wide" type="submit">Zaregistrovat se</button>
      </form>
      <button class="btn link wide" data-act="toLogin">Už mám účet, přihlásit se</button>
    </div>
  </div>`;
}

function homeHtml() {
  const u = S.user;
  const rank = rankFor(u.totalPoints || 0);
  const promo = activePromo(S.config);
  const ready = Boolean(u.memberCode);
  return `
  <header class="topbar">
    <img class="wordmark small" src="/app/img/wordmark.png" alt="CannaClub">
    <button class="avatar" data-act="profile" aria-label="Profil" style="--rank:${rank.color}">${esc(initials(u.name))}</button>
  </header>
  <div class="pill home">
    <h1 class="hello">Ahoj${firstName(u.name) ? `, ${esc(firstName(u.name))}` : ""}</h1>
    ${installHint("cc_hint_c")}
    ${promo ? `<div class="promo">${icon("sparkle")}<div><b>${esc(promo.label)}</b>
      <span>Za každý nákup ${promo.multiplier}× víc bodů · ${esc(czechUntil(promo.endsAt))}</span></div></div>` : ""}

    <section class="hero">
      <div class="hero-top"><span class="eyebrow light">Věrnostní body</span>
        <span class="rank-chip"><i style="background:${rank.color}"></i>${esc(rank.label)}${rank.bonus ? ` · +${rank.bonus} %` : ""}</span></div>
      <div class="hero-points">${num(u.points)}<small>b</small></div>
      <div class="hero-sub">${esc(nextRewardText(u.points || 0))}</div>
      <div class="bar"><i style="width:${(rank.progress * 100).toFixed(1)}%"></i></div>
      <div class="hero-foot">
        <span>${rank.next ? `Do ranku ${esc(rank.next.label)} (+${rank.next.bonus} % bodů) chybí ${num(rank.missing)} b` : "Nejvyšší rank, patříš do rodiny"}</span>
        <button class="link-light" data-act="rewards">Odměny ${icon("chevron")}</button>
      </div>
    </section>

    <button class="card-btn" data-act="card" ${ready ? "" : "disabled"}>
      <span class="card-ic">${icon("barcode")}</span>
      <span><b>Ukázat kartičku</b><small>${ready ? "Prodavač ji naskenuje u pokladny" : "Kartička se připravuje…"}</small></span>
      ${icon("chevron", "dim")}
    </button>

    <div class="tiles">
      <button class="tile" data-act="products"><span class="bubble">${icon("leaf")}</span><b>Nabídka</b><small>Co máme skladem</small></button>
      <a class="tile" href="${WOLT_URL}" target="_blank" rel="noopener"><span class="bubble">${icon("bike")}</span><b>Rozvoz</b><small>Objednej přes Wolt</small></a>
    </div>

    <h2 class="section">Jak sbírat body</h2>
    ${howHtml()}

    <h2 class="section">Pohyby bodů</h2>
    ${txHtml()}

    <p class="foot-links"><a href="${PRIVACY_URL}" target="_blank" rel="noopener">Ochrana osobních údajů</a></p>
  </div>`;
}

function howHtml() {
  const steps = [
    ["barcode", "Ukaž kartičku", "U pokladny klepni na Ukázat kartičku, prodavač ji naskenuje."],
    ["bag", "Nakup jako obvykle", `Za každých ${EARN_KC_PER_POINT} Kč dostaneš 1 bod, připíšou se do pár minut.`],
    ["gift", "Vyměň body", "Za odměny z nabídky – papírky, zapalovač, kytky a další."],
  ];
  return `<div class="how">${steps.map(([ic, t, d], i) => `
    <div class="how-step"><span class="bubble">${icon(ic)}</span><div><b>${i + 1}. ${t}</b><p>${d}</p></div></div>`).join("")}</div>`;
}

const TX_PREVIEW = 8;
const signed = (n) => (n >= 0 ? `+${num(n)}` : `−${num(-n)}`);
const txValue = (t) => (t.type === "SUBTRACT" ? -1 : 1) * (Number(t.amount) || 0);

function txSummary() {
  const since = Date.now() - 30 * 864e5;
  const recent = S.txs.filter((t) => toMillis(t.createdAt) >= since);
  const gained = recent.filter((t) => t.type !== "SUBTRACT").reduce((a, t) => a + (t.amount || 0), 0);
  const spent = recent.filter((t) => t.type === "SUBTRACT").reduce((a, t) => a + (t.amount || 0), 0);
  const visits = recent.filter((t) => t.type !== "SUBTRACT" && (t.reason || "").startsWith("Nákup")).length;
  return `<div class="tx-stats">
      <div><b class="pos">+${num(gained)}</b><span>Získáno</span></div>
      <div><b class="neg">${spent ? `−${num(spent)}` : "0"}</b><span>Uplatněno</span></div>
      <div><b>${num(visits)}</b><span>Nákupů</span></div>
    </div><p class="tx-note">za posledních 30 dní</p>`;
}

function txRow(t) {
  const ms = toMillis(t.createdAt), pos = t.type !== "SUBTRACT";
  const chips = [
    t.promoBonus > 0 ? `<em class="bonus honey">+${num(t.promoBonus)} ${esc(t.promoLabel || "akce")}</em>` : "",
    t.rankBonus > 0 ? `<em class="bonus">+${num(t.rankBonus)} rank ${esc(t.rankLabel || "")}</em>` : "",
  ].join("");
  return `<div class="tx">
    <span class="bubble ${pos ? "" : "red"}">${icon(pos ? (t.reason || "").startsWith("Nákup") ? "bag" : "sparkle" : "gift")}</span>
    <div class="tx-text"><b>${esc(t.reason || (pos ? "Připsáno" : "Uplatněno"))}</b>
      <small>${timeLabel(ms)}${chips}</small></div>
    <span class="tx-amt ${pos ? "pos" : "neg"}">${pos ? "+" : "−"}${num(t.amount)} b</span>
  </div>`;
}

function txHtml() {
  if (!S.txs.length) return `<div class="empty">${icon("bag")}<p>Zatím tu nic není. Body se objeví po prvním nákupu.</p></div>`;
  const shown = S.txAll ? S.txs : S.txs.slice(0, TX_PREVIEW);
  const days = [];
  for (const t of shown) {
    const day = dayLabel(toMillis(t.createdAt));
    if (days.at(-1)?.day !== day) days.push({ day, items: [] });
    days.at(-1).items.push(t);
  }
  return `${txSummary()}<div class="tx-list">${days.map((d) => `
    <div class="tx-day"><i></i>${esc(d.day)}<span>${signed(d.items.reduce((a, t) => a + txValue(t), 0))} b</span></div>
    <div class="tx-group">${d.items.map(txRow).join("")}</div>`).join("")}</div>
    ${!S.txAll && S.txs.length > TX_PREVIEW ? `<button class="more-btn" data-act="txAll">Zobrazit starší pohyby (${S.txs.length - TX_PREVIEW})</button>` : ""}`;
}

// ── Kartička přes celou obrazovku ──
let wake = null;
async function showCard() {
  const u = S.user;
  if (!u?.memberCode) return;
  const s = sheet(`
    <div class="member-card" data-close>
      <img class="wordmark" src="/app/img/wordmark.png" alt="CannaClub">
      <div class="mc-name">${esc(u.name)}</div>
      <div class="mc-code">${barcodeSvg(u.memberCode, { height: 120, width: 2.6 })}</div>
      <div class="mc-digits">${esc(chunk4(u.memberCode))}</div>
      <p class="mc-hint">Ukaž prodavači k naskenování</p>
      <p class="mc-close">Klepnutím zavřeš</p>
    </div>`, { cls: "fullscreen", onClose: () => { wake?.release?.(); wake = null; } });
  s.el.querySelector(".sheet").classList.add("white");
  wake = await keepAwake();
}

// ── Odměny ──
function showRewards() {
  const u = S.user, pts = u.points || 0, rank = rankFor(u.totalPoints || 0);
  sheet(`
    <div class="sheet-head"><h2>Odměny</h2><span class="pill-badge">Máš ${num(pts)} b</span></div>
    <div class="eyebrow">Za body si můžeš vzít</div>
    <div class="list-card">${rewardItems().map((it) => {
      const ok = pts >= it.cost;
      return `<div class="row"><span class="bubble">${icon(it.icon)}</span>
        <div class="row-text"><b>${esc(it.name)}</b><small>${it.cost} b</small></div>
        <span class="badge ${ok ? "ok" : ""}">${ok ? `${icon("check")} Můžeš si vzít` : `chybí ${it.cost - pts} b`}</span></div>`;
    }).join("")}</div>
    <p class="note">${icon("info")} Jak uplatnit: řekni si u pokladny o odměnu a ukaž kartičku. Obsluha ti body odečte.</p>
    <div class="eyebrow">Tvůj rank</div>
    <div class="rank-card">
      <span class="rank-dot big" style="background:${rank.color}">${icon("leaf")}</span>
      <div class="row-text"><b class="serif">${esc(rank.label)}</b>
        <small>+${rank.bonus} % bodů za každý nákup · celkem ${num(u.totalPoints)} b</small>
        <div class="bar dark"><i style="width:${(rank.progress * 100).toFixed(1)}%"></i></div>
        <small>${rank.next ? `Do ranku ${esc(rank.next.label)} (+${rank.next.bonus} % bodů) chybí ${num(rank.missing)} b` : "Nejvyšší rank"}</small></div>
    </div>
    <div class="eyebrow">Cesta ranků</div>
    <div class="rank-path">${RANKS.map((r, i) => `
      <div class="rp ${i <= rank.index ? "done" : ""}"><span class="rank-dot" style="background:${r.color}"></span>
        <b>${esc(r.label)}</b><small>od ${num(r.from)} b</small><span class="rp-bonus">${r.bonus ? `+${r.bonus} %` : "základ"}</span></div>`).join("")}
    </div>
    <p class="note muted">Rank se počítá z bodů nasbíraných za celou dobu, uplatnění bodů ho nesnižuje.</p>
  `);
}

const rewardItems = () => [...(S.rewards?.length ? S.rewards : SHOP_ITEMS)].sort((a, b) => a.cost - b.cost);

function nextRewardText(points) {
  const list = rewardItems(), next = list.find((r) => r.cost > points), can = list.filter((r) => r.cost <= points).length;
  if (!next) return can ? "Můžeš si vzít jakoukoli odměnu" : "Sbírej body za každý nákup";
  if (can) return `Na ${can} ${can === 1 ? "odměnu" : can < 5 ? "odměny" : "odměn"} už máš · do další chybí ${next.cost - points} b`;
  return `Do odměny „${next.name}“ chybí ${next.cost - points} b`;
}

// ── Nabídka ──
function productsHtml() {
  return `<div class="shop-head">
      <button class="round-btn" data-close aria-label="Zpět">${icon("back")}</button>
      ${S.products.length ? `<span class="count-pill">${S.products.length} produktů</span>` : ""}
    </div>
    <div class="shop-title"><span class="eyebrow">CannaClub · Hradec Králové</span><h1>Nabídka</h1>
      <p>Na prodejně ti s výběrem rádi poradíme.</p></div>
    ${S.products.length ? productsByCategory(S.products, S.cat) : `<div class="pgrid">${'<div class="pcard skel"></div>'.repeat(4)}</div>`}`;
}

// ── Profil ──
function showProfile() {
  const u = S.user, rank = rankFor(u.totalPoints || 0);
  const s = sheet(`
    <div class="profile-head"><span class="avatar big" style="--rank:${rank.color}">${esc(initials(u.name))}</span>
      <div><h2>${esc(u.name)}</h2><small class="muted">Rank ${esc(rank.label)}</small></div></div>
    <div class="list-card info">
      <div class="row"><small>E-mail</small><b>${esc(u.email)}</b></div>
      <div class="row"><small>Telefon</small><b>${esc(u.phone || "—")}</b></div>
      <div class="row"><small>Členské číslo</small><b>${esc(u.memberCode ? chunk4(u.memberCode) : "připravuje se")}</b></div>
    </div>
    <div class="list-card">
      <button class="row action" data-act="how">${icon("info")}<span>Jak to funguje</span>${icon("chevron", "dim")}</button>
      <a class="row action" href="${PRIVACY_URL}" target="_blank" rel="noopener">${icon("user")}<span>Ochrana osobních údajů</span>${icon("chevron", "dim")}</a>
      <button class="row action red" data-act="logout">${icon("logout")}<span>Odhlásit se</span></button>
    </div>
    <p class="note muted center">Změnu údajů ti rádi uděláme na prodejně.</p>
    <button class="btn link small danger-text center-block" data-act="deleteAccount">Smazat účet</button>
  `);
  bindActions(s.el, {
    how: () => { s.close(); sheet(`<h2>Jak to funguje</h2>${howHtml()}
      <p class="note">${icon("sparkle")} Vyšší rank = víc bodů za každý nákup, až +20 %. Sleduj v appce i akce s dvojnásobnými body.</p>`); },
    logout: async () => {
      if (await confirmDialog({ title: "Odhlásit se?", text: "Body ti zůstanou. Příště se přihlásíš e-mailem a telefonem.", ok: "Odhlásit", danger: true })) {
        s.close(); logoutLocal();
      }
    },
    deleteAccount: async (btn) => {
      const ok = await confirmDialog({
        title: "Smazat účet natrvalo?",
        text: `Smažou se tvoje body (${num(u.points)} b), historie nákupů v appce i členská kartička. Tohle nejde vrátit.`,
        ok: "Smazat účet", danger: true,
      });
      if (!ok) return;
      await busy(btn, async () => {
        try { await call("deleteMyAccount"); s.close(); await logoutLocal("Účet je smazaný."); }
        catch (e) { toast(errMsg(e, "Účet se nepodařilo smazat."), "error"); }
      }, "Mažu…");
    },
  });
}

// ════════════════════════════════════════════════════════════════════
// Akce
// ════════════════════════════════════════════════════════════════════
const formData = (f) => Object.fromEntries(new FormData(f));

root.addEventListener("input", (e) => { if (e.target.name) S.draft[e.target.name] = e.target.value; });

root.addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = e.target, btn = f.querySelector("[type=submit]"), d = formData(f);
  const email = (d.email || "").trim().toLowerCase();
  if (f.id === "loginForm") {
    if (!email) return toast("Vyplň e-mail.", "error");
    if (!d.phone?.trim() && !d.name?.trim()) return toast("Vyplň telefon (nebo jméno, když telefon nemáme).", "error");
    await busy(btn, async () => {
      try {
        await ensureDevice();
        const { userId } = await call("customerLogin", { email, phone: (d.phone || "").trim(), name: (d.name || "").trim() });
        store.set(userId); S.draft = {}; listen(userId);
      } catch (err) {
        toast(err?.code === "functions/not-found"
          ? "Nenašli jsme tě. Zkontroluj e-mail a telefon, nebo se zaregistruj."
          : errMsg(err, "Přihlášení se nepovedlo."), "error");
      }
    }, "Přihlašuji…");
  }
  if (f.id === "registerForm") {
    if ((d.name || "").trim().split(/\s+/).length < 2) return toast("Vyplň jméno i příjmení.", "error");
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) return toast("Neplatný e-mail.", "error");
    if ((d.phone || "").replace(/\D/g, "").length < 9) return toast("Neplatné telefonní číslo.", "error");
    if (!d.consent) return toast("Potvrď prosím souhlas.", "error");
    await busy(btn, async () => {
      try {
        await ensureDevice();
        const { userId } = await call("customerRegister", { name: d.name.trim(), email, phone: d.phone.trim(), consent: true });
        store.set(userId); S.draft = {}; listen(userId);
        toast("Vítej v CannaClubu!");
      } catch (err) { toast(errMsg(err, "Registrace se nepovedla."), "error"); }
    }, "Registruji…");
  }
});

bindActions(document.body, {
  toRegister: () => { S.view = "register"; render(); },
  toLogin: () => { S.view = "login"; render(); },
  profile: showProfile,
  rewards: showRewards,
  card: showCard,
  products: () => { productsSheet = sheet(productsHtml(), { cls: "shop", onClose: () => { productsSheet = null; } }); },
  product: (el) => { const p = S.products.find((x) => x.id === el.dataset.id); if (p) sheet(productDetailHtml(p)); },
  dismissHint,
  txAll: () => { S.txAll = true; render(); },
  cat: (el) => { S.cat = el.dataset.cat; productsSheet?.update(productsHtml()); productsSheet?.el.querySelector(".sheet").scrollTo({ top: 0 }); },
});

render();
