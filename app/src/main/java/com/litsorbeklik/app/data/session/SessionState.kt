package com.litsorbeklik.app.data.session

/**
 * Holds the user's password ONLY in memory for the lifetime of the app process — needed to
 * (re)derive the AES key for SecretCrypto when reading/writing user_secrets. Never persisted,
 * never sent anywhere, cleared on logout/process death.
 */
object SessionState {
    var userId: String? = null
        private set
    var inMemoryPassword: String? = null
        private set

    fun onAuthenticated(userId: String, password: String) {
        this.userId = userId
        this.inMemoryPassword = password
    }

    fun clear() {
        userId = null
        inMemoryPassword = null
    }
}
