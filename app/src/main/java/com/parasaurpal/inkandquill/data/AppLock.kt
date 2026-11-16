package com.parasaurpal.inkandquill.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppLock {
    private const val PREFS = "ink_quill_app_lock"
    private const val ITERATIONS = 240_000

    fun isEnabled(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("hash")

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    fun setPin(context: Context, pin: String) {
        require(pin.length >= 6) { "Use at least 6 characters for your app lock." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP)).apply()
        hash.fill(0)
    }

    fun verify(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val salt = prefs.getString("salt", null) ?: return false
        val expected = prefs.getString("hash", null) ?: return false
        val actual = derive(pin, Base64.decode(salt, Base64.NO_WRAP))
        return try { MessageDigest.isEqual(actual, Base64.decode(expected, Base64.NO_WRAP)) }
        finally { actual.fill(0) }
    }

    fun disable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
