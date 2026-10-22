package com.example.wakepc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A PC that can be woken.
 * [address] is optional: blank = auto (LAN broadcast). Fill it with a hostname/IP only if you
 * forward a UDP port on your router (wake from outside your home network).
 */
data class Device(
    val id: String,
    val name: String,
    val mac: String,
    val address: String = "",
    val port: Int = 9,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("mac", mac)
        .put("address", address)
        .put("port", port)

    companion object {
        fun fromJson(o: JSONObject) = Device(
            id = o.getString("id"),
            name = o.getString("name"),
            mac = o.getString("mac"),
            address = o.optString("address", ""),
            port = o.optInt("port", 9),
        )
    }
}

/** Tiny SharedPreferences-backed storage for devices, the default device and widget bindings. */
class DeviceStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("wakepc", Context.MODE_PRIVATE)

    fun all(): List<Device> {
        val raw = prefs.getString("devices", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Device.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun write(list: List<Device>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("devices", arr.toString()).apply()
    }

    fun find(id: String?): Device? = all().firstOrNull { it.id == id }

    fun upsert(device: Device) {
        val list = all().toMutableList()
        val i = list.indexOfFirst { it.id == device.id }
        if (i >= 0) list[i] = device else list.add(device)
        write(list)
        if (find(defaultId) == null) defaultId = device.id
    }

    fun delete(id: String) {
        write(all().filter { it.id != id })
        if (defaultId == id) defaultId = all().firstOrNull()?.id
    }

    var defaultId: String?
        get() = prefs.getString("default", null)
        set(value) = prefs.edit().putString("default", value).apply()

    /** The default device, or the first one if none is marked. */
    fun defaultDevice(): Device? = find(defaultId) ?: all().firstOrNull()

    /** If true, opening the app itself wakes the default PC (handy for an Assistant routine). */
    var wakeOnLaunch: Boolean
        get() = prefs.getBoolean("wake_on_launch", false)
        set(value) = prefs.edit().putBoolean("wake_on_launch", value).apply()

    fun setWidgetDevice(widgetId: Int, deviceId: String) =
        prefs.edit().putString("widget_$widgetId", deviceId).apply()

    fun widgetDevice(widgetId: Int): Device? = find(prefs.getString("widget_$widgetId", null))

    fun removeWidget(widgetId: Int) = prefs.edit().remove("widget_$widgetId").apply()
}
