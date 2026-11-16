package com.pro.aifitnesstracker.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import com.google.gson.annotations.SerializedName

data class FoodSearchResponse(
    val products: List<FoodProduct>?
)

data class FoodProduct(
    val product_name: String?,
    val nutriments: Nutriments?
)

data class Nutriments(
    @SerializedName("energy-kcal_100g")
    val energy_kcal_100g: Double?,
    
    @SerializedName("proteins_100g")
    val proteins_100g: Double?,
    
    @SerializedName("carbohydrates_100g")
    val carbohydrates_100g: Double?,
    
    @SerializedName("fat_100g")
    val fat_100g: Double?
)

data class BarcodeSearchResponse(
    val code: String?,
    val product: FoodProduct?
)

interface FoodApi {
    @GET("cgi/search.pl?search_simple=1&action=process&json=1")
    suspend fun searchFood(@Query("search_terms") query: String): FoodSearchResponse

    @GET("api/v2/product/{barcode}.json")
    suspend fun searchByBarcode(@retrofit2.http.Path("barcode") barcode: String): BarcodeSearchResponse
}

object FoodNetworkManager {
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "AIFitnessTracker - Android - Version 1.0")
            .build()
        chain.proceed(request)
    }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: FoodApi = retrofit.create(FoodApi::class.java)
}
