// CannaClub — webová appka pro zákazníky (hlavně iPhone)
// Funguje stejně jako Android: anonymní přihlášení zařízení + ověření
// e-mailem a telefonem na serveru (functions/customers.js).
// Vzhled i chování drží krok s Android appkou (app/src/main/.../ui/user).
import { initFirebase } from "./fb.js";
import { signInAnonymously, onAuthStateChanged, signOut } from "firebase/auth";
import { doc, onSnapshot, collection, query, orderBy, limit } from "firebase/firestore";
import {
  RANKS, SHOP_ITEMS, WOLT_URL, PRIVACY_URL, EARN_KC_PER_POINT,
  esc, num, chunk4, initials, firstName, toMillis, dayLabel, timeLabel, czechUntil,
  rankFor, activePromo, errMsg, icon, toast, sheet, confirmDialog, bindActions, busy,
  barcodeSvg, productsByCategory, productDetailHtml, prefetchProductImages, installHint, dismissHint, keepAwake,
  rankBadge, mountPlants, emptyProducts, shopHead,
} from "./common.js";

const { auth, db, call } = initFirebase("customer");
const KEY = "cc_user_id";
const root = document.getElementById("app");
mountPlants("customer");

const S = {
  view: "loading",          // loading | login | register | home
  userId: null,
  user: null,
  txs: [],
  config: null,
  products: [],
  productsLoaded: false,
  rewards: null,            // ceník odměn z config/rewards (null = výchozí)
  cat: null,                // vybraná kategorie v nabídce
  draft: {},                // rozepsaný formulář při přepínání login/registrace
  hero: null,               // právě zobrazené body a postup ranku (kvůli animaci)
  pendingOnboarding: false, // po přihlášení / registraci ukázat úvod
};
let unsubs = [];
let productsSheet = null;

