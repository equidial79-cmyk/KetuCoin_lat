package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CryptoPriceCard
import com.example.ui.components.KetuCoinLogo
import com.example.ui.components.KycStatusBadge
import com.example.ui.components.formatInr
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
import com.example.ui.viewmodel.KetuCoinViewModel

@Composable
fun HomeScreen(
    viewModel: KetuCoinViewModel,
    onNavigateToSell: () -> Unit,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val marketRates by viewModel.marketRates.collectAsStateWithLifecycle()
    val totalBalanceInr by viewModel.totalBalanceInr.collectAsStateWithLifecycle()

    var calcCryptoAmount by remember { mutableStateOf("100") }
    var selectedCalcCurrency by remember { mutableStateOf("USDT") }

    val bannerScrollState = rememberScrollState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Row: Brand Logo, Greeting & Chat Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KetuCoinLogo(sizeDp = 44)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "KetuCoin Exchange",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        currentProfile?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                KycStatusBadge(status = it.kycStatus)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SuccessGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(SuccessGreen)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cloud Live", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onNavigateToChat,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NavyCard)
                        .border(1.dp, NavyCardBorder, CircleShape)
                        .testTag("home_support_chat_button")
                ) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = SuccessGreen)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "24/7 Support Desk",
                            tint = GoldPrimary
                        )
                    }
                }
            }
        }

        // Promotional / Event Offers Section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Promotional & Event Offers",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(bannerScrollState),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PromoBannerCard(
                        title = "Zero Fee Diwali Bonanza",
                        tagline = "0% conversion fee on USDT to INR transfers over ₹50,000",
                        badge = "LIMITED TIME",
                        gradient = listOf(Color(0xFFB45309), Color(0xFF78350F)),
                        onClick = onNavigateToSell
                    )

                    PromoBannerCard(
                        title = "Instant IMPS Payouts",
                        tagline = "Sell crypto directly into your Indian bank account in 15 mins",
                        badge = "SUPERFAST",
                        gradient = listOf(Color(0xFF065F46), Color(0xFF064E3B)),
                        onClick = onNavigateToSell
                    )

                    PromoBannerCard(
                        title = "VIP Trader Tier",
                        tagline = "Bulk OTC deals for BTC & ETH with custom settlement rates",
                        badge = "OTC DESK",
                        gradient = listOf(Color(0xFF1E3A8A), Color(0xFF172554)),
                        onClick = onNavigateToChat
                    )
                }
            }
        }

        // Live Crypto to INR Converter Card
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
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Crypto to INR Calculator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Live OTC Rate",
                            fontSize = 12.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = calcCryptoAmount,
                            onValueChange = { calcCryptoAmount = it },
                            label = { Text("Amount ($selectedCalcCurrency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = NavyCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Currency toggles
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("USDT", "BTC", "ETH", "SOL").forEach { curr ->
                                val isSelected = selectedCalcCurrency == curr
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GoldPrimary else NavyDark)
                                        .border(1.dp, if (isSelected) GoldPrimary else NavyCardBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedCalcCurrency = curr }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = curr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val selectedRate = marketRates.find { it.currency == selectedCalcCurrency }?.currentPriceInr ?: 90.0
                    val amountVal = calcCryptoAmount.toDoubleOrNull() ?: 0.0
                    val estimatedInr = amountVal * selectedRate

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavyDark)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimated INR Payout:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = "₹${formatInr(estimatedInr)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(onClick = onNavigateToSell)
                                .padding(4.dp)
                        ) {
                            Text("Sell Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Market Prices Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Market Rates",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "INR Pairs",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Crypto price cards
        items(marketRates) { cryptoItem ->
            CryptoPriceCard(
                item = cryptoItem,
                onClick = onNavigateToSell
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PromoBannerCard(
    title: String,
    tagline: String,
    badge: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradient))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldLight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Icon(Icons.Default.Celebration, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
            }

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = tagline,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2
            )
        }
    }
}
