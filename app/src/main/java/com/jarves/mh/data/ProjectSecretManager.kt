package com.jarves.mh.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ProjectSecretEntry(
    val keyName: String,
    val secretValue: String,
    val projectId: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    val maskedValue: String
        get() = when {
            secretValue.length <= 6 -> "••••••"
            else -> secretValue.take(3) + "••••••••" + secretValue.takeLast(4)
        }
}

/**
 * Manages per-project and app-level encrypted API keys and credentials
 * (e.g. GEMINI_API_KEY, OPENAI_API_KEY, STRIPE_KEY, SUPABASE_KEY, etc.).
 * Stored securely using Android Keystore AES-256 GCM encryption.
 */
class ProjectSecretManager(context: Context) {
    private val preferences = context.getSharedPreferences("pocket_project_secrets", Context.MODE_PRIVATE)
    private val alias = "pocket-project-secrets-key"

    @Synchronized
    fun getSecret(projectId: String, keyName: String): String? {
        val storageId = secretStorageId(projectId, keyName)
        return getEncrypted(storageId)
    }

    @Synchronized
    fun saveSecret(projectId: String, keyName: String, value: String) {
        val cleanKey = keyName.trim().uppercase()
        val cleanVal = value.trim()
        if (cleanKey.isBlank() || cleanVal.isBlank()) return

        val storageId = secretStorageId(projectId, cleanKey)
        putEncrypted(storageId, cleanVal)

        // Register in project secret index
        val currentKeys = getSecretKeys(projectId).toMutableSet()
        currentKeys.add(cleanKey)
        saveSecretKeys(projectId, currentKeys)
    }

    @Synchronized
    fun removeSecret(projectId: String, keyName: String) {
        val cleanKey = keyName.trim().uppercase()
        val storageId = secretStorageId(projectId, cleanKey)
        preferences.edit()
            .remove("$storageId.iv")
            .remove("$storageId.value")
            .apply()

        val currentKeys = getSecretKeys(projectId).toMutableSet()
        currentKeys.remove(cleanKey)
        saveSecretKeys(projectId, currentKeys)
    }

    @Synchronized
    fun listSecrets(projectId: String): List<ProjectSecretEntry> {
        val keys = getSecretKeys(projectId)
        return keys.mapNotNull { key ->
            getSecret(projectId, key)?.let { secretVal ->
                ProjectSecretEntry(
                    keyName = key,
                    secretValue = secretVal,
                    projectId = projectId,
                )
            }
        }.sortedBy { it.keyName }
    }

    @Synchronized
    fun listAllSecrets(): Map<String, List<ProjectSecretEntry>> {
        val all = preferences.all.keys
        val projectIds = all.filter { it.endsWith(".keys") }.map { it.removeSuffix(".keys") }.distinct()
        return projectIds.associateWith { listSecrets(it) }.filterValues { it.isNotEmpty() }
    }

    private fun getSecretKeys(projectId: String): Set<String> = runCatching {
        val raw = preferences.getString("$projectId.keys", "[]") ?: "[]"
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }.toSet()
    }.getOrDefault(emptySet())

    private fun saveSecretKeys(projectId: String, keys: Set<String>) {
        val arr = JSONArray()
        keys.sorted().forEach(arr::put)
        preferences.edit().putString("$projectId.keys", arr.toString()).apply()
    }

    private fun secretStorageId(projectId: String, keyName: String): String =
        "sec_${projectId}_$keyName"

    private fun putEncrypted(storageId: String, secret: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString("$storageId.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("$storageId.value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    private fun getEncrypted(storageId: String): String? = runCatching {
        val iv = Base64.decode(preferences.getString("$storageId.iv", null), Base64.NO_WRAP)
        val encrypted = Base64.decode(preferences.getString("$storageId.value", null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(alias, null)?.let { return it as SecretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
