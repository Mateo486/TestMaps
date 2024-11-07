package com.example.testmaps

import okhttp3.*
import java.io.IOException

class HTTPManager {
    private val client = OkHttpClient()

    fun get(url: String, callback: (Boolean, String?) -> Unit) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let {
                        callback(true, it)
                    } ?: callback(false, null)
                } else {
                    callback(false, null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false, null)
            }
        })
    }
}