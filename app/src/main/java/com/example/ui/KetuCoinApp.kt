package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BinanceSendSuccessDialog
import com.example.ui.components.KycVerificationDialog
import com.example.ui.components.NoInternetScreen
import com.example.ui.components.TransactionHistoryDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SellScreen
import com.example.util.NetworkMonitor
import com.example.ui.screens.SupportChatScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.KetuCoinViewModel

enum class NavigationTab(val label: String) {
    HOME("Home"),
    SELL("Sell"),
    WALLET("Wallet"),
    SUPPORT("Support"),
    PROFILE("Profile")
}

@Composable
fun KetuCoinApp(
    viewModel: KetuCoinViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    val isKycApproved = currentProfile?.kycStatus == "approved"

    val snackbarHostState = remember { SnackbarHostState() }

    // Network connectivity monitoring - KetuCoin strictly requires data connection to operate
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnlineFlow = remember { networkMonitor.isOnlineFlow }
    val isConnected by isOnlineFlow.collectAsStateWithLifecycle(initialValue = networkMonitor.isOnline())

    LaunchedEffect(isConnected) {
        viewModel.updateNetworkState(isConnected)
    }

    // Navigation State
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    // If device is offline, enforce data connection requirement
    if (!isConnected) {
        NoInternetScreen(
            onRetry = {
                val onlineNow = networkMonitor.isOnline()
                viewModel.updateNetworkState(onlineNow)
                if (!onlineNow) {
                    viewModel.showError("No internet connection detected. Please enable Mobile Data or Wi-Fi.")
                }
            },
            modifier = modifier
        )
        return
    }

    // Handle Snackbars
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    if (!isUserLoggedIn) {
        AuthScreen(viewModel = viewModel, modifier = modifier)
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = NavyCard,
                    contentColor = TextPrimary,
                    modifier = Modifier
                        .border(1.dp, NavyCardBorder)
                        .testTag("main_bottom_nav")
                ) {
                    // 1. HOME TAB
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.HOME,
                        onClick = { currentTab = NavigationTab.HOME },
                        icon = {
                            Icon(
                                if (currentTab == NavigationTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentTab == NavigationTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldPrimary,
                            selectedTextColor = GoldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NavyDark
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    // 2. SELL TAB
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.SELL,
                        onClick = { currentTab = NavigationTab.SELL },
                        icon = {
                            Icon(
                                if (currentTab == NavigationTab.SELL) Icons.Filled.CurrencyExchange else Icons.Outlined.CurrencyExchange,
                                contentDescription = "Sell"
                            )
                        },
                        label = { Text("Sell", fontSize = 11.sp, fontWeight = if (currentTab == NavigationTab.SELL) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldPrimary,
                            selectedTextColor = GoldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NavyDark
                        ),
                        modifier = Modifier.testTag("nav_tab_sell")
                    )

                    // 3. WALLET TAB (Highlighted Primary Center Tab)
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.WALLET,
                        onClick = {
                            currentTab = NavigationTab.WALLET
                            if (!isKycApproved) {
                                // Direct to KYC verification in case of unverified KYC
                                viewModel.openKycModal()
                            }
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentTab == NavigationTab.WALLET)
                                            Brush.linearGradient(listOf(GoldLight, GoldPrimary))
                                         else
                                            Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = if (currentTab == NavigationTab.WALLET) Color.Black else AccentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (!isKycApproved) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "KYC Required",
                                            tint = Color.Black,
                                            modifier = Modifier.size(8.dp)
                                        )
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                "Wallet",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTab == NavigationTab.WALLET) GoldPrimary else AccentCyan
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            unselectedIconColor = AccentCyan,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("nav_tab_wallet")
                    )

                    // 4. SUPPORT TAB (Placed between Wallet and Profile)
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.SUPPORT,
                        onClick = { currentTab = NavigationTab.SUPPORT },
                        icon = {
                            Icon(
                                if (currentTab == NavigationTab.SUPPORT) Icons.Filled.SupportAgent else Icons.Outlined.SupportAgent,
                                contentDescription = "Support"
                            )
                        },
                        label = { Text("Support", fontSize = 11.sp, fontWeight = if (currentTab == NavigationTab.SUPPORT) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldPrimary,
                            selectedTextColor = GoldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NavyDark
                        ),
                        modifier = Modifier.testTag("nav_tab_support")
                    )

                    // 5. PROFILE TAB
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.PROFILE,
                        onClick = { currentTab = NavigationTab.PROFILE },
                        icon = {
                            Icon(
                                if (currentTab == NavigationTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Profile"
                            )
                        },
                        label = { Text("Profile", fontSize = 11.sp, fontWeight = if (currentTab == NavigationTab.PROFILE) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldPrimary,
                            selectedTextColor = GoldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NavyDark
                        ),
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition",
                modifier = Modifier.padding(paddingValues)
            ) { targetTab ->
                when (targetTab) {
                    NavigationTab.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSell = { currentTab = NavigationTab.SELL },
                        onNavigateToChat = { currentTab = NavigationTab.SUPPORT }
                    )
                    NavigationTab.SELL -> SellScreen(
                        viewModel = viewModel
                    )
                    NavigationTab.WALLET -> WalletScreen(
                        viewModel = viewModel
                    )
                    NavigationTab.SUPPORT -> SupportChatScreen(
                        viewModel = viewModel,
                        onBack = { currentTab = NavigationTab.WALLET }
                    )
                    NavigationTab.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { currentTab = NavigationTab.SUPPORT }
                    )
                }
            }
        }

        // Binance-style Successful Crypto Send Window
        if (uiState.showSendSuccessWindow && uiState.lastSendTransaction != null) {
            BinanceSendSuccessDialog(
                transaction = uiState.lastSendTransaction!!,
                onViewHistory = {
                    viewModel.openHistoryFromSuccessWindow()
                },
                onDismiss = {
                    viewModel.closeSendSuccessWindow()
                }
            )
        }

        // Complete Transaction History Dialog
        if (uiState.showTransactionHistoryModal) {
            TransactionHistoryDialog(
                transactions = transactions,
                onDismiss = {
                    viewModel.closeTransactionHistoryModal()
                }
            )
        }

        // KYC Verification Dialog (Global)
        if (uiState.showKycModal) {
            KycVerificationDialog(
                currentKycStatus = currentProfile?.kycStatus ?: "unverified",
                onDismiss = { viewModel.closeKycModal() },
                onSubmit = { fullName, docType, imageBytes, autoApprove ->
                    viewModel.submitKyc(fullName, docType, imageBytes, autoApprove)
                }
            )
        }
    }
}
