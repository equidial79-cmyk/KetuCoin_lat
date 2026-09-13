package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppUpdateInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.KycStatusBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.KetuCoinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: KetuCoinViewModel,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val latestAppUpdate by viewModel.latestAppUpdate.collectAsStateWithLifecycle()

    // Notification permission launcher for Android 13+ (POST_NOTIFICATIONS)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Update notifications enabled", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Admin Push Update form state
    var updateVersionName by remember { mutableStateOf("v1.1.0") }
    var updateVersionCode by remember { mutableStateOf("2") }
    var updateDownloadUrl by remember { mutableStateOf("https://github.com/ketucoin/releases/download/v1.1.0/ketucoin-v1.1.0.apk") }
    var updateReleaseNotes by remember {
        mutableStateOf(
            "• Instant INR settlements via IMPS/NEFT\n" +
            "• Solana (SOL) multi-chain deposit address\n" +
            "• Real-time OTC rate lock during checkout\n" +
            "• Security patches and performance boosts"
        )
    }
    var updateIsForce by remember { mutableStateOf(false) }

    // Bank Account Edit state
    var editAccNum by remember { mutableStateOf(currentProfile?.bankAccount?.accountNumber ?: "") }
    var editIfsc by remember { mutableStateOf(currentProfile?.bankAccount?.ifscCode ?: "") }
    var editBankName by remember { mutableStateOf(currentProfile?.bankAccount?.bankName ?: "") }
    var editHolderName by remember { mutableStateOf(currentProfile?.bankAccount?.holderName ?: "") }

    // Change PIN state
    var newPinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Account & Profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Profile Identity Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentProfile?.email ?: "user@ketucoin.io",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Role: ${currentProfile?.role?.uppercase() ?: "USER"}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            currentProfile?.let { KycStatusBadge(status = it.kycStatus) }
                        }
                    }
                }
            }
        }

        // KYC Status & Verification Flow Card
        item {
            val kycStatus = currentProfile?.kycStatus ?: "unverified"
            val canUpdateKyc = kycStatus == "unverified" || kycStatus == "rejected"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Identity Verification (KYC)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }
                        KycStatusBadge(status = kycStatus)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when (kycStatus) {
                            "approved" -> "Your KYC is verified. You have full access to the multi-currency Wallet system and OTC withdrawals."
                            "pending" -> "Your ID document is under review by KetuCoin desk. Wallets will unlock upon verification approval."
                            "rejected" -> "Your previous document submission was declined. Please re-submit a clear photo of your ID Card."
                            else -> "Verify your identity to enable crypto wallets, deposit addresses, and instant INR bank settlements."
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (kycStatus == "approved") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wallet System Unlocked", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                        }
                    } else if (kycStatus == "pending") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verification in review by compliance team", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.openKycModal() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                            ) {
                                Text("Re-upload Documents", fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Unverified or Rejected
                        Button(
                            onClick = { viewModel.openKycModal() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("update_kyc_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Identity (KYC)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Bank Account Details Card
        item {
            val bank = currentProfile?.bankAccount
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Linked Bank Account (INR)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        }
                        IconButton(onClick = {
                            editAccNum = bank?.accountNumber ?: ""
                            editIfsc = bank?.ifscCode ?: ""
                            editBankName = bank?.bankName ?: ""
                            editHolderName = bank?.holderName ?: ""
                            viewModel.openBankEditModal()
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Bank Account", tint = AccentCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (bank != null && bank.accountNumber.isNotBlank()) {
                        Text(text = "Bank: ${bank.bankName}", fontSize = 13.sp, color = TextPrimary)
                        Text(text = "A/C Number: •••••••• ${bank.accountNumber.takeLast(4)}", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "IFSC: ${bank.ifscCode}", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "Beneficiary: ${bank.holderName}", fontSize = 13.sp, color = TextMuted)
                    } else {
                        Text(text = "No bank account added yet. Add details for INR payouts.", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }

        // Security & PIN Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Security & PIN", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your 4-digit Security PIN is hashed and required for authorizing crypto sales & transfers.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.openChangePinModal() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_pin_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add / Change Security PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Live Chat & Support Desk shortcut
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onNavigateToChat),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("24/7 Support Desk", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Live chat with KetuCoin OTC executives", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Text("Open Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                }
            }
        }

        // APP VERSION & UPDATES SECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
                    .testTag("app_update_card"),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("App Version & Updates", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Installed Build: v1.0 (Build 1)", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        if (latestAppUpdate != null && latestAppUpdate!!.versionCode > 1) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoldPrimary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Text(
                                    text = "UPDATE AVAILABLE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
                            ) {
                                Text(
                                    text = "✓ UP TO DATE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (latestAppUpdate != null && latestAppUpdate!!.versionCode > 1) {
                        val update = latestAppUpdate!!
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = NavyDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "KetuCoin ${update.versionName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = GoldLight
                                    )
                                    Text(
                                        text = "${update.fileSizeMb} MB",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = update.releaseNotes,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.openUpdateDetailDialog() },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("view_update_details_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Changelog", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { viewModel.downloadAndInstallUpdate(context, update) },
                                modifier = Modifier.weight(1.3f).height(44.dp).testTag("download_update_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download & Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "You are running the latest official version of KetuCoin Exchange. You will receive a push notification whenever a new update is pushed.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { viewModel.checkForUpdates(context) },
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("check_updates_button"),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.isCheckingUpdate,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                        ) {
                            if (uiState.isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GoldPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking for Updates...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check for Updates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ADMIN DESK: APP RELEASE & PUSH NOTIFICATIONS
        item {
            val isAdmin = currentProfile?.role == "admin"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, if (isAdmin) AccentCyan.copy(alpha = 0.5f) else NavyCardBorder, RoundedCornerShape(16.dp))
                    .testTag("admin_release_card"),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Admin Release Desk", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Push updates & alerts to all users", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        TextButton(
                            onClick = { viewModel.toggleAdminRole() },
                            modifier = Modifier.testTag("toggle_admin_mode_button")
                        ) {
                            Text(
                                text = if (isAdmin) "Role: ADMIN" else "Role: USER (Tap to Switch)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) AccentCyan else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isAdmin) {
                        Text(
                            text = "As Administrator, publishing an update broadcasts the new version to all user devices and dispatches real-time push notifications prompting users to download and install the APK.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.openAdminPushUpdateDialog() },
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("push_app_update_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Push New App Update to All Devices", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Text(
                            text = "Admin update publishing controls are reserved for administrators. Tap 'Role: USER' above to switch to Admin mode for testing and deployment.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Sign Out Button
        item {
            OutlinedButton(
                onClick = { viewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // BANK ACCOUNT EDIT MODAL
    if (uiState.showBankEditModal) {
        AlertDialog(
            onDismissRequest = { viewModel.closeBankEditModal() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bank Account Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Enter bank account for receiving INR settlements.", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editHolderName,
                        onValueChange = { editHolderName = it },
                        label = { Text("Account Holder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bank_holder_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editBankName,
                        onValueChange = { editBankName = it },
                        label = { Text("Bank Name (e.g. HDFC, SBI)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bank_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editAccNum,
                        onValueChange = { editAccNum = it },
                        label = { Text("Account Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bank_account_number_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editIfsc,
                        onValueChange = { editIfsc = it.uppercase() },
                        label = { Text("IFSC Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bank_ifsc_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editHolderName.isBlank()) {
                            Toast.makeText(context, "Please enter account holder name", Toast.LENGTH_SHORT).show()
                        } else if (editBankName.isBlank()) {
                            Toast.makeText(context, "Please enter bank name", Toast.LENGTH_SHORT).show()
                        } else if (editAccNum.isBlank() || editAccNum.length < 6) {
                            Toast.makeText(context, "Please enter a valid account number", Toast.LENGTH_SHORT).show()
                        } else if (editIfsc.isBlank() || editIfsc.length != 11) {
                            Toast.makeText(context, "Please enter valid 11-character IFSC code", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateBankAccount(editAccNum.trim(), editIfsc.trim().uppercase(), editBankName.trim(), editHolderName.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_bank_account_button")
                ) {
                    Text("Save Bank Details", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeBankEditModal() }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // CHANGE SECURITY PIN MODAL
    if (uiState.showChangePinModal) {
        AlertDialog(
            onDismissRequest = { viewModel.closeChangePinModal() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Security PIN", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("PIN is hashed with SHA-256 and stored in Supabase profiles.", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinText = it },
                        label = { Text("New 4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPinText,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPinText = it },
                        label = { Text("Confirm 4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinText == confirmPinText) {
                            viewModel.changePin(newPinText)
                        } else {
                            Toast.makeText(context, "PINs do not match", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("Update PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeChangePinModal() }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ADMIN PUSH UPDATE MODAL
    if (uiState.showAdminPushUpdateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAdminPushUpdateDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Push App Update", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Broadcasting will send real-time push notifications to all KetuCoin users, prompting them to download and install the new APK release.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = updateVersionName,
                        onValueChange = { updateVersionName = it },
                        label = { Text("Version Name (e.g. v1.1.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("update_version_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = updateVersionCode,
                        onValueChange = { if (it.all { c -> c.isDigit() }) updateVersionCode = it },
                        label = { Text("Version Code (Integer > 1)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("update_version_code_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = updateDownloadUrl,
                        onValueChange = { updateDownloadUrl = it },
                        label = { Text("APK Download URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("update_download_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = updateReleaseNotes,
                        onValueChange = { updateReleaseNotes = it },
                        label = { Text("Release Notes / Changelog") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("update_release_notes_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Force Update", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Require users to update to continue", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = updateIsForce,
                            onCheckedChange = { updateIsForce = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (updateVersionName.isBlank() || updateDownloadUrl.isBlank()) {
                            Toast.makeText(context, "Please provide version name and download URL", Toast.LENGTH_SHORT).show()
                        } else {
                            val code = updateVersionCode.toIntOrNull() ?: 2
                            viewModel.pushAppUpdate(
                                context = context,
                                versionName = updateVersionName.trim(),
                                versionCode = code,
                                releaseNotes = updateReleaseNotes.trim(),
                                downloadUrl = updateDownloadUrl.trim(),
                                isForce = updateIsForce
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("submit_broadcast_update_button")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Broadcast Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAdminPushUpdateDialog() }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // UPDATE DETAILS / CHANGELOG MODAL
    if (uiState.showUpdateDetailDialog && latestAppUpdate != null) {
        val update = latestAppUpdate!!
        AlertDialog(
            onDismissRequest = { viewModel.closeUpdateDetailDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update: ${update.versionName}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Build Code: ${update.versionCode}", fontSize = 12.sp, color = TextSecondary)
                        Text("Size: ${update.fileSizeMb} MB", fontSize = 12.sp, color = TextSecondary)
                    }

                    if (update.isForceUpdate) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ErrorRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                        ) {
                            Text(
                                text = "⚠️ Mandatory Security & Feature Update",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text("Release Notes & Highlights:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldLight)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, NavyCardBorder, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = NavyDark)
                    ) {
                        Text(
                            text = update.releaseNotes,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text(
                        text = "Tapping 'Download & Install' initiates the direct APK download via Android's Download Manager.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadAndInstallUpdate(context, update) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("modal_download_update_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download & Install", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeUpdateDetailDialog() }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = NavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
