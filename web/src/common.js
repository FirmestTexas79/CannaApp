// Sdílené pomůcky pro zákaznickou i admin část
import JsBarcode from "jsbarcode";

export const WOLT_URL = "https://wolt.com/cs/cze/hradec-kralove/venue/cannaclub-cannabis-cbd";
export const PRIVACY_URL = "/ochrana-udaju.html";
export const EARN_KC_PER_POINT = 10;

// ── Ranky — musí sedět s functions/loyalty.js a MemberRank v Android appce ──
export const RANKS = [
  { key: "ZAKAZNIK", label: "Zákazník", from: 0, bonus: 0, color: "#7C9A5E" },
  { key: "BRONZOVY", label: "Bronzový", from: 250, bonus: 5, color: "#A86B3C" },
  { key: "STRIBRNY", label: "Stříbrný", from: 500, bonus: 10, color: "#8A9096" },
  { key: "ZLATY", label: "Zlatý", from: 1000, bonus: 15, color: "#C39A2B" },
  { key: "RODINA", label: "Rodina", from: 2500, bonus: 20, color: "#3E5E35" },
];

export function rankFor(total = 0) {
  let i = 0;
  while (i + 1 < RANKS.length && total >= RANKS[i + 1].from) i++;
  const r = RANKS[i], next = RANKS[i + 1] || null;
  const progress = next ? Math.min(1, Math.max(0, (total - r.from) / (next.from - r.from))) : 1;
  return { ...r, index: i, next, progress, missing: next ? next.from - total : 0 };
}

// ── Odměny z ceníku (stejné jako RewardsSheet.kt) ──
export const SHOP_ITEMS = [
  { icon: "preroll", name: "Prémiový preroll", cost: 20 },
  { icon: "preroll", name: "3× preroll", cost: 40 },
  { icon: "bud", name: "3g kytky", cost: 50 },
  { icon: "cube", name: "4g hashe", cost: 100 },
];

// ── Text a čísla ──
const ESC = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };
export const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ESC[c]);
export const num = (n) => Number(n || 0).toLocaleString("cs-CZ");
export const chunk4 = (s) => String(s || "").replace(/(.{4})(?=.)/g, "$1 ");
export const initials = (name) =>
  String(name || "").trim().split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join("") || "?";
export const firstName = (name) => String(name || "").trim().split(/\s+/)[0] || "";
export const toMillis = (ts) => (ts?.toMillis ? ts.toMillis() : ts?.seconds ? ts.seconds * 1000 : Number(ts) || 0);

const czDay = new Intl.DateTimeFormat("cs-CZ", { weekday: "long", day: "numeric", month: "long" });
const czTime = new Intl.DateTimeFormat("cs-CZ", { hour: "2-digit", minute: "2-digit" });
export function dayLabel(ms) {
  const d = new Date(ms), today = new Date();
  const diff = Math.round((new Date(today.toDateString()) - new Date(d.toDateString())) / 86_400_000);
  if (diff === 0) return "Dnes";
  if (diff === 1) return "Včera";
  const t = czDay.format(d);
  return t[0].toUpperCase() + t.slice(1);
}
export const timeLabel = (ms) => czTime.format(new Date(ms));

/** "do neděle 23:59", "do dnes 20:00" … */
export function czechUntil(ms) {
  const d = new Date(ms), now = new Date();
  const days = Math.round((new Date(d.toDateString()) - new Date(now.toDateString())) / 86_400_000);
  const t = czTime.format(d);
  if (days <= 0) return `dnes do ${t}`;
  if (days === 1) return `do zítřka ${t}`;
  return `do ${new Intl.DateTimeFormat("cs-CZ", { weekday: "long" }).format(d)} ${t}`;
}

export function activePromo(config, now = Date.now()) {
  const p = config?.promo;
  if (!p || !p.endsAt || Number(p.multiplier) <= 1) return null;
  const start = toMillis(p.startsAt), end = toMillis(p.endsAt);
  if (now < start || now > end) return null;
  return { multiplier: Number(p.multiplier), label: p.label || `${p.multiplier}× body`, endsAt: end };
}

export function errMsg(e, fallback = "Něco se nepovedlo, zkus to znovu.") {
  const m = e?.message || "";
  if (/network|Failed to fetch|unavailable/i.test(m)) return "Nejsi připojený k internetu.";
  if (!m || /^internal$/i.test(m) || m.includes("Firebase:")) return fallback;
  return m;
}

// ── Obrázky produktů: nahrané ve Storage, nebo výchozí 4 odrůdy ──
export const productImg = (p) => p.imageUrl || (p.imageName ? `/app/img/${p.imageName}.png` : "");

