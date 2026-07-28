package com.example

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Exception) {
                    val options = FirebaseOptions.Builder()
                        .setProjectId("ison-bc154")
                        .setApplicationId("1:423299705791:android:d988889295ff413e89cf0a")
                        .setApiKey("AIzaSyBgILtk_9s0LVkv8g2W5REy2T_7Nj8-4SI")
                        .setGcmSenderId("423299705791")
                        .setStorageBucket("ison-bc154.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
