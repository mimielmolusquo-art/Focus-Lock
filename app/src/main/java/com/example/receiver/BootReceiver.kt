package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.FocusSessionManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Restore any active session or cleanly finalize expired sessions based on persisted timestamps
            val sessionManager = FocusSessionManager.getInstance(context)
            sessionManager.restoreActiveSessionIfAny()
        }
    }
}
