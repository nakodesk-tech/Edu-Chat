package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.SessionManager
import com.example.data.model.SupabaseErrorResponse
import com.example.data.model.SupabaseTokenResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    const val R2_CREATE_UPLOAD_URL_FUNCTION_PATH = "functions/v1/r2-create-upload-url"
    const val DEFAULT_R2_EDGE_FUNCTION_URL = "https://jycfkvcainmqcqxeaxly.supabase.co/functions/v1/r2-create-upload-url"

    fun getR2UploadFunctionUrl(context: Context): String {
        val baseUrl = getSupabaseUrl(context)
        return if (baseUrl.isNotBlank()) {
            val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            "${normalized}$R2_CREATE_UPLOAD_URL_FUNCTION_PATH"
        } else {
            DEFAULT_R2_EDGE_FUNCTION_URL
        }
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
    private var cachedR2Api: R2UploadApi? = null
    private var appContext: Context? = null
    private val refreshLock = Any()

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            val oldAuthHeader = request.header("Authorization")
            val isAuthTokenEndpoint = request.url.encodedPath.contains("auth/v1/token") ||
                    request.url.encodedPath.contains("auth/v1/signup") ||
                    request.url.encodedPath.contains("auth/v1/logout")

            if (response.code == 401 && !oldAuthHeader.isNullOrBlank() && !isAuthTokenEndpoint) {
                val ctx = appContext
                if (ctx != null) {
                    val sessionManager = SessionManager(ctx)
                    val refreshToken = sessionManager.getRefreshToken()

                    if (!refreshToken.isNullOrBlank()) {
                        val latestAccessToken = synchronized(refreshLock) {
                            val currentToken = sessionManager.getAccessToken()
                            if (!currentToken.isNullOrBlank() && "Bearer $currentToken" != oldAuthHeader) {
                                currentToken
                            } else {
                                performTokenRefresh(ctx, sessionManager, refreshToken)
                            }
                        }

                        if (!latestAccessToken.isNullOrBlank()) {
                            response.close()
                            val newRequest = request.newBuilder()
                                .header("Authorization", "Bearer $latestAccessToken")
                                .build()
                            return@addInterceptor chain.proceed(newRequest)
                        }
                    }
                }
            }

            response
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            // BASIC level logs only request method, URL, response status code, and execution time
            // Strictly avoids logging sensitive request bodies, passwords, or auth tokens
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private fun performTokenRefresh(context: Context, sessionManager: SessionManager, refreshToken: String): String? {
        val baseUrl = SupabaseConfig.getSupabaseUrl(context).let {
            if (it.isBlank()) return null else if (!it.endsWith("/")) "$it/" else it
        }
        val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
        if (anonKey.isBlank()) return null

        val jsonBody = """{"refresh_token":"$refreshToken"}"""
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull() ?: return null
        val refreshUrl = "${baseUrl}auth/v1/token?grant_type=refresh_token"

        val refreshReq = Request.Builder()
            .url(refreshUrl)
            .header("apikey", anonKey)
            .post(jsonBody.toRequestBody(mediaType))
            .build()

        val rawClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        try {
            rawClient.newCall(refreshReq).execute().use { res ->
                if (res.isSuccessful && res.body != null) {
                    val resString = res.body!!.string()
                    val adapter = moshi.adapter(SupabaseTokenResponse::class.java)
                    val tokenResponse = adapter.fromJson(resString)
                    if (tokenResponse != null && tokenResponse.accessToken.isNotBlank()) {
                        sessionManager.updateTokens(
                            newAccessToken = tokenResponse.accessToken,
                            newRefreshToken = tokenResponse.refreshToken
                        )
                        return tokenResponse.accessToken
                    }
                } else if (res.code == 400 || res.code == 401) {
                    sessionManager.clearSession()
                }
            }
        } catch (_: Exception) {
            // Network error during refresh
        }
        return null
    }

    fun getApi(context: Context): SupabaseAuthApi {
        appContext = context.applicationContext
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

    fun getR2Api(context: Context): R2UploadApi {
        appContext = context.applicationContext
        val url = SupabaseConfig.getSupabaseUrl(context).let {
            if (it.isBlank()) "https://jycfkvcainmqcqxeaxly.supabase.co/" else if (!it.endsWith("/")) "$it/" else it
        }

        if (cachedR2Api != null && currentUrl == url) {
            return cachedR2Api!!
        }

        currentUrl = url
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(R2UploadApi::class.java)
        cachedR2Api = api
        return api
    }

    fun reset() {
        cachedApi = null
        cachedR2Api = null
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
