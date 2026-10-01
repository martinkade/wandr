package com.wandr.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    // Supabase base URL without '/rest/v1/' subpath (SDK appends endpoint paths automatically)
    const val URL = "https://urhisbyqjygjqrfarply.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVyaGlzYnlxanlnanFyZmFycGx5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA4NDI0MzQsImV4cCI6MjEwNjQxODQzNH0.FC1UjgqhbFTY9EBeROBTmGbLgV5KmqpPNvH8_vwYbP0"
}

class SupabaseClientFactory {
    fun create(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
