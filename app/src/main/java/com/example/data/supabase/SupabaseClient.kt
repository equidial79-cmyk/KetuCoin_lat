package com.example.data.supabase

import android.util.Log
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Supabase project credentials constants.
 * Configure these with your active Supabase project URL and anonymous API key.
 */
const val SUPABASE_URL: String = "https://owkweerllxpqmymollql.supabase.co"
const val SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im93a3dlZXJsbHhwcW15bW9sbHFsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxMjQ1NTAsImV4cCI6MjEwNDcwMDU1MH0.PUOzpXodSDd6odVNwc1iYjudy_Jcq-uLTmha3uhfrPg"
const val SUPABASE_PROJECT_URL: String = SUPABASE_URL
const val SUPABASE_API_KEY: String = SUPABASE_ANON_KEY

/**
 * Singleton object for managing the global Supabase client instance using supabase-kt.
 * Initializes the client with Auth, Postgrest, Realtime, and Storage plugins.
 */
object SupabaseClientManager {
    private const val TAG = "SupabaseClientManager"

    const val PROJECT_URL = SUPABASE_PROJECT_URL
    const val API_KEY = SUPABASE_API_KEY

    @Volatile
    private var currentUrl: String = SUPABASE_PROJECT_URL

    @Volatile
    private var currentKey: String = SUPABASE_API_KEY

    @Volatile
    private var clientInstance: JanSupabaseClient? = null

    /**
     * Globally accessible SupabaseClient instance.
     */
    val client: JanSupabaseClient
        get() = getInstance()

    /**
     * Direct plugin accessors for convenience
     */
    val auth: Auth
        get() = client.auth

    val postgrest: Postgrest
        get() = client.postgrest

    val realtime: Realtime
        get() = client.realtime

    val storage: Storage
        get() = client.storage

    /**
     * Returns the global SupabaseClient singleton, initializing it lazily if not already present.
     */
    fun getInstance(): JanSupabaseClient {
        return clientInstance ?: synchronized(this) {
            clientInstance ?: buildClient(currentUrl, currentKey).also {
                clientInstance = it
                Log.d(TAG, "SupabaseClient initialized with URL: $currentUrl")
            }
        }
    }

    /**
     * Reconfigures or initializes the global Supabase client instance with custom URL and API key.
     */
    fun initialize(projectUrl: String = SUPABASE_PROJECT_URL, apiKey: String = SUPABASE_API_KEY): JanSupabaseClient {
        return synchronized(this) {
            currentUrl = projectUrl.trim()
            currentKey = apiKey.trim()
            buildClient(currentUrl, currentKey).also {
                clientInstance = it
                Log.d(TAG, "SupabaseClient reconfigured with URL: $currentUrl")
            }
        }
    }

    /**
     * Factory function to construct and configure a new SupabaseClient with required plugins.
     */
    fun buildClient(url: String, key: String): JanSupabaseClient {
        val sanitizedUrl = url.trim().trimEnd('/')
        val sanitizedKey = key.trim()
        return createSupabaseClient(
            supabaseUrl = sanitizedUrl,
            supabaseKey = sanitizedKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}

/**
 * Global convenience alias for Supabase instance management.
 */
object SupabaseManager {
    val client: JanSupabaseClient get() = SupabaseClientManager.client
    val auth: Auth get() = SupabaseClientManager.auth
    val postgrest: Postgrest get() = SupabaseClientManager.postgrest
    val realtime: Realtime get() = SupabaseClientManager.realtime
    val storage: Storage get() = SupabaseClientManager.storage

    fun getInstance(): JanSupabaseClient = SupabaseClientManager.getInstance()
    fun initialize(url: String = SUPABASE_URL, key: String = SUPABASE_ANON_KEY): JanSupabaseClient =
        SupabaseClientManager.initialize(url, key)
}

/**
 * Object for managing the Supabase connection instance globally.
 */
object SupabaseClient {
    const val URL: String = SUPABASE_URL
    const val ANON_KEY: String = SUPABASE_ANON_KEY

    val client: JanSupabaseClient get() = SupabaseClientManager.client
    val auth: Auth get() = client.auth
    val postgrest: Postgrest get() = client.postgrest
    val realtime: Realtime get() = client.realtime
    val storage: Storage get() = client.storage

    fun getInstance(): JanSupabaseClient = SupabaseClientManager.getInstance()
    fun initialize(url: String = SUPABASE_URL, key: String = SUPABASE_ANON_KEY): JanSupabaseClient =
        SupabaseClientManager.initialize(url, key)
}

/**
 * Global property shortcut for SupabaseClient access.
 */
val supabase: JanSupabaseClient get() = SupabaseClientManager.client
val supabaseClient: JanSupabaseClient get() = SupabaseClientManager.client
