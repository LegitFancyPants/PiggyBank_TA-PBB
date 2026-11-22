package com.example.piggybank

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.piggybank.ui.screens.GoalDetailScreen
import com.example.piggybank.ui.screens.GoalFormScreen
import com.example.piggybank.ui.screens.GoalListScreen
import com.example.piggybank.ui.screens.TransactionDetailScreen
import com.example.piggybank.ui.screens.TransactionFormScreen
import com.example.piggybank.ui.screens.TransactionListScreen
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.SoftMint
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.viewmodel.GoalViewModel
import com.example.piggybank.viewmodel.TransactionViewModel

private data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.WHITE,
                AndroidColor.WHITE
            )
        )
        setContent {
            PiggyBankTheme {
                PiggyBankApp()
            }
        }
    }
}

@Composable
fun PiggyBankApp() {
    val navController = rememberNavController()
    val transactionViewModel: TransactionViewModel = viewModel()
    val goalViewModel: GoalViewModel = viewModel()

    val bottomNavItems = listOf(
        BottomNavItem(
            label = "Kas",
            route = Screen.Transactions.route,
            icon = Icons.Default.AccountBalanceWallet
        ),
        BottomNavItem(
            label = "Celengan",
            route = Screen.Goals.route,
            icon = Icons.Default.Savings
        ),
        BottomNavItem(
            label = "Profil",
            route = Screen.About.route,
            icon = Icons.Default.Person
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(
        Screen.Transactions.route,
        Screen.Goals.route,
        Screen.About.route
    )

    Scaffold(
        containerColor = BackgroundCanvas,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = CardBorder)
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryTeal,
                                    selectedTextColor = PrimaryTeal,
                                    indicatorColor = SoftMint,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Transactions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Layar 1: Daftar Transaksi Kas
            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    viewModel = transactionViewModel,
                    onTransactionClick = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    },
                    onAddTransactionClick = {
                        navController.navigate(Screen.TransactionForm.createRoute())
                    }
                )
            }

            // Layar 2: Detail Transaksi
            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("id") ?: -1L
                TransactionDetailScreen(
                    transactionId = transactionId,
                    viewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { id ->
                        navController.navigate(Screen.TransactionForm.createRoute(id))
                    },
                    onDeleteSuccess = { navController.popBackStack() }
                )
            }

            // Layar 3: Form Tambah / Edit Transaksi
            composable(
                route = Screen.TransactionForm.route,
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val idArg = backStackEntry.arguments?.getLong("id") ?: -1L
                val transactionId = if (idArg > 0L) idArg else null
                TransactionFormScreen(
                    transactionId = transactionId,
                    viewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // Layar 4: Daftar Target Tabungan / Celengan
            composable(Screen.Goals.route) {
                GoalListScreen(
                    viewModel = goalViewModel,
                    onGoalClick = { id ->
                        navController.navigate(Screen.GoalDetail.createRoute(id))
                    },
                    onAddGoalClick = {
                        navController.navigate(Screen.GoalForm.createRoute())
                    }
                )
            }

            // Layar 5: Detail Target Tabungan
            composable(
                route = Screen.GoalDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getLong("id") ?: -1L
                GoalDetailScreen(
                    goalId = goalId,
                    viewModel = goalViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { id ->
                        navController.navigate(Screen.GoalForm.createRoute(id))
                    },
                    onDeleteSuccess = { navController.popBackStack() }
                )
            }

            // Layar 6: Form Tambah / Edit Target Tabungan
            composable(
                route = Screen.GoalForm.route,
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val idArg = backStackEntry.arguments?.getLong("id") ?: -1L
                val goalId = if (idArg > 0L) idArg else null
                GoalFormScreen(
                    goalId = goalId,
                    viewModel = goalViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // Layar 7: Profil / About Aplikasi (Placeholder sementara)
            composable(Screen.About.route) {
                PlaceholderScreen(
                    title = "Profil & Tentang Aplikasi",
                    subtitle = "Halaman profil pengguna lokal & tentang aplikasi PiggyBank (Layar 7) akan diimplementasikan pada bagian berikutnya.",
                    showBackButton = false,
                    onBackClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
    ) {
        if (showBackButton) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSurface
                )
            )
            HorizontalDivider(thickness = 1.dp, color = CardBorder)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (showBackButton) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryTeal,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Kembali")
                }
            }
        }
    }
}
