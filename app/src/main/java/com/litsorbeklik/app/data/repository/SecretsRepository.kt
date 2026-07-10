package com.litsorbeklik.app.data.repository

import com.litsorbeklik.app.data.crypto.SecretCrypto
import com.litsorbeklik.app.data.supabase.SupabaseModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stores/retrieves the user's AI provider API key and GitHub PAT — always encrypted client-side
 * (see [SecretCrypto]) before it ever reaches Supabase. `password` here is the user's own account
 * password, used only in-memory to derive the AES key; it is never sent anywhere.
 */
class SecretsRepository(
    private val client: SupabaseClient = SupabaseModule.client,
) {
    suspend fun saveAiApiKey(ownerId: String, provider: String, plainApiKey: String, password: String) {
        val ciphertext = SecretCrypto.encrypt(plainApiKey, password.toCharArray())
        client.from("user_secrets").upsert(
            SecretUpsertDto(ownerId = ownerId, secretType = "ai_api_key", provider = provider, ciphertext = ciphertext)
        )
    }

    suspend fun saveGithubToken(ownerId: String, plainToken: String, password: String) {
        val ciphertext = SecretCrypto.encrypt(plainToken, password.toCharArray())
        client.from("user_secrets").upsert(
            SecretUpsertDto(ownerId = ownerId, secretType = "github_pat", provider = null, ciphertext = ciphertext)
        )
    }

    suspend fun loadAiApiKey(ownerId: String, password: String): String? =
        loadDecrypted(ownerId, "ai_api_key", password)

    suspend fun loadGithubToken(ownerId: String, password: String): String? =
        loadDecrypted(ownerId, "github_pat", password)

    private suspend fun loadDecrypted(ownerId: String, secretType: String, password: String): String? {
        val row = client.from("user_secrets")
            .select()
            .decodeList<SecretRowDto>()
            .firstOrNull { it.ownerId == ownerId && it.secretType == secretType }
            ?: return null
        return SecretCrypto.decrypt(row.ciphertext, password.toCharArray())
    }
}

@Serializable
private data class SecretUpsertDto(
    @SerialName("owner_id") val ownerId: String,
    @SerialName("secret_type") val secretType: String,
    val provider: String?,
    val ciphertext: String,
)

@Serializable
private data class SecretRowDto(
    @SerialName("owner_id") val ownerId: String,
    @SerialName("secret_type") val secretType: String,
    val provider: String? = null,
    val ciphertext: String,
)
