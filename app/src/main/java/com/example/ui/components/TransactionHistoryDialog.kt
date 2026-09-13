package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CryptoTransaction
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TransactionHistoryDialog(
    transactions: List<CryptoTransaction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tab Index: 0 = "All", 1 = "Send", 2 = "Sell"
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTypes = listOf("All", "Send", "Sell")

    val filteredList = when (selectedFilterIndex) {
        1 -> transactions.filter { it.type.equals("SEND", ignoreCase = true) }
        2 -> transactions.filter { it.type.equals("SELL", ignoreCase = true) }
        else -> transactions
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NavyCardBorder, RoundedCornerShape(24.dp))
                .testTag("transaction_history_dialog"),
            color = NavyCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NavyDark)
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transaction History",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${filteredList.size} transactions found",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_tx_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Tabs
                TabRow(
                    selectedTabIndex = selectedFilterIndex,
                    containerColor = NavyDark,
                    contentColor = GoldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilterIndex]),
                            color = GoldPrimary,
                            height = 2.5.dp
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(10.dp))
                ) {
                    filterTypes.forEachIndexed { index, label ->
                        val isSelected = selectedFilterIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedFilterIndex = index },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) GoldPrimary else TextSecondary
                                )
                            },
                            modifier = Modifier.testTag("filter_${label.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transaction Items List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No transactions found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                            Text(
                                text = "Your send and sell activities will appear here.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { tx ->
                            TransactionItemCard(transaction = tx)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: CryptoTransaction,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }

    val isSend = transaction.type.equals("SEND", ignoreCase = true)
    val isCompleted = transaction.status.equals("completed", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("tx_item_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = NavyDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSend) GoldPrimary.copy(alpha = 0.18f) else SuccessGreen.copy(alpha = 0.18f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSend) Icons.Default.CallMade else Icons.Default.CurrencyExchange,
                            contentDescription = transaction.type,
                            tint = if (isSend) GoldPrimary else SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isSend) "Sent ${transaction.currency}" else "Sold ${transaction.currency}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = transaction.network.ifBlank { if (isSend) "External Wallet" else "Bank Settlement" },
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isSend) "-${"%,.4f".format(transaction.amount)} ${transaction.currency}"
                               else "+₹${formatInr(transaction.inrValue)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSend) TextPrimary else SuccessGreen
                    )

                    Surface(
                        color = if (isCompleted) SuccessGreen.copy(alpha = 0.18f) else GoldPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = transaction.status.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) SuccessGreen else GoldPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Expandable details block
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = NavyCardBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Date & Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = transaction.createdAt?.take(16)?.replace("T", " ") ?: "Recent",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // INR Equivalent / Crypto Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isSend) "INR Value" else "Crypto Sold", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = if (isSend) "≈ ₹${formatInr(transaction.inrValue)}" else "${transaction.amount} ${transaction.currency}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldLight
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Destination / Account
                    val dest = transaction.recipientAddress ?: ""
                    if (dest.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isSend) "Recipient" else "Account", fontSize = 11.sp, color = TextSecondary)
                            val displayDest = if (dest.length > 18) "${dest.take(8)}...${dest.takeLast(6)}" else dest
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(dest))
                                    Toast.makeText(context, "Copied: $displayDest", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(displayDest, fontSize = 11.sp, color = AccentCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // TxID Hash
                    val txId = transaction.txHash?.ifBlank { null } ?: ("0x" + transaction.id.replace("-", ""))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TxID", fontSize = 11.sp, color = TextSecondary)
                        val displayHash = if (txId.length > 18) "${txId.take(8)}...${txId.takeLast(6)}" else txId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(txId))
                                Toast.makeText(context, "TxID copied", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(displayHash, fontSize = 11.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        }
                    }

                    if (isSend && transaction.networkFee > 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Network Fee", fontSize = 11.sp, color = TextSecondary)
                            Text("${transaction.networkFee} ${transaction.currency}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Small expand / collapse chevron indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
