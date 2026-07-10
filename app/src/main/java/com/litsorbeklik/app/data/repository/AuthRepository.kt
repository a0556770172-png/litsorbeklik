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
        // signUpWith's own return carries the created user's id even when email confirmation
        // is required and no session is established yet — currentUserOrNull() would be null
        // in that case, which used to make every registration fail on this project.
        val signedUpUser = auth.signUpWith(Email) {
            this.email = sanitizeEmail(email)
            this.password = password
        }
        val userId = signedUpUser?.id ?: error("ההרשמה נכשלה — לא התקבל מזהה משתמש מהשרת")

        // Without an active session (confirmation pending), RLS blocks inserting the profile
        // row as this request is unauthenticated. Defer it — [login] creates it lazily once the
        // user actually has a session, right after they confirm their email and log in.
        if (auth.currentSessionOrNull() != null) {
            val userCode = generateUserCode()
            client.from("profiles").insert(ProfileInsertDto(id = userId, fullName = fullName, userCode = userCode))
            Profile(id = userId, fullName = fullName, userCode = userCode)
        } else {
            Profile(id = userId, fullName = fullName, userCode = "")
        }
    }

    suspend fun login(email: String, password: String): Result<Profile> = runCatching {
        auth.signInWith(Email) {
            this.email = sanitizeEmail(email)
            this.password = password
        }
        val userId = auth.currentUserOrNull()?.id ?: error("Sign-in succeeded but no user id was returned")

        val existing = client.from("profiles").select().decodeSingleOrNull<ProfileRowDto>()
        val row = existing ?: run {
            // Self-heal: the profile row is normally created at registration time, but that step
            // is skipped when email confirmation was pending (see [register]) — create it now
            // that we have a real, authenticated session and RLS will allow the insert.
            val userCode = generateUserCode()
            val fallbackName = email.substringBefore('@')
            client.from("profiles").insert(ProfileInsertDto(id = userId, fullName = fallbackName, userCode = userCode))
            ProfileRowDto(id = userId, fullName = fallbackName, userCode = userCode)
        }
        Profile(id = row.id, fullName = row.fullName, userCode = row.userCode, createdAt = row.createdAt)
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
