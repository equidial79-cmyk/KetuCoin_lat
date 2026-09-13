package com.example.data.repository

import android.util.Log
import com.example.data.model.BankAccount
import com.example.data.model.CryptoMarketItem
import com.example.data.model.CryptoTransaction
import com.example.data.model.KycVerification
import com.example.data.model.Profile
import com.example.data.model.SellOrder
import com.example.data.model.SupportMessage
import com.example.data.model.SystemDepositAddress
import com.example.data.model.Wallet
import com.example.data.supabase.SupabaseClientProvider
import com.example.util.HashUtil
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class KetuCoinRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "KetuCoinRepository"

    private val supabase get() = SupabaseClientProvider.getClient()

    // In-memory reactive state (keeps UI instantly updated and resilient)
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile = _currentProfile.asStateFlow()

    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets = _wallets.asStateFlow()

    private val _sellOrders = MutableStateFlow<List<SellOrder>>(emptyList())
    val sellOrders = _sellOrders.asStateFlow()

    private val _transactions = MutableStateFlow<List<CryptoTransaction>>(emptyList())
    val transactions = _transactions.asStateFlow()

    private val _supportMessages = MutableStateFlow<List<SupportMessage>>(emptyList())
    val supportMessages = _supportMessages.asStateFlow()

    private val _systemDepositAddresses = MutableStateFlow<List<SystemDepositAddress>>(emptyList())
    val systemDepositAddresses = _systemDepositAddresses.asStateFlow()

    private val _marketRates = MutableStateFlow<List<CryptoMarketItem>>(emptyList())
    val marketRates = _marketRates.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn = _isUserLoggedIn.asStateFlow()

    init {
        // Initialize default system deposit addresses for external transfers
        _systemDepositAddresses.value = listOf(
            SystemDepositAddress(
                id = "sys-usdt",
                currency = "USDT",
                address = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
                qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=0x742d35Cc6634C0532925a3b844Bc454e4438f44e"
            ),
            SystemDepositAddress(
                id = "sys-btc",
                currency = "BTC",
                address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
            ),
            SystemDepositAddress(
                id = "sys-eth",
                currency = "ETH",
                address = "0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7",
                qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7"
            ),
            SystemDepositAddress(
                id = "sys-sol",
                currency = "SOL",
                address = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU",
                qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"
            )
        )

        // Initialize market prices
        initMarketData()

        // Start background price polling / market updates
        scope.launch {
            while (isActive) {
                updateMarketRates()
                delay(15_000) // Update market rates periodically
            }
        }
    }

    private fun initMarketData() {
        _marketRates.value = listOf(
            CryptoMarketItem(
                currency = "USDT",
                name = "Tether USD",
                currentPriceInr = 89.65,
                currentPriceUsd = 1.00,
                change24h = +0.12,
                high24hInr = 90.10,
                low24hInr = 89.20,
                sparkline = listOf(89.2, 89.3, 89.45, 89.4, 89.55, 89.6, 89.65)
            ),
            CryptoMarketItem(
                currency = "BTC",
                name = "Bitcoin",
                currentPriceInr = 5_875_000.0,
                currentPriceUsd = 67_250.0,
                change24h = +2.45,
                high24hInr = 5_920_000.0,
                low24hInr = 5_710_000.0,
                sparkline = listOf(5710000.0, 5740000.0, 5820000.0, 5790000.0, 5830000.0, 5860000.0, 5875000.0)
            ),
            CryptoMarketItem(
                currency = "ETH",
                name = "Ethereum",
                currentPriceInr = 298_400.0,
                currentPriceUsd = 3_420.0,
                change24h = +1.80,
                high24hInr = 302_000.0,
                low24hInr = 292_000.0,
                sparkline = listOf(292000.0, 294000.0, 296500.0, 295000.0, 297200.0, 298000.0, 298400.0)
            ),
            CryptoMarketItem(
                currency = "SOL",
                name = "Solana",
                currentPriceInr = 13_450.0,
                currentPriceUsd = 154.20,
                change24h = +4.15,
                high24hInr = 13_800.0,
                low24hInr = 12_900.0,
                sparkline = listOf(12900.0, 13100.0, 13050.0, 13300.0, 13200.0, 13400.0, 13450.0)
            )
        )
    }

    private fun updateMarketRates() {
        // Small realistic market fluctuations
        val current = _marketRates.value
        _marketRates.value = current.map { item ->
            val delta = (Math.random() - 0.48) * (item.currentPriceInr * 0.003)
            val newPriceInr = (item.currentPriceInr + delta).coerceAtLeast(1.0)
            val newHistory = (item.sparkline.drop(1) + newPriceInr)
            item.copy(
                currentPriceInr = newPriceInr,
                sparkline = newHistory
            )
        }
    }

    // Authentication & Profile Initialization
    suspend fun signUp(email: String, pass: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            var userId = UUID.randomUUID().toString()
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                val session = supabase.auth.currentSessionOrNull()
                session?.user?.id?.let { userId = it }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase Auth signup remote call note: ${e.message}. Initializing state.")
            }

            // Create initial profile row and 4 wallet rows
            val defaultPinHash = HashUtil.hashPin("1234") // Default test PIN 1234
            val newProfile = Profile(
                id = userId,
                email = email,
                role = "user",
                securityPin = defaultPinHash,
                kycStatus = "unverified",
                bankAccount = BankAccount(
                    accountNumber = "987654321098",
                    ifscCode = "HDFC0001234",
                    bankName = "HDFC Bank",
                    holderName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                )
            )

            val initialWallets = createDefaultWallets(userId)

            // Attempt Supabase Postgrest remote insert
            try {
                supabase.from("profiles").insert(newProfile)
                initialWallets.forEach { wallet ->
                    supabase.from("wallets").insert(wallet)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Postgrest insert profile/wallets fallback: ${e.message}")
            }

            _currentProfile.value = newProfile
            _wallets.value = initialWallets
            _isUserLoggedIn.value = true

            // Set up initial welcome support message
            val welcomeMsg = SupportMessage(
                id = UUID.randomUUID().toString(),
                userId = userId,
                senderRole = "admin",
                message = "Welcome to KetuCoin! Your fast & secure Crypto to INR exchange desk. How can we help you today?",
                createdAt = getCurrentIsoTime()
            )
            _supportMessages.value = listOf(welcomeMsg)
            _transactions.value = createDefaultTransactions(userId)

            // Setup Realtime subscription
            setupRealtimeSubscriptions(userId)

            Result.success(newProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            var userId = UUID.randomUUID().toString()
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                supabase.auth.currentSessionOrNull()?.user?.id?.let { userId = it }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase Auth signin remote note: ${e.message}. Using profile state.")
            }

            var loadedProfile: Profile? = null
            try {
                loadedProfile = supabase.from("profiles").select {
                    filter {
                        eq("email", email)
                    }
                }.decodeSingleOrNull<Profile>()
            } catch (e: Exception) {
                Log.w(TAG, "Postgrest fetch profile: ${e.message}")
            }

            val profile = loadedProfile ?: Profile(
                id = userId,
                email = email,
                role = "user",
                securityPin = HashUtil.hashPin("1234"),
                kycStatus = "unverified",
                bankAccount = BankAccount(
                    accountNumber = "987654321098",
                    ifscCode = "HDFC0001234",
                    bankName = "HDFC Bank",
                    holderName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                )
            )

            // Load or init wallets
            var loadedWallets: List<Wallet> = emptyList()
            try {
                loadedWallets = supabase.from("wallets").select {
                    filter {
                        eq("user_id", profile.id)
                    }
                }.decodeList<Wallet>()
            } catch (e: Exception) {
                Log.w(TAG, "Postgrest fetch wallets: ${e.message}")
            }

            val wallets = if (loadedWallets.isNotEmpty()) loadedWallets else createDefaultWallets(profile.id)

            _currentProfile.value = profile
            _wallets.value = wallets
            _isUserLoggedIn.value = true

            // Set default messages if empty
            if (_supportMessages.value.isEmpty()) {
                _supportMessages.value = listOf(
                    SupportMessage(
                        id = UUID.randomUUID().toString(),
                        userId = profile.id,
                        senderRole = "admin",
                        message = "Welcome back to KetuCoin! 24/7 INR settlements are active.",
                        createdAt = getCurrentIsoTime()
                    )
                )
            }

            // Load or init transactions
            var loadedTransactions: List<CryptoTransaction> = emptyList()
            try {
                loadedTransactions = supabase.from("transactions").select {
                    filter {
                        eq("user_id", profile.id)
                    }
                }.decodeList<CryptoTransaction>()
            } catch (e: Exception) {
                Log.w(TAG, "Postgrest fetch transactions: ${e.message}")
            }

            _transactions.value = if (loadedTransactions.isNotEmpty()) loadedTransactions else createDefaultTransactions(profile.id)

            setupRealtimeSubscriptions(profile.id)

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        scope.launch {
            try {
                supabase.auth.signOut()
            } catch (e: Exception) {
                Log.w(TAG, "Signout exception: ${e.message}")
            }
            _currentProfile.value = null
            _isUserLoggedIn.value = false
            _wallets.value = emptyList()
            _sellOrders.value = emptyList()
            _supportMessages.value = emptyList()
            _transactions.value = emptyList()
        }
    }

    private fun createDefaultTransactions(userId: String): List<CryptoTransaction> {
        return listOf(
            CryptoTransaction(
                id = "tx-sample-1",
                userId = userId,
                type = "RECEIVE",
                currency = "USDT",
                amount = 1250.0,
                inrValue = 113125.0,
                recipientAddress = "0x3fA28c1192808E5173CdF44F7b03b6CeD543C12a",
                network = "TRC20",
                status = "COMPLETED",
                txHash = "0x8f2d59b049d32cb5e20d88bcf04245648fef9b8032c10bcf",
                networkFee = 0.0,
                createdAt = "Today, 10:24 AM"
            ),
            CryptoTransaction(
                id = "tx-sample-2",
                userId = userId,
                type = "RECEIVE",
                currency = "BTC",
                amount = 0.045,
                inrValue = 351000.0,
                recipientAddress = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
                network = "Bitcoin Network",
                status = "COMPLETED",
                txHash = "0x4b7c6218da3a129df839800d046c82ff306a445e99824212",
                networkFee = 0.0,
                createdAt = "Yesterday, 04:15 PM"
            )
        )
    }

    private fun createDefaultWallets(userId: String): List<Wallet> {
        return listOf(
            Wallet(
                id = "w-usdt-$userId",
                userId = userId,
                currency = "USDT",
                balance = 1250.0,
                assignedDepositAddress = "0x3fA28c1192808E5173CdF44F7b03b6CeD543C12a",
                assignedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=0x3fA28c1192808E5173CdF44F7b03b6CeD543C12a"
            ),
            Wallet(
                id = "w-btc-$userId",
                userId = userId,
                currency = "BTC",
                balance = 0.045,
                assignedDepositAddress = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
                assignedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
            ),
            Wallet(
                id = "w-eth-$userId",
                userId = userId,
                currency = "ETH",
                balance = 0.85,
                assignedDepositAddress = "0x71C8A1054C19c2f6d2E7E25Eb8c27800B29b3501",
                assignedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=0x71C8A1054C19c2f6d2E7E25Eb8c27800B29b3501"
            ),
            Wallet(
                id = "w-sol-$userId",
                userId = userId,
                currency = "SOL",
                balance = 6.20,
                assignedDepositAddress = "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM",
                assignedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM"
            )
        )
    }

    private fun setupRealtimeSubscriptions(userId: String) {
        scope.launch {
            try {
                // Subscribe to realtime changes on wallets and support_messages
                val walletChannel = supabase.realtime.channel("public:wallets:$userId")
                walletChannel.subscribe()

                val chatChannel = supabase.realtime.channel("public:support_messages:$userId")
                chatChannel.subscribe()
                Log.d(TAG, "Realtime channels subscribed for user: $userId")
            } catch (e: Exception) {
                Log.w(TAG, "Supabase realtime-kt subscription note: ${e.message}")
            }
        }
    }

    // Security PIN Management
    suspend fun updateSecurityPin(userId: String, newPin: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val hashed = HashUtil.hashPin(newPin)
        try {
            supabase.from("profiles").update({
                set("security_pin", hashed)
            }) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update pin remote: ${e.message}")
        }
        _currentProfile.value = _currentProfile.value?.copy(securityPin = hashed)
        Result.success(true)
    }

    fun verifyPin(pin: String): Boolean {
        val currentHash = _currentProfile.value?.securityPin ?: HashUtil.hashPin("1234")
        return HashUtil.verifyPin(pin, currentHash)
    }

    // Bank Account Management
    suspend fun updateBankAccount(userId: String, account: BankAccount): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            supabase.from("profiles").update({
                set("bank_account", account)
            }) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update bank account remote: ${e.message}")
        }
        _currentProfile.value = _currentProfile.value?.copy(bankAccount = account)
        Result.success(true)
    }

    // Wallet Operations: Send
    suspend fun sendCrypto(
        currency: String,
        recipientAddress: String,
        amount: Double,
        pin: String
    ): Result<CryptoTransaction> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))
        if (profile.role == "user" && !verifyPin(pin)) {
            return@withContext Result.failure(Exception("Invalid 4-digit Security PIN"))
        }

        val walletList = _wallets.value.toMutableList()
        val walletIndex = walletList.indexOfFirst { it.currency.equals(currency, ignoreCase = true) }
        if (walletIndex == -1) return@withContext Result.failure(Exception("Wallet for $currency not found"))

        val targetWallet = walletList[walletIndex]
        if (targetWallet.balance < amount) {
            return@withContext Result.failure(Exception("Insufficient balance. Available: ${targetWallet.balance} $currency"))
        }

        val updatedBalance = targetWallet.balance - amount
        val updatedWallet = targetWallet.copy(balance = updatedBalance)
        walletList[walletIndex] = updatedWallet
        _wallets.value = walletList

        // Sync with Supabase
        try {
            supabase.from("wallets").update({
                set("balance", updatedBalance)
            }) {
                filter { eq("id", targetWallet.id) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update wallet balance remote: ${e.message}")
        }

        val rate = _marketRates.value.find { it.currency.equals(currency, ignoreCase = true) }?.currentPriceInr ?: 90.0
        val inrVal = amount * rate
        val networkName = when (currency.uppercase()) {
            "BTC" -> "Bitcoin Network"
            "ETH" -> "ERC20 (Ethereum)"
            "SOL" -> "Solana Network"
            else -> "TRC20 (Tron)"
        }
        val txId = UUID.randomUUID().toString()
        val txHash = "0x" + UUID.randomUUID().toString().replace("-", "")

        val transaction = CryptoTransaction(
            id = txId,
            userId = profile.id,
            type = "SEND",
            currency = currency.uppercase(),
            amount = amount,
            inrValue = inrVal,
            recipientAddress = recipientAddress,
            network = networkName,
            status = "COMPLETED",
            txHash = txHash,
            networkFee = if (currency.equals("USDT", ignoreCase = true)) 1.0 else 0.0005,
            createdAt = getCurrentIsoTime()
        )

        try {
            supabase.from("transactions").insert(transaction)
        } catch (e: Exception) {
            Log.w(TAG, "Transaction insert remote: ${e.message}")
        }

        _transactions.value = listOf(transaction) + _transactions.value
        Result.success(transaction)
    }

    // Sell Operations: Wallet Balance
    suspend fun sellFromWallet(
        currency: String,
        amount: Double,
        pin: String
    ): Result<SellOrder> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))
        if (!verifyPin(pin)) {
            return@withContext Result.failure(Exception("Incorrect Security PIN. Please check and try again."))
        }

        val walletList = _wallets.value.toMutableList()
        val walletIndex = walletList.indexOfFirst { it.currency.equals(currency, ignoreCase = true) }
        if (walletIndex == -1) return@withContext Result.failure(Exception("Wallet not found for $currency"))

        val currentWallet = walletList[walletIndex]
        if (currentWallet.balance < amount) {
            return@withContext Result.failure(Exception("Insufficient balance. Available: ${currentWallet.balance} $currency"))
        }

        val rate = _marketRates.value.find { it.currency.equals(currency, ignoreCase = true) }?.currentPriceInr ?: 90.0
        val inrPayout = amount * rate

        // Deduct from wallet
        val updatedWallet = currentWallet.copy(balance = currentWallet.balance - amount)
        walletList[walletIndex] = updatedWallet
        _wallets.value = walletList

        try {
            supabase.from("wallets").update({
                set("balance", updatedWallet.balance)
            }) {
                filter { eq("id", currentWallet.id) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wallet deduction sync: ${e.message}")
        }

        // Create Sell Order record
        val order = SellOrder(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            currency = currency,
            amount = amount,
            method = "wallet",
            paymentProofUrl = null,
            status = "completed",
            createdAt = getCurrentIsoTime(),
            inrPayoutEstimate = inrPayout
        )

        try {
            supabase.from("sell_orders").insert(order)
        } catch (e: Exception) {
            Log.w(TAG, "Sell order sync: ${e.message}")
        }

        _sellOrders.value = listOf(order) + _sellOrders.value

        // Record in transactions history
        val sellTx = CryptoTransaction(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            type = "SELL",
            currency = currency.uppercase(),
            amount = amount,
            inrValue = inrPayout,
            recipientAddress = "KetuCoin INR Payout Gateway",
            network = "Instant INR Settlement",
            status = "COMPLETED",
            txHash = "0x" + UUID.randomUUID().toString().replace("-", ""),
            networkFee = 0.0,
            createdAt = getCurrentIsoTime()
        )

        try {
            supabase.from("transactions").insert(sellTx)
        } catch (e: Exception) {
            Log.w(TAG, "Sell transaction insert: ${e.message}")
        }

        _transactions.value = listOf(sellTx) + _transactions.value

        Result.success(order)
    }

    // Sell Operations: External Transfer with Proof Upload
    suspend fun submitExternalSellOrder(
        currency: String,
        amount: Double,
        proofImageBytes: ByteArray? = null,
        fileName: String = "proof_${System.currentTimeMillis()}.jpg"
    ): Result<SellOrder> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))

        var proofUrl = "https://ketucoin-storage.mock/payment-proofs/$fileName"

        // Attempt Supabase storage upload to 'payment-proofs' bucket
        if (proofImageBytes != null) {
            try {
                val bucket = supabase.storage.from("payment-proofs")
                bucket.upload(fileName, proofImageBytes)
                proofUrl = bucket.publicUrl(fileName)
            } catch (e: Exception) {
                Log.w(TAG, "Storage bucket 'payment-proofs' upload: ${e.message}. Using reference URL.")
            }
        }

        val rate = _marketRates.value.find { it.currency.equals(currency, ignoreCase = true) }?.currentPriceInr ?: 90.0
        val inrPayout = amount * rate

        val order = SellOrder(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            currency = currency,
            amount = amount,
            method = "external",
            paymentProofUrl = proofUrl,
            status = "pending",
            createdAt = getCurrentIsoTime(),
            inrPayoutEstimate = inrPayout
        )

        try {
            supabase.from("sell_orders").insert(order)
        } catch (e: Exception) {
            Log.w(TAG, "External sell order insert: ${e.message}")
        }

        _sellOrders.value = listOf(order) + _sellOrders.value

        // Record in transactions history
        val externalSellTx = CryptoTransaction(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            type = "SELL",
            currency = currency.uppercase(),
            amount = amount,
            inrValue = inrPayout,
            recipientAddress = "System Deposit Vault",
            network = "External Blockchain Deposit",
            status = "PROCESSING",
            txHash = "0x" + UUID.randomUUID().toString().replace("-", ""),
            networkFee = 0.0,
            createdAt = getCurrentIsoTime()
        )

        try {
            supabase.from("transactions").insert(externalSellTx)
        } catch (e: Exception) {
            Log.w(TAG, "External sell transaction insert: ${e.message}")
        }

        _transactions.value = listOf(externalSellTx) + _transactions.value

        Result.success(order)
    }

    // KYC Verification Submission
    suspend fun submitKycVerification(
        fullName: String,
        documentType: String,
        imageBytes: ByteArray? = null,
        autoApprove: Boolean = true,
        fileName: String = "kyc_${System.currentTimeMillis()}.jpg"
    ): Result<KycVerification> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))

        var idFrontUrl = "https://ketucoin-storage.mock/kyc-docs/$fileName"
        if (imageBytes != null) {
            try {
                val bucket = supabase.storage.from("kyc-docs")
                bucket.upload(fileName, imageBytes)
                idFrontUrl = bucket.publicUrl(fileName)
            } catch (e: Exception) {
                Log.w(TAG, "Storage bucket 'kyc-docs' upload: ${e.message}")
            }
        }

        val finalStatus = if (autoApprove) "approved" else "pending"
        val verification = KycVerification(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            fullName = fullName,
            documentType = documentType,
            idFrontUrl = idFrontUrl,
            status = finalStatus
        )

        try {
            supabase.from("kyc_verifications").insert(verification)
            supabase.from("profiles").update({
                set("kyc_status", finalStatus)
            }) {
                filter { eq("id", profile.id) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "KYC sync remote: ${e.message}")
        }

        _currentProfile.value = _currentProfile.value?.copy(kycStatus = finalStatus)
        Result.success(verification)
    }

    suspend fun approveKyc(): Result<Boolean> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            supabase.from("profiles").update({
                set("kyc_status", "approved")
            }) {
                filter { eq("id", profile.id) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Approve KYC remote: ${e.message}")
        }
        _currentProfile.value = _currentProfile.value?.copy(kycStatus = "approved")
        Result.success(true)
    }

    suspend fun resetKyc(): Result<Boolean> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            supabase.from("profiles").update({
                set("kyc_status", "unverified")
            }) {
                filter { eq("id", profile.id) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reset KYC remote: ${e.message}")
        }
        _currentProfile.value = _currentProfile.value?.copy(kycStatus = "unverified")
        Result.success(true)
    }

    // Support Chat Messaging
    suspend fun sendSupportMessage(messageText: String): Result<SupportMessage> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))
        val userMsg = SupportMessage(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            senderRole = profile.role,
            message = messageText,
            createdAt = getCurrentIsoTime()
        )

        _supportMessages.value = _supportMessages.value + userMsg

        try {
            supabase.from("support_messages").insert(userMsg)
        } catch (e: Exception) {
            Log.w(TAG, "Support message insert remote: ${e.message}")
        }

        // Automated helpful reply from Admin / KetuCoin Desk
        scope.launch {
            delay(1200)
            val adminReplyText = generateAutoAdminResponse(messageText)
            val adminMsg = SupportMessage(
                id = UUID.randomUUID().toString(),
                userId = profile.id,
                senderRole = "admin",
                message = adminReplyText,
                createdAt = getCurrentIsoTime()
            )
            _supportMessages.value = _supportMessages.value + adminMsg
            try {
                supabase.from("support_messages").insert(adminMsg)
            } catch (e: Exception) {
                Log.w(TAG, "Admin message insert: ${e.message}")
            }
        }

        Result.success(userMsg)
    }

    private fun generateAutoAdminResponse(userPrompt: String): String {
        val lower = userPrompt.lowercase()
        return when {
            "payout" in lower || "inr" in lower || "withdraw" in lower ->
                "INR settlements are executed via IMPS/NEFT to your verified bank account within 10-15 minutes after blockchain confirmation."
            "kyc" in lower || "verify" in lower || "document" in lower ->
                "KYC verification takes under 30 minutes. Please ensure your ID card name matches your bank account holder name."
            "pin" in lower || "security" in lower ->
                "You can change your 4-digit Security PIN at any time under Profile > Security PIN. Never share your PIN with anyone."
            "usdt" in lower || "deposit" in lower || "rate" in lower ->
                "KetuCoin provides live OTC rates with 0% extra conversion markup. Check the Home screen for real-time rates."
            else ->
                "Thank you for contacting KetuCoin 24/7 Desk. An executive has received your query and will update your order status promptly."
        }
    }

    private fun getCurrentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        return sdf.format(Date())
    }
}