const store = {
  get: () => { try { return localStorage.getItem(KEY); } catch { return null; } },
  set: (v) => { try { v ? localStorage.setItem(KEY, v) : localStorage.removeItem(KEY); } catch { /* soukromý režim */ } },
};
// Kolik bodů zákazník viděl naposledy (oslava, i když body dorazily při zavřené appce)
const seen = {
  get: (id) => { try { const v = localStorage.getItem(`cc_seen_${id}`); return v == null ? null : Number(v); } catch { return null; } },
  set: (id, v) => { try { localStorage.setItem(`cc_seen_${id}`, String(v)); } catch { /* nevadí */ } },
};
const onboardingDone = () => { try { return Boolean(localStorage.getItem("cc_onboarded")); } catch { return false; } };

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
    checkPointsGain(S.user);
    if (S.pendingOnboarding) { S.pendingOnboarding = false; if (!onboardingDone()) showOnboarding(); }
  }, () => logoutLocal("Přihlášení vypršelo, přihlas se znovu.")));

  unsubs.push(onSnapshot(
    query(collection(db, "users", userId, "transactions"), orderBy("createdAt", "desc"), limit(20)),
    (snap) => { S.txs = snap.docs.map((d) => ({ ...d.data(), id: d.id })); render(); },
    () => {}
  ));
  unsubs.push(onSnapshot(doc(db, "config", "loyalty"), (snap) => { S.config = snap.data() || null; render(); }, () => {}));
  unsubs.push(onSnapshot(doc(db, "config", "rewards"), (snap) => { S.rewards = snap.data()?.items || null; render(); }, () => {}));
  unsubs.push(onSnapshot(query(collection(db, "products"), orderBy("orderIndex")), (snap) => {
    S.products = snap.docs.map((d) => ({ ...d.data(), id: d.id }));
    S.productsLoaded = true;
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
  Object.assign(S, { userId: null, user: null, txs: [], view: "login", hero: null, productsLoaded: false, txAll: false });
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
  if (v === "loading" || (v === "home" && !S.user)) root.innerHTML = splashHtml();
  else if (v === "login") root.innerHTML = loginHtml();
  else if (v === "register") root.innerHTML = registerHtml();
  else { root.innerHTML = homeHtml(); animateHero(); }
}

/** Úvodní obrazovka: logo se plynule objeví (jako SplashScreen v Android appce). */
const splashHtml = () => `<div class="splash"><img class="splash-logo" src="/app/img/wordmark.png" alt="CannaClub"></div>`;

const field = (name, label, type, placeholder, extra = "") => `
  <label class="field"><span>${label}</span>
    <input name="${name}" type="${type}" placeholder="${esc(placeholder)}" value="${esc(S.draft[name] || "")}" ${extra}>
  </label>`;

function loginHtml() {
  return `<div class="auth-wrap">
    <div class="pill auth-card">
      <img class="wordmark" src="/app/img/wordmark.png" alt="CannaClub">
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
    <div class="pill auth-card">
      <img class="wordmark" src="/app/img/wordmark.png" alt="CannaClub">
      <h1>Registrace</h1>
      <p class="muted">Po registraci dostaneš členskou kartičku. Ukaž ji u pokladny a za každý nákup ti naskočí body.</p>
      <form id="registerForm" novalidate>
        ${field("name", "Jméno a příjmení", "text", "Marry Jane", 'autocomplete="name" required')}
        ${field("email", "E-mail", "email", "vas@email.cz", 'autocomplete="email" inputmode="email" required')}
        ${field("phone", "Telefon", "tel", "+420 777 000 000", 'autocomplete="tel" inputmode="tel" required')}
        <label class="consent"><input type="checkbox" name="consent">
          <span>Je mi 18 let a souhlasím se zpracováním jména, e-mailu a telefonu pro věrnostní program CannaClub.</span>
        </label>
        <a class="privacy-link" href="${PRIVACY_URL}" target="_blank" rel="noopener">Zásady ochrany osobních údajů</a>
        <button class="btn primary wide" type="submit">Zaregistrovat se</button>
      </form>
      <button class="btn link wide faint" data-act="toLogin">Už mám účet, přihlásit se</button>
    </div>
  </div>`;
}

function homeHtml() {
  const u = S.user;
  const rank = rankFor(u.totalPoints || 0);
  const promo = activePromo(S.config);
  const ready = Boolean(u.memberCode);
  const h = S.hero || { p: u.points || 0, g: rank.progress };
  return `
  <div class="pill home">
    <header class="topbar">
      <img class="wordmark small" src="/app/img/wordmark.png" alt="CannaClub">
      <button class="avatar" data-act="profile" aria-label="Profil">${esc(initials(u.name))}</button>
    </header>
    <h1 class="hello">Ahoj${firstName(u.name) ? `, ${esc(firstName(u.name))}` : ""}</h1>
    ${installHint("cc_hint_c")}
    ${promo ? `<div class="promo"><span class="promo-x">${promo.multiplier}×</span><div><b>${esc(promo.label)}</b>
      <span>Za každý nákup ${promo.multiplier}× víc bodů · ${esc(czechUntil(promo.endsAt))}</span></div>${icon("sparkle")}</div>` : ""}

    <section class="hero" data-act="rewards">
      <span class="hero-wm">${icon("leaf")}</span>
      <div class="hero-top"><span class="eyebrow light">Věrnostní body</span>
        <span class="rank-chip">${rankBadge(rank, { size: 20 })}${esc(rank.label)}${rank.bonus ? ` · +${rank.bonus} %` : ""}</span></div>
      <div class="hero-points"><b id="heroPts">${Math.round(h.p)}</b><small>b</small></div>
      <div class="hero-sub">${esc(nextRewardText(u.points || 0))}</div>
      <div class="bar"><i id="heroBar" style="width:${(h.g * 100).toFixed(1)}%"></i></div>
      <div class="hero-foot">
        <span>${rank.next ? `Do ranku ${esc(rank.next.label)} (+${rank.next.bonus} % bodů) chybí ${num(rank.missing)} b` : "Nejvyšší rank, patříš do rodiny"}</span>
        <span class="link-light">Odměny ${icon("chevron")}</span>
      </div>
    </section>

    <button class="card-btn" data-act="card">
      <span class="card-ic">${icon("barcode")}</span>
      <span><b>Ukázat kartičku</b><small>${ready ? "Prodavač ji naskenuje u pokladny" : "Kartička se připravuje…"}</small></span>
      ${icon("chevron", "dim")}
    </button>

    <div class="tiles">
      <button class="tile" data-act="products"><span class="bubble">${icon("leaf")}</span><b>Nabídka</b><small>Co máme skladem</small></button>
      <a class="tile" href="${WOLT_URL}" target="_blank" rel="noopener"><span class="bubble">${icon("bike")}</span><b>Rozvoz</b><small>Objednej přes Wolt</small></a>
    </div>

    ${S.txs.length ? txHtml() : `<h2 class="section">Jak sbírat body</h2>${howHtml()}`}
  </div>`;
}

/** Plynulé dopočítání bodů a postupu k dalšímu ranku (jako animace karty v Android appce). */
function bezier(x1, y1, x2, y2) {
  const cx = 3 * x1, bx = 3 * (x2 - x1) - cx, ax = 1 - cx - bx, cy = 3 * y1, by = 3 * (y2 - y1) - cy, ay = 1 - cy - by;
  const X = (t) => ((ax * t + bx) * t + cx) * t, Y = (t) => ((ay * t + by) * t + cy) * t;
  return (x) => {
    let t = x;
    for (let i = 0; i < 6; i++) { const d = (3 * ax * t + 2 * bx) * t + cx; if (Math.abs(d) < 1e-6) break; t -= (X(t) - x) / d; }
    return Y(Math.min(1, Math.max(0, t)));
  };
}
const fastOutSlowIn = bezier(0.4, 0, 0.2, 1);
let heroRaf = 0;
function animateHero() {
  const u = S.user; if (!u) return;
  const toP = u.points || 0, toG = rankFor(u.totalPoints || 0).progress;
  if (!S.hero) { S.hero = { p: toP, g: toG }; return; }
  const fromP = S.hero.p, fromG = S.hero.g;
  cancelAnimationFrame(heroRaf);
  if (fromP === toP && fromG === toG) return;
  const t0 = performance.now();
  const step = (now) => {
    const kp = fastOutSlowIn(Math.min(1, (now - t0) / 1100)), kg = fastOutSlowIn(Math.min(1, (now - t0) / 1200));
    S.hero = { p: fromP + (toP - fromP) * kp, g: fromG + (toG - fromG) * kg };
    const pts = document.getElementById("heroPts"), bar = document.getElementById("heroBar");
    if (pts) pts.textContent = Math.round(S.hero.p);
    if (bar) bar.style.width = `${(S.hero.g * 100).toFixed(1)}%`;
    if (now - t0 < 1200) heroRaf = requestAnimationFrame(step);
    else S.hero = { p: toP, g: toG };
  };
  heroRaf = requestAnimationFrame(step);
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
  const visits = recent.filter((t) => t.type !== "SUBTRACT" && (t.reason || "").toLowerCase().startsWith("nákup")).length;
  return `<div class="tx-stats">
      <div><b class="pos">+${gained}</b><span>Získáno</span></div>
      <div><b class="neg">${spent ? `−${spent}` : "0"}</b><span>Uplatněno</span></div>
      <div><b>${visits}</b><span>Nákupů</span></div>
    </div><p class="tx-note">za posledních 30 dní</p>`;
}

function txRow(t) {
  const ms = toMillis(t.createdAt), pos = t.type !== "SUBTRACT";
  const chips = [
    t.promoBonus > 0 ? `<em class="bonus honey">+${num(t.promoBonus)} ${esc(t.promoLabel || "akce")}</em>` : "",
    t.rankBonus > 0 ? `<em class="bonus">+${num(t.rankBonus)} rank ${esc(t.rankLabel || "")}</em>` : "",
  ].join("");
  return `<div class="tx">
    <span class="bubble ${pos ? "" : "red"}">${icon(pos ? (t.reason || "").toLowerCase().startsWith("nákup") ? "bag" : "sparkle" : "gift")}</span>
    <div class="tx-text"><b>${esc(t.reason || (pos ? "Připsáno" : "Uplatněno"))}</b>
      <small>${timeLabel(ms)}${chips}</small></div>
    <span class="tx-amt ${pos ? "pos" : "neg"}">${pos ? "+" : "−"}${num(t.amount)} b</span>
  </div>`;
}

function txHtml() {
  const shown = S.txAll ? S.txs : S.txs.slice(0, TX_PREVIEW);
  const days = [];
  for (const t of shown) {
    const day = dayLabel(toMillis(t.createdAt));
    if (days.at(-1)?.day !== day) days.push({ day, items: [] });
    days.at(-1).items.push(t);
  }
  return `<h2 class="section">Pohyby bodů</h2>${txSummary()}<div class="tx-list">${days.map((d) => `
    <div class="tx-day"><i></i>${esc(d.day)}<span>${signed(d.items.reduce((a, t) => a + txValue(t), 0))} b</span></div>
    <div class="tx-group">${d.items.map(txRow).join("")}</div>`).join("")}</div>
    ${!S.txAll && S.txs.length > TX_PREVIEW ? `<button class="more-btn" data-act="txAll">Zobrazit starší pohyby (${S.txs.length - TX_PREVIEW})</button>` : ""}`;
}

// ── Kartička přes celou obrazovku ──
let wake = null;
async function showCard() {
  const u = S.user;
  if (!u) return;
  const ready = Boolean(u.memberCode), code = u.memberCode || u.id;
  const s = sheet(`
    <div class="member-card" data-close>
      <img class="wordmark" src="/app/img/wordmark.png" alt="CannaClub">
      ${u.name ? `<div class="mc-name">${esc(u.name)}</div>` : ""}
      <div class="mc-code">${barcodeSvg(code, { height: 120, width: 2.6 })}</div>
      ${ready ? `<div class="mc-digits">${esc(chunk4(u.memberCode))}</div>` : ""}
      <p class="mc-hint">${ready ? "Ukaž prodavači k naskenování" : "Kód pro pokladnu se ještě připravuje, zkus to za chvíli"}</p>
      <p class="mc-close">Klepnutím zavřeš</p>
    </div>`, { cls: "fullscreen", onClose: () => { wake?.release?.(); wake = null; } });
  s.el.querySelector(".sheet").classList.add("white");
  wake = await keepAwake();
}

// ── Odměny ──
const rewardItems = () => [...(S.rewards?.length ? S.rewards : SHOP_ITEMS)].sort((a, b) => a.cost - b.cost);

function rewardRow(pts) {
  return (it) => {
    const ok = pts >= it.cost;
    return `<div class="row"><span class="bubble ${ok ? "" : "off"}">${icon(it.icon)}</span>
      <div class="row-text"><b>${esc(it.name)}</b><small>${it.cost} b</small></div>
      ${ok ? `<span class="badge ok">${icon("check")} Můžeš si vzít</span>`
        : `<div class="need"><small>chybí ${it.cost - pts} b</small><div class="bar dark thin"><i style="width:${Math.min(100, (pts / it.cost) * 100).toFixed(0)}%"></i></div></div>`}</div>`;
  };
}

function showRewards() {
  const u = S.user, pts = u.points || 0, total = u.totalPoints || 0, rank = rankFor(total);
  sheet(`
    <div class="sheet-head"><h2>Odměny</h2><span class="pill-badge">Máš ${pts} b</span></div>
    <div class="eyebrow">Za body si můžeš vzít</div>
    <div class="list-card rewards">${rewardItems().map(rewardRow(pts)).join("")}</div>
    <p class="note">${icon("info")} Jak uplatnit: řekni si u pokladny o odměnu a ukaž kartičku. Obsluha ti body odečte.</p>
    <div class="eyebrow wide">Tvůj rank</div>
    <div class="rank-card">
      ${rankBadge(rank, { size: 52 })}
      <div class="row-text"><b class="serif">${esc(rank.label)}</b>
        <small>${rank.bonus > 0 ? `+${rank.bonus} % bodů za každý nákup · celkem ${total} b` : `Celkem nasbíráno ${total} b`}</small>
        <div class="bar dark"><i style="width:${(rank.progress * 100).toFixed(1)}%;background:${rank.color}"></i></div>
        <small class="faint">${rank.next ? `Do ranku ${esc(rank.next.label)} (+${rank.next.bonus} % bodů) chybí ${num(rank.missing)} b` : "Nejvyšší rank, patříš do rodiny"}</small></div>
    </div>
    <div class="eyebrow wide">Cesta ranků</div>
    <div class="rank-path">${RANKS.map((r, i) => {
      const unlocked = total >= r.from, current = r.key === rank.key;
      const conn = i < RANKS.length - 1 ? `<div class="rp-conn ${total >= RANKS[i + 1].from ? "on" : ""}"><i></i></div>` : "";
      return `<div class="rp">${rankBadge(r, { size: 46, locked: !unlocked })}
        <div class="row-text"><b class="${current ? "cur" : ""} ${unlocked ? "" : "dim"}">${esc(r.label)}</b>
          <small>${r.from === 0 ? "Hned od registrace" : `od ${r.from} nasbíraných bodů`}${r.bonus ? ` · +${r.bonus} % bodů` : ""}</small></div>
        ${current ? `<span class="now" style="background:${r.color}">Teď</span>` : unlocked ? icon("check", "ok") : ""}</div>${conn}`;
    }).join("")}</div>
  `);
}

function nextRewardText(points) {
  const list = rewardItems(), next = list.find((r) => r.cost > points), can = list.filter((r) => r.cost <= points).length;
  if (!next) return can ? "Můžeš si vzít jakoukoli odměnu" : "Sbírej body za každý nákup";
  if (can) return `Na ${can} ${can === 1 ? "odměnu" : can < 5 ? "odměny" : "odměn"} už máš · do další chybí ${next.cost - points} b`;
  return `Do odměny „${next.name}“ chybí ${next.cost - points} b`;
}

// ── Nabídka ──
function productsHtml() {
  const body = !S.productsLoaded ? `<div class="pgrid">${'<div class="pcard skel"></div>'.repeat(4)}</div>`
    : S.products.length ? productsByCategory(S.products, S.cat) : emptyProducts();
  return `${shopHead(S.productsLoaded ? S.products.length : null)}${body}`;
}

// ── Profil ──
function showProfile() {
  const u = S.user, rank = rankFor(u.totalPoints || 0);
  const s = sheet(`
    <div class="profile-head"><span class="avatar xl">${esc(initials(u.name))}</span>
      <h2>${esc(u.name)}</h2>
      <div class="prank">${rankBadge(rank, { size: 20 })}<small class="muted">Rank ${esc(rank.label)}</small></div></div>
    <div class="list-card info">
      <div class="row"><span>E-mail</span><b>${esc(u.email)}</b></div>
      <div class="row"><span>Telefon</span><b>${esc(u.phone || "—")}</b></div>
      <div class="row"><span>Členské číslo</span><b>${esc(u.memberCode ? chunk4(u.memberCode) : "připravuje se")}</b></div>
    </div>
    <div class="list-card">
      <button class="row action" data-act="how">${icon("info")}<span>Jak to funguje</span>${icon("chevron", "dim")}</button>
      <a class="row action" href="${PRIVACY_URL}" target="_blank" rel="noopener">${icon("user")}<span>Ochrana osobních údajů</span>${icon("chevron", "dim")}</a>
      <button class="row action red" data-act="logout">${icon("logout")}<span>Odhlásit se</span>${icon("chevron", "dim")}</button>
    </div>
    <p class="note muted center faint">Změnu údajů ti rádi uděláme na prodejně.</p>
    <button class="btn link small danger-text center-block" data-act="deleteAccount">Smazat účet</button>
  `);
  bindActions(s.el, {
    how: () => { s.close(); showOnboarding(); },
    logout: async () => {
      if (await confirmDialog({ title: "Odhlásit se?", text: "Body ti zůstanou. Příště se přihlásíš e-mailem a telefonem.", ok: "Odhlásit", danger: true })) {
        s.close(); logoutLocal();
      }
    },
    deleteAccount: async (btn) => {
      const ok = await confirmDialog({
        title: "Smazat účet natrvalo?",
        text: `Smažou se tvoje body (${u.points || 0} b), historie nákupů v appce i členská kartička. Tohle nejde vrátit.`,
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
// Úvod „Jak to funguje“ — 3 obrazovky, poprvé po přihlášení nebo z profilu
// ════════════════════════════════════════════════════════════════════
const INTRO = [
  ["barcode", "Tvoje kartička je v telefonu", "U pokladny klepni na Ukázat kartičku a prodavač ji naskenuje. Nic dalšího nosit nemusíš."],
  ["bag", "Nakupuj a sbírej", `Za každých ${EARN_KC_PER_POINT} Kč dostaneš 1 bod. Připíšou se samy do pár minut.`],
  ["gift", "Body = odměny", "Vyměň body za odměny z nabídky. Čím víc nasbíráš, tím vyšší rank a víc bodů za každý nákup."],
];

function showOnboarding() {
  if (document.querySelector(".onboard")) return;
  let i = 0;
  const el = document.createElement("div");
  el.className = "onboard";
  el.innerHTML = `
    <button class="skip" data-skip>Přeskočit</button>
    <div class="ob-pages"><div class="ob-track">${INTRO.map(([ic, t, d]) => `
      <div class="ob-page"><span class="ob-ring"><span>${icon(ic)}</span></span><h1>${t}</h1><p>${d}</p></div>`).join("")}</div></div>
    <div class="ob-dots">${INTRO.map(() => "<i></i>").join("")}</div>
    <button class="ob-next" data-next></button>`;
  document.body.append(el);
  document.body.classList.add("locked");
  const track = el.querySelector(".ob-track"), dots = [...el.querySelectorAll(".ob-dots i")], next = el.querySelector("[data-next]");
  const show = (n) => {
    i = Math.max(0, Math.min(INTRO.length - 1, n));
    track.style.transform = `translateX(${-i * 100}%)`;
    dots.forEach((d, k) => d.classList.toggle("on", k === i));
    next.textContent = i === INTRO.length - 1 ? "Začít sbírat" : "Další";
  };
  const finish = () => {
    try { localStorage.setItem("cc_onboarded", "1"); } catch { /* nevadí */ }
    document.removeEventListener("keydown", onKey);
    el.classList.add("out");
    setTimeout(() => el.remove(), 220);
    document.body.classList.remove("locked");
  };
  const onKey = (e) => { if (e.key === "Escape") finish(); };
  document.addEventListener("keydown", onKey);
  el.querySelector("[data-skip]").onclick = finish;
  next.onclick = () => (i === INTRO.length - 1 ? finish() : show(i + 1));
  let x0 = null;
  el.addEventListener("touchstart", (e) => { x0 = e.touches[0].clientX; }, { passive: true });
  el.addEventListener("touchend", (e) => {
    if (x0 == null) return;
    const dx = e.changedTouches[0].clientX - x0; x0 = null;
    if (Math.abs(dx) > 50) show(i + (dx < 0 ? 1 : -1));
  });
  show(0);
}

// ════════════════════════════════════════════════════════════════════
// Oslava: přibyly body → padající lístky, vibrace, velké „+64 b“
// ════════════════════════════════════════════════════════════════════
let cel = null;
function checkPointsGain(u) {
  const s = seen.get(u.id), p = u.points || 0;
  if (s === null) seen.set(u.id, p);                 // první spuštění, nic neslavit
  else if (p > s) celebrate(p - s);
  else if (p < s) seen.set(u.id, p);                 // uplatnil odměnu
}

function celebrate(gained) {
  if (cel) { cel.textContent = `+${gained} b`; return; }
  const el = document.createElement("div");
  el.className = "celebrate";
  el.innerHTML = `<canvas></canvas><div class="cel-card"><span class="cel-ic">${icon("sparkle")}</span>
    <div class="cel-num">+${gained} b</div><p>Body ti přibyly. Díky za nákup!</p></div>`;
  document.body.append(el);
  cel = el.querySelector(".cel-num");
  navigator.vibrate?.(40);

  const canvas = el.querySelector("canvas"), ctx = canvas.getContext("2d");
  const dpr = window.devicePixelRatio || 1, W = window.innerWidth, H = window.innerHeight;
  canvas.width = W * dpr; canvas.height = H * dpr; ctx.scale(dpr, dpr);
  const palette = [["#526935", 1], ["#3E5E35", 1], ["#7C9A5E", 1], ["#D9A441", 1], ["#D9A441", 0.8]];
  const leaves = Array.from({ length: 46 }, () => {
    const [color, alpha] = palette[Math.floor(Math.random() * palette.length)];
    return {
      x: Math.random(), delay: Math.random() * 0.35, speed: 0.7 + Math.random() * 0.6, size: 9 + Math.random() * 12,
      spin: Math.random() * 720 - 360, sway: Math.random() * 60 - 30, color, alpha,
    };
  });
  const t0 = performance.now();
  let raf = 0, done = false;
  const draw = (now) => {
    const t = Math.min(1, (now - t0) / 3200);
    ctx.clearRect(0, 0, W, H);
    for (const l of leaves) {
      const p = Math.min(1.2, Math.max(0, ((t - l.delay) / (1 - l.delay)) * l.speed));
      if (p <= 0) continue;
      const y = -40 + p * (H + 80), x = l.x * W + (Math.sin(p * 9) * l.sway) / 2.6, w = l.size / 2.2;
      ctx.save();
      ctx.translate(x, y); ctx.rotate((l.spin * p * Math.PI) / 180);
      ctx.globalAlpha = l.alpha * Math.min(1, Math.max(0, 1 - Math.max(p - 0.85, 0) * 4));
      ctx.fillStyle = l.color; ctx.beginPath(); ctx.ellipse(0, 0, w, w * 0.45, 0, 0, Math.PI * 2); ctx.fill();
      ctx.restore();
    }
    if (t < 1) raf = requestAnimationFrame(draw);
  };
  raf = requestAnimationFrame(draw);
  const finish = () => {
    if (done) return; done = true;
    cancelAnimationFrame(raf); clearTimeout(timer);
    cel = null;
    if (S.user) seen.set(S.user.id, S.user.points || 0);
    el.classList.add("out");
    setTimeout(() => el.remove(), 250);
  };
  const timer = setTimeout(finish, 4200);
  el.addEventListener("click", finish);
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
        S.pendingOnboarding = true;
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
    if (!d.consent) return toast("Potvrď, že je ti 18 let a souhlasíš se zpracováním údajů.", "error");
    await busy(btn, async () => {
      try {
        await ensureDevice();
        const { userId } = await call("customerRegister", { name: d.name.trim(), email, phone: d.phone.trim(), consent: true });
        S.pendingOnboarding = true;
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
