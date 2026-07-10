package com.litsorbeklik.app.data.repository

import com.litsorbeklik.app.data.model.Profile
import com.litsorbeklik.app.data.supabase.SupabaseModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Wraps Supabase Auth (email/password) + the `profiles` table.
 * Every authenticated user gets exactly one `profiles` row, keyed by their auth.users id,
 * enforced by the `profiles_self` RLS policy (see supabase/migrations/0001_init.sql).
 */
class AuthRepository(
    private val client: SupabaseClient = SupabaseModule.client,
) {
    private val auth get() = client.auth

    suspend fun register(fullName: String, email: String, password: String): Result<Profile> = runCatching {
        auth.signUpWith(Email) {
            this.email = sanitizeEmail(email)
            this.password = password
        }
        val userId = auth.currentUserOrNull()?.id
            ?: error("Sign-up succeeded but no session/user was returned — check email confirmation settings")

        val userCode = generateUserCode()
        val row = ProfileInsertDto(id = userId, fullName = fullName, userCode = userCode)
        client.from("profiles").insert(row)

        Profile(id = userId, fullName = fullName, userCode = userCode)
    }

    suspend fun login(email: String, password: String): Result<Profile> = runCatching {
        auth.signInWith(Email) {
            this.email = sanitizeEmail(email)
            this.password = password
        }
        val userId = auth.currentUserOrNull()?.id ?: error("Sign-in succeeded but no user id was returned")

        client.from("profiles")
            .select()
            .decodeSingle<ProfileRowDto>()
            .let { Profile(id = it.id, fullName = it.fullName, userCode = it.userCode, createdAt = it.createdAt) }
    }

    suspend fun logout() {
        auth.signOut()
    }

    fun currentUserId(): String? = auth.currentUserOrNull()?.id

    /**
     * RTL text fields (this app's UI is Hebrew) can silently embed invisible bidi-control
     * characters (LRM/RLM/isolates, Unicode category Cf) around Latin-script input, which
     * Supabase's server-side email regex then rejects as "invalid format" even though the
     * address looks correct on screen. Strip those out before it ever reaches the network.
     */
    private fun sanitizeEmail(raw: String): String =
        raw.trim().filterNot { it.category == CharCategory.FORMAT }

    private fun generateUserCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no ambiguous chars
        return (1..8).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }
}

@Serializable
private data class ProfileInsertDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("user_code") val userCode: String,
)

@Serializable
private data class ProfileRowDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("created_at") val createdAt: String? = null,
)
