# Napojení CannaApp na Dotykačku — návod krok za krokem

## Jak to funguje

1. Každý zákazník v appce má **členský kód** (12 číslic, začíná `29`). Appka ho ukazuje jako čárový kód s číslem pod ním. Přečte ho laserová i 2D čtečka, a když čtečka nezabere, dá se číslo opsat. Klepnutím se zvětší na celou obrazovku s jasem na maximum.
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

V `functions/.env` už jsou vyplněné hodnoty, které byly dřív v kódu (Cloud ID `304899966`, Branch ID `191083401` — ověřeno testem).

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

## Převzetí stávajících zákazníků z Dotykačky

V admin appce dole klikni na **Převzít zákazníky z pokladny**. Appka nejdřív jen spočítá, kolik zákazníků se převezme, a nic nezakládá. Teprve po potvrzení založí účty.

- Převezmou se zákazníci **s e-mailem**, kteří v appce ještě nejsou. Bez e-mailu to nejde, protože e-mail slouží k přihlášení.
- Kdo má v Dotykačce čárový kód (třeba plastovou kartu), dostane ho jako členský kód. Ostatním se kód vygeneruje a server ho do pár minut zapíše do Dotykačky.
- Body začínají na nule.
- Převzatý zákazník se v appce přihlásí e-mailem a telefonem. Když v Dotykačce telefon nemá, přihlásí se e-mailem a jménem a zadaný telefon se mu uloží.
- Import jde spustit opakovaně, už převzaté zákazníky přeskočí.

## Chování, o kterém obsluha musí vědět

- Zákazníka **naskenuj před zaplacením**. Zaplacený účet už zákazníka nepřijme (Dotykačka to neumožňuje).
- Když čtečka telefon nepřečte: ať zákazník klepne na kód (zvětší se a zjasní). Když to pořád nejde, naskenuj ho v admin appce.
- Nový zákazník se do pokladny propisuje desítky sekund. Když ho čtečka hned po registraci nenajde, chvíli počkej.
- Body se počítají jen za nákupy **od nasazení dál**, zpětně ne.

---

## Akce, bonusy za rank a zprávy zákazníkům

**Bonus za rank** běží automaticky: podle ranku zákazníka dostane za každý nákup víc bodů. Bronzový +5 %, Stříbrný +10 %, Zlatý +15 %, Rodina +20 %. Počítá se podle ranku před nákupem. Procenta jsou v kódu: `RANK_BONUS` ve `functions/loyalty.js` a stejná hodnota `DEFAULT_RANK_BONUS` v `LoyaltyConfig.kt`.

V admin appce je nahoře dlaždice **Akce a zprávy**:

- **Akce „více bodů“**: 2× nebo 3× body za nákup na zvolenou dobu (do konce dne, 24 hodin, do neděle, 7 dní). Zákazníci v appce uvidí banner a můžou dostat upozornění. Rozhoduje čas, kdy se účet na pokladně založil, takže se započítají i nákupy, které server zpracuje až po konci akce. Akce se sčítá s bonusem za rank.
- **Zpráva zákazníkům**: push notifikace všem, nebo jen od určitého ranku. Před odesláním appka ukáže, kolik lidí ji dostane. Odeslané zprávy se ukládají do kolekce `broadcasts`.

Příklad: nákup za 640 Kč, rank Zlatý, běží dvojité body → 64 b + 64 b akce + 19 b za rank = **147 b**. Zákazník rozpis vidí v pohybech bodů.

Běžící akce je uložená ve Firestore v `config/loyalty` a zapisuje ji jen server (funkce `adminUpdateLoyalty`, `adminBroadcast`, kód v `functions/loyalty.js`). Aby to fungovalo, je potřeba jednou `firebase deploy --only functions`.

---

## Google Peněženka (kartička v Google Wallet)

Zákazník klepne v appce na **Přidat do Peněženky Google** a kartička s čárovým kódem se mu uloží do Peněženky. Ukáže ji u pokladny i bez otevření appky, případně rovnou ze zamčené obrazovky. Body, sleva a rank se na kartičce aktualizují samy při každé změně (funkce `syncWalletPass`).

Jak to funguje: kartičku zakládá a podepisuje server (`functions/wallet.js`), appka jen dostane podepsaný odkaz. Klíč servisního účtu je proto jen v Secret Manageru, stejně jako token Dotykačky.

