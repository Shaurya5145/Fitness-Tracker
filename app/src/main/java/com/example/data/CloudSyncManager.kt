package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import org.json.JSONObject
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudSyncManager(val repository: AppRepository) {
    private val syncMutex = Mutex()

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun uploadPhotoToCloudinary(photoPath: String, onError: (String) -> Unit): String? = withContext(Dispatchers.IO) {
        val file = File(photoPath)
        if (!file.exists()) {
            onError("Local file not found.")
            return@withContext null
        }

        val url = com.example.BuildConfig.CLOUDINARY_URL
        if (url.isNullOrEmpty() || url.contains("DEFAULT")) {
            onError("Cloudinary is not configured. Photo stays local and will not be included in cloud backup.")
            return@withContext null
        }
        
        try {
            val stripped = url.removePrefix("cloudinary://")
            val parts = stripped.split("@")
            if (parts.size != 2) {
                onError("Invalid Cloudinary URL format.")
                return@withContext null
            }
            val keySecret = parts[0].split(":")
            if (keySecret.size != 2) {
                onError("Invalid Cloudinary API Key/Secret format.")
                return@withContext null
            }
            val apiKey = keySecret[0]
            val apiSecret = keySecret[1]
            val cloudName = parts[1]

            val file = File(photoPath)
            if (!file.exists()) {
                onError("Local file not found.")
                return@withContext null
            }

            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val stringToSign = "timestamp=$timestamp$apiSecret"
            val signature = sha1(stringToSign)

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                .addFormDataPart("api_key", apiKey)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val jsonStr = response.body?.string()

            if (response.isSuccessful && !jsonStr.isNullOrEmpty()) {
                val json = JSONObject(jsonStr)
                val secureUrl = json.optString("secure_url").takeIf { it.isNotBlank() }
                if (secureUrl != null && secureUrl.isNotEmpty()) {
                    return@withContext secureUrl
                } else {
                    onError("Upload succeeded but no secure_url returned.")
                    return@withContext null
                }
            } else {
                val errorMsg = if (!jsonStr.isNullOrEmpty()) {
                    JSONObject(jsonStr).optJSONObject("error")?.optString("message", "Unknown error") ?: jsonStr
                } else {
                    response.message
                }
                onError("API Error: $errorMsg (HTTP ${response.code})")
            }
        } catch(e: Exception) {
            onError("Exception: ${e.message}")
        }
        return@withContext null
    }

    suspend fun deletePhotoFromCloudinary(remoteUrl: String, onError: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (remoteUrl.startsWith("data:image")) return@withContext
        val url = com.example.BuildConfig.CLOUDINARY_URL
        if (url.isNullOrEmpty() || url.contains("DEFAULT")) {
            return@withContext
        }
        
        try {
            val stripped = url.removePrefix("cloudinary://")
            val parts = stripped.split("@")
            if (parts.size != 2) return@withContext
            val keySecret = parts[0].split(":")
            if (keySecret.size != 2) return@withContext
            val apiKey = keySecret[0]
            val apiSecret = keySecret[1]
            val cloudName = parts[1]

            val publicId = remoteUrl.substringAfterLast("/").substringBeforeLast(".")
            if (publicId.isEmpty()) return@withContext

            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val stringToSign = "public_id=$publicId&timestamp=$timestamp$apiSecret"
            val signature = sha1(stringToSign)

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("public_id", publicId)
                .addFormDataPart("api_key", apiKey)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/destroy")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val jsonStr = response.body?.string()
                val errorMsg = if (!jsonStr.isNullOrEmpty()) {
                    JSONObject(jsonStr).optJSONObject("error")?.optString("message", "Unknown error") ?: jsonStr
                } else {
                    response.message
                }
                onError("API Error: $errorMsg (HTTP ${response.code})")
            } else {
                Log.d("Cloudinary", "Deleted successfully: $publicId")
            }
        } catch(e: Exception) {
            onError("Exception: ${e.message}")
        }
    }

    // Call this after any important local change
    suspend fun backupToCloud(): Boolean = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val user = auth?.currentUser ?: return@withContext false
            val db = firestore ?: return@withContext false

            try {
                val weightRecords = repository.getAllWeightRecordsSnapshot()
                val meals = repository.getAllMealsSnapshot()
                val mealLogs = repository.getAllMealLogsSnapshot()
                val progressPhotos = repository.getAllProgressPhotosSnapshot()
                val fullWorkoutSessions = repository.getFullWorkoutSessionsSnapshot()
                val exerciseMappings = repository.getAllExerciseMappingsSnapshot()
                val targetWeight = repository.getTargetWeightSnapshot()
                val targetNutrition = repository.getTargetNutritionSnapshot()
                val routines = repository.getAllRoutinesWithExercisesSnapshot()

                val data = hashMapOf(
                    "weightRecords" to weightRecords,
                    "meals" to meals,
                    "mealLogs" to mealLogs,
                    "progressPhotos" to progressPhotos,
                    "workoutSessions" to fullWorkoutSessions,
                    "exerciseMappings" to exerciseMappings,
                    "targetWeight" to targetWeight,
                    "targetNutrition" to targetNutrition,
                    "routines" to routines,
                    "updatedAt" to System.currentTimeMillis()
                )

                db.collection("users").document(user.uid)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d("CloudSync", "Backup successful")
                true
            } catch (e: Exception) {
                Log.e("CloudSync", "Backup failed", e)
                false
            }
        }
    }

    suspend fun restoreFromCloud(): Boolean = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val user = auth?.currentUser ?: return@withContext false
            val db = firestore ?: return@withContext false

            try {
                val doc = db.collection("users").document(user.uid).get().await()
                if (!doc.exists()) {
                    Log.d("CloudSync", "No cloud data found.")
                    return@withContext false
                }

                val meals = doc.get("meals") as? List<Map<String, Any>> ?: emptyList()
                val mealEntities = meals.map { map ->
                    val id = (map["id"] as? Number)?.toLong() ?: 0L
                    val name = map["name"] as? String ?: ""
                    val calories = (map["calories"] as? Number)?.toInt() ?: 0
                    val protein = (map["protein"] as? Number)?.toFloat() ?: 0f
                    val reminderHour = (map["reminderHour"] as? Number)?.toInt()
                    val reminderMinute = (map["reminderMinute"] as? Number)?.toInt()
                    val isTemplate = map["isTemplate"] as? Boolean ?: true
                    Meal(id, name, calories, protein, reminderHour, reminderMinute, isTemplate)
                }
                
                val weightRecords = doc.get("weightRecords") as? List<Map<String, Any>> ?: emptyList()
                val weightEntities = weightRecords.map { map ->
                    val dateStamp = (map["dateStamp"] as? Number)?.toLong() ?: 0L
                    val weightKg = (map["weightKg"] as? Number)?.toFloat() ?: 0f
                    WeightRecord(dateStamp, weightKg)
                }
                
                val mealLogs = doc.get("mealLogs") as? List<Map<String, Any>> ?: emptyList()
                val mealLogEntities = mealLogs.map { map ->
                    val id = (map["id"] as? Number)?.toLong() ?: 0L
                    val mealId = (map["mealId"] as? Number)?.toLong() ?: 0L
                    val dateStamp = (map["dateStamp"] as? Number)?.toLong() ?: 0L
                    val mealType = map["mealType"] as? String ?: "Snacks"
                    MealLog(id, mealId, dateStamp, mealType)
                }

                val progressPhotos = doc.get("progressPhotos") as? List<Map<String, Any>> ?: emptyList()
                val progressPhotoEntities = progressPhotos.map { map ->
                    val id = (map["id"] as? Number)?.toLong() ?: 0L
                    val dateStamp = (map["dateStamp"] as? Number)?.toLong() ?: 0L
                    val imagePath = map["imagePath"] as? String ?: ""
                    val viewType = map["viewType"] as? String ?: "Front"
                    val remoteUrl = map["remoteUrl"] as? String
                    val weightKg = (map["weightKg"] as? Number)?.toFloat()
                    val notes = map["notes"] as? String ?: ""
                    
                    if (remoteUrl != null && remoteUrl.startsWith("data:image")) {
                        try {
                            val base64 = remoteUrl.substring(remoteUrl.indexOf(",") + 1)
                            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            val imageFile = File(imagePath)
                            imageFile.parentFile?.mkdirs()
                            java.io.FileOutputStream(imageFile).use { it.write(bytes) }
                        } catch (e: Exception) {
                            Log.e("CloudSync", "Failed to restore legacy embedded photo", e)
                        }
                    }

                    ProgressPhoto(id, dateStamp, imagePath, viewType, remoteUrl, weightKg, notes)
                }

                val workoutSessions = doc.get("workoutSessions") as? List<Map<String, Any>> ?: emptyList()
                val workoutSessionEntities = workoutSessions.mapNotNull { sessionMapWrapper ->
                    val sessionMap = sessionMapWrapper["session"] as? Map<String, Any> ?: return@mapNotNull null
                    val sessionId = (sessionMap["id"] as? Number)?.toLong() ?: 0L
                    val timestamp = (sessionMap["timestamp"] as? Number)?.toLong() ?: 0L
                    val startTimeStamp = (sessionMap["startTimeStamp"] as? Number)?.toLong() ?: 0L
                    val endTimeStamp = (sessionMap["endTimeStamp"] as? Number)?.toLong() ?: 0L
                    val sessionName = sessionMap["name"] as? String ?: ""
                    val notes = sessionMap["notes"] as? String ?: ""
                    WorkoutSession(id = sessionId, timestamp = timestamp, startTimeStamp = startTimeStamp, endTimeStamp = endTimeStamp, name = sessionName, notes = notes)
                }

                val workoutExerciseEntities = mutableListOf<WorkoutExercise>()
                val exerciseSetEntities = mutableListOf<ExerciseSet>()
                workoutSessions.forEach { sessionMapWrapper ->
                    val sessionMap = sessionMapWrapper["session"] as? Map<String, Any> ?: return@forEach
                    val sessionId = (sessionMap["id"] as? Number)?.toLong() ?: 0L
                    val exercisesList = sessionMapWrapper["exercises"] as? List<Map<String, Any>> ?: emptyList()
                    exercisesList.forEach { exMapWrapper ->
                        val exMap = exMapWrapper["exercise"] as? Map<String, Any> ?: return@forEach
                        val exId = (exMap["id"] as? Number)?.toLong() ?: 0L
                        val exName = exMap["exerciseName"] as? String ?: ""
                        workoutExerciseEntities.add(WorkoutExercise(id = exId, sessionId = sessionId, exerciseName = exName))

                        val setsList = exMapWrapper["sets"] as? List<Map<String, Any>> ?: emptyList()
                        setsList.forEach { setMap ->
                            val setId = (setMap["id"] as? Number)?.toLong() ?: 0L
                            val setNumber = (setMap["setNumber"] as? Number)?.toInt() ?: 1
                            val reps = (setMap["reps"] as? Number)?.toInt() ?: 0
                            val weightKg = (setMap["weightKg"] as? Number)?.toFloat() ?: 0f
                            val isDropSet = (setMap["isDropSet"] as? Boolean) ?: (setMap["dropSet"] as? Boolean) ?: false
                            val durationMinutes = (setMap["durationMinutes"] as? Number)?.toInt() ?: 0
                            val distance = (setMap["distance"] as? Number)?.toFloat() ?: 0f
                            exerciseSetEntities.add(ExerciseSet(id = setId, exerciseId = exId, setNumber = setNumber, reps = reps, weightKg = weightKg, isDropSet = isDropSet, durationMinutes = durationMinutes, distance = distance))
                        }
                    }
                }

                val exerciseMappings = doc.get("exerciseMappings") as? List<Map<String, Any>> ?: emptyList()
                val exerciseMappingEntities = exerciseMappings.mapNotNull { map ->
                    val exerciseName = map["exerciseName"] as? String ?: ""
                    val muscleGroup = map["muscleGroup"] as? String ?: "Other"
                    if (exerciseName.isNotEmpty()) ExerciseMapping(exerciseName, muscleGroup) else null
                }

                val targetWeightMap = doc.get("targetWeight") as? Map<String, Any>
                val targetWeightEntity = if (targetWeightMap != null) {
                    val weightKg = (targetWeightMap["targetWeightKg"] as? Number)?.toFloat() ?: 0f
                    val dateStamp = (targetWeightMap["targetDateStamp"] as? Number)?.toLong() ?: 0L
                    val startWeightKg = (targetWeightMap["startWeightKg"] as? Number)?.toFloat() ?: 0f
                    TargetWeight(1, weightKg, dateStamp, startWeightKg)
                } else null

                val targetNutritionMap = doc.get("targetNutrition") as? Map<String, Any>
                val targetNutritionEntity = if (targetNutritionMap != null) {
                    val calories = (targetNutritionMap["targetCalories"] as? Number)?.toInt() ?: 0
                    val protein = (targetNutritionMap["targetProtein"] as? Number)?.toFloat() ?: 0f
                    TargetNutrition(1, calories, protein)
                } else null

                val routinesList = doc.get("routines") as? List<Map<String, Any>> ?: emptyList()
                val routineEntities = mutableListOf<Routine>()
                val routineExerciseEntities = mutableListOf<RoutineExercise>()
                val setTargetEntities = mutableListOf<SetTarget>()
                routinesList.forEach { routineMapWrapper ->
                    val routineMap = routineMapWrapper["routine"] as? Map<String, Any> ?: return@forEach
                    val routineId = (routineMap["id"] as? Number)?.toLong() ?: 0L
                    val name = routineMap["name"] as? String ?: ""
                    val description = routineMap["description"] as? String ?: ""
                    val folderId = routineMap["folderId"] as? String ?: ""
                    val estimatedDuration = (routineMap["estimatedDuration"] as? Number)?.toInt() ?: 0
                    val createdAt = (routineMap["createdAt"] as? Number)?.toLong() ?: 0L
                    val updatedAt = (routineMap["updatedAt"] as? Number)?.toLong() ?: 0L
                    val exerciseOrder = routineMap["exerciseOrder"] as? String ?: ""
                    
                    routineEntities.add(Routine(id = routineId, name = name, description = description, folderId = folderId, estimatedDuration = estimatedDuration, createdAt = createdAt, updatedAt = updatedAt, exerciseOrder = exerciseOrder))
                    
                    val exercisesList = routineMapWrapper["exercises"] as? List<Map<String, Any>> ?: emptyList()
                    exercisesList.forEach { exMapWrapper ->
                        val exMap = exMapWrapper["exercise"] as? Map<String, Any> ?: return@forEach
                        val exId = (exMap["id"] as? Number)?.toLong() ?: 0L
                        val exerciseName = exMap["exerciseName"] as? String ?: ""
                        val orderIndex = (exMap["orderIndex"] as? Number)?.toInt() ?: 0
                        val note = exMap["note"] as? String ?: ""
                        val restDurationSeconds = (exMap["restDurationSeconds"] as? Number)?.toInt() ?: 0
                        val supersetGroupId = exMap["supersetGroupId"] as? String ?: ""
                        
                        routineExerciseEntities.add(RoutineExercise(id = exId, routineId = routineId, exerciseName = exerciseName, orderIndex = orderIndex, note = note, restDurationSeconds = restDurationSeconds, supersetGroupId = supersetGroupId))
                        
                        val targetsList = exMapWrapper["targets"] as? List<Map<String, Any>> ?: emptyList()
                        targetsList.forEach { targetMap ->
                            val targetId = (targetMap["id"] as? Number)?.toLong() ?: 0L
                            val targetOrderIndex = (targetMap["orderIndex"] as? Number)?.toInt() ?: 0
                            val setType = targetMap["setType"] as? String ?: "Normal"
                            val targetWeight = (targetMap["targetWeight"] as? Number)?.toFloat() ?: 0f
                            val targetReps = (targetMap["targetReps"] as? Number)?.toInt() ?: 0
                            val minimumReps = (targetMap["minimumReps"] as? Number)?.toInt() ?: 0
                            val maximumReps = (targetMap["maximumReps"] as? Number)?.toInt() ?: 0
                            val targetDuration = (targetMap["targetDuration"] as? Number)?.toInt() ?: 0
                            val targetDistance = (targetMap["targetDistance"] as? Number)?.toFloat() ?: 0f
                            
                            setTargetEntities.add(SetTarget(id = targetId, routineExerciseId = exId, orderIndex = targetOrderIndex, setType = setType, targetWeight = targetWeight, targetReps = targetReps, minimumReps = minimumReps, maximumReps = maximumReps, targetDuration = targetDuration, targetDistance = targetDistance))
                        }
                    }
                }

                repository.replaceAllDataForRestore {
                    mealEntities.forEach { insertMeal(it) }
                    weightEntities.forEach { insertWeightRecord(it) }
                    mealLogEntities.forEach { insertMealLog(it) }
                    progressPhotoEntities.forEach { insertProgressPhoto(it) }
                    workoutSessionEntities.forEach { insertWorkoutSession(it) }
                    workoutExerciseEntities.forEach { insertWorkoutExercise(it) }
                    exerciseSetEntities.forEach { insertExerciseSet(it) }
                    if (exerciseMappingEntities.isNotEmpty()) {
                        insertExerciseMappings(exerciseMappingEntities)
                    }
                    targetWeightEntity?.let { insertTargetWeight(it) }
                    targetNutritionEntity?.let { insertTargetNutrition(it) }
                    routineEntities.forEach { insertRoutine(it) }
                    routineExerciseEntities.forEach { insertRoutineExercise(it) }
                    setTargetEntities.forEach { insertSetTarget(it) }
                }

                Log.d("CloudSync", "Restore successful")
                true
            } catch (e: Exception) {
                Log.e("CloudSync", "Restore failed", e)
                false
            }
        }
    }
}
