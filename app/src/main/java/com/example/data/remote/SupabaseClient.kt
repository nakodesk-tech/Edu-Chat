package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.SupabaseErrorResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    private const val TAG = "SupabaseConfig"
    private const val PREFS_NAME = "supabase_config_prefs"
    private const val KEY_CUSTOM_URL = "custom_supabase_url"
    private const val KEY_CUSTOM_KEY = "custom_supabase_anon_key"

    // Default or Placeholder values
    val defaultUrl: String
        get() = try {
            val field = BuildConfig::class.java.getField("SUPABASE_URL")
            (field.get(null) as? String)?.takeIf { it.isNotBlank() && !it.contains("your-project") } ?: ""
        } catch (_: Exception) {
            ""
        }

    val defaultAnonKey: String
        get() = try {
            val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
            (field.get(null) as? String)?.takeIf { it.isNotBlank() && !it.contains("your-supabase") } ?: ""
        } catch (_: Exception) {
            ""
        }

    fun getSupabaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customUrl = prefs.getString(KEY_CUSTOM_URL, null)
        if (!customUrl.isNullOrBlank()) return customUrl
        return defaultUrl
    }

    fun getSupabaseAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customKey = prefs.getString(KEY_CUSTOM_KEY, null)
        if (!customKey.isNullOrBlank()) return customKey
        return defaultAnonKey
    }

    fun saveCustomConfig(context: Context, url: String, anonKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CUSTOM_URL, url.trim().trimEnd('/'))
            .putString(KEY_CUSTOM_KEY, anonKey.trim())
            .apply()
        SupabaseClient.reset()
    }

    fun isConfigured(context: Context): Boolean {
        val url = getSupabaseUrl(context)
        val key = getSupabaseAnonKey(context)
        return url.isNotBlank() && key.isNotBlank() && url.startsWith("http")
    }
}

object SupabaseClient {
    private var currentUrl: String? = null
    private var cachedApi: SupabaseAuthApi? = null

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            // BASIC level logs only request method, URL, response status code, and execution time
            // Strictly avoids logging sensitive request bodies, passwords, or auth tokens
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun getApi(context: Context): SupabaseAuthApi {
        val url = SupabaseConfig.getSupabaseUrl(context).let {
            if (it.isBlank()) "https://placeholder.supabase.co" else if (!it.endsWith("/")) "$it/" else it
        }

        if (cachedApi != null && currentUrl == url) {
            return cachedApi!!
        }

        currentUrl = url
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(SupabaseAuthApi::class.java)
        cachedApi = api
        return api
    }

    fun reset() {
        cachedApi = null
        currentUrl = null
    }

    fun parseError(errorJson: String?): String? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            val adapter = moshi.adapter(SupabaseErrorResponse::class.java)
            val res = adapter.fromJson(errorJson)
            res?.errorDescription ?: res?.message ?: res?.msg ?: res?.error
        } catch (_: Exception) {
            null
        }
    }
}
