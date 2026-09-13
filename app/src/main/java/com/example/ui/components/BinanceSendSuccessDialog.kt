package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CryptoTransaction
import com.example.ui.theme.AccentCyan
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

/**
 * Binance-style Crypto Transfer Receipt / Success Window.
 * Appears when the user sends cryptocurrency to an external address or recipient.
 */
@Composable
fun BinanceSendSuccessDialog(
    transaction: CryptoTransaction,
    onViewHistory: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .testTag("binance_send_success_dialog"),
            color = NavyCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_send_success_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Binance-style Glowing Success Badge
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SuccessGreen.copy(alpha = 0.35f),
                                    GoldPrimary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(2.dp, SuccessGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = "Transfer Submitted",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = "Sent via Blockchain Network",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Amount Display (Binance style)
                Text(
                    text = "- ${"%,.4f".format(transaction.amount)} ${transaction.currency}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldLight,
                    textAlign = TextAlign.Center
                )

                if (transaction.inrValue > 0.0) {
                    Text(
                        text = "≈ ₹${formatInr(transaction.inrValue)} INR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Binance Transaction Details Receipt Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyCardBorder, GoldPrimary.copy(alpha = 0.3f))))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status Row
                        BinanceDetailRow(
                            label = "Status",
                            customContent = {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(SuccessGreen)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = transaction.status,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = NavyCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                        // Network
                        BinanceDetailRow(
                            label = "Network",
                            value = "${transaction.network} (${transaction.currency})"
                        )

                        HorizontalDivider(color = NavyCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                        // Recipient Address with Copy
                        val addr = transaction.recipientAddress ?: "External Wallet"
                        val displayAddr = if (addr.length > 18) {
                            "${addr.take(8)}...${addr.takeLast(6)}"
                        } else addr

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Address",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(addr))
                                    Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = displayAddr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Address",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = NavyCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                        // TxID / Hash with Copy
                        val hash = transaction.txHash ?: "0x${transaction.id.replace("-", "").take(24)}"
                        val displayHash = if (hash.length > 18) {
                            "${hash.take(8)}...${hash.takeLast(6)}"
                        } else hash

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TxID / Hash",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(hash))
                                    Toast.makeText(context, "Transaction Hash copied", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = displayHash,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy TxID",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = NavyCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                        // Network Fee
                        BinanceDetailRow(
                            label = "Network Fee",
                            value = "${"%.4f".format(transaction.networkFee)} ${transaction.currency}"
                        )

                        HorizontalDivider(color = NavyCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                        // Date & Time
                        BinanceDetailRow(
                            label = "Date & Time",
                            value = transaction.createdAt.ifBlank { "Just now" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action 1: View Transaction History (Prominent Binance Action)
                Button(
                    onClick = {
                        onViewHistory()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("binance_view_history_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Transaction History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Done / Close
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("binance_done_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(NavyCardBorder, NavyCardBorder))
                    )
                ) {
                    Text(
                        text = "Done",
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BinanceDetailRow(
    label: String,
    value: String? = null,
    customContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextMuted
        )
        if (customContent != null) {
            customContent()
        } else if (value != null) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
