package com.ecomer.vku.app.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecomer.vku.app.ui.screens.auth.SignInScreen
import com.ecomer.vku.app.ui.screens.auth.SignUpScreen
import com.ecomer.vku.app.ui.screens.products.ProductsScreen

/**
 * Dieu huong chinh cua app.
 *
 * Luong: Dang nhap -> Du lieu san pham. Dang xuat quay ve man dang nhap
 * de doi tai khoan.
 */
@Composable
fun EcomerNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Screen.SIGN_IN) {
            SignInScreen(
                onSignedIn = {
                    navController.navigate(Screen.PRODUCTS) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SIGN_UP) },
            )
        }

        composable(Screen.SIGN_UP) {
            SignUpScreen(
                onSignedUp = {
                    navController.navigate(Screen.PRODUCTS) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignIn = { navController.popBackStack() },
            )
        }

        composable(Screen.PRODUCTS) {
            ProductsScreen(
                onSignOut = {
                    navController.navigate(Screen.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
