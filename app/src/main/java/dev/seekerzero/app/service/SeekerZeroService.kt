package dev.seekerzero.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SeekerZeroService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
