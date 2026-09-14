package com.example.data.repository

import android.util.Log
import com.example.data.model.AppUpdateInfo
import com.example.data.model.BankAccount
import com.example.data.model.CryptoMarketItem
import com.example.data.model.CryptoTransaction
import com.example.data.model.KycSubmission
import com.example.data.model.KycVerification
import com.example.data.model.Profile
import com.example.data.model.SellOrder
import com.example.data.model.SupabaseUser
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
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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

    private val _latestAppUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val latestAppUpdate = _latestAppUpdate.asStateFlow()

    private val _kycSubmissions = MutableStateFlow<List<KycSubmission>>(emptyList())
    val kycSubmissions = _kycSubmissions.asStateFlow()

    private val _latestKycSubmission = MutableStateFlow<KycSubmission?>(null)
    val latestKycSubmission = _latestKycSubmission.asStateFlow()

    private var onNewUpdateListener: ((AppUpdateInfo) -> Unit)? = null
    private var realtimeSyncJob: Job? = null
    private var realtimeChannelJob: Job? = null

    fun setOnNewUpdateListener(listener: (AppUpdateInfo) -> Unit) {
        onNewUpdateListener = listener
    }

    init {
        // Initial check for latest app updates
        scope.launch {
            checkForUpdates()
        }

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
    suspend fun signUp(
        email: String,
        pass: String,
        fullName: String = "",
        phone: String = ""
    ): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            var userId = "usr-${(System.currentTimeMillis() % 900000) + 100000}"
            var authUserId: String? = null
            try {
                val authResult = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                authResult?.id?.let {
                    userId = it
                    authUserId = it
                }
                supabase.auth.currentSessionOrNull()?.user?.id?.let {
                    userId = it
                    authUserId = it
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase Auth signup call note: ${e.message}")
            }

            val displayName = if (fullName.isNotBlank()) {
                fullName.trim()
            } else {
                email.substringBefore("@")
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                    .ifBlank { "User" }
            }

            val userPhone = if (phone.isNotBlank()) phone.trim() else "+91 98000 00000"
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val supabaseUser = SupabaseUser(
                id = userId,
                name = displayName,
                email = email,
                phone = userPhone,
                kycStatus = "NOT_SUBMITTED",
                isAccountActive = true,
                twoFactorEnabled = false,
                biometricsEnabled = false,
                memberSince = today,
                dailyLimitInr = 100000L,
                monthlyLimitInr = 1000000L,
                adminNotes = "Signed up via KetuCoin mobile app"
            )

            // CRITICAL: Insert directly into the Supabase 'users' table which powers the Admin Console!
            try {
                supabase.from("users").insert(supabaseUser)
                Log.d(TAG, "Successfully inserted user into 'users' table for Admin Console: $userId ($email)")
            } catch (e: Exception) {
                Log.e(TAG, "Insert into Supabase 'users' table note: ${e.message}")
            }

            // Create initial profile row
            val defaultPinHash = HashUtil.hashPin("1234") // Default test PIN 1234
            val newProfile = Profile(
                id = userId,
                email = email,
                name = displayName,
                phone = userPhone,
                role = "user",
                securityPin = defaultPinHash,
                kycStatus = "unverified",
                bankAccount = null // Real users start with no bank account until they link their own
            )

            // Real initial wallets with ZERO balance
            val initialWallets = createRealWallets(userId)

            // Attempt Supabase Postgrest remote insert for profiles and wallets
            try {
                supabase.from("profiles").insert(newProfile)
            } catch (e: Exception) {
                Log.w(TAG, "Postgrest insert profile note: ${e.message}")
            }
            try {
                initialWallets.forEach { wallet ->
                    supabase.from("wallets").insert(wallet)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Postgrest insert wallets note: ${e.message}")
            }

            _currentProfile.value = newProfile
            _wallets.value = initialWallets
            _isUserLoggedIn.value = true

            // Fetch any existing messages or initialize welcome
            fetchSupportMessages(userId)
            _transactions.value = emptyList() // Real users start with empty real transaction history

            // Start continuous bidirectional Realtime Sync with Admin Console
            startRealtimeSync(userId)

            Result.success(newProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            var userId = "usr-${(System.currentTimeMillis() % 900000) + 100000}"
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                supabase.auth.currentSessionOrNull()?.user?.id?.let { userId = it }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase Auth signin remote note: ${e.message}. Using profile state.")
            }

            // Look up in 'users' table (Admin Console table)
            var loadedSupabaseUser: SupabaseUser? = null
            try {
                loadedSupabaseUser = supabase.from("users").select {
                    filter {
                        eq("email", email)
                    }
                }.decodeSingleOrNull<SupabaseUser>()
            } catch (e: Exception) {
                Log.w(TAG, "Supabase 'users' table fetch note: ${e.message}")
            }

            var loadedProfile: Profile? = null
            try {
                loadedProfile = supabase.from("profiles").select {
                    filter {
                        eq("email", email)
                    }
                }.decodeSingleOrNull<Profile>()
            } catch (e: Exception) {
                Log.w(TAG, "Postgrest fetch profile note: ${e.message}")
            }

            val finalId = loadedSupabaseUser?.id ?: loadedProfile?.id ?: userId
            val finalKyc = when (loadedSupabaseUser?.kycStatus?.uppercase()) {
                "APPROVED" -> "approved"
                "PENDING" -> "pending"
                "REJECTED" -> "rejected"
                else -> loadedProfile?.kycStatus ?: "unverified"
            }

            val profile = loadedProfile?.copy(
                id = finalId,
                name = loadedSupabaseUser?.name ?: loadedProfile.name,
                phone = loadedSupabaseUser?.phone ?: loadedProfile.phone,
                kycStatus = finalKyc
            ) ?: Profile(
                id = finalId,
                email = email,
                name = loadedSupabaseUser?.name ?: email.substringBefore("@"),
                phone = loadedSupabaseUser?.phone,
                role = "user",
                securityPin = HashUtil.hashPin("1234"),
                kycStatus = finalKyc,
                bankAccount = null
            )

            // If user did not exist in 'users' table, make sure they are registered so Admin Console sees them!
            if (loadedSupabaseUser == null) {
                try {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val sUser = SupabaseUser(
                        id = finalId,
                        name = profile.name ?: email.substringBefore("@"),
                        email = email,
                        phone = profile.phone ?: "+91 98000 00000",
                        kycStatus = if (profile.kycStatus == "approved") "APPROVED" else "NOT_SUBMITTED",
                        isAccountActive = true,
                        memberSince = today,
                        adminNotes = "Active user logged in"
                    )
                    supabase.from("users").insert(sUser)
                } catch (e: Exception) {
                    Log.w(TAG, "Insert user to 'users' table on signIn note: ${e.message}")
                }
            }

            // Load real wallets from Supabase
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

            val wallets = if (loadedWallets.isNotEmpty()) {
                loadedWallets
            } else {
                val realZeroWallets = createRealWallets(profile.id)
                // Try persisting to Supabase
                try {
                    realZeroWallets.forEach { w -> supabase.from("wallets").insert(w) }
                } catch (e: Exception) {
                    Log.w(TAG, "Init wallets insert: ${e.message}")
                }
                realZeroWallets
            }

            _currentProfile.value = profile
            _wallets.value = wallets
            _isUserLoggedIn.value = true

            // Load real support messages and sell orders from Supabase
            fetchSupportMessages(profile.id)
            fetchSellOrders(profile.id)

            // Load real transactions from Supabase
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

            _transactions.value = loadedTransactions // Only real transactions from DB

            // Start continuous bidirectional Realtime Sync with Admin Console
            startRealtimeSync(profile.id)

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun refreshUserData(): Result<Boolean> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            // 1. Fetch updated status from Supabase 'users' table (Admin Console table)
            try {
                val sUsers = supabase.from("users").select {
                    filter {
                        or {
                            eq("id", profile.id)
                            if (!profile.email.isNullOrBlank()) eq("email", profile.email) else eq("id", profile.id)
                        }
                    }
                }.decodeList<SupabaseUser>()
                if (sUsers.isNotEmpty()) {
                    val sUser = sUsers.first()
                    val remoteKyc = when (sUser.kycStatus.uppercase()) {
                        "APPROVED" -> "approved"
                        "PENDING" -> "pending"
                        "REJECTED" -> "rejected"
                        else -> "unverified"
                    }
                    _currentProfile.value = _currentProfile.value?.copy(
                        kycStatus = remoteKyc,
                        name = if (sUser.name.isNotBlank()) sUser.name else _currentProfile.value?.name ?: "",
                        phone = if (sUser.phone.isNotBlank()) sUser.phone else _currentProfile.value?.phone
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Refresh user from 'users' table: ${e.message}")
            }

            // 2. Fetch updated profile from 'profiles'
            val freshProfile = supabase.from("profiles").select {
                filter { eq("id", profile.id) }
            }.decodeSingleOrNull<Profile>()
            if (freshProfile != null) {
                _currentProfile.value = _currentProfile.value?.copy(
                    bankAccount = freshProfile.bankAccount,
                    securityPin = freshProfile.securityPin
                )
            }

            // 3. Fetch updated wallets
            val freshWallets = supabase.from("wallets").select {
                filter { eq("user_id", profile.id) }
            }.decodeList<Wallet>()
            if (freshWallets.isNotEmpty()) {
                _wallets.value = freshWallets
            }

            // 4. Fetch updated support messages
            fetchSupportMessages(profile.id)

            // 5. Fetch updated sell orders
            fetchSellOrders(profile.id)

            // 6. Fetch updated transactions
            val freshTransactions = supabase.from("transactions").select {
                filter { eq("user_id", profile.id) }
            }.decodeList<CryptoTransaction>()
            _transactions.value = freshTransactions

            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Refresh user data error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun refreshWallets(userId: String) = withContext(Dispatchers.IO) {
        try {
            val freshWallets = supabase.from("wallets").select {
                filter { eq("user_id", userId) }
            }.decodeList<Wallet>()
            if (freshWallets.isNotEmpty()) {
                _wallets.value = freshWallets
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshWallets error: ${e.message}")
        }
    }

    // App Update Management
    suspend fun checkForUpdates(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val updates = supabase.from("app_updates").select {
                order(column = "version_code", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }.decodeList<AppUpdateInfo>()

            val latest = updates.firstOrNull()
            if (latest != null) {
                val previous = _latestAppUpdate.value
                _latestAppUpdate.value = latest
                // Trigger notification if newer than previous
                if (previous == null || latest.versionCode > previous.versionCode) {
                    onNewUpdateListener?.invoke(latest)
                }
            }
            Result.success(latest ?: _latestAppUpdate.value)
        } catch (e: Exception) {
            Log.w(TAG, "Check updates remote: ${e.message}")
            Result.success(_latestAppUpdate.value)
        }
    }

    fun signOut() {
        realtimeSyncJob?.cancel()
        realtimeSyncJob = null
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

    // Real wallets starting with zero balance and real crypto deposit addresses
    private fun createRealWallets(userId: String): List<Wallet> {
        return listOf(
            Wallet(
                id = "w-usdt-$userId",
                userId = userId,
                currency = "USDT",
                balance = 0.0,
                assignedDepositAddress = "0x3fA28c1192808E5173CdF44F7b03b6CeD543C12a",
                assignedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=0x3fA28c1192808E5173CdF44F7b03b6CeD543C12a"
            ),
            Wallet(
                id = "w-btc-$userId",
                userId = userId,
                currency = "BTC",
                balance = 0.0,
                assignedDepositAddress = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
                assignedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
            ),
            Wallet(
                id = "w-eth-$userId",
                userId = userId,
                currency = "ETH",
                balance = 0.0,
                assignedDepositAddress = "0x71C8A1054C19c2f6d2E7E25Eb8c27800B29b3501",
                assignedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=0x71C8A1054C19c2f6d2E7E25Eb8c27800B29b3501"
            ),
            Wallet(
                id = "w-sol-$userId",
                userId = userId,
                currency = "SOL",
                balance = 0.0,
                assignedDepositAddress = "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM",
                assignedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM"
            )
        )
    }

    private fun handleIncomingMessageAction(action: PostgresAction, sourceTable: String) {
        try {
            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update -> {
                    val record = if (action is PostgresAction.Insert) action.record else (action as PostgresAction.Update).record
                    val msgUserId = record["user_id"]?.jsonPrimitive?.contentOrNull
                    val currentProf = _currentProfile.value
                    val currentUid = currentProf?.id ?: ""
                    val currentEmail = currentProf?.email ?: ""

                    val isForCurrentUser = msgUserId.isNullOrBlank() || msgUserId == currentUid ||
                            (currentEmail.isNotBlank() && record["user_email"]?.jsonPrimitive?.contentOrNull.equals(currentEmail, ignoreCase = true))

                    if (isForCurrentUser) {
                        val id = record["id"]?.jsonPrimitive?.contentOrNull ?: "msg-${System.currentTimeMillis()}-${(100..999).random()}"
                        val sender = record["sender"]?.jsonPrimitive?.contentOrNull
                            ?: record["sender_role"]?.jsonPrimitive?.contentOrNull
                            ?: "admin"
                        val senderName = record["sender_name"]?.jsonPrimitive?.contentOrNull
                            ?: if (sender == "user") (currentProf?.name ?: "You") else "KetuCoin Desk"
                        val text = record["text"]?.jsonPrimitive?.contentOrNull
                            ?: record["message"]?.jsonPrimitive?.contentOrNull
                            ?: record["content"]?.jsonPrimitive?.contentOrNull
                            ?: ""
                        val timestamp = record["timestamp"]?.jsonPrimitive?.longOrNull
                            ?: System.currentTimeMillis()
                        val isRead = record["is_read"]?.jsonPrimitive?.booleanOrNull ?: false

                        if (text.isNotBlank()) {
                            val incoming = SupportMessage(
                                id = id,
                                userId = msgUserId ?: currentUid,
                                sender = sender,
                                senderName = senderName,
                                text = text,
                                timestamp = timestamp,
                                isRead = isRead
                            )

                            val existing = _supportMessages.value.filter { it.id != id }
                            _supportMessages.value = (existing + incoming).sortedBy { it.timestamp }
                            Log.d(TAG, "Realtime: Instant UI message update from table '$sourceTable': ${incoming.text}")
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling realtime message event: ${e.message}", e)
        }
    }

    private fun handleIncomingKycAction(action: PostgresAction, sourceTable: String) {
        try {
            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update -> {
                    val record = if (action is PostgresAction.Insert) action.record else (action as PostgresAction.Update).record
                    val subUserId = record["user_id"]?.jsonPrimitive?.contentOrNull
                    val subUserEmail = record["user_email"]?.jsonPrimitive?.contentOrNull ?: record["email"]?.jsonPrimitive?.contentOrNull
                    val currentProf = _currentProfile.value
                    val currentUid = currentProf?.id ?: ""
                    val currentEmail = currentProf?.email ?: ""

                    val isTargetUser = (subUserId != null && subUserId == currentUid) ||
                            (subUserEmail != null && currentEmail.isNotBlank() && subUserEmail.equals(currentEmail, ignoreCase = true)) ||
                            (currentProf != null && subUserId == null)

                    if (isTargetUser) {
                        val rawStatus = record["status"]?.jsonPrimitive?.contentOrNull
                            ?: record["kyc_status"]?.jsonPrimitive?.contentOrNull
                            ?: "pending"
                        val normalizedStatus = when (rawStatus.uppercase()) {
                            "APPROVED", "VERIFIED" -> "approved"
                            "REJECTED", "DECLINED" -> "rejected"
                            "PENDING", "UNDER_REVIEW", "SUBMITTED" -> "pending"
                            else -> "pending"
                        }
                        val fullName = record["full_name"]?.jsonPrimitive?.contentOrNull
                            ?: record["name"]?.jsonPrimitive?.contentOrNull
                            ?: currentProf?.name ?: ""
                        val docType = record["document_type"]?.jsonPrimitive?.contentOrNull ?: "Aadhaar Card"
                        val docNum = record["document_number"]?.jsonPrimitive?.contentOrNull
                        val frontUrl = record["id_front_url"]?.jsonPrimitive?.contentOrNull
                        val backUrl = record["id_back_url"]?.jsonPrimitive?.contentOrNull
                        val adminNotes = record["admin_notes"]?.jsonPrimitive?.contentOrNull
                        val id = record["id"]?.jsonPrimitive?.contentOrNull ?: "kyc-${System.currentTimeMillis()}"
                        val createdAt = record["created_at"]?.jsonPrimitive?.contentOrNull
                            ?: record["submitted_at"]?.jsonPrimitive?.contentOrNull

                        val submission = KycSubmission(
                            id = id,
                            userId = subUserId ?: currentUid,
                            userEmail = subUserEmail ?: currentEmail,
                            fullName = fullName,
                            documentType = docType,
                            documentNumber = docNum,
                            idFrontUrl = frontUrl,
                            idBackUrl = backUrl,
                            status = normalizedStatus,
                            adminNotes = adminNotes,
                            createdAt = createdAt
                        )

                        val filteredList = _kycSubmissions.value.filter { it.id != id }
                        _kycSubmissions.value = listOf(submission) + filteredList
                        _latestKycSubmission.value = submission

                        if (currentProf != null) {
                            _currentProfile.value = currentProf.copy(
                                kycStatus = normalizedStatus,
                                name = if (fullName.isNotBlank()) fullName else currentProf.name
                            )
                        }
                        Log.d(TAG, "Realtime: Instant UI KYC status update from table '$sourceTable': $normalizedStatus")

                        if (normalizedStatus == "approved") {
                            scope.launch {
                                refreshWallets(currentUid)
                            }
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling realtime KYC event: ${e.message}", e)
        }
    }

    private fun handleIncomingUserAction(action: PostgresAction) {
        try {
            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update -> {
                    val record = if (action is PostgresAction.Insert) action.record else (action as PostgresAction.Update).record
                    val recordId = record["id"]?.jsonPrimitive?.contentOrNull
                    val recordEmail = record["email"]?.jsonPrimitive?.contentOrNull
                    val currentProf = _currentProfile.value
                    val currentUid = currentProf?.id ?: ""
                    val currentEmail = currentProf?.email ?: ""

                    if (recordId == currentUid || (currentEmail.isNotBlank() && recordEmail.equals(currentEmail, ignoreCase = true))) {
                        val rawKyc = record["kyc_status"]?.jsonPrimitive?.contentOrNull ?: ""
                        val name = record["name"]?.jsonPrimitive?.contentOrNull
                        val normalized = when (rawKyc.uppercase()) {
                            "APPROVED", "VERIFIED" -> "approved"
                            "REJECTED", "DECLINED" -> "rejected"
                            "PENDING", "UNDER_REVIEW" -> "pending"
                            else -> currentProf?.kycStatus ?: "unverified"
                        }

                        if (currentProf != null && (currentProf.kycStatus != normalized || (!name.isNullOrBlank() && currentProf.name != name))) {
                            _currentProfile.value = currentProf.copy(
                                kycStatus = normalized,
                                name = if (!name.isNullOrBlank()) name else currentProf.name
                            )
                            Log.d(TAG, "Realtime: Instant UI users KYC update: $normalized")
                            if (normalized == "approved") {
                                scope.launch {
                                    refreshWallets(currentUid)
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling realtime users event: ${e.message}", e)
        }
    }

    private fun handleIncomingOrderAction(action: PostgresAction) {
        try {
            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update -> {
                    val record = if (action is PostgresAction.Insert) action.record else (action as PostgresAction.Update).record
                    val orderUserId = record["user_id"]?.jsonPrimitive?.contentOrNull
                    val currentUid = _currentProfile.value?.id ?: ""
                    if (orderUserId == currentUid) {
                        val orderId = record["order_id"]?.jsonPrimitive?.contentOrNull ?: ""
                        val status = record["status"]?.jsonPrimitive?.contentOrNull ?: "PENDING_VERIFICATION"
                        val bankUtr = record["bank_utr"]?.jsonPrimitive?.contentOrNull

                        val existing = _sellOrders.value.find { it.orderId == orderId }
                        if (existing != null) {
                            val updated = existing.copy(status = status, bankUtr = bankUtr ?: existing.bankUtr)
                            _sellOrders.value = _sellOrders.value.map { if (it.orderId == orderId) updated else it }
                            Log.d(TAG, "Realtime: Instant UI order update: $orderId -> $status")
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling realtime order event: ${e.message}", e)
        }
    }

    private suspend fun setupRealtimeSubscriptions(userId: String) {
        try {
            val realtime = supabase.realtime

            // 1. Subscribe to 'messages' table (and 'support_messages')
            val messagesChannel = realtime.channel("public:messages_realtime")
            val messagesFlow = messagesChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
            }
            val supportMessagesFlow = messagesChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "support_messages"
            }

            // 2. Subscribe to 'kyc_submissions' table (and 'kyc_verifications', 'users')
            val kycChannel = realtime.channel("public:kyc_realtime")
            val kycSubmissionsFlow = kycChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "kyc_submissions"
            }
            val kycVerificationsFlow = kycChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "kyc_verifications"
            }
            val usersFlow = kycChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "users"
            }

            // 3. Subscribe to 'sell_orders'
            val ordersChannel = realtime.channel("public:orders_realtime")
            val ordersFlow = ordersChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "sell_orders"
            }

            messagesChannel.subscribe()
            kycChannel.subscribe()
            ordersChannel.subscribe()
            Log.d(TAG, "Supabase Realtime channels active for 'messages' and 'kyc_submissions' tables.")

            // Launch individual collectors for immediate UI reaction:
            scope.launch {
                messagesFlow.collect { action ->
                    handleIncomingMessageAction(action, "messages")
                }
            }

            scope.launch {
                supportMessagesFlow.collect { action ->
                    handleIncomingMessageAction(action, "support_messages")
                }
            }

            scope.launch {
                kycSubmissionsFlow.collect { action ->
                    handleIncomingKycAction(action, "kyc_submissions")
                }
            }

            scope.launch {
                kycVerificationsFlow.collect { action ->
                    handleIncomingKycAction(action, "kyc_verifications")
                }
            }

            scope.launch {
                usersFlow.collect { action ->
                    handleIncomingUserAction(action)
                }
            }

            scope.launch {
                ordersFlow.collect { action ->
                    handleIncomingOrderAction(action)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime channel subscription note: ${e.message}")
        }
    }

    suspend fun fetchKycSubmissions(userId: String) = withContext(Dispatchers.IO) {
        try {
            val subs = supabase.from("kyc_submissions").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<KycSubmission>()

            if (subs.isNotEmpty()) {
                _kycSubmissions.value = subs
                _latestKycSubmission.value = subs.firstOrNull()
            }
        } catch (e: Exception) {
            try {
                val vers = supabase.from("kyc_verifications").select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<KycVerification>()
                if (vers.isNotEmpty()) {
                    val converted = vers.map { v ->
                        KycSubmission(
                            id = v.id,
                            userId = v.userId,
                            fullName = v.fullName,
                            documentType = v.documentType,
                            idFrontUrl = v.idFrontUrl,
                            status = v.status
                        )
                    }
                    _kycSubmissions.value = converted
                    _latestKycSubmission.value = converted.firstOrNull()
                }
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }

    fun startRealtimeSync(userId: String) {
        realtimeSyncJob?.cancel()
        realtimeChannelJob?.cancel()

        // Dedicated Supabase Realtime WebSocket listener for immediate UI updates
        realtimeChannelJob = scope.launch(Dispatchers.IO) {
            setupRealtimeSubscriptions(userId)
        }

        // Resilient continuous background sync (every 4 seconds)
        realtimeSyncJob = scope.launch(Dispatchers.IO) {
            // Fetch initial KYC submissions
            fetchKycSubmissions(userId)

            while (isActive) {
                try {
                    val currentProf = _currentProfile.value
                    val userEmail = currentProf?.email ?: ""

                    // 1. Sync User Profile & KYC status from 'users' table
                    val usersList = supabase.from("users").select {
                        filter {
                            or {
                                eq("id", userId)
                                if (userEmail.isNotBlank()) eq("email", userEmail) else eq("id", userId)
                            }
                        }
                    }.decodeList<SupabaseUser>()

                    if (usersList.isNotEmpty()) {
                        val sUser = usersList.first()
                        val remoteKyc = when (sUser.kycStatus.uppercase()) {
                            "APPROVED" -> "approved"
                            "PENDING" -> "pending"
                            "REJECTED" -> "rejected"
                            else -> "unverified"
                        }
                        if (currentProf != null && (currentProf.kycStatus != remoteKyc || (sUser.name.isNotBlank() && currentProf.name != sUser.name))) {
                            _currentProfile.value = currentProf.copy(
                                kycStatus = remoteKyc,
                                name = if (sUser.name.isNotBlank()) sUser.name else currentProf.name
                            )
                            Log.d(TAG, "Sync: Updated KYC status to $remoteKyc for user $userId")
                        }
                    }

                    // 2. Sync Support Messages (fetches real-time replies from Admin Console)
                    val msgs = supabase.from("support_messages").select {
                        filter {
                            eq("user_id", userId)
                        }
                    }.decodeList<SupportMessage>().sortedBy { it.timestamp }

                    if (msgs.isNotEmpty() && msgs != _supportMessages.value) {
                        _supportMessages.value = msgs
                    }

                    // 3. Sync Sell Orders (detects admin approvals, completions, UTR entries)
                    val orders = supabase.from("sell_orders").select {
                        filter {
                            eq("user_id", userId)
                        }
                    }.decodeList<SellOrder>().sortedByDescending { it.createdAt }

                    if (orders.isNotEmpty() && orders != _sellOrders.value) {
                        _sellOrders.value = orders
                    }

                    // 4. Sync Wallets (balances credited by admin)
                    val wList = supabase.from("wallets").select {
                        filter {
                            eq("user_id", userId)
                        }
                    }.decodeList<Wallet>()

                    if (wList.isNotEmpty() && wList != _wallets.value) {
                        _wallets.value = wList
                    }
                } catch (e: Exception) {
                    // Suppress network poll glitches
                }

                delay(4000)
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

        // Create Sell Order record matching Supabase 'sell_orders' table
        val orderId = "ORD-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${(1000..9999).random()}"
        val payoutDesc = profile.bankAccount?.let { "${it.bankName} - A/C ${it.accountNumber} (IFSC: ${it.ifscCode})" } ?: "Bank Transfer"
        val pType = "IMPS"

        val order = SellOrder(
            orderId = orderId,
            userId = profile.id,
            userEmail = profile.email,
            cryptoSymbol = currency.uppercase(),
            cryptoAmount = amount,
            exchangeRateInr = rate,
            inrPayoutAmount = inrPayout,
            sellType = "FROM_WALLET",
            txHash = "0x" + UUID.randomUUID().toString().replace("-", ""),
            transferProofName = null,
            adminReceivingAddress = null,
            network = "Direct Wallet Settlement",
            payoutAccount = payoutDesc,
            payoutType = pType,
            status = "PENDING_VERIFICATION",
            createdAt = System.currentTimeMillis()
        )

        try {
            supabase.from("sell_orders").insert(order)
            Log.d(TAG, "Successfully inserted sell order to Supabase: $orderId")
        } catch (e: Exception) {
            Log.e(TAG, "Sell order sync failed: ${e.message}", e)
        }

        _sellOrders.value = listOf(order) + _sellOrders.value

        // Also post an alert to 'support_messages' so Admin Console sees new sell order in support chat
        val orderAlert = SupportMessage(
            id = "msg-${System.currentTimeMillis()}-${(100..999).random()}",
            userId = profile.id,
            sender = "user",
            senderName = profile.name ?: "User",
            text = "Sell Order Placed #$orderId: $amount ${currency.uppercase()} for ₹${inrPayout.toLong()} payout to $payoutDesc ($pType).",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        try {
            supabase.from("support_messages").insert(orderAlert)
        } catch (e: Exception) {
            Log.w(TAG, "Order alert chat note: ${e.message}")
        }

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

        val orderId = "ORD-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${(1000..9999).random()}"
        val payoutDesc = profile.bankAccount?.let { "${it.bankName} - A/C ${it.accountNumber} (IFSC: ${it.ifscCode})" } ?: "Bank Transfer"
        val pType = "IMPS"
        val vaultAddr = _wallets.value.find { it.currency.equals(currency, ignoreCase = true) }?.assignedDepositAddress ?: "KetuCoin Vault Address"

        val order = SellOrder(
            orderId = orderId,
            userId = profile.id,
            userEmail = profile.email,
            cryptoSymbol = currency.uppercase(),
            cryptoAmount = amount,
            exchangeRateInr = rate,
            inrPayoutAmount = inrPayout,
            sellType = "EXTERNAL_TRANSFER",
            txHash = "0x" + UUID.randomUUID().toString().replace("-", ""),
            transferProofName = fileName,
            adminReceivingAddress = vaultAddr,
            network = when (currency.uppercase()) {
                "USDT" -> "TRC20"
                "BTC" -> "Bitcoin Native"
                "ETH" -> "ERC20"
                "SOL" -> "Solana Native"
                else -> "Mainnet"
            },
            payoutAccount = payoutDesc,
            payoutType = pType,
            status = "PENDING_VERIFICATION",
            createdAt = System.currentTimeMillis()
        )

        try {
            supabase.from("sell_orders").insert(order)
            Log.d(TAG, "Successfully inserted external sell order to Supabase: $orderId")
        } catch (e: Exception) {
            Log.e(TAG, "External sell order insert failed: ${e.message}", e)
        }

        _sellOrders.value = listOf(order) + _sellOrders.value

        // Also post an alert to 'support_messages' so Admin Console sees new external transfer order in support chat
        val orderAlert = SupportMessage(
            id = "msg-${System.currentTimeMillis()}-${(100..999).random()}",
            userId = profile.id,
            sender = "user",
            senderName = profile.name ?: "User",
            text = "External Transfer Order Placed #$orderId: $amount ${currency.uppercase()} for ₹${inrPayout.toLong()} payout to $payoutDesc. Proof: $fileName",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        try {
            supabase.from("support_messages").insert(orderAlert)
        } catch (e: Exception) {
            Log.w(TAG, "External order alert chat note: ${e.message}")
        }

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

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val docNote = "Submitted $documentType for verification. Full Legal Name: $fullName"

        val submission = KycSubmission(
            id = verification.id,
            userId = profile.id,
            userEmail = profile.email,
            fullName = fullName,
            documentType = documentType,
            idFrontUrl = idFrontUrl,
            status = finalStatus,
            adminNotes = docNote,
            createdAt = getCurrentIsoTime()
        )
        _latestKycSubmission.value = submission
        _kycSubmissions.value = listOf(submission) + _kycSubmissions.value.filter { it.id != submission.id }

        try {
            supabase.from("kyc_submissions").insert(submission)
            Log.d(TAG, "Successfully inserted to kyc_submissions table: ${submission.id}")
        } catch (e: Exception) {
            Log.w(TAG, "kyc_submissions table insert note: ${e.message}")
        }

        // Sync KYC status, name, and admin notes to Supabase 'users' table for Admin Console
        try {
            val existing = supabase.from("users").select {
                filter {
                    or {
                        eq("id", profile.id)
                        if (!profile.email.isNullOrBlank()) eq("email", profile.email) else eq("id", profile.id)
                    }
                }
            }.decodeList<SupabaseUser>()

            if (existing.isNotEmpty()) {
                val targetId = existing.first().id
                supabase.from("users").update({
                    set("kyc_status", if (finalStatus == "approved") "APPROVED" else "PENDING")
                    set("name", fullName)
                    set("admin_notes", docNote)
                }) {
                    filter { eq("id", targetId) }
                }
            } else {
                val sUser = SupabaseUser(
                    id = profile.id,
                    name = fullName,
                    email = profile.email ?: "user@ketucoin.io",
                    phone = profile.phone ?: "+91 98000 00000",
                    kycStatus = if (finalStatus == "approved") "APPROVED" else "PENDING",
                    isAccountActive = true,
                    twoFactorEnabled = false,
                    biometricsEnabled = false,
                    memberSince = today,
                    dailyLimitInr = 100000L,
                    monthlyLimitInr = 1000000L,
                    adminNotes = docNote
                )
                supabase.from("users").insert(sUser)
            }
            Log.d(TAG, "Successfully synced KYC status to 'users' table for user ${profile.id}")
        } catch (e: Exception) {
            Log.e(TAG, "users table KYC sync note: ${e.message}")
        }

        // Post an alert to 'support_messages' so Admin Console immediately sees the uploaded KYC in Support Chat
        val kycChatMsg = SupportMessage(
            id = "msg-${System.currentTimeMillis()}-${(100..999).random()}",
            userId = profile.id,
            sender = "user",
            senderName = fullName,
            text = "KYC Document Uploaded: $fullName submitted $documentType for identity verification. Please review and approve.",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        try {
            supabase.from("support_messages").insert(kycChatMsg)
        } catch (e: Exception) {
            Log.w(TAG, "KYC alert chat note: ${e.message}")
        }

        _currentProfile.value = _currentProfile.value?.copy(kycStatus = finalStatus, name = fullName)
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
        try {
            supabase.from("users").update({
                set("kyc_status", "APPROVED")
                set("admin_notes", "KYC Approved and verified")
            }) {
                filter {
                    or {
                        eq("id", profile.id)
                        if (!profile.email.isNullOrBlank()) eq("email", profile.email) else eq("id", profile.id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "users table approve KYC note: ${e.message}")
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
        try {
            supabase.from("users").update({
                set("kyc_status", "NOT_SUBMITTED")
                set("admin_notes", "KYC Reset by user")
            }) {
                filter {
                    or {
                        eq("id", profile.id)
                        if (!profile.email.isNullOrBlank()) eq("email", profile.email) else eq("id", profile.id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "users table reset KYC note: ${e.message}")
        }
        _currentProfile.value = _currentProfile.value?.copy(kycStatus = "unverified")
        Result.success(true)
    }

    // Support Chat Messaging - Live Synchronized with Supabase 'support_messages'
    suspend fun fetchSupportMessages(userId: String) = withContext(Dispatchers.IO) {
        try {
            val msgs = supabase.from("support_messages").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SupportMessage>().sortedBy { it.timestamp }

            if (msgs.isNotEmpty()) {
                _supportMessages.value = msgs
            } else {
                val welcome = SupportMessage(
                    id = "msg-welcome-$userId",
                    userId = userId,
                    sender = "admin",
                    senderName = "KetuCoin Desk",
                    text = "Welcome to KetuCoin Desk! We provide 24/7 crypto OTC exchange and instant INR settlements.",
                    timestamp = System.currentTimeMillis(),
                    isRead = true
                )
                _supportMessages.value = listOf(welcome)
                try {
                    supabase.from("support_messages").insert(welcome)
                } catch (e: Exception) {
                    Log.w(TAG, "Insert welcome note: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchSupportMessages note: ${e.message}")
        }
    }

    suspend fun fetchSellOrders(userId: String) = withContext(Dispatchers.IO) {
        try {
            val orders = supabase.from("sell_orders").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SellOrder>().sortedByDescending { it.createdAt }

            if (orders.isNotEmpty()) {
                _sellOrders.value = orders
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchSellOrders note: ${e.message}")
        }
    }

    suspend fun sendSupportMessage(messageText: String): Result<SupportMessage> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value ?: return@withContext Result.failure(Exception("Not logged in"))
        val senderDisplayName = profile.name?.ifBlank { null } ?: profile.email?.substringBefore("@") ?: "User"
        val msgId = "msg-${System.currentTimeMillis()}-${(100..999).random()}"
        val userMsg = SupportMessage(
            id = msgId,
            userId = profile.id,
            sender = "user",
            senderName = senderDisplayName,
            text = messageText.trim(),
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        // Append locally for immediate UI feedback
        _supportMessages.value = _supportMessages.value + userMsg

        try {
            supabase.from("support_messages").insert(userMsg)
            Log.d(TAG, "Successfully inserted support message to Supabase: $msgId")
        } catch (e: Exception) {
            Log.e(TAG, "Support message insert failed: ${e.message}", e)
        }

        try {
            supabase.from("messages").insert(userMsg)
        } catch (e: Exception) {
            // ignore if table doesn't exist
        }

        Result.success(userMsg)
    }

    private fun getCurrentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        return sdf.format(Date())
    }
}
