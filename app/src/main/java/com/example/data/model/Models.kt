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
data class SellOrder(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val currency: String,
    val amount: Double,
    val method: String, // 'wallet' or 'external'
    @SerialName("payment_proof_url") val paymentProofUrl: String? = null,
    val status: String = "pending", // 'pending', 'completed', 'rejected'
    @SerialName("created_at") val createdAt: String? = null,
    val inrPayoutEstimate: Double = 0.0
)

@Serializable
data class SupportMessage(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("sender_role") val senderRole: String, // 'user' or 'admin'
    val message: String,
    @SerialName("created_at") val createdAt: String? = null
)

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