// ── Čárový kód (Code 128, stejný jako v Android appce a v Dotykačce) ──
export function barcodeSvg(code, { height = 90, width = 2.2 } = {}) {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  try {
    JsBarcode(svg, String(code), { format: "CODE128", displayValue: false, margin: 0, height, width, background: "transparent" });
  } catch { return ""; }
  svg.setAttribute("preserveAspectRatio", "none");
  svg.classList.add("barcode");
  return svg.outerHTML;
}

// ── Ikony (stejná sada jako res/drawable/ic_*.xml, kreslené čárou) ──
const P = {
  card: '<rect x="3" y="5" width="18" height="14" rx="3"/><path d="M7 9v6M10 9v6M13 9v6M16 9v6"/>',
  gift: '<rect x="3" y="8" width="18" height="13" rx="2"/><path d="M12 8v13M3 12h18M12 8c-2-4-6-4-6-1.5S9 8 12 8c3 0 6 1 6-1.5S14 4 12 8"/>',
  leaf: '<path d="M5 19c0-8 5-14 15-14 0 10-6 15-14 15"/><path d="M5 19c3-4 6-7 10-9"/>',
  bike: '<circle cx="6" cy="17" r="3.5"/><circle cx="18" cy="17" r="3.5"/><path d="M6 17l4-7h6l2 7M10 10l-1-3H7m6 3l2-4h2"/>',
  bag: '<path d="M5 8h14l-1 12H6L5 8z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
  barcode: '<path d="M4 6v12M7 6v12M10 6v12M14 6v12M17 6v12M20 6v12"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c1-4 4-6 8-6s7 2 8 6"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2 20c.8-3.5 3.4-5.5 7-5.5s6.2 2 7 5.5"/><path d="M16 4.5a3.5 3.5 0 0 1 0 7M18.5 14.8c1.8.8 3 2.6 3.5 5.2"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.5"/>',
  logout: '<path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/><path d="M10 16l-4-4 4-4M6 12h10"/>',
  close: '<path d="M6 6l12 12M18 6L6 18"/>',
  chevron: '<path d="M9 6l6 6-6 6"/>',
  back: '<path d="M15 6l-6 6 6 6"/>',
  sparkle: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"/><path d="M19 15l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7z"/>',
  tag: '<path d="M3 12V4h8l10 10-8 8z"/><circle cx="7.5" cy="8.5" r="1.5"/>',
  trend: '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="M16 16l5 5"/>',
  scan: '<path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M4 12h16"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7"/>',
  edit: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="M13.5 6.5l4 4"/>',
  preroll: '<path d="M4 20L17 7l2 2L6 22z" transform="translate(0 -2)"/><path d="M17 5c1-1 2.5-1 3 0"/>',
  bud: '<path d="M12 21V11"/><path d="M12 11c-4-1-6-4-6-8 4 0 6 3 6 8zM12 11c4-1 6-4 6-8-4 0-6 3-6 8z"/><path d="M12 15c-3 0-5-1.5-6-4M12 15c3 0 5-1.5 6-4"/>',
  cube: '<path d="M12 3l8 4.5v9L12 21l-8-4.5v-9z"/><path d="M4 7.5l8 4.5 8-4.5M12 12v9"/>',
  bell: '<path d="M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z"/><path d="M10 20a2 2 0 0 0 4 0"/>',
  trash: '<path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13"/>',
  share: '<path d="M12 15V4M8 8l4-4 4 4"/><path d="M5 12v7a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-7"/>',
  camera: '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7l1.5-3h5L16 7"/><circle cx="12" cy="13.5" r="3.5"/>',
};
export const icon = (name, cls = "") =>
  `<svg class="ic ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${P[name] || ""}</svg>`;

// ── Toast ──
let toastTimer;
export function toast(msg, kind = "") {
  let el = document.getElementById("toast");
  if (!el) { el = document.createElement("div"); el.id = "toast"; document.body.append(el); }
  el.className = `toast show ${kind}`;
  el.textContent = msg;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove("show"), 3200);
}

