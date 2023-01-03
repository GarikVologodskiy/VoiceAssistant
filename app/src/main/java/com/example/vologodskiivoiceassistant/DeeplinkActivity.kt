package com.example.vologodskiivoiceassistant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import com.yandex.metrica.YandexMetrica


class DeeplinkActivity : Activity() {

    val TAG1 = "test_deeplink_sep"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG1, "$intent.data")
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            YandexMetrica.reportAppOpen(this)
            Log.d(TAG1, "$intent.data")
        }
    }

    override fun onNewIntent(intent: Intent) {
        Log.d(TAG1, "$intent.data")
        super.onNewIntent(intent)
        YandexMetrica.reportAppOpen(intent)
        Log.d(TAG1, "$intent.data")
    }
}
