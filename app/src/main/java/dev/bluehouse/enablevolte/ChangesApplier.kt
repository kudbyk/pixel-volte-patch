package dev.bluehouse.enablevolte

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import kotlinx.serialization.json.Json

class ChangesApplier(
    private val context: Context,
) {
    private val prefs by lazy { context.getSharedPreferences("changes", Context.MODE_PRIVATE) }

    @RequiresApi(Build.VERSION_CODES.P)
    fun recordChange(
        subId: Int,
        key: String,
        value: Any?,
    ) {
        val moder = SubscriptionModer(context, subId)
        val original = moder.getOriginalCarrierConfig(key)

        val changes = getChanges(subId)

        if (value == original) {
            changes.bundle.remove(key)
        } else {
            when (value) {
                is Boolean -> {
                    changes.bundle.putBoolean(key, value)
                }

                is Int -> {
                    changes.bundle.putInt(key, value)
                }

                is String -> {
                    changes.bundle.putString(key, value)
                }

                else -> {}
            }
        }

        saveChanges(subId, changes)
    }

    fun getChanges(subId: Int): Changes {
        val json = prefs.getString(subId.toString(), null)
        return if (json != null) {
            Json.decodeFromString(Changes.serializer(), json)
        } else {
            Changes(bundle = android.os.PersistableBundle())
        }
    }

    private fun saveChanges(
        subId: Int,
        changes: Changes,
    ) {
        if (changes.bundle.isEmpty) {
            prefs.edit { remove(subId.toString()) }
        } else {
            val json = Json.encodeToString(Changes.serializer(), changes)
            prefs.edit { putString(subId.toString(), json) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun apply(subId: Int) {
        val moder = SubscriptionModer(context, subId)
        val changes = getChanges(subId)
        moder.updateCarrierConfig(changes.bundle)
    }

    fun reset(subId: Int) {
        prefs.edit { remove(subId.toString()) }
    }

    companion object {
        private const val ACTION_APPLY = "dev.bluehouse.enablevolte.ACTION_APPLY"

        fun apply(context: Context) {
            val intent = Intent(context, ChangesApplier::class.java)
            intent.action = ACTION_APPLY
            context.startService(intent)
        }
    }
}
