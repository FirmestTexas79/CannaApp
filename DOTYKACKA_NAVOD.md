# Napojení CannaApp na Dotykačku — návod krok za krokem

## Jak to funguje

1. Zákazník si v obchodě vybere zboží, obsluha ho namarkuje do Dotykačky (vznikne otevřený účet).
2. Obsluha v admin appce naskenuje QR kód zákazníka.
3. Server (Cloud Function `assignCustomerToOrder`) najde zákazníka v Dotykačce podle e-mailu, nebo ho založí, a připojí ho k **nejnověji založenému otevřenému účtu bez zákazníka** na pokladně. V kartě zákazníka v appce se ukáže „✓ Připojeno k účtu na pokladně (450 Kč)“.
4. Obsluha účet normálně zaplatí v Dotykačce.
5. Každých 5 minut server (`syncDotykackaOrders`) projde zaplacené účty se zákazníkem a připíše body (výchozí 1 bod za 10 Kč, nastavuje se v `functions/.env`). Každý účet se započítá jen jednou.
6. Při každé změně bodů (nákup i ruční úprava) dostane zákazník push notifikaci (`notifyOnTransaction`).

Appka s Dotykačkou **nekomunikuje přímo**. Refresh token nikdy nevyprší a dává plný přístup k pokladně (tržby, zákazníci, zakládání a placení účtů). Kdyby byl v APK, dostal by ho každý, kdo si appku stáhne a rozbalí. Proto je uložený jen na serveru ve Firebase Secret Manageru.

---

## Co potřebuješ a odkud to vzít

| Údaj | Kde ho vzít | Kam patří | Tajný? |
|---|---|---|---|
| **Client ID** (`cannaapp`) | E-mail od Dotykačky po registraci API aplikace | jen do `tools/dotykacka-connector.html` při kroku 2 | ne |
| **Client Secret** | Stejný e-mail od Dotykačky | jen do `tools/dotykacka-connector.html` při kroku 2, nikam jinam | **ano** |
| **Refresh Token** | Krok 2: vrátí ho Dotykačka po přihlášení majitele | Firebase Secret Manager (krok 4) | **ano, nejvíc** |
| **Cloud ID** | Krok 2: vrátí ho Dotykačka spolu s tokenem (`cloudid=…`) | `functions/.env` → `DOTYKACKA_CLOUD_ID` | ne |
| **Branch ID** | Krok 3: vypíše ho testovací skript | `functions/.env` → `DOTYKACKA_BRANCH_ID` | ne |

V `functions/.env` už jsou vyplněné hodnoty, které byly dřív v kódu (`304899966` / `191033401`). Krok 3 ověří, jestli sedí.

---

## Krok 1: Kontrola před návštěvou

- [ ] Máš **Client ID a Client Secret** (e-mail od Dotykačky). Pokud Secret nemáš, požádej o něj podporu Dotykačky (kontakt je v tom e-mailu). Bez něj se token získat nedá.
- [ ] Na svém počítači máš **Node.js 22** a **Firebase CLI** (`npm i -g firebase-tools`, pak `firebase login`).
- [ ] Projekt ve Firebase je na tarifu **Blaze** (už ho máš, jinak by nešly nasadit původní funkce).
- [ ] Domluv se s majitelem: bude potřeba **jeho přihlášení do Dotykačky** (e-mail + heslo k admin.dotykacka.cz), cca 5 minut.
- [ ] Pokladna musí mít **Dotypos 2.17 nebo novější** (Nastavení → O aplikaci).

## Krok 2: Refresh Token (u majitele, na počítači)

1. Otevři v prohlížeči soubor `tools/dotykacka-connector.html` (dvojklik). Funguje offline, nic se nikam neodesílá kromě Dotykačky.
2. Vyplň **Client ID** (`cannaapp`) a **Client Secret** a klikni na **Přihlásit se do Dotykačky**.
3. V novém okně se **přihlásí majitel**, vybere provozovnu Cannaclub a potvrdí přístup.
4. Okno skončí na `https://dotykacka.cz/?token=XXXX&cloudid=YYYY`. **Zkopíruj celou adresu z adresního řádku** a vlož ji do pole na stránce konektoru. Stránka ti token a Cloud ID vypíše.

> **Proč to minule nefungovalo:** starý postup používal `GET /client/connect`, který je zrušený. Nově se musí poslat POST s HMAC podpisem (`HMAC-SHA256(client_secret, timestamp)`). Přesně tohle stránka dělá. Kód navíc volal neexistující `/v2/auth/token` místo `/v2/signin/token`.
>
> **„The connection has expired“:** hodiny v počítači nejdou přesně. Zapni automatický čas a zkus to znovu.
>
> **„Unknown client application or wrong application secret“:** překlep v Client ID nebo Secretu.

