package com.example.vologodskiivoiceassistant

import android.app.Application
import android.os.Bundle
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig


class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = YandexMetricaConfig.newConfigBuilder("4221cbff-2d00-4cf2-abdf-793b03d58780").build()
        YandexMetrica.activate(applicationContext, config)
        YandexMetrica.enableActivityAutoTracking(this)
    }
}
