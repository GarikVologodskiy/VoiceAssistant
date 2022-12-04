package com.example.vologodskiivoiceassistant

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "start of onCreate function")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firstname: String = "Ivan"
        val lastname: String = "Ivanov"
        var age: Int = 37
        var height: Double = 172.4
        //var weight: Double = 90.0

        val summary: String = "firstname: $firstname lastname: $lastname age: $age height: $height"

        val output: TextView = findViewById(R.id.output)
        output.text = summary

        Log.d(TAG, "end of onCreate function")
 /*     Log.e(TAG, summary)
        Log.w(TAG, summary)
        Log.i(TAG, summary)
        Log.v(TAG, summary)*/
    }
}