## Krok 3: Ověření údajů

V kořeni repa spusť:

```bash
node tools/dotykacka-test.mjs <REFRESH_TOKEN> <CLOUD_ID>
```

Skript zkontroluje přihlášení, vypíše **pobočky s jejich ID**, ověří čtení zákazníků a pošle na pokladnu `hello`. Na konci napíše, co dát do `functions/.env`. Pokud má cloud víc poboček, spusť ho znovu s ID té správné jako třetím parametrem.

| Chyba | Příčina |
|---|---|
| ❌ u kroku 1, HTTP 401 | Token je pro jiný cloud, nebo je zkopírovaný špatně (chybí konec) |
| ❌ Pokladna neodpověděla | Pokladna je vypnutá nebo offline. Zapni ji a zkus znovu |
| ❌ HTTP 403 u POS akcí | Aplikace `cannaapp` nemá povolené POS akce. Napiš podpoře Dotykačky pro API |

## Krok 4: Uložení údajů na server

```bash
# 1) Refresh token do Secret Manageru (CLI se na něj zeptá, vlož ho a Enter)
firebase functions:secrets:set DOTYKACKA_REFRESH_TOKEN

# 2) Zkontroluj/uprav functions/.env
#    DOTYKACKA_CLOUD_ID=...
#    DOTYKACKA_BRANCH_ID=...
#    KC_PER_POINT=10
```

Refresh token **nepiš** do `.env`, do kódu ani do gitu.

## Krok 5: Nasazení

```bash
cd functions && npm install && cd ..
firebase deploy --only functions
```

- Funkce běží v `us-central1`, protože Firestore je v `nam5`. Jiný region nefunguje.
- CLI se zeptá, jestli smazat staré funkce, které v kódu už nejsou (`onOrderCompleted` a případně stará funkce na `notifications_queue`). **Odpověz ano.** Nové funkce je nahrazují, a kdyby zůstaly obě, notifikace by chodily dvakrát.
- Při prvním deployi scheduleru se může objevit výzva k povolení Cloud Scheduler API. Potvrď ji.

Zkontroluj ve Firebase konzoli → Functions, že tam jsou 4 funkce: `assignCustomerToOrder`, `dotykackaStatus`, `syncDotykackaOrders`, `notifyOnTransaction`.

## Krok 6: Zkouška naostro

1. Na pokladně otevři směnu a namarkuj cokoliv (třeba za 10 Kč).
2. V admin appce naskenuj QR testovacího zákazníka. Karta by měla ukázat **„✓ Připojeno k účtu na pokladně (10 Kč)“** a na pokladně se u účtu objeví zákazník.
3. Zaplať účet.
4. Do 5 minut přibudou zákazníkovi body a přijde mu notifikace.

První běh `syncDotykackaOrders` jen zapíše startovní čas (`integration/dotykacka` ve Firestore) a zpětně nic nepočítá. Body se připisují za nákupy **od nasazení dál**.

Když něco nejde: Firebase konzole → Functions → Logs (nebo `firebase functions:log`). Chybové hlášky jsou česky.

---

## Chování, o kterém obsluha musí vědět

- **Nejdřív markovat, pak skenovat.** Bez otevřeného účtu napíše appka „Na pokladně není otevřený účet“.
- Když je otevřených účtů bez zákazníka víc, připojí se k **nejnověji založenému** a appka upozorní „otevřených je N, zkontroluj pokladnu“.
- Nový zákazník založený v Dotykačce přes API se do pokladny dostane se zpožděním (desítky sekund). Když appka napíše „Zákazník ještě není v pokladně“, stačí za chvíli ťuknout na **Znovu**.
- Zaplacený účet už zákazníka nepřijme (Dotykačka to neumožňuje). Skenovat se musí před zaplacením.

## Soubory

- `functions/index.js` – serverová logika (Dotykačka, body, notifikace)
- `functions/.env` – Cloud ID, Branch ID, kurz bodů (netajné)
- `tools/dotykacka-connector.html` – získání Refresh Tokenu
- `tools/dotykacka-test.mjs` – ověření údajů a zjištění Branch ID
- `app/.../repository/DotykackaRepository.kt` – appka jen volá funkci `assignCustomerToOrder`

Dokumentace Dotykačky: https://docs.api.dotypos.com (Authorization, POS Actions, Order, Customer).
