package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class ExtractedMeal(
    val name: String,
    val calories: Int,
    val protein: Float
)
    
object GeminiService {
    suspend fun extractMealInfoFromImage(base64Image: String, mimeType: String, additionalQuery: String): ExtractedMeal? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }
        
        val promptText = if (additionalQuery.isNotBlank()) {
            "Extract meal info from this image, considering this context: $additionalQuery"
        } else {
            "Extract meal info from this image."
        }
        
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = promptText),
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                responseMimeType = "application/json",
                responseSchema = buildJsonObject {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") { put("type", "STRING") }
                        putJsonObject("calories") { put("type", "INTEGER") }
                        putJsonObject("protein") { put("type", "NUMBER") }
                    }
                    put("required", kotlinx.serialization.json.buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("name"))
                        add(kotlinx.serialization.json.JsonPrimitive("calories"))
                        add(kotlinx.serialization.json.JsonPrimitive("protein"))
                    })
                }
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a nutrition assistant. Extract the implicit or explicit name, total calories and total protein (in grams) from the user's food description or image. Default calories to 0 and protein to 0 if totally unknown, but attempt to estimate if possible."))
            )
        )
        
        var attempts = 0
        var lastException: Exception? = null
        while (attempts < 2) {
            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val jsonResponseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonResponseText != null) {
                    val json = Json { ignoreUnknownKeys = true }
                    return@withContext json.decodeFromString<ExtractedMeal>(jsonResponseText)
                }
            } catch (e: Exception) {
                lastException = e
            }
            attempts++
        }
        lastException?.printStackTrace()
        return@withContext null
    }

    suspend fun extractMealInfo(query: String): ExtractedMeal? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null // Cannot extract without API key
        }
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Extract meal info: $query")))),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                responseMimeType = "application/json",
                responseSchema = buildJsonObject {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") { put("type", "STRING") }
                        putJsonObject("calories") { put("type", "INTEGER") }
                        putJsonObject("protein") { put("type", "NUMBER") }
                    }
                    put("required", kotlinx.serialization.json.buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("name"))
                        add(kotlinx.serialization.json.JsonPrimitive("calories"))
                        add(kotlinx.serialization.json.JsonPrimitive("protein"))
                    })
                }
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a nutrition assistant. Extract the implicit or explicit name, total calories and total protein (in grams) from the user's food description. Default calories to 0 and protein to 0 if totally unknown, but attempt to estimate if possible."))
            )
        )
        
        var attempts = 0
        var lastException: Exception? = null
        while (attempts < 2) {
            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val jsonResponseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonResponseText != null) {
                    val json = Json { ignoreUnknownKeys = true }
                    return@withContext json.decodeFromString<ExtractedMeal>(jsonResponseText)
                }
            } catch (e: Exception) {
                lastException = e
            }
            attempts++
        }
        lastException?.printStackTrace()
        return@withContext null
    }
}