// ── Spodní panel (sheet) / dialog ──
const openSheets = [];
export function sheet(html, { onClose, dialog = false, cls = "" } = {}) {
  const wrap = document.createElement("div");
  wrap.className = `overlay ${dialog ? "as-dialog" : "as-sheet"} ${cls}`;
  wrap.innerHTML = `<div class="sheet" role="dialog" aria-modal="true">${dialog ? "" : '<div class="grab"></div>'}<div class="sheet-body"></div></div>`;
  const body = wrap.querySelector(".sheet-body");
  body.innerHTML = html;
  const api = {
    el: wrap, body,
    update(h) { body.innerHTML = h; },
    close() {
      if (!wrap.isConnected) return;
      wrap.classList.remove("open");
      setTimeout(() => wrap.remove(), 220);
      openSheets.splice(openSheets.indexOf(api), 1);
      document.body.classList.toggle("locked", openSheets.length > 0);
      onClose?.();
    },
  };
  wrap.addEventListener("click", (e) => {
    if (e.target === wrap || e.target.closest("[data-close]")) api.close();
  });
  document.body.append(wrap);
  document.body.classList.add("locked");
  openSheets.push(api);
  void wrap.offsetWidth;   // bez rAF: ve skryté záložce by se panel neukázal
  wrap.classList.add("open");
  return api;
}
document.addEventListener("keydown", (e) => { if (e.key === "Escape") openSheets.at(-1)?.close(); });

/** Potvrzovací dialog → Promise<boolean> */
export function confirmDialog({ title, text = "", ok = "Potvrdit", danger = false }) {
  return new Promise((resolve) => {
    let done = false;
    const s = sheet(
      `<h3 class="dlg-title">${esc(title)}</h3>${text ? `<p class="muted">${esc(text)}</p>` : ""}
       <div class="dlg-actions"><button class="btn ghost" data-close>Zrušit</button>
       <button class="btn ${danger ? "danger" : "primary"}" data-ok>${esc(ok)}</button></div>`,
      { dialog: true, onClose: () => { if (!done) resolve(false); } }
    );
    s.body.querySelector("[data-ok]").onclick = () => { done = true; resolve(true); s.close(); };
  });
}

/** Klik na [data-act="x"] → handlers.x(el, event) */
export function bindActions(root, handlers) {
  root.addEventListener("click", (e) => {
    const el = e.target.closest("[data-act]");
    if (!el || !root.contains(el)) return;
    const fn = handlers[el.dataset.act];
    if (fn) { e.preventDefault(); fn(el, e); }
  });
}

/** Tlačítko do stavu "pracuji…" po dobu běhu fn */
export async function busy(btn, fn, label = "Moment…") {
  if (!btn || btn.disabled) return;
  const old = btn.innerHTML;
  btn.disabled = true; btn.innerHTML = `<span class="spin"></span>${esc(label)}`;
  try { return await fn(); } finally { if (btn.isConnected) { btn.disabled = false; btn.innerHTML = old; } }
}

// ── Seznam produktů "Naše zeleň" (zákazník i admin) ──
export function productCard(p, { admin = false } = {}) {
  const img = productImg(p);
  return `<article class="product${admin ? " tappable" : ""}" ${admin ? `data-act="editProduct" data-id="${esc(p.id)}"` : ""}>
    <div class="product-img">${img ? `<img src="${esc(img)}" alt="" loading="lazy">` : icon("leaf")}</div>
    <div class="product-text">
      <h4>${esc(p.name)}</h4>
      <p>${esc(p.description)}</p>
      ${(p.forms || []).length ? `<div class="chips small">${p.forms.map((f) => `<span class="chip">${esc(f)}</span>`).join("")}</div>` : ""}
    </div>
    ${admin ? `<span class="product-edit">${icon("edit")}</span>` : ""}
  </article>`;
}

export const isIOS = () => /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
export const isStandalone = () => window.matchMedia("(display-mode: standalone)").matches || navigator.standalone === true;

/** Návod "Přidat na plochu" pro iPhone v Safari (jednou za čas). */
export function installHint(key) {
  if (!isIOS() || isStandalone()) return "";
  try { if (localStorage.getItem(key)) return ""; } catch { /* soukromý režim */ }
  return `<div class="install-hint" data-hint="${esc(key)}">
    <span class="ih-icon">${icon("share")}</span>
    <span>Přidej si appku na plochu: klepni na <b>Sdílet</b> a pak <b>Přidat na plochu</b>.</span>
    <button class="icon-btn" data-act="dismissHint" aria-label="Zavřít">${icon("close")}</button>
  </div>`;
}
export function dismissHint(el) {
  const box = el.closest(".install-hint");
  try { localStorage.setItem(box.dataset.hint, "1"); } catch { /* nevadí */ }
  box.remove();
}

/** Obrazovka nesmí zhasnout, když prodavač skenuje kód (kde to prohlížeč umí). */
export async function keepAwake() {
  try { return await navigator.wakeLock?.request("screen"); } catch { return null; }
}
