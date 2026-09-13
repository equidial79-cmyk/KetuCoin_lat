package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Wallet
import com.example.ui.components.CoinAvatar
import com.example.ui.components.KycStatusBadge
import com.example.ui.components.QrCodeDisplay
import com.example.ui.components.SecurityPinDialog
import com.example.ui.components.formatInr
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

@Composable
fun WalletScreen(
    viewModel: KetuCoinViewModel,
    modifier: Modifier = Modifier
) {
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val isKycApproved = currentProfile?.kycStatus == "approved"

    // Strict KYC Gate: Wallet system is accessible only after KYC verification
    if (!isKycApproved) {
        WalletKycLockedView(
            kycStatus = currentProfile?.kycStatus ?: "unverified",
            onStartKyc = { viewModel.openKycModal() },
            modifier = modifier
        )
        return
    }

    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val marketRates by viewModel.marketRates.collectAsStateWithLifecycle()
    val totalBalanceInr by viewModel.totalBalanceInr.collectAsStateWithLifecycle()
    val totalBalanceUsd by viewModel.totalBalanceUsd.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Send Modal Dialog state
    var sendCurrency by remember { mutableStateOf("USDT") }
    var sendAddress by remember { mutableStateOf("") }
    var sendAmount by remember { mutableStateOf("") }
    var sendPin by remember { mutableStateOf("") }

    // Receive Modal state
    var receiveCurrency by remember { mutableStateOf("USDT") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Total Portfolio Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = NavyCard
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Portfolio Balance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SuccessGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Real-time Sync",
                                        fontSize = 11.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$${"%,.2f".format(totalBalanceUsd)} USD",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Text(
                            text = "≈ ₹${formatInr(totalBalanceInr)} INR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldLight
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Send & Receive Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.openSendModal() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("wallet_send_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Button(
                                onClick = { viewModel.openReceiveModal() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("wallet_receive_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NavyDark,
                                    contentColor = AccentCyan
                                )
                            ) {
                                Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Receive", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Assets with Quick History Link
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Assets (Wallets)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = { viewModel.openTransactionHistoryModal() },
                    modifier = Modifier.testTag("wallet_view_history_link")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "History",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }
        }

        // Individual Wallet Cards
        items(wallets) { wallet ->
            val rate = marketRates.find { it.currency.equals(wallet.currency, ignoreCase = true) }
            val inrValue = wallet.balance * (rate?.currentPriceInr ?: 90.0)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
                    .testTag("wallet_card_${wallet.currency.lowercase()}"),
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
                        CoinAvatar(currency = wallet.currency, sizeDp = 44)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = wallet.currency,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "1 ${wallet.currency} ≈ ₹${formatInr(rate?.currentPriceInr ?: 0.0)}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${"%,.4f".format(wallet.balance)} ${wallet.currency}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "≈ ₹${formatInr(inrValue)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoldLight
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // SEND MODAL DIALOG
    if (uiState.showSendModal) {
        val selectedWallet = wallets.find { it.currency == sendCurrency } ?: wallets.firstOrNull()
        val availableBal = selectedWallet?.balance ?: 0.0

        AlertDialog(
            onDismissRequest = { viewModel.closeSendModal() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallMade, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Cryptocurrency", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Transfer crypto to an external wallet address.", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Currency Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("USDT", "BTC", "ETH", "SOL").forEach { curr ->
                            val isSel = sendCurrency == curr
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GoldPrimary else NavyDark)
                                    .border(1.dp, if (isSel) GoldPrimary else NavyCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { sendCurrency = curr }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    curr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.Black else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Available: $availableBal $sendCurrency",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recipient Address Input
                    OutlinedTextField(
                        value = sendAddress,
                        onValueChange = { sendAddress = it },
                        label = { Text("Recipient Address") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_recipient_address_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Amount Input
                    OutlinedTextField(
                        value = sendAmount,
                        onValueChange = { sendAmount = it },
                        label = { Text("Amount ($sendCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = { sendAmount = availableBal.toString() }) {
                                Text("MAX", color = GoldPrimary, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-digit PIN Input (protected if user)
                    OutlinedTextField(
                        value = sendPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) sendPin = it },
                        label = { Text("Security PIN (4 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_pin_input"),
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
                        val amount = sendAmount.toDoubleOrNull() ?: 0.0
                        viewModel.sendCrypto(sendCurrency, sendAddress, amount, sendPin)
                        sendAddress = ""
                        sendAmount = ""
                        sendPin = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("send_confirm_button")
                ) {
                    Text("Confirm Transfer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.closeSendModal()
                    sendAddress = ""
                    sendAmount = ""
                    sendPin = ""
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // RECEIVE MODAL DIALOG
    if (uiState.showReceiveModal) {
        val targetWallet = wallets.find { it.currency == receiveCurrency } ?: wallets.firstOrNull()
        val depositAddress = targetWallet?.assignedDepositAddress ?: "0x0000000000000000000000000000000000000000"
        val qrUrl = targetWallet?.assignedQrUrl ?: "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$depositAddress"

        AlertDialog(
            onDismissRequest = { viewModel.closeReceiveModal() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallReceived, contentDescription = null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Receive Cryptocurrency", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Admin-assigned deposit address for your account", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Currency Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf("USDT", "BTC", "ETH", "SOL").forEach { curr ->
                            val isSel = receiveCurrency == curr
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GoldPrimary else NavyDark)
                                    .border(1.dp, if (isSel) GoldPrimary else NavyCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { receiveCurrency = curr }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    curr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.Black else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Render assigned QR URL
                    QrCodeDisplay(
                        qrUrl = qrUrl,
                        contentDescription = "$receiveCurrency Deposit QR",
                        sizeDp = 160
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Assigned $receiveCurrency Deposit Address:",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Address box with Copy button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavyDark)
                            .border(1.dp, NavyCardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = depositAddress,
                            fontSize = 11.sp,
                            color = GoldLight,
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(depositAddress))
                                Toast.makeText(context, "Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.closeReceiveModal() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = NavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun WalletKycLockedView(
    kycStatus: String,
    onStartKyc: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Crypto Wallet Vault",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Multi-Currency Ledger & Transfer Desk",
                        fontSize = 12.sp,
                        color = AccentCyan
                    )
                }
                KycStatusBadge(status = kycStatus)
            }
        }

        // Main Security Gate Locked Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0B132B))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Glowing Lock Badge
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f))
                                .border(2.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Wallet Access Locked",
                                tint = GoldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Wallet Access Restricted",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Identity Verification (KYC) Required",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldLight
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "In compliance with AML (Anti-Money Laundering) regulations and financial compliance standards, multi-currency crypto wallets, private deposit addresses, sending, and receiving assets are restricted to verified accounts.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Status-dependent call-to-action
                        when (kycStatus.lowercase()) {
                            "pending" -> {
                                Surface(
                                    color = AccentCyan.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Verification Under Review",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AccentCyan
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Your submitted ID documents are currently being validated by our compliance desk. Wallets will unlock immediately upon approval.",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedButton(
                                    onClick = onStartKyc,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("wallet_view_kyc_status_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View KYC Submission", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                            "rejected" -> {
                                Surface(
                                    color = ErrorRed.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Verification Declined",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = ErrorRed
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Your document was not accepted. Please re-upload a clear copy of your government ID card.",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = onStartKyc,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("wallet_reverify_kyc_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Re-submit KYC Verification", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            else -> {
                                // Unverified
                                Button(
                                    onClick = onStartKyc,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("wallet_verify_kyc_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verify Identity (KYC)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Locked Features Overview
        item {
            Text(
                text = "Services Unlocked with KYC",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GatedFeatureCard(
                    icon = Icons.Default.QrCode,
                    title = "Private Blockchain Deposit Addresses",
                    description = "Unique TRC20, Bitcoin, Ethereum, and Solana blockchain deposit addresses with QR codes"
                )
                GatedFeatureCard(
                    icon = Icons.Default.CallMade,
                    title = "External Crypto Send & Transfer",
                    description = "Withdraw assets securely to any external crypto wallet protected by your personal 4-digit PIN"
                )
                GatedFeatureCard(
                    icon = Icons.Default.CallReceived,
                    title = "Direct Inward Blockchain Transfers",
                    description = "Instant reception of funds into your multi-currency crypto storage vaults"
                )
                GatedFeatureCard(
                    icon = Icons.Default.CurrencyExchange,
                    title = "Real-time INR Portfolio Valuation",
                    description = "Automated balance valuation against live OTC rates and direct Indian bank account settlements"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GatedFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = NavyCard.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NavyDark)
                    .border(1.dp, NavyCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = GoldPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LOCKED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrimary
                    )
                }
            }
        }
    }
}
