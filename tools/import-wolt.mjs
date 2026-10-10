// Jednorázový import nabídky z Woltu do "Naše zeleň" (Firestore products + obrázky do Storage).
// Spuštění (Windows, přihlášený firebase CLI):  firebase projects:list  &&  node tools/import-wolt.mjs [--dry]
// Bere token z firebase CLI (~/.config/configstore/firebase-tools.json).
import { readFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";

const PROJECT = "cannaapp-e4e8b";
const BUCKET = "cannaapp-e4e8b.firebasestorage.app";
const SLUG = "cannaclub-cannabis-cbd";
const DRY = process.argv.includes("--dry");

// Wolt kategorie → kategorie v appce (pořadí = pořadí v appce)
const CATEGORIES = [
  ["CANNABIS", "Konopí"], ["CBD", "CBD"], ["KOSMETIKA", "Kosmetika"], ["KUŘÁCKÉ POTŘEBY", "Kuřácké potřeby"],
  ["PRO MAZLÍČKY", "Pro mazlíčky"], ["PITÍ", "Pití"], ["DOPLŇKY", "Doplňky"],
];
// Odrůdy, které už v appce jsou (s fotkou sklenice) — jen doplníme popis a formy
const SEEDED = { "AK-47": "seed_ak47", "Lemon Haze": "seed_lemon_haze", "Orange Bud": "seed_orange_bud", "Skunk Candy": "seed_skunk_candy" };

const token = JSON.parse(readFileSync(join(homedir(), ".config/configstore/firebase-tools.json"), "utf8")).tokens.access_token;
const auth = { Authorization: `Bearer ${token}` };

const res = await fetch(`https://consumer-api.wolt.com/consumer-api/consumer-assortment/v1/venues/slug/${SLUG}/assortment?language=cs`, { headers: { "User-Agent": "Mozilla/5.0" } });
const data = await res.json();
const items = new Map(data.items.map((i) => [i.id, i]));
const clean = (s) => String(s || "").replace(/\s+\n/g, "\n").replace(/[ \t]+/g, " ").trim();
const short = (s, n = 420) => { s = clean(s); if (s.length <= n) return s; const cut = s.slice(0, n); return cut.slice(0, Math.max(cut.lastIndexOf(". ") + 1, cut.lastIndexOf(" "))).trim() + (cut.endsWith(".") ? "" : "…"); };
const catName = (raw) => CATEGORIES.find(([k]) => raw.toUpperCase().includes(k))?.[1];

const products = [];   // { id, name, description, forms, category, image, seeded }
const seen = new Set();
for (const c of data.categories) {
  const category = catName(c.name);
  if (!category) { console.warn("Neznámá kategorie", c.name); continue; }
  const ids = [...(c.item_ids || []), ...(c.subcategories || []).flatMap((s) => s.item_ids || [])];
  for (const id of ids) {
    const it = items.get(id); if (!it) continue;
    let name = clean(it.name), forms = [];
    if (category === "Konopí") {
      const m = name.match(/^(.*?)\s+(\d+g)$/);
      if (m) { name = m[1]; forms = [m[2]]; }
      const prev = products.find((p) => p.category === "Konopí" && p.name === name);
      if (prev) { forms.forEach((f) => prev.forms.includes(f) || prev.forms.push(f)); continue; }
    }
    if (seen.has(category + name)) continue;
    seen.add(category + name);
    const seedId = category === "Konopí" ? SEEDED[name] : null;
    products.push({
      id: seedId || `wolt_${id}`, name, description: short(it.description), forms, category,
      image: it.images?.[0]?.url || "", seeded: Boolean(seedId),
    });
  }
}
products.forEach((p, i) => { p.orderIndex = i; });
console.log(`Produktů: ${products.length}`, Object.fromEntries(CATEGORIES.map(([, n]) => [n, products.filter((p) => p.category === n).length])));
if (DRY) { console.log(products.map((p) => `${p.category} | ${p.name} | ${p.forms.join(",")} | ${p.description.slice(0, 50)}`).join("\n")); process.exit(0); }

// ── Obrázky → Storage (products/{id}.jpg, veřejně čitelné podle storage.rules) ──
const publicUrl = (id) => `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/${encodeURIComponent(`products/${id}.jpg`)}?alt=media`;
for (const p of products) {
  if (p.seeded || !p.image) continue;
  const img = await fetch(`${p.image}?w=600`);
  if (!img.ok) { console.warn("Obrázek nejde stáhnout", p.name); p.image = ""; continue; }
  const up = await fetch(`https://storage.googleapis.com/upload/storage/v1/b/${BUCKET}/o?uploadType=media&name=${encodeURIComponent(`products/${p.id}.jpg`)}`, {
    method: "POST", headers: { ...auth, "Content-Type": img.headers.get("content-type") || "image/jpeg" }, body: Buffer.from(await img.arrayBuffer()),
  });
  if (!up.ok) throw new Error(`Upload ${p.name}: ${up.status} ${await up.text()}`);
  p.imageUrl = publicUrl(p.id);
}

// ── Firestore (po 400 zápisech) ──
const str = (v) => ({ stringValue: v }), int = (v) => ({ integerValue: String(v) });
const writes = products.map((p) => {
  const fields = {
    id: str(p.id), name: str(p.name), description: str(p.description), category: str(p.category),
    forms: { arrayValue: { values: p.forms.map(str) } }, orderIndex: int(p.orderIndex),
  };
  if (!p.seeded) Object.assign(fields, { imageUrl: str(p.imageUrl || ""), imageName: str("") });
  const w = { update: { name: `projects/${PROJECT}/databases/(default)/documents/products/${p.id}`, fields } };
  if (p.seeded) w.updateMask = { fieldPaths: Object.keys(fields) };   // fotka sklenice zůstane
  return w;
});
for (let i = 0; i < writes.length; i += 400) {
  const r = await fetch(`https://firestore.googleapis.com/v1/projects/${PROJECT}/databases/(default)/documents:commit`, {
    method: "POST", headers: { ...auth, "Content-Type": "application/json" }, body: JSON.stringify({ writes: writes.slice(i, i + 400) }),
  });
  if (!r.ok) throw new Error(`Firestore: ${r.status} ${await r.text()}`);
}
console.log("Hotovo.");
