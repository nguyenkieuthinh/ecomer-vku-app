package com.ecomer.vku.app.ui.nav

import androidx.compose.foundation.layout.Box
import com.ecomer.vku.app.ui.screens.detail.ImageViewerScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.User
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.ecomer.vku.app.ui.components.BottomNav
import com.ecomer.vku.app.ui.screens.account.AccountScreen
import com.ecomer.vku.app.ui.screens.account.ChangePasswordScreen
import com.ecomer.vku.app.ui.screens.address.AddressScreen
import com.ecomer.vku.app.ui.screens.checkout.CheckoutScreen
import com.ecomer.vku.app.ui.screens.admin.AdminDashboardScreen
import com.ecomer.vku.app.ui.screens.admin.AdminOrdersScreen
import com.ecomer.vku.app.ui.screens.admin.AdminProductsScreen
import com.ecomer.vku.app.ui.screens.admin.AdminUsersScreen
import com.ecomer.vku.app.ui.screens.auth.SignInScreen
import com.ecomer.vku.app.ui.screens.auth.SignUpScreen
import com.ecomer.vku.app.ui.screens.cart.CartScreen
import com.ecomer.vku.app.ui.screens.detail.ProductDetailScreen
import com.ecomer.vku.app.ui.screens.home.HomeScreen
import com.ecomer.vku.app.ui.screens.orders.OrdersScreen

/**
 * Dieu huong chinh cua app.
 *
 * Bottom nav chi hien o 4 tab (trang chu, gio hang, don hang, tai khoan).
 * Cac man hinh khac (chi tiet san pham, quan ly san pham, doi mat khau) dung
 * full-screen khong co bottom nav.
 */
@Composable
fun EcomerNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Admin co giao dien rieng: chi 2 tab (Shop, Tai khoan), don hang xem
    // qua trang "Quan tri". Quyen xac dinh bang email trong token — cung
    // cach Firestore Rules kiem tra.
    // KHONG dung `remember` — no se cache gia tri luc NavHost duoc compose,
    // co the truoc khi nguoi dung dang nhap xong. Theo doi trang thai dang
    // nhap de luon dung quyen hien tai.
    var isAdmin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        FirebaseExt.observeAuth { _ ->
            isAdmin = FirebaseExt.auth?.currentUser?.email
                ?.lowercase() in User.ADMIN_EMAILS
        }
    }

    // Giu anh dang xem phong to — anh la chuoi base64 nen khong dua vao route.
    val imageViewerViewModel: ImageViewerViewModel = viewModel()

    val showBottomNav = currentRoute in setOf(
        Screen.HOME,
        Screen.CART,
        Screen.ORDERS,
        Screen.ACCOUNT,
    )

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNav(
                    currentRoute = currentRoute,
                    isAdmin = isAdmin,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // KHONG dung saveState/restoreState: khi do ViewModel
                            // cu duoc giu lai, va sau khi doi tai khoan no van
                            // hien du lieu cua tai khoan truoc.
                            popUpTo(Screen.HOME)
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
        // App dung edge-to-edge nen phai tu chuyen them khoang cho status bar.
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Screen.SIGN_IN) {
                SignInScreen(
                    onSignedIn = {
                        navController.navigate(Screen.HOME) {
                            popUpTo(Screen.SIGN_IN) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(Screen.SIGN_UP) },
                )
            }

            composable(Screen.SIGN_UP) {
                SignUpScreen(
                    onSignedUp = {
                        navController.navigate(Screen.HOME) {
                            popUpTo(Screen.SIGN_IN) { inclusive = true }
                        }
                    },
                    onNavigateToSignIn = { navController.popBackStack() },
                )
            }

            composable(Screen.HOME) {
                HomeScreen(
                    contentPadding = innerPadding,
                    onProductClick = { product ->
                        navController.navigate(Screen.productDetail(product.id))
                    },
                )
            }

            composable(
                route = Screen.PRODUCT_DETAIL,
                arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                ProductDetailScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onAddedToCart = { navController.navigate(Screen.CART) },
                    onViewImages = { images, index ->
                        imageViewerViewModel.show(images, index)
                        navController.navigate(Screen.IMAGE_VIEWER)
                    },
                    // Mua ngay: them san pham vao gio roi sang man thanh toan.
                    // Thanh toan se yeu cau them dia chi neu chua co.
                    onBuyNow = {
                        navController.navigate(Screen.CHECKOUT) {
                            popUpTo(Screen.HOME)
                            launchSingleTop = true
                        }
                    },
                )
            }

            // Dia chi giao hang.
            composable(Screen.ADDRESS) {
                AddressScreen(
                    onBack = { navController.popBackStack() },
                    // Luu xong dia chi thi quay lai man thanh toan neu den tu do.
                    onCheckoutRequired = { navController.popBackStack() },
                )
            }

            // Thanh toan — bat buoc co dia chi giao hang.
            composable(Screen.CHECKOUT) {
                CheckoutScreen(
                    onBack = { navController.popBackStack() },
                    onAddAddress = { navController.navigate(Screen.ADDRESS) },
                    onPlacedOrder = {
                        navController.navigate(Screen.ORDERS) {
                            popUpTo(Screen.HOME)
                            launchSingleTop = true
                        }
                    },
                )
            }

            // Man xem anh phong to.
            composable(Screen.IMAGE_VIEWER) {
                val images by imageViewerViewModel.images.collectAsStateWithLifecycle()
                val initialIndex by imageViewerViewModel.initialIndex.collectAsStateWithLifecycle()
                ImageViewerScreen(
                    images = images,
                    initialIndex = initialIndex,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.CART) {
                CartScreen(
                    contentPadding = innerPadding,
                    onPlacedOrder = {
                        // Sang man thanh toan de chon dia chi truoc khi dat.
                        navController.navigate(Screen.CHECKOUT) {
                            popUpTo(Screen.HOME)
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Screen.ORDERS) {
                OrdersScreen(contentPadding = innerPadding)
            }

            composable(Screen.ACCOUNT) {
                AccountScreen(
                    contentPadding = innerPadding,
                    onManageProducts = { navController.navigate(Screen.ADMIN_DASHBOARD) },
                    onMyOrders = {
                        navController.navigate(Screen.ORDERS) {
                            popUpTo(Screen.HOME)
                            launchSingleTop = true
                        }
                    },
                    onChangePassword = { navController.navigate(Screen.CHANGE_PASSWORD) },
                    onAddresses = { navController.navigate(Screen.ADDRESS) },
                    onSignOut = {
                        navController.navigate(Screen.SIGN_IN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            // Trang quan tri cua admin.
            composable(Screen.ADMIN_DASHBOARD) {
                AdminDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onManageProducts = { navController.navigate(Screen.ADMIN_PRODUCTS) },
                    onManageOrders = { navController.navigate(Screen.ADMIN_ORDERS) },
                    onManageUsers = { navController.navigate(Screen.ADMIN_USERS) },
                )
            }

            composable(Screen.ADMIN_PRODUCTS) {
                AdminProductsScreen(
                    onBack = { navController.popBackStack() },
                    onViewProduct = { product ->
                        navController.navigate(Screen.productDetail(product.id))
                    },
                )
            }

            composable(Screen.ADMIN_ORDERS) {
                AdminOrdersScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.ADMIN_USERS) {
                AdminUsersScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.CHANGE_PASSWORD) {
                ChangePasswordScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
