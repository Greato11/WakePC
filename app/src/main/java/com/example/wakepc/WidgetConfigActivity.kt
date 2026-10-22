package com.example.wakepc

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Opens when a widget is added; asks which PC the widget should wake. */
class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val devices = DeviceStore(this).all()
        when {
            devices.isEmpty() -> {
                Toast.makeText(this, R.string.add_first, Toast.LENGTH_LONG).show()
                finish()
            }
            devices.size == 1 -> complete(devices[0])
            else -> MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_pc)
                .setItems(devices.map { it.name }.toTypedArray()) { _, which -> complete(devices[which]) }
                .setOnCancelListener { finish() }
                .show()
        }
    }

    private fun complete(device: Device) {
        DeviceStore(this).setWidgetDevice(widgetId, device.id)
        WakeWidgetProvider.update(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
