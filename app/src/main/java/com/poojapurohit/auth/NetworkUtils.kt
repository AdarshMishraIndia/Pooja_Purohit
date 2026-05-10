package com.poojapurohit.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NetworkUtils {

    /**
     * Fast check using ConnectivityManager — no network I/O.
     * Returns false if device reports no active network or no internet capability.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Deep reachability check — actually pings Google's generate_204 endpoint.
     * Lightweight: nobody, just HTTP 204 response expected.
     * Use this when ConnectivityManager says online but auth still fails.
     * Timeout: 5 seconds.
     */
    suspend fun isInternetReachable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("https://connectivitycheck.gstatic.com/generate_204")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = "GET"
            connection.connect()
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 204
        } catch (_: Exception) {
            false
        }
    }
}
