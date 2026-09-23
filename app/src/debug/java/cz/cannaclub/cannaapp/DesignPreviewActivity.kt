package cz.cannaclub.cannaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.Timestamp
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.model.TransactionType
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.ui.theme.CannaAppTheme
import cz.cannaclub.cannaapp.ui.user.DashboardContent
import cz.cannaclub.cannaapp.ui.user.DashboardOverlay
import java.util.Date

/**
 * Jen pro vývoj (debug build): obrazovky s ukázkovými daty, bez přihlášení a bez zápisu do Firebase.
 *
 *   adb shell am start -n cz.cannaclub.cannaapp/.DesignPreviewActivity --es screen rewards
 *
 * screen = dashboard | empty | rewards | profile | card | celebrate | onboarding | admin | login | register  | splash | products
 */
class DesignPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        val screen = intent.getStringExtra("screen") ?: "dashboard"

        val user = User(
            id          = "demo",
            name        = "Jana Nováková",
            email       = "jana.novakova@email.cz",
            phone       = "+420 777 123 456",
            points      = 114,
            totalPoints = 640,
            memberCode  = "290362013070"
        )
        val now = System.currentTimeMillis()
        fun ago(h: Long) = Timestamp(Date(now - h * 3_600_000))
        val tx = listOf(
            Transaction("1", TransactionType.ADD,      64, "Nákup 640 Kč",          ago(0)),
            Transaction("2", TransactionType.SUBTRACT, 40, "Odměna: 3× preroll",    ago(1)),
            Transaction("3", TransactionType.ADD,      38, "Nákup 385 Kč",          ago(26)),
            Transaction("4", TransactionType.ADD,      20, "Bonus za registraci",   ago(24 * 5)),
            Transaction("5", TransactionType.ADD,      32, "Nákup 320 Kč",          ago(24 * 9)),
        )

        if (screen == "login" || screen == "register") {
            setContent {
                CannaAppTheme {
                    val vm: cz.cannaclub.cannaapp.viewmodel.UserViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    if (screen == "login") cz.cannaclub.cannaapp.ui.user.LoginScreen(
                        viewModel = vm, onLoginSuccess = {}, onAdminClick = {}, onRegisterClick = {}
                    ) else cz.cannaclub.cannaapp.ui.user.RegisterScreen(viewModel = vm, onRegistered = {}, onBack = {})
                }
            }
            return
        }
        if (screen == "products") {
            setContent {
                CannaAppTheme {
                    cz.cannaclub.cannaapp.ui.components.ProductShowcaseScreen(
                        viewModel = androidx.lifecycle.viewmodel.compose.viewModel(), isAdmin = false, onBack = {}
                    )
                }
            }
            return
        }
        if (screen == "splash") {
            setContent { CannaAppTheme { cz.cannaclub.cannaapp.ui.user.SplashScreen(onFinished = {}) } }
            return
        }

        if (screen == "admin") {
            val vm = cz.cannaclub.cannaapp.viewmodel.AdminViewModel()
            fun u(n: String, e: String, p: Int, t: Int, d: Long) =
                User(id = n, name = n, email = e, points = p, totalPoints = t, memberCode = "29000000000$d",
                     createdAt = ago(d * 24))
            vm.setUsersForPreview(listOf(
                u("Jana Nováková",  "jana.novakova@email.cz", 114, 640, 2),
                u("Petr Svoboda",   "petr.svoboda@seznam.cz", 32, 1210, 40),
                u("Lucie Černá",    "lucie.cerna@gmail.com", 0, 0, 1),
                u("Tomáš Dvořák",   "tomas.dvorak@email.cz", 268, 2710, 90),
                u("Karel Veselý",   "karel.vesely@post.cz", 51, 290, 12),
            ))
            setContent {
                CannaAppTheme {
                    cz.cannaclub.cannaapp.ui.admin.AdminListScreen(viewModel = vm, onLogout = {})
                }
            }
            return
        }

        setContent {
            CannaAppTheme {
                DashboardContent(
                    user                 = user,
                    transactions         = if (screen == "empty") emptyList() else tx,
                    celebration          = if (screen == "celebrate") 64 else null,
                    showOnboarding       = screen == "onboarding",
                    onCelebrationShown   = {},
                    onOnboardingFinished = {},
                    onOpenOnboarding     = {},
                    onLogout             = {},
                    onProductsClick      = {},
                    initialOverlay       = when (screen) {
                        "rewards" -> DashboardOverlay.REWARDS
                        "profile" -> DashboardOverlay.PROFILE
                        "card"    -> DashboardOverlay.CARD
                        else      -> DashboardOverlay.NONE
                    }
                )
            }
        }
    }
}
