package com.mistbottle.jpnwordtrainer.data.remote

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SyncApiException(message: String) : Exception(message)

class SyncApiClient(
    baseUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val authRequestAdapter: JsonAdapter<AuthRequestDto> = moshi.adapter(AuthRequestDto::class.java)
    private val tokenResponseAdapter: JsonAdapter<AuthTokenResponseDto> = moshi.adapter(AuthTokenResponseDto::class.java)
    private val syncPayloadAdapter: JsonAdapter<SyncPayloadDto> = moshi.adapter(SyncPayloadDto::class.java)
    private val builtinDeckUpdateAdapter: JsonAdapter<BuiltinDeckUpdatePackageDto> = moshi.adapter(BuiltinDeckUpdatePackageDto::class.java)
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun register(username: String, password: String): Unit = withContext(Dispatchers.IO) {
        val requestJson = authRequestAdapter.toJson(AuthRequestDto(username = username, password = password))
        val request = Request.Builder()
            .url("$normalizedBaseUrl/auth/register")
            .post(requestJson.toRequestBody(jsonMediaType))
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SyncApiException(response.body?.string()?.ifBlank { null } ?: "회원가입에 실패했습니다.")
            }
        }
    }

    suspend fun login(username: String, password: String): AuthTokenResponseDto = withContext(Dispatchers.IO) {
        val requestJson = authRequestAdapter.toJson(AuthRequestDto(username = username, password = password))
        val request = Request.Builder()
            .url("$normalizedBaseUrl/auth/login")
            .post(requestJson.toRequestBody(jsonMediaType))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncApiException(raw.ifBlank { "로그인에 실패했습니다." })
            }
            return@withContext tokenResponseAdapter.fromJson(raw)
                ?: throw SyncApiException("로그인 응답을 해석하지 못했습니다.")
        }
    }

    suspend fun push(token: String, payload: SyncPayloadDto) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$normalizedBaseUrl/sync/push")
            .header("Authorization", "Bearer $token")
            .post(syncPayloadAdapter.toJson(payload).toRequestBody(jsonMediaType))
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SyncApiException(response.body?.string()?.ifBlank { null } ?: "동기화 업로드에 실패했습니다.")
            }
        }
    }

    suspend fun pull(token: String): SyncPayloadDto = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$normalizedBaseUrl/sync/pull")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncApiException(raw.ifBlank { "동기화 다운로드에 실패했습니다." })
            }
            return@withContext syncPayloadAdapter.fromJson(raw)
                ?: throw SyncApiException("동기화 응답을 해석하지 못했습니다.")
        }
    }

    suspend fun getBuiltinDeckUpdatePackage(
        token: String,
        stableKey: String,
        currentVersionCode: Int,
    ): BuiltinDeckUpdatePackageDto = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$normalizedBaseUrl/builtin-decks/$stableKey/update-package?current_version_code=$currentVersionCode")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncApiException(raw.ifBlank { "덱 업데이트 정보를 가져오지 못했습니다." })
            }
            return@withContext builtinDeckUpdateAdapter.fromJson(raw)
                ?: throw SyncApiException("덱 업데이트 응답을 해석하지 못했습니다.")
        }
    }
}
