package com.litsorbeklik.app.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Single Supabase client instance for the whole app.
 *
 * SECURITY: only the anon/public key belongs here. It relies entirely on Row Level Security
 * policies (see supabase/migrations/0001_init.sql) to keep each user scoped to their own rows —
 * the service_role key must NEVER ship inside the client app.
 */
object SupabaseModule {
    // TODO: move these two into local.properties / BuildConfig fields instead of hardcoding,
    // once the project is opened in Android Studio (they are safe to expose, RLS enforces access).
    private const val SUPABASE_URL = "https://krhbgeewzruuvpdxjbiv.supabase.co"
    private const val SUPABASE_ANON_KEY = "REPLACE_WITH_ANON_PUBLIC_KEY"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}
