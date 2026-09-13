package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.CryptoMarketItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SparklineChart(
    points: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val strokeColor = if (isPositive) SuccessGreen else ErrorRed
    val gradientColor = if (isPositive) SuccessGreen.copy(alpha = 0.25f) else ErrorRed.copy(alpha = 0.25f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val min = points.minOrNull() ?: 0.0
        val max = points.maxOrNull() ?: 1.0
        val range = if (max - min > 0.0001) max - min else 1.0

        val stepX = width / (points.size - 1)
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val normalizedY = ((pt - min) / range).toFloat()
            val y = height - (normalizedY * (height * 0.8f) + (height * 0.1f))

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

@Composable
fun CryptoPriceCard(
    item: CryptoMarketItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPositive = item.change24h >= 0
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("crypto_card_${item.currency.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Coin Icon & Names
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinAvatar(currency = item.currency)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.currency,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = item.name,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Sparkline Graph
            SparklineChart(
                points = item.sparkline,
                isPositive = isPositive,
                modifier = Modifier
                    .width(72.dp)
                    .height(36.dp)
            )

            // Price & Change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${formatInr(item.currentPriceInr)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isPositive) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${"%.2f".format(item.change24h)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPositive) SuccessGreen else ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
fun KetuCoinLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 44,
    showBorder: Boolean = true
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp * 0.24).dp))
            .background(Color(0xFF070D1E))
            .then(
                if (showBorder) {
                    Modifier.border(
                        1.dp,
                        Brush.linearGradient(listOf(GoldPrimary.copy(alpha = 0.6f), Color(0xFF10B981).copy(alpha = 0.4f))),
                        RoundedCornerShape((sizeDp * 0.24).dp)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_ketu_logo),
            contentDescription = "KetuCoin Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Composable
fun CoinAvatar(currency: String, sizeDp: Int = 40) {
    val (bgColor, labelColor) = when (currency.uppercase()) {
        "USDT" -> Pair(Color(0xFF26A17B), Color.White)
        "BTC" -> Pair(Color(0xFFF7931A), Color.White)
        "ETH" -> Pair(Color(0xFF627EEA), Color.White)
        "SOL" -> Pair(Color(0xFF14F195), Color(0xFF0F172A))
        else -> Pair(GoldPrimary, Color.Black)
    }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currency.take(1).uppercase(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (sizeDp * 0.45).sp,
            color = labelColor
        )
    }
}

@Composable
fun SecurityPinDialog(
    isOpen: Boolean,
    title: String = "Security PIN Verification",
    description: String = "Please enter your 4-digit security PIN to authorize this action.",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!isOpen) return

    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(description, fontSize = 14.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            pinText = it
                            pinError = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_pin_input"),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    placeholder = { Text("Enter 4-digit PIN", color = TextMuted) },
                    isError = pinError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = NavyCardBorder
                    )
                )
                if (pinError) {
                    Text(
                        text = "Please enter all 4 digits",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText.length == 4) {
                        onConfirm(pinText)
                        pinText = ""
                    } else {
                        pinError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("pin_confirm_button")
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    pinText = ""
                    onDismiss()
                }
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = NavyCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun KycStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (badgeBg, badgeText, badgeColor) = when (status.lowercase()) {
        "approved" -> Triple(SuccessGreen.copy(alpha = 0.2f), "KYC Verified", SuccessGreen)
        "pending" -> Triple(GoldPrimary.copy(alpha = 0.2f), "KYC Pending", GoldPrimary)
        "rejected" -> Triple(ErrorRed.copy(alpha = 0.2f), "KYC Rejected", ErrorRed)
        else -> Triple(Color(0xFF64748B).copy(alpha = 0.2f), "KYC Unverified", Color(0xFF94A3B8))
    }

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = badgeText,
                color = badgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun QrCodeDisplay(
    qrUrl: String,
    contentDescription: String = "Deposit QR Code",
    sizeDp: Int = 180,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = qrUrl,
            contentDescription = contentDescription,
            loading = {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = NavyDark,
                    strokeWidth = 3.dp
                )
            },
            error = {
                // Fallback elegant dynamic QR placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Scan to Deposit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun formatInr(amount: Double): String {
    return when {
        amount >= 10_000_000 -> "%.2f Cr".format(amount / 10_000_000)
        amount >= 100_000 -> "%.2f L".format(amount / 100_000)
        amount >= 1_000 -> "%,.2f".format(amount)
        else -> "%.2f".format(amount)
    }
}
