# Napojení CannaApp na Dotykačku — návod krok za krokem

## Jak to funguje

1. Každý zákazník v appce má **členský kód** (12 číslic, začíná `29`). Appka ho ukazuje jako QR i jako klasický čárový kód. Klepnutím se zvětší na celou obrazovku s jasem na maximum.
2. Server tenhle kód zapíše zákazníkovi v Dotykačce do pole **Čárový kód** (najde ho podle e-mailu, případně ho založí). Pokud už tam zákazník čárový kód má, třeba z plastové karty, převezme ho naopak appka, takže stará karta funguje dál.
3. Prodavač namarkuje zboží a **naskenuje telefon zákazníka čtečkou na pokladně**. Dotykačka podle čárového kódu sama načte zákazníka na účet.
4. Účet se normálně zaplatí.
5. Každých 5 minut server (`syncDotykackaOrders`) projde zaplacené účty se zákazníkem a připíše body (výchozí 1 bod za 10 Kč, nastavuje se v `functions/.env`). Každý účet se započítá jen jednou. Zákazníkovi přijde notifikace.

**Záloha:** když čtečka telefon nepřečte, prodavač kód naskenuje kamerou v admin appce (📷). Zákazník se pak připojí k nejnovějšímu otevřenému účtu na pokladně (`assignCustomerToOrder`). Kód se dá do vyhledávání v adminu i napsat.

**Čtečka:** telefonní displej přečte jen **2D čtečka (imager)**. Klasické laserové čtečky čárových kódů z displeje většinou nečtou. Zkus to hned při první návštěvě.

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

## Krok 2: Refresh Token (na tvém notebooku s projektem, majitel jen zadá heslo)

Všechno se dělá na **tvém notebooku**, kde je projekt a Firebase CLI. Majitel do otevřeného okna Dotykačky jen napíše svůj e-mail a heslo a potvrdí přístup. Na pokladně ani na jeho počítači se nic neinstaluje. Pokladna musí být jen zapnutá a online, kvůli testu v kroku 3.


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

Zkontroluj ve Firebase konzoli → Functions, že tam je 5 funkcí: `assignCustomerToOrder`, `dotykackaStatus`, `syncDotykackaOrders`, `onUserCreated`, `notifyOnTransaction`.

Při prvním běhu (do 5 minut po deployi) server projde **všechny stávající zákazníky**, přidělí jim členské kódy a zapíše je do Dotykačky. Ve Firestore u každého zákazníka pak uvidíš `memberCode` a `dotykackaSynced: true`. Když se to u někoho nepovede, najdeš u něj `dotykackaSyncError` s důvodem a server to každých 5 minut zkusí znovu.

## Krok 6: Zkouška naostro

1. Počkej 5 minut po deployi a zkontroluj v Dotykačce (admin.dotykacka.cz → Zákazníci), že testovací zákazník má vyplněný **Čárový kód** stejný jako v appce.
2. Na pokladně otevři směnu a namarkuj cokoliv, třeba za 10 Kč.
3. Na telefonu zákazníka klepni na členský kód (zvětší se) a **naskenuj ho čtečkou pokladny**. Na účtu se musí objevit jméno zákazníka.
4. Zaplať účet.
5. Do 5 minut zákazník dostane body a notifikaci.

První běh `syncDotykackaOrders` jen zapíše startovní čas (`integration/dotykacka` ve Firestore) a zpětně nic nepočítá. Body se připisují za nákupy **od nasazení dál**.

Když něco nejde: Firebase konzole → Functions → Logs (nebo `firebase functions:log`). Chybové hlášky jsou česky.

---

## Chování, o kterém obsluha musí vědět

- Zákazníka **naskenuj před zaplacením**. Zaplacený účet už zákazníka nepřijme (Dotykačka to neumožňuje).
- Když čtečka telefon nepřečte: ať zákazník klepne na kód (zvětší se a zjasní). Když to pořád nejde, naskenuj ho v admin appce.
- Nový zákazník se do pokladny propisuje desítky sekund. Když ho čtečka hned po registraci nenajde, chvíli počkej.
- Body se počítají jen za nákupy **od nasazení dál**, zpětně ne.

## Soubory

- `functions/index.js` – serverová logika (Dotykačka, body, notifikace)
- `functions/.env` – Cloud ID, Branch ID, kurz bodů (netajné)
- `tools/dotykacka-connector.html` – získání Refresh Tokenu
- `tools/dotykacka-test.mjs` – ověření údajů a zjištění Branch ID
- `app/.../repository/DotykackaRepository.kt` – appka jen volá funkci `assignCustomerToOrder`

Dokumentace Dotykačky: https://docs.api.dotypos.com (Authorization, POS Actions, Order, Customer).
