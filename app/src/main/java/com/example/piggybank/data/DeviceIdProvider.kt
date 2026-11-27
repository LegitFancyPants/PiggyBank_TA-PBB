package com.example.piggybank.data

import android.content.Context
import java.util.UUID

class DeviceIdentityPreferencesStorage(context: Context) : KeyValueStorage {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "piggybank_device_identity"
    }
}

/**
 * Generates and persists a stable per-device UUID used in the X-Device-Id header
 * so each physical phone/emulator maintains isolated data on the backend.
 */
class DeviceIdProvider(
    private val storage: KeyValueStorage,
    private val uuidGenerator: () -> String = { UUID.randomUUID().toString() }
) {
    private val lock = Any()

    @Volatile
    private var cachedId: String? = null

    fun getDeviceId(): String {
        cachedId?.let { return it }
        return synchronized(lock) {
            cachedId?.let { return it }
            val existing = storage.getString(KEY_DEVICE_ID)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val resolved = existing ?: uuidGenerator().trim().also { generated ->
                storage.putString(KEY_DEVICE_ID, generated)
            }
            cachedId = resolved
            resolved
        }
    }

    companion object {
        const val HEADER_DEVICE_ID = "X-Device-Id"
        private const val KEY_DEVICE_ID = "device_id"

        @Volatile
        private var sharedInstance: DeviceIdProvider? = null

        fun getInstance(context: Context): DeviceIdProvider =
            sharedInstance ?: synchronized(this) {
                sharedInstance ?: DeviceIdProvider(
                    DeviceIdentityPreferencesStorage(context.applicationContext)
                ).also { sharedInstance = it }
            }
    }
}
