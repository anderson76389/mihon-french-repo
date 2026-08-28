package eu.kanade.tachiyomi.network

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request

fun GET(url: String, headers: Headers = Headers.Builder().build()): Request {
    return Request.Builder().url(url).headers(headers).build()
}

class NetworkHelper {
    val client: OkHttpClient = OkHttpClient()
    val cloudflareClient: OkHttpClient = OkHttpClient()
}
