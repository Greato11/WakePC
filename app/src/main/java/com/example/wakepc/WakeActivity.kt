package com.example.wakepc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Invisible entry point for shortcuts and voice assistants. Sends the magic packet and closes.
 *
 * Which PC is woken:
 *  1. extra "device_id" (launcher / per-PC shortcuts)
 *  2. extra "feature" text from an assistant ("wake Desktop") matched against PC names
 *  3. otherwise the default PC
 */
class WakeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = DeviceStore(this)
        val device = resolve(store, intent.getStringExtra(EXTRA_DEVICE_ID), intent.getStringExtra("feature"))

        if (device == null) {
            Toast.makeText(this, R.string.add_first, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        WakeRunner.wake(this, device) { finish() }
    }

    private fun resolve(store: DeviceStore, id: String?, feature: String?): Device? {
        val all = store.all()
        if (id != null) all.firstOrNull { it.id == id }?.let { return it }
        if (!feature.isNullOrBlank()) {
            val f = feature.lowercase()
            all.firstOrNull { f.contains(it.name.lowercase()) }?.let { return it }
        }
        return store.defaultDevice()
    }

    companion object {
        const val EXTRA_DEVICE_ID = "device_id"
    }
}
