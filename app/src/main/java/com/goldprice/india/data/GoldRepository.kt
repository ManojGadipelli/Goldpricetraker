package com.goldprice.india.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
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
    val fetchedAt: Long = System.currentTimeMillis()
)

object GoldRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-IN,en;q=0.9")
                .build()
            chain.proceed(request)
        }
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
        City("Coimbatore", "coimbatore"),
    )

    fun getCities(): List<City> = CITIES

    fun fetchGoldPrices(citySlug: String): Result<GoldData> {
        return try {
            val url = if (citySlug.isEmpty())
                "https://www.goodreturns.in/gold-rates/"
            else
                "https://www.goodreturns.in/gold-rates/$citySlug.html"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return Result.failure(Exception("Empty response"))
            val doc = Jsoup.parse(html)

            val prices = mutableListOf<GoldPrice>()

            // Strategy 1: Look for the main gold rate table
            // GoodReturns uses a table with class containing "gold" or similar
            val tables = doc.select("table")
            for (table in tables) {
                val rows = table.select("tr")
                for (row in rows) {
                    val cells = row.select("td")
                    if (cells.size >= 3) {
                        val text = cells[0].text().trim()
                        val karat = when {
                            text.contains("24", ignoreCase = true) -> "24K"
                            text.contains("22", ignoreCase = true) -> "22K"
                            text.contains("18", ignoreCase = true) -> "18K"
                            else -> null
                        } ?: continue

                        // Extract prices from remaining cells
                        val priceTexts = cells.drop(1).map { it.text().replace("[^0-9]".toRegex(), "") }
                        val numericPrices = priceTexts.mapNotNull { it.toLongOrNull() }.filter { it > 500 }

                        if (numericPrices.isNotEmpty()) {
                            val perGram = numericPrices.firstOrNull { it in 3000..25000 } ?: continue
                            val per10g = numericPrices.firstOrNull { it in 30000..250000 } ?: (perGram * 10)
                            val perTola = (perGram * 11.664).toLong()
                            prices.add(
                                GoldPrice(
                                    karat = karat,
                                    purity = karatPurity(karat),
                                    pricePerGram = perGram,
                                    pricePerTenGram = per10g,
                                    pricePerTola = perTola
                                )
                            )
                        }
                    }
                }
            }

            // Strategy 2: Regex extraction from full text if table parsing failed
            if (prices.size < 2) {
                prices.clear()
                val fullText = doc.text()
                val karatConfigs = listOf(
                    Triple("24K", "99.9%", listOf(
                        Regex("""24\s*[Kk](?:arat)?[^₹\d]{0,30}₹?\s*(\d[\d,]+)"""),
                        Regex("""(\d[\d,]+)\s*(?:per gram|/gram)[^\n]{0,30}24"""),
                    )),
                    Triple("22K", "91.6%", listOf(
                        Regex("""22\s*[Kk](?:arat)?[^₹\d]{0,30}₹?\s*(\d[\d,]+)"""),
                        Regex("""(\d[\d,]+)\s*(?:per gram|/gram)[^\n]{0,30}22"""),
                    )),
                    Triple("18K", "75.0%", listOf(
                        Regex("""18\s*[Kk](?:arat)?[^₹\d]{0,30}₹?\s*(\d[\d,]+)"""),
                        Regex("""(\d[\d,]+)\s*(?:per gram|/gram)[^\n]{0,30}18"""),
                    )),
                )
                for ((karat, purity, patterns) in karatConfigs) {
                    for (pattern in patterns) {
                        val match = pattern.find(fullText)
                        if (match != null) {
                            val raw = match.groupValues[1].replace(",", "").toLongOrNull() ?: continue
                            val perGram = when {
                                raw in 3000..25000 -> raw
                                raw in 30000..250000 -> raw / 10
                                else -> continue
                            }
                            prices.add(
                                GoldPrice(
                                    karat = karat,
                                    purity = purity,
                                    pricePerGram = perGram,
                                    pricePerTenGram = perGram * 10,
                                    pricePerTola = (perGram * 11.664).toLong()
                                )
                            )
                            break
                        }
                    }
                }
            }

            // Sort by karat descending
            prices.sortByDescending { it.karat }

            val cityName = CITIES.find { it.slug == citySlug }?.name ?: "India"
            Result.success(GoldData(city = cityName, prices = prices.distinctBy { it.karat }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun karatPurity(karat: String) = when (karat) {
        "24K" -> "99.9%"
        "22K" -> "91.6%"
        "18K" -> "75.0%"
        else -> ""
    }
}

data class City(val name: String, val slug: String)
