// Sestaví webovou appku do hosting/app a hosting/admin:  npm run build
// Výsledek nasadíš:  firebase deploy --only hosting
import { build } from "esbuild";
import { cpSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";

const out = new URL("../hosting/", import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1");
const v = Date.now().toString(36);   // verze proti starým souborům v cache

await build({
  entryPoints: { "app/customer": "src/customer.js", "admin/admin": "src/admin.js" },
  bundle: true, minify: true, format: "esm", target: ["safari15", "chrome100"],
  outdir: out, legalComments: "none", logLevel: "info",
});

mkdirSync(out + "app", { recursive: true });
mkdirSync(out + "admin", { recursive: true });
cpSync("static/app", out + "app", { recursive: true });
cpSync("static/admin", out + "admin", { recursive: true });
for (const f of ["app/index.html", "admin/index.html"]) {
  writeFileSync(out + f, readFileSync(out + f, "utf8").replaceAll("__V__", v));
}
console.log("Hotovo, verze", v);
