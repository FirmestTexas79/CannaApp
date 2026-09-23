#!/usr/bin/env node
/**
 * Kontrola zákazníků v Dotykačce: kolik jich má čárový kód a jestli tam někdo není dvakrát.
 *   node tools/dotykacka-kontrola.mjs <REFRESH_TOKEN> 304899966
 * Jen čte, nic nemění.
 */
const [rt, cloud = "304899966"] = process.argv.slice(2);
if (!rt) { console.log("Použití: node tools/dotykacka-kontrola.mjs <REFRESH_TOKEN> [CLOUD_ID]"); process.exit(1); }
const API = "https://api.dotykacka.cz/v2";

const t = await fetch(`${API}/signin/token`, {
  method: "POST",
  headers: { Authorization: `User ${rt}`, "Content-Type": "application/json" },
  body: JSON.stringify({ _cloudId: cloud }),
});
if (!t.ok) { console.log("Přihlášení selhalo:", t.status); process.exit(1); }
const { accessToken } = await t.json();

let all = [];
for (let p = 1; p <= 50; p++) {
  const r = await fetch(`${API}/clouds/${cloud}/customers?limit=100&page=${p}`, { headers: { Authorization: `Bearer ${accessToken}` } });
  if (r.status === 404) break;
  const j = await r.json();
  all.push(...j.data);
  if (j.data.length < 100) break;
}
all = all.filter((c) => !c.deleted);

const name = (c) => `${c.firstName || ""} ${c.lastName || ""}`.trim() || c.companyName || "(bez jména)";
const code = (c) => String(c.barcode || "").trim();
const hasEmail = (c) => String(c.email || "").includes("@");

const withEmail = all.filter(hasEmail);
const missing = withEmail.filter((c) => !code(c));
const counts = {};
all.forEach((c) => { const k = name(c).toLowerCase(); counts[k] = (counts[k] || 0) + 1; });
const dups = Object.entries(counts).filter(([, n]) => n > 1);

console.log(`\nZákazníků v Dotykačce:        ${all.length}`);
console.log(`  s e-mailem:                 ${withEmail.length}`);
console.log(`  s e-mailem a čárovým kódem: ${withEmail.length - missing.length}`);
console.log(`  bez e-mailu:                ${all.length - withEmail.length}`);
console.log(`  vytvořeno z appky (tag):    ${all.filter((c) => (c.tags || []).includes("CannaApp")).length}`);

console.log(missing.length ? `\n⚠️  S e-mailem, ale BEZ čárového kódu (${missing.length}):` : "\n✅ Všichni s e-mailem mají čárový kód.");
missing.forEach((c) => console.log(`   - ${name(c)} <${c.email}>`));

console.log(dups.length ? `\n⚠️  Stejné jméno vícekrát (${dups.length}):` : "\n✅ Nikdo tam není dvakrát.");
dups.forEach(([n, k]) => console.log(`   - ${n}: ${k}×`));
console.log();
