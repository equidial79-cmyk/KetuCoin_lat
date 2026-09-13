package com.example.data.supabase

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    val supabaseUrl: String
        get() = SupabaseClientManager.PROJECT_URL
    val supabaseKey: String
        get() = SupabaseClientManager.API_KEY

    fun getClient(): SupabaseClient {
        return SupabaseClientManager.getInstance()
    }

    fun configure(url: String, key: String) {
        SupabaseClientManager.initialize(url, key)
    }
}
