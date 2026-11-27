package com.example.piggybank

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.piggybank.network.ApiClient
import com.example.piggybank.ui.screens.AboutDetailScreen
import com.example.piggybank.ui.screens.ChartsScreen
import com.example.piggybank.ui.screens.ExportExcelScreen
import com.example.piggybank.ui.screens.GoalDetailScreen
import com.example.piggybank.ui.screens.GoalFormScreen
import com.example.piggybank.ui.screens.GoalListScreen
import com.example.piggybank.ui.screens.MonthlyBudgetScreen
import com.example.piggybank.ui.screens.MonthlyStatisticsScreen
import com.example.piggybank.ui.screens.ProfileScreen
import com.example.piggybank.ui.screens.ReportScreen
import com.example.piggybank.ui.screens.TransactionDetailScreen
import com.example.piggybank.ui.screens.TransactionFormScreen
import com.example.piggybank.ui.screens.TransactionListScreen
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.viewmodel.GoalViewModel
import com.example.piggybank.viewmodel.TransactionViewModel

private data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.initialize(applicationContext)
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
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                transactionViewModel.fetchTransactions()
                goalViewModel.fetchGoals()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem(
            label = "Kas",
            route = Screen.Transactions.route,
            selectedIcon = Icons.Filled.AccountBalanceWallet,
            unselectedIcon = Icons.Outlined.AccountBalanceWallet
        ),
        BottomNavItem(
            label = "Celengan",
            route = Screen.Goals.route,
            selectedIcon = Icons.Filled.Savings,
            unselectedIcon = Icons.Outlined.Savings
        ),
        BottomNavItem(
            label = "Grafik",
            route = Screen.Charts.route,
            selectedIcon = Icons.Filled.PieChart,
            unselectedIcon = Icons.Outlined.PieChart
        ),
        BottomNavItem(
            label = "Profil",
            route = Screen.About.route,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(
        Screen.Transactions.route,
        Screen.Charts.route,
        Screen.Goals.route,
        Screen.About.route
    )

    Scaffold(
        containerColor = BackgroundCanvas,
        bottomBar = {
            if (showBottomBar) {
                PiggyBankBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onAddClick = {
                        when (currentRoute) {
                            Screen.Goals.route -> navController.navigate(Screen.GoalForm.createRoute())
                            else -> navController.navigate(Screen.TransactionForm.createRoute())
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Transactions.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Layar 1: Daftar Transaksi Kas
            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    viewModel = transactionViewModel,
                    onTransactionClick = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    }
                )
            }

            // Layar 1B: Grafik Pengeluaran & Pemasukan
            composable(Screen.Charts.route) {
                ChartsScreen(
                    viewModel = transactionViewModel
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
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val idArg = backStackEntry.arguments?.getLong("id") ?: 0L
                val transactionId = if (idArg != 0L) idArg else null
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
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val idArg = backStackEntry.arguments?.getLong("id") ?: 0L
                val goalId = if (idArg != 0L) idArg else null
                GoalFormScreen(
                    goalId = goalId,
                    viewModel = goalViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // Layar 7: Profil Pengguna
            composable(Screen.About.route) {
                ProfileScreen(
                    transactionViewModel = transactionViewModel,
                    goalViewModel = goalViewModel,
                    onReportClick = {
                        navController.navigate(Screen.Report.route)
                    },
                    onExportExcelClick = {
                        navController.navigate(Screen.ExportExcel.route)
                    },
                    onAboutDetailClick = {
                        navController.navigate(Screen.AboutDetail.route)
                    }
                )
            }

            // Layar 8: Sub-halaman Laporan (Statistik Bulanan & Anggaran Bulanan)
            composable(Screen.Report.route) {
                ReportScreen(
                    transactionViewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() },
                    onMonthlyStatisticsClick = {
                        navController.navigate(Screen.MonthlyStatistics.route)
                    },
                    onMonthlyBudgetClick = {
                        navController.navigate(Screen.MonthlyBudget.route)
                    }
                )
            }

            // Layar 9: Sub-halaman Detail Statistik Bulanan
            composable(Screen.MonthlyStatistics.route) {
                MonthlyStatisticsScreen(
                    transactionViewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Layar 10: Sub-halaman Detail Anggaran Bulanan
            composable(Screen.MonthlyBudget.route) {
                MonthlyBudgetScreen(
                    transactionViewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Layar 11: Sub-halaman Export Excel
            composable(Screen.ExportExcel.route) {
                ExportExcelScreen(
                    transactionViewModel = transactionViewModel,
                    goalViewModel = goalViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Layar 12: Sub-halaman Tentang Aplikasi
            composable(Screen.AboutDetail.route) {
                AboutDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PiggyBankBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val leftFirstItem = items[0]
    val leftSecondItem = items[1]
    val rightFirstItem = items[2]
    val rightSecondItem = items[3]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = CardBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavActionItem(
                        item = leftFirstItem,
                        selected = currentRoute == leftFirstItem.route,
                        onClick = { onItemClick(leftFirstItem.route) },
                        modifier = Modifier.weight(1f)
                    )
                    BottomNavActionItem(
                        item = leftSecondItem,
                        selected = currentRoute == leftSecondItem.route,
                        onClick = { onItemClick(leftSecondItem.route) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.size(64.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavActionItem(
                        item = rightFirstItem,
                        selected = currentRoute == rightFirstItem.route,
                        onClick = { onItemClick(rightFirstItem.route) },
                        modifier = Modifier.weight(1f)
                    )
                    BottomNavActionItem(
                        item = rightSecondItem,
                        selected = currentRoute == rightSecondItem.route,
                        onClick = { onItemClick(rightSecondItem.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Surface(
            onClick = onAddClick,
            shape = CircleShape,
            color = PrimaryTeal,
            contentColor = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Baru",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomNavActionItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) PrimaryTeal else TextSecondary
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