### Co je potřeba jednou nastavit (cca 20 minut)

1. **Účet vydavatele:** otevři https://pay.google.com/business/console, přihlas se a založ firemní profil (CannaClub, adresa prodejny). V sekci **Google Wallet API** najdeš **Issuer ID**, dlouhé číslo.
2. **Zapnout API:** v https://console.cloud.google.com vyber projekt `cannaapp-e4e8b` → *APIs & Services* → *Library* → vyhledej **Google Wallet API** → *Enable*.
3. **Servisní účet:** tamtéž *IAM & Admin* → *Service Accounts* → *Create service account*, název třeba `wallet-issuer`. Role nepřidávej, dej rovnou *Done*. Pak ho otevři → *Keys* → *Add key* → *Create new key* → **JSON**. Stáhne se soubor s klíčem.
4. **Pustit servisní účet k Peněžence:** zpátky v Pay & Wallet Console → *Users* → pozvi e-mail servisního účtu (`wallet-issuer@cannaapp-e4e8b.iam.gserviceaccount.com`) s rolí **Developer**.
5. **Issuer ID** vlož do `functions/.env`:
   ```
   WALLET_ISSUER_ID=3388000000012345678
   ```
6. **Klíč do Secret Manageru** (a pak stažený JSON smaž, do gitu nepatří):
   ```
   firebase functions:secrets:set WALLET_SA_KEY --data-file C:\Users\matas\Downloads\klic.json
   ```
7. **Nasazení:** `firebase deploy --only functions`. Přibydou 3 funkce: `walletPassJwt`, `syncWalletPass`, `walletAsset`.

> Pozor: dokud neexistuje secret `WALLET_SA_KEY`, deploy funkcí selže. Pokud chceš nasadit něco jiného dřív, než máš Peněženku nastavenou, vlož do secretu dočasně libovolný text (`firebase functions:secrets:set WALLET_SA_KEY` a napiš třeba `zatim-ne`). Tlačítko v appce pak jen ohlásí, že Peněženka ještě není nastavená.

### Testování a zveřejnění

- Nový účet vydavatele je v **demo režimu**: kartičku si uloží jen účty přidané v Pay & Wallet Console (*Users* nebo *Test accounts*). Přidej tam svůj Google účet a vyzkoušej to.
- Pro všechny zákazníky: v Pay & Wallet Console → *Google Wallet API* → **Request publishing access**. Google žádost ručně schvaluje (obvykle pár dní). Vzhledem k sortimentu (CBD/konopí) může být schvalování přísnější, v žádosti uveď, že jde o věrnostní program kamenné prodejny pro dospělé.
- Vzhled kartičky: zelená `#3E5E35`, logo a banner jsou v `functions/wallet-assets/` (Google si je stahuje přes funkci `walletAsset`). Když je změníš, zvyš `CLASS_VERSION` ve `wallet.js` a kartičky se přepíšou.

| Chyba v logu `walletPassJwt` | Příčina |
|---|---|
| Google Wallet 403 | Servisní účet není pozvaný v Pay & Wallet Console, nebo není zapnuté Google Wallet API |
| Google odmítl přihlášení servisního účtu | Špatný nebo neúplný JSON v `WALLET_SA_KEY` |
| chybí WALLET_ISSUER_ID | Není vyplněné v `functions/.env` (a znovu nasadit) |

## Soubory

- `functions/index.js` – serverová logika (Dotykačka, body, notifikace)
- `functions/.env` – Cloud ID, Branch ID, kurz bodů (netajné)
- `tools/dotykacka-connector.html` – získání Refresh Tokenu
- `tools/dotykacka-test.mjs` – ověření údajů a zjištění Branch ID
- `app/.../repository/DotykackaRepository.kt` – appka jen volá funkci `assignCustomerToOrder`
- `functions/loyalty.js` – bonus za rank, akce „více bodů“, hromadné zprávy
- `functions/wallet.js`, `functions/wallet-assets/` – kartička v Google Peněžence
- `app/.../repository/WalletRepository.kt` – tlačítko Přidat do Peněženky Google

Dokumentace Dotykačky: https://docs.api.dotypos.com (Authorization, POS Actions, Order, Customer).
