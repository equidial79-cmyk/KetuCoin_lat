package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BankAccount(
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("ifsc_code") val ifscCode: String = "",
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("holder_name") val holderName: String = ""
)

@Serializable
data class Profile(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val role: String = "user", // 'user' or 'admin'
    @SerialName("security_pin") val securityPin: String? = null, // hashed PIN
    @SerialName("kyc_status") val kycStatus: String = "unverified", // 'pending', 'approved', 'rejected', 'unverified'
    @SerialName("bank_account") val bankAccount: BankAccount? = null
)

@Serializable
data class SupabaseUser(
    val id: String,
    val name: String = "",
    val email: String,
    val phone: String = "",
    @SerialName("kyc_status") val kycStatus: String = "NOT_SUBMITTED",
    @SerialName("is_account_active") val isAccountActive: Boolean = true,
    @SerialName("two_factor_enabled") val twoFactorEnabled: Boolean = false,
    @SerialName("biometrics_enabled") val biometricsEnabled: Boolean = false,
    @SerialName("member_since") val memberSince: String = "",
    @SerialName("daily_limit_inr") val dailyLimitInr: Long = 100000L,
    @SerialName("monthly_limit_inr") val monthlyLimitInr: Long = 1000000L,
    @SerialName("admin_notes") val adminNotes: String = "Signed up via KetuCoin mobile app"
)

@Serializable
data class Wallet(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val currency: String, // USDT, BTC, ETH, SOL
    val balance: Double = 0.0,
    @SerialName("assigned_deposit_address") val assignedDepositAddress: String = "",
    @SerialName("assigned_qr_url") val assignedQrUrl: String = ""
)

@Serializable
data class SystemDepositAddress(
    val id: String = "",
    val currency: String,
    val address: String,
    @SerialName("qr_url") val qrUrl: String = ""
)

@Serializable
data class KycVerification(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("full_name") val fullName: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("id_front_url") val idFrontUrl: String,
    val status: String = "pending" // 'pending', 'approved', 'rejected'
)

@Serializable
data class KycSubmission(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("document_type") val documentType: String = "Aadhaar Card",
    @SerialName("document_number") val documentNumber: String? = null,
    @SerialName("id_front_url") val idFrontUrl: String? = null,
    @SerialName("id_back_url") val idBackUrl: String? = null,
    val status: String = "pending", // 'pending', 'approved', 'rejected'
    @SerialName("admin_notes") val adminNotes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null
) {
    val displayStatus: String get() = status.lowercase()
}

@Serializable
data class SellOrder(
    @SerialName("order_id") val orderId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("crypto_symbol") val cryptoSymbol: String = "USDT",
    @SerialName("crypto_amount") val cryptoAmount: Double = 0.0,
    @SerialName("exchange_rate_inr") val exchangeRateInr: Double = 0.0,
    @SerialName("inr_payout_amount") val inrPayoutAmount: Double = 0.0,
    @SerialName("sell_type") val sellType: String = "FROM_WALLET", // 'FROM_WALLET' or 'EXTERNAL_TRANSFER'
    @SerialName("tx_hash") val txHash: String? = null,
    @SerialName("transfer_proof_name") val transferProofName: String? = null,
    @SerialName("admin_receiving_address") val adminReceivingAddress: String? = null,
    val network: String? = null,
    @SerialName("payout_account") val payoutAccount: String? = null,
    @SerialName("payout_type") val payoutType: String? = "IMPS",
    val status: String = "PENDING_VERIFICATION", // 'PENDING_VERIFICATION', 'PROCESSING', 'COMPLETED', 'REJECTED'
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long? = null,
    @SerialName("admin_notes") val adminNotes: String? = null,
    @SerialName("bank_utr") val bankUtr: String? = null
) {
    // Backwards-compatible properties
    val id: String get() = orderId
    val currency: String get() = cryptoSymbol
    val amount: Double get() = cryptoAmount
    val method: String get() = if (sellType == "FROM_WALLET") "wallet" else "external"
    val inrPayoutEstimate: Double get() = inrPayoutAmount
    val paymentProofUrl: String? get() = transferProofName
}

@Serializable
data class SupportMessage(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val sender: String = "user", // 'user' or 'admin'
    @SerialName("sender_name") val senderName: String? = null,
    val text: String = "",
    @SerialName("message") val bodyMessage: String? = null,
    @SerialName("content") val contentText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("created_at") val createdAtRaw: String? = null,
    @SerialName("is_read") val isRead: Boolean = false
) {
    // Backwards-compatible properties
    val message: String get() = when {
        text.isNotBlank() -> text
        !bodyMessage.isNullOrBlank() -> bodyMessage
        !contentText.isNullOrBlank() -> contentText
        else -> ""
    }
    val senderRole: String get() = sender
    val createdAt: String get() = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}

data class CryptoMarketItem(
    val currency: String,
    val name: String,
    val currentPriceInr: Double,
    val currentPriceUsd: Double,
    val change24h: Double,
    val high24hInr: Double,
    val low24hInr: Double,
    val sparkline: List<Double>
)

@Serializable
data class CryptoTransaction(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val type: String, // "SEND", "SELL", "RECEIVE"
    val currency: String, // "USDT", "BTC", "ETH", "SOL"
    val amount: Double,
    @SerialName("inr_value") val inrValue: Double = 0.0,
    @SerialName("recipient_address") val recipientAddress: String? = null,
    val network: String = "TRC20",
    val status: String = "COMPLETED", // "COMPLETED", "PROCESSING", "FAILED"
    @SerialName("tx_hash") val txHash: String? = null,
    @SerialName("network_fee") val networkFee: Double = 0.0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class AppUpdateInfo(
    val id: String = "",
    @SerialName("version_name") val versionName: String = "1.0",
    @SerialName("version_code") val versionCode: Int = 1,
    @SerialName("release_notes") val releaseNotes: String = "",
    @SerialName("download_url") val downloadUrl: String = "",
    @SerialName("is_force_update") val isForceUpdate: Boolean = false,
    @SerialName("released_at") val releasedAt: String = "",
    @SerialName("file_size_mb") val fileSizeMb: Double = 14.5
)

