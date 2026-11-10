package com.dominickp.firetvandroidtv.data.network

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * SearchApi — Retrofit interface for searching content.
 *
 * Mirrors Roku's search endpoint from roku-channel-template:
 *   - GET /search?q=query
 *   - Returns list of VideoItems matching the query
 */
interface SearchApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50,
    ): retrofit2.Response<ResponseBody>
}

/**
 * Creates a SearchApi Retrofit instance.
 */
fun createSearchApi(baseUrl: String): SearchApi {
    val okHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(SearchApi::class.java)
}
