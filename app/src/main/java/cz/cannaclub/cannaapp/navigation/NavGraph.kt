package cz.cannaclub.cannaapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.cannaclub.cannaapp.ui.admin.AdminListScreen
import cz.cannaclub.cannaapp.ui.admin.AdminLoginScreen
import cz.cannaclub.cannaapp.ui.components.ProductShowcaseScreen
import cz.cannaclub.cannaapp.ui.user.DashboardScreen
import cz.cannaclub.cannaapp.ui.user.LoginScreen
import cz.cannaclub.cannaapp.viewmodel.AdminViewModel
import cz.cannaclub.cannaapp.viewmodel.ProductViewModel
import cz.cannaclub.cannaapp.viewmodel.UserViewModel

object Routes {
    const val USER_LOGIN        = "user_login"
    const val DASHBOARD         = "dashboard"
    const val ADMIN_LOGIN       = "admin_login"
    const val ADMIN_LIST        = "admin_list"
    const val PRODUCT_SHOWCASE  = "product_showcase"
    const val ADMIN_PRODUCTS    = "admin_products"
}

@Composable
fun CannaNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val userViewModel: UserViewModel     = viewModel()
    val adminViewModel: AdminViewModel   = viewModel()
    val productViewModel: ProductViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = Routes.USER_LOGIN
    ) {

        // ── User Login ────────────────────────────────────────
        composable(Routes.USER_LOGIN) {
            LoginScreen(
                viewModel      = userViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.USER_LOGIN) { inclusive = true }
                    }
                },
                onAdminClick = {
                    navController.navigate(Routes.ADMIN_LOGIN)
                }
            )
        }

        // ── Dashboard ─────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel       = userViewModel,
                onLogout        = {
                    userViewModel.logout()
                    navController.navigate(Routes.USER_LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                },
                onProductsClick = {
                    navController.navigate(Routes.PRODUCT_SHOWCASE)
                }
            )
        }

        // ── Zákaznická výstavka produktů ──────────────────────
        composable(Routes.PRODUCT_SHOWCASE) {
            ProductShowcaseScreen(
                viewModel = productViewModel,
                isAdmin   = false,
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Admin Login ───────────────────────────────────────
        composable(Routes.ADMIN_LOGIN) {
            AdminLoginScreen(
                viewModel      = adminViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.ADMIN_LIST) {
                        popUpTo(Routes.ADMIN_LOGIN) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Admin List ────────────────────────────────────────
        composable(Routes.ADMIN_LIST) {
            AdminListScreen(
                viewModel       = adminViewModel,
                onLogout        = {
                    adminViewModel.logout()
                    navController.navigate(Routes.USER_LOGIN) {
                        popUpTo(Routes.ADMIN_LIST) { inclusive = true }
                    }
                },
                onProductsClick = {
                    navController.navigate(Routes.ADMIN_PRODUCTS)
                }
            )
        }

        // ── Admin výstavka produktů (s CRUD) ─────────────────
        composable(Routes.ADMIN_PRODUCTS) {
            ProductShowcaseScreen(
                viewModel = productViewModel,
                isAdmin   = true,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}