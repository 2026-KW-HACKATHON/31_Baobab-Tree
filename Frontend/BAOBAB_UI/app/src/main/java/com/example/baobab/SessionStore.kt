package com.example.baobab

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionStore {
    fun read(): String?
    fun write(token: String)
    fun clear()
}

class MemorySessionStore : SessionStore {
    private var saved: String? = null
    override fun read() = saved
    override fun write(token: String) { saved = token }
    override fun clear() { saved = null }
}

class EncryptedSessionStore(context: Context, private val apiUrl: String) : SessionStore {
    private val preferences = context.applicationContext.getSharedPreferences("baobab_session", Context.MODE_PRIVATE)
    private val alias = "baobab_session_key"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    override fun read(): String? {
        val encrypted = preferences.getString("token", null) ?: return null
        if (preferences.getString("api", null) != apiUrl) { clear(); return null }
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.getDecoder().decode(preferences.getString("iv", null))
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), Charsets.UTF_8)
        }.getOrElse { clear(); null }
    }

    override fun write(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("token", Base64.getEncoder().encodeToString(encrypted))
            .putString("iv", Base64.getEncoder().encodeToString(cipher.iv)).putString("api", apiUrl).commit()) {
            "로그인 정보를 저장하지 못했습니다. 다시 시도해주세요."
        }
    }

    override fun clear() { preferences.edit().clear().commit() }
}
