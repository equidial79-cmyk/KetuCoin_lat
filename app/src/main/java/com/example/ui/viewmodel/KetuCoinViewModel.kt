package com.example.ui.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppUpdateInfo
import com.example.data.model.BankAccount
import com.example.data.model.CryptoMarketItem
import com.example.data.model.CryptoTransaction
import com.example.data.model.Profile
import com.example.data.model.SellOrder
import com.example.data.model.SupportMessage
import com.example.data.model.SystemDepositAddress
import com.example.data.model.Wallet
import com.example.data.repository.KetuCoinRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KetuCoinViewModel(
    private val repository: KetuCoinRepository = KetuCoinRepository()
) : ViewModel() {

    val isUserLoggedIn: StateFlow<Boolean> = repository.isUserLoggedIn
    val currentProfile: StateFlow<Profile?> = repository.currentProfile
    val wallets: StateFlow<List<Wallet>> = repository.wallets
    val marketRates: StateFlow<List<CryptoMarketItem>> = repository.marketRates
    val sellOrders: StateFlow<List<SellOrder>> = repository.sellOrders
    val supportMessages: StateFlow<List<SupportMessage>> = repository.supportMessages
    val systemDepositAddresses: StateFlow<List<SystemDepositAddress>> = repository.systemDepositAddresses
    val transactions: StateFlow<List<CryptoTransaction>> = repository.transactions
    val latestAppUpdate: StateFlow<AppUpdateInfo?> = repository.latestAppUpdate

    // Network connectivity state
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun updateNetworkState(connected: Boolean) {
        _isConnected.value = connected
    }

    // Compute Total Balance in INR and USD
    val totalBalanceInr: StateFlow<Double> = combine(wallets, marketRates) { walletList, rates ->
        walletList.sumOf { w ->
            val rate = rates.find { it.currency.equals(w.currency, ignoreCase = true) }?.currentPriceInr ?: 90.0
            w.balance * rate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalanceUsd: StateFlow<Double> = combine(wallets, marketRates) { walletList, rates ->
        walletList.sumOf { w ->
            val rate = rates.find { it.currency.equals(w.currency, ignoreCase = true) }?.currentPriceUsd ?: 1.0
            w.balance * rate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val showSendModal: Boolean = false,
        val showReceiveModal: Boolean = false,
        val showKycModal: Boolean = false,
        val showBankEditModal: Boolean = false,
        val showChangePinModal: Boolean = false,
        val selectedWalletCurrency: String = "USDT",
        val showSendSuccessWindow: Boolean = false,
        val lastSendTransaction: CryptoTransaction? = null,
        val showTransactionHistoryModal: Boolean = false,
        val showUpdateDetailDialog: Boolean = false,
        val showAdminPushUpdateDialog: Boolean = false,
        val isCheckingUpdate: Boolean = false
    )

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun openSendModal(currency: String = "USDT") {
        _uiState.value = _uiState.value.copy(showSendModal = true, selectedWalletCurrency = currency)
    }

    fun closeSendModal() {
        _uiState.value = _uiState.value.copy(showSendModal = false)
    }

    fun openReceiveModal(currency: String = "USDT") {
        _uiState.value = _uiState.value.copy(showReceiveModal = true, selectedWalletCurrency = currency)
    }

    fun closeReceiveModal() {
        _uiState.value = _uiState.value.copy(showReceiveModal = false)
    }

    fun openKycModal() {
        _uiState.value = _uiState.value.copy(showKycModal = true)
    }

    fun closeKycModal() {
        _uiState.value = _uiState.value.copy(showKycModal = false)
    }

    fun openBankEditModal() {
        _uiState.value = _uiState.value.copy(showBankEditModal = true)
    }

    fun closeBankEditModal() {
        _uiState.value = _uiState.value.copy(showBankEditModal = false)
    }

    fun openChangePinModal() {
        _uiState.value = _uiState.value.copy(showChangePinModal = true)
    }

    fun closeChangePinModal() {
        _uiState.value = _uiState.value.copy(showChangePinModal = false)
    }

    fun openTransactionHistoryModal() {
        _uiState.value = _uiState.value.copy(showTransactionHistoryModal = true)
    }

    fun closeTransactionHistoryModal() {
        _uiState.value = _uiState.value.copy(showTransactionHistoryModal = false)
    }

    fun closeSendSuccessWindow() {
        _uiState.value = _uiState.value.copy(showSendSuccessWindow = false)
    }

    fun openHistoryFromSuccessWindow() {
        _uiState.value = _uiState.value.copy(
            showSendSuccessWindow = false,
            showTransactionHistoryModal = true
        )
    }

    // Authentication Actions
    fun signUp(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter valid email and password")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.signUp(email, pass)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Account created successfully!")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter email and password")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.signIn(email, pass)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Welcome back!")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        repository.signOut()
    }

    // Send Crypto from Wallet
    fun sendCrypto(currency: String, address: String, amount: Double, pin: String) {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a valid amount to send")
            return
        }
        if (address.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Recipient address cannot be empty")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.sendCrypto(currency, address, amount, pin)
            result.onSuccess { tx ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showSendModal = false,
                    showSendSuccessWindow = true,
                    lastSendTransaction = tx,
                    successMessage = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Transfer failed")
            }
        }
    }

    // Sell Crypto from Wallet Balance (PIN protected)
    fun sellWalletBalance(currency: String, amount: Double, pin: String, onSuccess: () -> Unit = {}) {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter an amount greater than 0")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.sellFromWallet(currency, amount, pin)
            result.onSuccess { order ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Sold $amount $currency for ₹${"%,.2f".format(order.inrPayoutEstimate)}! Settlement initiated."
                )
                onSuccess()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Sell failed")
            }
        }
    }

    // Sell Crypto via External Transfer (Proof upload)
    fun submitExternalSell(currency: String, amount: Double, proofBytes: ByteArray?, onSuccess: () -> Unit = {}) {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid amount sent")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.submitExternalSellOrder(currency, amount, proofBytes)
            result.onSuccess { order ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Order created! Payment proof submitted. ₹${"%,.2f".format(order.inrPayoutEstimate)} will be credited upon confirmation."
                )
                onSuccess()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Failed to submit sell order")
            }
        }
    }

    // KYC Submission
    fun submitKyc(fullName: String, docType: String, imageBytes: ByteArray?, autoApprove: Boolean = true) {
        if (fullName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your full legal name")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.submitKycVerification(fullName, docType, imageBytes, autoApprove)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showKycModal = false,
                    successMessage = if (autoApprove) "KYC Verification Approved! Wallet system unlocked." else "KYC documents submitted! Verification is now pending."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "KYC submission failed")
            }
        }
    }

    // Direct Instant KYC Approval (Test & Compliance Desk)
    fun approveKyc() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.approveKyc()
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showKycModal = false,
                    successMessage = "Identity Verified! Multi-currency Wallet system is now fully unlocked."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Approval failed")
            }
        }
    }

    // Reset KYC for testing locked gate
    fun resetKyc() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.resetKyc()
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "KYC status reset to Unverified. Wallet system is now locked."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Reset failed")
            }
        }
    }

    // Bank Account Update
    fun updateBankAccount(accountNumber: String, ifsc: String, bankName: String, holderName: String) {
        val user = currentProfile.value ?: return
        if (accountNumber.isBlank() || ifsc.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Account number and IFSC are required")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val account = BankAccount(accountNumber, ifsc, bankName, holderName)
            val result = repository.updateBankAccount(user.id, account)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showBankEditModal = false,
                    successMessage = "Bank details updated successfully!"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Failed to update bank details")
            }
        }
    }

    // Change Security PIN
    fun changePin(newPin: String) {
        val user = currentProfile.value ?: return
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(errorMessage = "PIN must be exactly 4 numeric digits")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.updateSecurityPin(user.id, newPin)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showChangePinModal = false,
                    successMessage = "Security PIN updated and hashed securely!"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Failed to update PIN")
            }
        }
    }

    // Refresh User Data & Wallets from Supabase
    fun refreshUserData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.refreshUserData()
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Wallets and data synced with server"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to refresh wallets"
                )
            }
        }
    }

    // Support Messaging
    fun sendSupportMessage(msgText: String) {
        if (msgText.isBlank()) return
        viewModelScope.launch {
            repository.sendSupportMessage(msgText)
        }
    }

    // App Update Dialog Actions
    fun openUpdateDetailDialog() {
        _uiState.value = _uiState.value.copy(showUpdateDetailDialog = true)
    }

    fun closeUpdateDetailDialog() {
        _uiState.value = _uiState.value.copy(showUpdateDetailDialog = false)
    }

    fun openAdminPushUpdateDialog() {
        _uiState.value = _uiState.value.copy(showAdminPushUpdateDialog = true)
    }

    fun closeAdminPushUpdateDialog() {
        _uiState.value = _uiState.value.copy(showAdminPushUpdateDialog = false)
    }

    fun checkForUpdates(context: Context? = null) {
        _uiState.value = _uiState.value.copy(isCheckingUpdate = true)
        viewModelScope.launch {
            val result = repository.checkForUpdates()
            _uiState.value = _uiState.value.copy(isCheckingUpdate = false)
            result.onSuccess { update ->
                if (update != null && update.versionCode > 1) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "New update available: ${update.versionName}!"
                    )
                    if (context != null) {
                        NotificationHelper.showUpdateNotification(context, update)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "KetuCoin is already up to date (v1.0)"
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    successMessage = "KetuCoin is up to date (v1.0)"
                )
            }
        }
    }

    fun pushAppUpdate(
        context: Context,
        versionName: String,
        versionCode: Int,
        releaseNotes: String,
        downloadUrl: String,
        isForce: Boolean
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val result = repository.pushAppUpdate(
                versionName = versionName,
                versionCode = versionCode,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                isForceUpdate = isForce
            )
            _uiState.value = _uiState.value.copy(isLoading = false, showAdminPushUpdateDialog = false)
            result.onSuccess { update ->
                // Trigger system push notification immediately
                NotificationHelper.showUpdateNotification(context, update)
                _uiState.value = _uiState.value.copy(
                    successMessage = "🚀 Update ${update.versionName} pushed to all devices with push notification!"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to push update"
                )
            }
        }
    }

    fun downloadAndInstallUpdate(context: Context, update: AppUpdateInfo) {
        try {
            if (update.downloadUrl.isNotBlank() && (update.downloadUrl.startsWith("http://") || update.downloadUrl.startsWith("https://"))) {
                try {
                    val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
                        .setTitle("KetuCoin ${update.versionName} Update")
                        .setDescription("Downloading KetuCoin APK package...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "KetuCoin_${update.versionName}.apk")
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                } catch (e: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                }

                _uiState.value = _uiState.value.copy(
                    showUpdateDetailDialog = false,
                    successMessage = "Starting download for KetuCoin ${update.versionName}. Check notification bar."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Download URL is invalid or empty."
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Unable to start download: ${e.message}"
            )
        }
    }

    fun toggleAdminRole() {
        val currentRole = currentProfile.value?.role ?: "user"
        val nextRole = if (currentRole == "admin") "user" else "admin"
        viewModelScope.launch {
            repository.updateUserRole(nextRole)
            _uiState.value = _uiState.value.copy(
                successMessage = "Switched to ${nextRole.uppercase()} mode"
            )
        }
    }
}
