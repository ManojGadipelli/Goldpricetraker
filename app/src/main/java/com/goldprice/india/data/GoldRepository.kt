package com.goldprice.india.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GoldPrice(
    val karat: String,
    val purity: String,
    val pricePerGram: Long,
    val pricePerTenGram: Long,
    val pricePerTola: Long,
)

data class GoldData(
    val city: String,
    val prices: List<GoldPrice>,
    val usdInr: Double,
    val fetchedAt: Long = System.currentTimeMillis()
)

data class City(val name: String, val slug: String)

object GoldRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val CITIES = listOf(
        City("India (National)", ""),
        City("Mumbai", "mumbai"),
        City("Delhi", "delhi"),
        City("Bangalore", "bangalore"),
        City("Chennai", "chennai"),
        City("Hyderabad", "hyderabad"),
        City("Pune", "pune"),
        City("Kolkata", "kolkata"),
        City("Ahmedabad", "ahmedabad"),
        City("Jaipur", "jaipur"),
        City("Noida", "noida"),
        City("Lucknow", "lucknow"),
        City("Surat", "surat"),
    )

    private const val PURITY_24K = 1.0000
    private const val PURITY_22K = 0.9166
    private const val PURITY_18K = 0.7500
    private const val GRAMS_PER_TROY_OZ = 31.1035

    fun getCities(): List<City> = CITIES

    fun fetchGoldPrices(citySlug: String): Result<GoldData> {
        return try {
            val goldPriceUsd = fetchGoldPriceUSD()
                ?: return Result.failure(Exception("Could not fetch gold price. Check your internet connection."))

            val usdInr = fetchUsdToInr() ?: 84.0

            val pricePerGram24kInr = (goldPriceUsd / GRAMS_PER_TROY_OZ) * usdInr
            val pricePerGram22kInr = pricePerGram24kInr * PURITY_22K
            val pricePerGram18kInr = pricePerGram24kInr * PURITY_18K

            val prices = listOf(
                GoldPrice(
                    karat = "24K", purity = "99.9%",
                    pricePerGram = pricePerGram24kInr.toLong(),
                    pricePerTenGram = (pricePerGram24kInr * 10).toLong(),
                    pricePerTola = (pricePerGram24kInr * 11.664).toLong()
                ),
                GoldPrice(
                    karat = "22K", purity = "91.6%",
                    pricePerGram = pricePerGram22kInr.toLong(),
                    pricePerTenGram = (pricePerGram22kInr * 10).toLong(),
                    pricePerTola = (pricePerGram22kInr * 11.664).toLong()
                ),
                GoldPrice(
                    karat = "18K", purity = "75.0%",
                    pricePerGram = pricePerGram18kInr.toLong(),
                    pricePerTenGram = (pricePerGram18kInr * 10).toLong(),
                    pricePerTola = (pricePerGram18kInr * 11.664).toLong()
                ),
            )

            val cityName = CITIES.find { it.slug == citySlug }?.name ?: "India"
            Result.success(GoldData(city = cityName, prices = prices, usdInr = usdInr))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchGoldPriceUSD(): Double? {
        return try {
            val request = Request.Builder()
                .url("https://api.gold-api.com/price/XAU")
                .header("Accept", "application/json")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            json.getDouble("price")
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchUsdToInr(): Double? {
        return try {
            val request = Request.Builder()
                .url("https://open.er-api.com/v6/latest/USD")
                .header("Accept", "application/json")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val rates = json.getJSONObject("rates")
            rates.getDouble("INR")
        } catch (e: Exception) {
            fetchUsdToInrFallback()
        }
    }

    private fun fetchUsdToInrFallback(): Double? {
        return try {
            val request = Request.Builder()
                .url("https://api.exchangerate-api.com/v4/latest/USD")
                .header("Accept", "application/json")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val rates = json.getJSONObject("rates")
            rates.getDouble("INR")
        } catch (e: Exception) {
            null
        }
    }
}
