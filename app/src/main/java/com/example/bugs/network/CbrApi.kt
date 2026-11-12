package com.example.bugs.network

import android.annotation.SuppressLint
import android.util.Log
import android.util.Xml
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParser
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.io.StringReader
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface CbrApiService {
    @Headers(
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
        "Accept: application/xml, text/xml, */*"
    )
    @GET("scripts/xml_metall.asp")
    suspend fun getMetallRatesRaw(
        @Query("date_req1") dateFrom: String,
        @Query("date_req2") dateTo: String
    ): ResponseBody
}

object CbrNetworkClient {
    private const val BASE_URL = "https://www.cbr.ru/"

    // Настройка доверия к сертификатам ЦБ РФ
    @SuppressLint("CustomX509TrustManager")
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("TrustAllX509TrustManager")
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Пропускаем проверку имени хоста для cbr.ru
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val service: CbrApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getUnsafeOkHttpClient())
            .build()
            .create(CbrApiService::class.java)
    }

    suspend fun fetchGoldPrice(): Float {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val toDate = dateFormat.format(calendar.time)

            calendar.add(Calendar.DAY_OF_YEAR, -14)
            val fromDate = dateFormat.format(calendar.time)

            val responseBody = service.getMetallRatesRaw(fromDate, toDate)
            val bytes = responseBody.bytes()
            val xmlString = String(bytes, Charset.forName("windows-1251"))

            Log.d("CBR_API", "XML Response length: ${xmlString.length}")
            parseLatestGoldPrice(xmlString) ?: 8500f
        } catch (e: Exception) {
            Log.e("CBR_API", "Ошибка запроса к ЦБ РФ", e)
            8500f
        }
    }

    private fun parseLatestGoldPrice(xml: String): Float? {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentCode = ""
        var currentBuy = ""
        var currentSell = ""
        var currentTag = ""
        var lastFoundPrice: Float? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("Record", ignoreCase = true)) {
                        currentCode = parser.getAttributeValue(null, "Code")
                            ?: parser.getAttributeValue(null, "code")
                            ?: ""
                        currentBuy = ""
                        currentSell = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        if (currentTag.equals("Buy", ignoreCase = true)) {
                            currentBuy = text
                        } else if (currentTag.equals("Sell", ignoreCase = true)) {
                            currentSell = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("Record", ignoreCase = true)) {
                        if (currentCode == "1") {
                            val priceStr = currentBuy.ifEmpty { currentSell }
                            val cleaned = priceStr
                                .replace("\u00A0", "")
                                .replace(" ", "")
                                .replace(",", ".")
                            cleaned.toFloatOrNull()?.let {
                                lastFoundPrice = it
                            }
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        Log.d("CBR_API", "Распарсенный курс золота: $lastFoundPrice")
        return lastFoundPrice
    }
}