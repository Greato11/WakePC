package com.example.wakepc

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.util.concurrent.Executors

/** Sends the packet off the main thread and shows a short toast with the result. */
object WakeRunner {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun wake(context: Context, device: Device, done: (() -> Unit)? = null) {
        val app = context.applicationContext
        executor.execute {
            val result = WolSender.send(app, device)
            main.post {
                val message = if (result.isSuccess) {
                    app.getString(R.string.sent_to, device.name)
                } else {
                    app.getString(R.string.send_failed, result.exceptionOrNull()?.message ?: "?")
                }
                Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
                done?.invoke()
            }
        }
    }
}
