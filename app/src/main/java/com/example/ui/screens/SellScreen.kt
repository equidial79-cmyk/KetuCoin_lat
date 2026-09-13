package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CoinAvatar
import com.example.ui.components.QrCodeDisplay
import com.example.ui.components.SecurityPinDialog
import com.example.ui.components.formatInr
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ErrorRed
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
fun SellScreen(
    viewModel: KetuCoinViewModel,
    modifier: Modifier = Modifier
) {
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val marketRates by viewModel.marketRates.collectAsStateWithLifecycle()
    val systemDepositAddresses by viewModel.systemDepositAddresses.collectAsStateWithLifecycle()
    val sellOrders by viewModel.sellOrders.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Method Selector: 0 = "Wallet Balance", 1 = "External Transfer"
    var selectedMethodIndex by remember { mutableIntStateOf(0) }
    var selectedCurrency by remember { mutableStateOf("USDT") }
    var sellAmountText by remember { mutableStateOf("") }
    var isPinDialogOpen by remember { mutableStateOf(false) }

    // External Proof Upload state
    var selectedProofUri by remember { mutableStateOf<Uri?>(null) }
    var proofImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Photo Picker launcher for proof upload
    val proofPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedProofUri = uri
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    proofImageBytes = stream.readBytes()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load proof image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val currentRate = marketRates.find { it.currency == selectedCurrency }?.currentPriceInr ?: 90.0
    val sellAmount = sellAmountText.toDoubleOrNull() ?: 0.0
    val estimatedPayoutInr = sellAmount * currentRate

    val userWallet = wallets.find { it.currency == selectedCurrency }
    val availableBalance = userWallet?.balance ?: 0.0

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
                text = "Sell Crypto for INR",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Instant OTC Payouts directly to your Indian Bank Account",
                fontSize = 13.sp,
                color = AccentCyan
            )
        }

        // Method Selector Tab Row
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                TabRow(
                    selectedTabIndex = selectedMethodIndex,
                    containerColor = NavyCard,
                    contentColor = GoldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedMethodIndex]),
                            color = GoldPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedMethodIndex == 0,
                        onClick = { selectedMethodIndex = 0 },
                        text = {
                            Text(
                                "Wallet Balance",
                                fontWeight = if (selectedMethodIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedMethodIndex == 0) GoldPrimary else TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedMethodIndex == 1,
                        onClick = { selectedMethodIndex = 1 },
                        text = {
                            Text(
                                "External Transfer",
                                fontWeight = if (selectedMethodIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedMethodIndex == 1) GoldPrimary else TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }
        }

        // Currency Selector Pills
        item {
            Column {
                Text("Select Crypto Currency:", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("USDT", "BTC", "ETH", "SOL").forEach { curr ->
                        val isSelected = selectedCurrency == curr
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GoldPrimary else NavyCard)
                                .border(1.dp, if (isSelected) GoldPrimary else NavyCardBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedCurrency = curr }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = curr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // METHOD 0: WALLET BALANCE
        if (selectedMethodIndex == 0) {
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
                            Text("Available in Wallet:", fontSize = 13.sp, color = TextMuted)
                            Text(
                                text = "$availableBalance $selectedCurrency",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GoldLight
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = sellAmountText,
                            onValueChange = { sellAmountText = it },
                            label = { Text("Amount to Sell ($selectedCurrency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            trailingIcon = {
                                TextButton(onClick = { sellAmountText = availableBalance.toString() }) {
                                    Text("MAX", color = GoldPrimary, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sell_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = NavyCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Payout Estimate Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavyDark)
                                .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Exchange Rate:", fontSize = 12.sp, color = TextMuted)
                                    Text("1 $selectedCurrency = ₹${formatInr(currentRate)}", fontSize = 12.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("You Receive (INR):", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Text(
                                        text = "₹${formatInr(estimatedPayoutInr)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldPrimary
                                    )
                                }

                                currentProfile?.bankAccount?.let { bank ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Payout to: ${bank.bankName} (A/C: •••• ${bank.accountNumber.takeLast(4)})",
                                        fontSize = 11.sp,
                                        color = AccentCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (sellAmount <= 0.0) {
                                    Toast.makeText(context, "Please enter amount", Toast.LENGTH_SHORT).show()
                                } else if (sellAmount > availableBalance) {
                                    Toast.makeText(context, "Amount exceeds available balance", Toast.LENGTH_SHORT).show()
                                } else {
                                    isPinDialogOpen = true
                                }
                            },
                            enabled = !uiState.isLoading && sellAmount > 0.0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("sell_wallet_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black)
                            } else {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sell & Authorize with PIN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // METHOD 1: EXTERNAL TRANSFER
        if (selectedMethodIndex == 1) {
            val systemAddress = systemDepositAddresses.find { it.currency.equals(selectedCurrency, ignoreCase = true) }
            val adminDepositAddress = systemAddress?.address ?: "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"
            val qrUrl = systemAddress?.qrUrl ?: "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$adminDepositAddress"

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Admin Designated $selectedCurrency Deposit Address",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Send $selectedCurrency from Binance, Bybit, or hardware wallet to this address",
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        QrCodeDisplay(
                            qrUrl = qrUrl,
                            contentDescription = "Admin $selectedCurrency QR Code",
                            sizeDp = 160
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Copy Address
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
                                text = adminDepositAddress,
                                fontSize = 11.sp,
                                color = GoldLight,
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(adminDepositAddress))
                                    Toast.makeText(context, "System deposit address copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentCyan, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Amount Transferred
                        OutlinedTextField(
                            value = sellAmountText,
                            onValueChange = { sellAmountText = it },
                            label = { Text("Amount Transferred ($selectedCurrency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("external_sell_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = NavyCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Proof of Payment Upload
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavyDark)
                                .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    proofPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (selectedProofUri != null) Icons.Default.CheckCircle else Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = if (selectedProofUri != null) SuccessGreen else AccentCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedProofUri != null) "Payment Proof Attached" else "Upload Payment Proof Screenshot",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedProofUri != null) SuccessGreen else TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (sellAmount <= 0.0) {
                                    Toast.makeText(context, "Enter amount sent", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.submitExternalSell(selectedCurrency, sellAmount, proofImageBytes) {
                                        sellAmountText = ""
                                        selectedProofUri = null
                                        proofImageBytes = null
                                    }
                                }
                            },
                            enabled = !uiState.isLoading && sellAmount > 0.0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_external_sell_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit Order & Proof", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // Recent Sell Orders Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Sell Orders",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = { viewModel.openTransactionHistoryModal() },
                    modifier = Modifier.testTag("sell_view_history_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "All Transactions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }
        }

        if (sellOrders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sell orders yet. Sell crypto above to withdraw INR.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(sellOrders) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinAvatar(currency = order.currency, sizeDp = 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Sold ${order.amount} ${order.currency}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Method: ${order.method.replaceFirstChar { it.uppercase() }}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${formatInr(order.inrPayoutEstimate)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            val isCompleted = order.status == "completed"
                            Surface(
                                color = if (isCompleted) SuccessGreen.copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = order.status.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) SuccessGreen else GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 4-Digit Security PIN Validation Dialog for Wallet Balance Sell
    SecurityPinDialog(
        isOpen = isPinDialogOpen,
        title = "Authorize Crypto Sale",
        description = "Enter your 4-digit Security PIN to confirm selling $sellAmount $selectedCurrency for ₹${formatInr(estimatedPayoutInr)}.",
        onDismiss = { isPinDialogOpen = false },
        onConfirm = { pin ->
            isPinDialogOpen = false
            viewModel.sellWalletBalance(selectedCurrency, sellAmount, pin) {
                sellAmountText = ""
            }
        }
    )
}
