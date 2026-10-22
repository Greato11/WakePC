package com.example.wakepc

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Publishes one launcher shortcut per PC. Each shortcut is also bound to the
 * actions.intent.OPEN_APP_FEATURE capability (declared in res/xml/shortcuts.xml), so
 * voice assistants that support App Actions can say e.g. "wake Desktop in WakePC".
 */
object Shortcuts {

    fun publish(context: Context) {
        try {
            val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtMost(4)
            val shortcuts = DeviceStore(context).all().take(max).map { d ->
                val intent = Intent(context, WakeActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(WakeActivity.EXTRA_DEVICE_ID, d.id)
                ShortcutInfoCompat.Builder(context, "wake_${d.id}")
                    .setShortLabel(context.getString(R.string.wake_label, d.name))
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(intent)
                    .addCapabilityBinding(
                        "actions.intent.OPEN_APP_FEATURE",
                        "feature",
                        listOf(d.name, "wake ${d.name}", "turn on ${d.name}"),
                    )
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Exception) {
            // Shortcuts are a convenience; never crash the app because of them.
        }
    }
}
