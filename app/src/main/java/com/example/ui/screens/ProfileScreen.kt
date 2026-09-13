package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color
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
                        viewModel.updateBankAccount(editAccNum, editIfsc, editBankName, editHolderName)
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
}
