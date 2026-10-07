# Vydání CannaClub na Google Play

## 1. Jednorázově ve Firebase (před nasazením)

1. **Firebase konzole → Authentication → Sign-in method → Anonymous → Povolit.**
   Zákazníci se přihlašují anonymně (neviditelně); e-mail + telefon ověřuje server.
2. (hotovo) Kontakt v zásadách = adresa prodejny.

## 2. Nasazení — v tomhle pořadí

```
firebase deploy --only functions
```
Pak nainstaluj **novou verzi appky** na svůj telefon a tablet obsluhy (stará verze se po kroku níž nepřihlásí).
```
firebase deploy --only firestore:rules,storage,hosting
```
- Pravidla: zákazník vidí jen svůj účet, body mění jen server a obsluha. Test: `tools/rules-test` → `npm install` → `npm test`.
- Hosting: https://cannaapp-e4e8b.web.app/ochrana-udaju.html a https://cannaapp-e4e8b.web.app/smazani-uctu.html
- Při prvním nasazení storage se CLI zeptá na oprávnění pro pravidla napříč službami (Storage čte Firestore) → **Yes**.

## 3. Podepsaný balíček (AAB)

Android Studio → **Build → Generate Signed App Bundle or APK → Android App Bundle**
- *Create new…* klíč: ulož ho **mimo projekt** (např. `Dokumenty\klice\cannaclub-upload.jks`), heslo do správce hesel. **Bez klíče už appku neaktualizuješ.** Zálohuj ho.
- Varianta **release** → výsledek `app/release/app-release.aab`.
- Každá další verze: zvyš `versionCode` (o 1) a `versionName` v `app/build.gradle.kts`.

## 4. Play Console

**Vytvořit aplikaci:** název *CannaClub*, výchozí jazyk čeština, Aplikace, Zdarma.

**Nastavení aplikace (Dashboard → Set up your app):**
| Položka | Co vyplnit |
|---|---|
| Přístup k aplikaci | Část funkcí vyžaduje přihlášení → přidej testovací účet zákazníka (jméno, e-mail, telefon) + pokyn „Přihlášení: jméno, e-mail, telefon“ |
| Reklamy | Ne |
| Hodnocení obsahu | Dotazník poctivě (zmínka o konopí/CBD) → vyjde 18+ |
| Cílová skupina | 18+ |
| Bezpečnost dat | Sbíráme: jméno, e-mail, telefon, historie nákupů (věrnostní program), ID zařízení pro notifikace. Nesdílíme, neprodáváme, šifrováno při přenosu, smazání: **ano** |
| Smazání účtu | URL: `https://cannaapp-e4e8b.web.app/smazani-uctu.html` |
| Zásady ochrany soukromí | `https://cannaapp-e4e8b.web.app/ochrana-udaju.html` |
| Kategorie | Nakupování (Shopping) |

**Stránka v obchodě:** krátký popis (80 znaků), dlouhý popis, ikona 512×512, banner 1024×500, min. 2 screenshoty telefonu.

## 5. Testování (osobní účet → povinné)

1. **Testování → Uzavřené testování → Vytvořit stopu**, nahraj AAB.
2. Testeři: seznam e-mailů (Gmail/Google účty) — **aspoň 12 lidí**, kteří appku nainstalují přes odkaz z Play a **nechají ji 14 dní v kuse**.
3. Po 14 dnech: Dashboard → **Požádat o přístup k produkci** (pár otázek o testu) → Produkce → nahrát stejný/novější AAB.

Kontrola Googlu trvá pár dní až týden. Aby prošla:
- texty: „CBD a konopné produkty“, ne THC,
- v popisu: věrnostní program kamenné prodejny, žádný prodej přes appku.
