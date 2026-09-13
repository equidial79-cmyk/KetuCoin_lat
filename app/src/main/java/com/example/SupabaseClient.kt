package com.example

import com.example.data.supabase.SupabaseClientManager
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Supabase project credentials constants.
 */
const val SUPABASE_URL: String = "https://owkweerllxpqmymollql.supabase.co/"
const val SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im93a3dlZXJsbHhwcW15bW9sbHFsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxMjQ1NTAsImV4cCI6MjEwNDcwMDU1MH0.PUOzpXodSDd6odVNwc1iYjudy_Jcq-uLTmha3uhfrPg"

/**
 * Global object for managing the Supabase connection instance.
 * Configured with Auth, Postgrest, Realtime, and Storage plugins.
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

val supabase: JanSupabaseClient get() = SupabaseClientManager.client
val supabaseClient: JanSupabaseClient get() = SupabaseClientManager.client
