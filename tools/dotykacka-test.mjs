#!/usr/bin/env node
/**
 * Ověří, že údaje k Dotykačce fungují, a vypíše ID poboček.
 *
 *   node tools/dotykacka-test.mjs <REFRESH_TOKEN> <CLOUD_ID> [BRANCH_ID]
 *
 * Nic neukládá a nic v pokladně nemění (jen čte + "hello" na pokladnu).
 */

const [refreshToken, cloudId, branchArg] = process.argv.slice(2);
if (!refreshToken || !cloudId) {
  console.log("Použití: node tools/dotykacka-test.mjs <REFRESH_TOKEN> <CLOUD_ID> [BRANCH_ID]");
  process.exit(1);
}

const API = "https://api.dotykacka.cz/v2";
const ok = (t) => console.log("  ✅ " + t);
const bad = (t) => { console.log("  ❌ " + t); process.exit(1); };

async function main() {
  console.log("\n1) Přihlášení (Refresh Token → Access Token)");
  const signin = await fetch(`${API}/signin/token`, {
    method: "POST",
    headers: { "Authorization": `User ${refreshToken}`, "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({ _cloudId: String(cloudId) }),
  });
  const signinText = await signin.text();
  if (!signin.ok) bad(`HTTP ${signin.status}: ${signinText}\n     → špatný Refresh Token, nebo token není pro tento Cloud ID`);
  const { accessToken } = JSON.parse(signinText);
  ok("Access Token získán");

  const get = async (path) => {
    const r = await fetch(`${API}/clouds/${cloudId}${path}`, {
      headers: { "Authorization": `Bearer ${accessToken}`, "Accept": "application/json" },
    });
    const text = await r.text();
    return { status: r.status, json: text ? JSON.parse(text) : null, text };
  };

  console.log("\n2) Pobočky (BRANCH_ID)");
  const br = await get("/branches?limit=100");
  if (br.status !== 200) bad(`HTTP ${br.status}: ${br.text}`);
  const branches = br.json.data || [];
  for (const b of branches) console.log(`     ${b.id}   ${b.name}${b.deleted ? "  (smazaná)" : ""}`);
  const branchId = branchArg || (branches.filter((b) => !b.deleted).length === 1 ? branches.find((b) => !b.deleted).id : null);
  if (!branchId) {
    console.log("\n  Poboček je víc — spusť znovu a jako 3. parametr dej ID té, kde stojí pokladna.");
    return;
  }
  ok(`Používám pobočku ${branchId}`);

  console.log("\n3) Zákazníci");
  const cu = await get("/customers?limit=1");
  if (cu.status === 200 || cu.status === 404) ok("Čtení zákazníků funguje");
  else bad(`HTTP ${cu.status}: ${cu.text}\n     → aplikace nemá oprávnění ke Customer`);

  console.log("\n4) Spojení s pokladnou (POS akce order/hello)");
  const pos = await fetch(`${API}/clouds/${cloudId}/branches/${branchId}/pos-actions`, {
    method: "POST",
    headers: { "Authorization": `Bearer ${accessToken}`, "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({ action: "order/hello" }),
  });
  const posText = await pos.text();
  if (pos.status === 404) bad("Pokladna neodpověděla do 21 s — je zapnutá, online a přihlášená?");
  if (!pos.ok) bad(`HTTP ${pos.status}: ${posText}\n     → aplikace možná nemá povolené POS akce (napiš podpoře Dotykačky)`);
  const hello = posText ? JSON.parse(posText) : {};
  ok(`Pokladna odpověděla: ${hello.appName || ""} ${hello.version?.name || ""}, směna: ${hello.registerStatus || "?"}`);
  if (hello.registerStatus === "closed") console.log("     (směna je zavřená — pro přiřazování zákazníků ji otevři)");

  console.log(`\nVšechno funguje. Do functions/.env dej:\n  DOTYKACKA_CLOUD_ID=${cloudId}\n  DOTYKACKA_BRANCH_ID=${branchId}\n`);
}

main().catch((e) => bad(e.message));
