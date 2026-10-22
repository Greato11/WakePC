package com.example.wakepc

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var store: DeviceStore
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var emptyView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = DeviceStore(this)

        // Keep the FAB clear of the navigation bar (edge-to-edge on Android 15+).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }

        emptyView = findViewById(R.id.empty)
        deviceAdapter = DeviceAdapter(onClick = { wake(it) }, onLongClick = { showActions(it) })
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }
        findViewById<View>(R.id.fab).setOnClickListener { showEditor(null) }

        if (savedInstanceState == null && store.wakeOnLaunch) {
            store.defaultDevice()?.let { wake(it) }
        }
        refresh()
    }

    private fun wake(device: Device) = WakeRunner.wake(this, device)

    private fun refresh() {
        val list = store.all()
        deviceAdapter.submit(list, store.defaultDevice()?.id)
        emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        Shortcuts.publish(this)
        WakeWidgetProvider.updateAll(this)
    }

    // ---- menu -------------------------------------------------------------------------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.action_wake_on_launch).isChecked = store.wakeOnLaunch
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_wake_on_launch -> {
            store.wakeOnLaunch = !store.wakeOnLaunch
            item.isChecked = store.wakeOnLaunch
            true
        }
        R.id.action_voice_help -> {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.voice_help_title)
                .setMessage(R.string.voice_help_body)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ---- dialogs ----------------------------------------------------------------------------

    private fun showActions(device: Device) {
        val options = arrayOf(
            getString(R.string.action_edit),
            getString(R.string.action_default),
            getString(R.string.action_delete),
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(device.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditor(device)
                    1 -> {
                        store.defaultId = device.id
                        refresh()
                    }
                    2 -> confirmDelete(device)
                }
            }
            .show()
    }

    private fun confirmDelete(device: Device) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.confirm_delete, device.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                store.delete(device.id)
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showEditor(existing: Device?) {
        val view = layoutInflater.inflate(R.layout.dialog_device, null)
        val nameEt = view.findViewById<TextInputEditText>(R.id.field_name)
        val macEt = view.findViewById<TextInputEditText>(R.id.field_mac)
        val addressEt = view.findViewById<TextInputEditText>(R.id.field_address)
        val portEt = view.findViewById<TextInputEditText>(R.id.field_port)

        existing?.let {
            nameEt.setText(it.name)
            macEt.setText(it.mac)
            addressEt.setText(it.address)
            portEt.setText(it.port.toString())
        }

        val dialog: AlertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) R.string.add_pc else R.string.edit_pc)
            .setView(view)
            .setPositiveButton(R.string.save, null) // set below so invalid input keeps the dialog open
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = nameEt.text.toString().trim()
                val macBytes = WolSender.parseMac(macEt.text.toString())
                val port = portEt.text.toString().trim().toIntOrNull() ?: 9
                when {
                    name.isEmpty() -> nameEt.error = getString(R.string.err_name)
                    macBytes == null -> macEt.error = getString(R.string.err_mac)
                    port !in 1..65535 -> portEt.error = getString(R.string.err_port)
                    else -> {
                        store.upsert(
                            Device(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                mac = WolSender.formatMac(macBytes),
                                address = addressEt.text.toString().trim(),
                                port = port,
                            )
                        )
                        refresh()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }
}

private class DeviceAdapter(
    val onClick: (Device) -> Unit,
    val onLongClick: (Device) -> Unit,
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    private var items: List<Device> = emptyList()
    private var defaultId: String? = null

    fun submit(list: List<Device>, defaultId: String?) {
        items = list
        this.defaultId = defaultId
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.item_name)
        val subtitle: TextView = view.findViewById(R.id.item_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        holder.name.text = d.name
        holder.subtitle.text = buildString {
            append(d.mac)
            if (d.id == defaultId) {
                append("  ·  ")
                append(holder.itemView.context.getString(R.string.default_label))
            }
        }
        holder.itemView.setOnClickListener { onClick(d) }
        holder.itemView.setOnLongClickListener { onLongClick(d); true }
    }
}
