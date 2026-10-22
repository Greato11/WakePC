package com.example.wakepc

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

/** Home-screen widget: one tap wakes the PC it was configured for. */
class WakeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val store = DeviceStore(context)
        ids.forEach { store.removeWidget(it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WAKE) {
            val device = DeviceStore(context).find(intent.getStringExtra(EXTRA_DEVICE_ID)) ?: return
            val pending = goAsync()
            WakeRunner.wake(context, device) { pending.finish() }
        }
    }

    companion object {
        const val ACTION_WAKE = "com.example.wakepc.ACTION_WAKE"
        const val EXTRA_DEVICE_ID = "device_id"

        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val device = DeviceStore(context).widgetDevice(widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_wake)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            if (device != null) {
                views.setTextViewText(R.id.widget_name, device.name)
                val intent = Intent(context, WakeWidgetProvider::class.java)
                    .setAction(ACTION_WAKE)
                    .setData(Uri.parse("wakepc://widget/$widgetId"))
                    .putExtra(EXTRA_DEVICE_ID, device.id)
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getBroadcast(context, widgetId, intent, flags),
                )
            } else {
                views.setTextViewText(R.id.widget_name, context.getString(R.string.widget_not_set))
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context, widgetId, Intent(context, MainActivity::class.java), flags,
                    ),
                )
            }
            manager.updateAppWidget(widgetId, views)
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WakeWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
        }
    }
}
