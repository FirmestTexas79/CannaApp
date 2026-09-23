# Firebase Cloud Functions — CannaApp

## Funkce: onOrderCompleted

Tato funkce se zavolá automaticky když Dotykačka dokončí objednávku.
Vypočítá body (hodnota objednávky ÷ 10) a přičte je zákazníkovi.

### Webhook endpoint
Po nasazení bude dostupný na:
https://europe-west1-cannaapp-e4e8b.cloudfunctions.net/onOrderCompleted

### Tento URL zadáš do Dotykačka administrace jako webhook URL.

### Nasazení
1. Nainstaluj Firebase CLI: npm install -g firebase-tools
2. firebase login
3. firebase init functions (vyber projekt cannaapp-e4e8b)
4. Zkopíruj kód níže do functions/index.js
5. firebase deploy --only functions

### Kód funkce (functions/index.js)
```javascript
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onOrderCompleted = functions
  .region("europe-west1")
  .https.onRequest(async (req, res) => {

    // Ověření že request přišel od Dotykačky
    // TODO: doplnit secret token po obdržení API klíče
    // const authHeader = req.headers.authorization
    // if (authHeader !== `Bearer ${SECRET_TOKEN}`) {
    //   return res.status(401).send("Unauthorized")
    // }

    try {
      const { customerId, orderTotal, orderId } = req.body

      if (!customerId || !orderTotal) {
        return res.status(400).send("Missing customerId or orderTotal")
      }

      // Výpočet bodů: 1 bod za každých 10 Kč
      const pointsToAdd = Math.floor(orderTotal / 10)

      if (pointsToAdd <= 0) {
        return res.status(200).send("No points to add")
      }

      const db = admin.firestore()

      // Najdi zákazníka podle dotykackaId
      const usersSnapshot = await db.collection("users")
        .where("dotykackaId", "==", customerId)
        .limit(1)
        .get()

      if (usersSnapshot.empty) {
        return res.status(404).send("Customer not found")
      }

      const userDoc = usersSnapshot.docs[0]
      const userData = userDoc.data()
      const currentPoints = userData.points || 0
      const currentTotal  = userData.totalPoints || 0

      // Atomický batch zápis
      const batch = db.batch()

      // 1. Aktualizuj body zákazníka
      batch.update(userDoc.ref, {
        points:      currentPoints + pointsToAdd,
        totalPoints: currentTotal  + pointsToAdd
      })

      // 2. Zapiš transakci
      const txRef = userDoc.ref.collection("transactions").doc()
      batch.set(txRef, {
        id:        txRef.id,
        type:      "ADD",
        amount:    pointsToAdd,
        reason:    `Nákup ${orderTotal} Kč`,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      })

      await batch.commit()

      console.log(`✅ Přidáno ${pointsToAdd} bodů zákazníkovi ${userDoc.id}`)
      return res.status(200).json({
        success:   true,
        userId:    userDoc.id,
        points:    pointsToAdd,
        newTotal:  currentPoints + pointsToAdd
      })

    } catch (error) {
      console.error("❌ Chyba:", error)
      return res.status(500).send("Internal error")
    }
  })
```