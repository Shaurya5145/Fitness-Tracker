package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class FitnessViewModel(private val repository: AppRepository) : ViewModel() {

    val authManager = AuthManager()
    val cloudSyncManager = CloudSyncManager(repository)

    // Rest Timer State
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()
    private var timerJob: kotlinx.coroutines.Job? = null
    private var targetTimeMillis: Long = 0

    fun startRestTimer(seconds: Int) {
        timerJob?.cancel()
        targetTimeMillis = System.currentTimeMillis() + (seconds * 1000L)
        _restTimerSeconds.value = seconds
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = ((targetTimeMillis - System.currentTimeMillis()) / 1000L).toInt()
                if (remaining <= 0) {
                    _restTimerSeconds.value = 0
                    break
                }
                _restTimerSeconds.value = remaining
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        _restTimerSeconds.value = 0
        targetTimeMillis = 0
    }

    fun addRestTime(seconds: Int) {
        if (_restTimerSeconds.value > 0) {
            targetTimeMillis += (seconds * 1000L)
            _restTimerSeconds.value = ((targetTimeMillis - System.currentTimeMillis()) / 1000L).toInt()
        }
    }

    init {
        viewModelScope.launch {
            authManager.trySignInAnonymously()
        }
    }

    fun signOutAndClearData(onComplete: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            authManager.signOut()
            repository.replaceAllDataForRestore()
            authManager.trySignInAnonymously()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = authManager.signInWithEmail(email, pass)
            if (success) {
                cloudSyncManager.restoreFromCloud()
            }
            onResult(success)
        }
    }

    fun signUpWithEmail(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = authManager.signUpWithEmail(email, pass)
            if (success) {
                cloudSyncManager.backupToCloud()
            }
            onResult(success)
        }
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = authManager.signInWithGoogleCredential(idToken)
            if (success) {
                cloudSyncManager.restoreFromCloud()
            }
            onResult(success)
        }
    }

    val weightRecords: StateFlow<List<WeightRecord>> = repository.allWeightRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val workoutSessions: StateFlow<List<SessionWithExercises>> = repository.fullWorkoutSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addWeightRecord(weightKg: Float, timestamp: Long) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            repository.insertWeightRecord(WeightRecord(dateStamp = cal.timeInMillis, weightKg = weightKg))
            cloudSyncManager.backupToCloud()
        }
    }

    fun addWorkoutSession(name: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertWorkoutSession(WorkoutSession(timestamp = timestamp, startTimeStamp = System.currentTimeMillis(), name = name))
            cloudSyncManager.backupToCloud()
        }
    }

    fun updateWorkoutSessionNotes(session: WorkoutSession, newNotes: String) {
        viewModelScope.launch {
            repository.updateWorkoutSession(session.copy(notes = newNotes))
            cloudSyncManager.backupToCloud()
        }
    }

    fun updateWorkoutSessionName(session: WorkoutSession, newName: String) {
        viewModelScope.launch {
            repository.updateWorkoutSession(session.copy(name = newName))
            cloudSyncManager.backupToCloud()
        }
    }

    fun finishWorkoutSession(session: WorkoutSession) {
        viewModelScope.launch {
            repository.updateWorkoutSession(session.copy(endTimeStamp = System.currentTimeMillis()))
            cloudSyncManager.backupToCloud()
        }
    }

    fun duplicateWorkoutSession(sessionData: SessionWithExercises, timestamp: Long) {
        viewModelScope.launch {
            val newSessionId = repository.insertWorkoutSession(
                WorkoutSession(name = sessionData.session.name, timestamp = timestamp)
            )
            sessionData.exercises.forEach { exWithSets ->
                val newExId = repository.insertWorkoutExercise(
                    WorkoutExercise(sessionId = newSessionId, exerciseName = exWithSets.exercise.exerciseName)
                )
                exWithSets.sets.forEach { set ->
                    repository.insertExerciseSet(
                        ExerciseSet(
                            exerciseId = newExId,
                            setNumber = set.setNumber,
                            reps = 0,
                            weightKg = 0f,
                            isDropSet = set.isDropSet
                        )
                    )
                }
            }
            cloudSyncManager.backupToCloud()
        }
    }

    fun addExerciseToSession(sessionId: Long, exerciseName: String) {
        viewModelScope.launch {
            repository.insertWorkoutExercise(WorkoutExercise(sessionId = sessionId, exerciseName = exerciseName))
            cloudSyncManager.backupToCloud()
            checkAndGenerateExerciseMapping(exerciseName.trim())
        }
    }

    private fun checkAndGenerateExerciseMapping(exerciseName: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentMappings = allExerciseMappings.value
            if (currentMappings.any { it.exerciseName.equals(exerciseName, ignoreCase = true) }) {
                return@launch
            }
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@launch
                
                val prompt = "Categorize the exercise '$exerciseName' into one of the following muscle groups: Chest, Back, Legs, Shoulders, Arms, Core, Cardio, Other. Respond with only the muscle group name."
                
                val request = com.example.data.GenerateContentRequest(
                    contents = listOf(
                        com.example.data.Content(
                            parts = listOf(com.example.data.Part(text = prompt))
                        )
                    )
                )
                val response = com.example.data.RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (responseText != null) {
                    val validGroups = setOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio", "Other")
                    val matchedGroup = validGroups.find { it.equals(responseText, ignoreCase = true) } ?: "Other"
                    repository.insertExerciseMappings(listOf(com.example.data.ExerciseMapping(exerciseName, matchedGroup)))
                    cloudSyncManager.backupToCloud()
                }
            } catch (e: Exception) {
                // Ignore API failure and fallback gracefully next time or default
            }
        }
    }

    fun updateExercise(exercise: WorkoutExercise) {
        viewModelScope.launch {
            repository.updateWorkoutExercise(exercise)
            cloudSyncManager.backupToCloud()
        }
    }

    fun deleteExercise(exercise: WorkoutExercise) {
        viewModelScope.launch {
            repository.deleteWorkoutExercise(exercise)
            cloudSyncManager.backupToCloud()
        }
    }

    fun deleteWorkoutSession(session: WorkoutSession) {
        viewModelScope.launch {
            repository.deleteWorkoutSession(session)
            cloudSyncManager.backupToCloud()
        }
    }

    fun addSetToExercise(exerciseId: Long, setNumber: Int, reps: Int, weightKg: Float, isDropSet: Boolean, durationMinutes: Int = 0, distance: Float = 0f) {
        viewModelScope.launch {
            repository.insertExerciseSet(ExerciseSet(exerciseId = exerciseId, setNumber = setNumber, reps = reps, weightKg = weightKg, isDropSet = isDropSet, durationMinutes = durationMinutes, distance = distance))
            cloudSyncManager.backupToCloud()
        }
    }

    fun cleanUpEmptySets(sessionId: Long) {
        viewModelScope.launch {
            val session = workoutSessions.value.find { it.session.id == sessionId }
            session?.exercises?.forEach { exerciseWithSets ->
                exerciseWithSets.sets.forEach { set ->
                    if (set.reps == 0 && set.weightKg == 0f && set.durationMinutes == 0 && set.distance == 0f) {
                        repository.deleteExerciseSet(set)
                    }
                }
            }
            cloudSyncManager.backupToCloud()
        }
    }

    fun updateSet(set: ExerciseSet) {
        viewModelScope.launch {
            repository.insertExerciseSet(set)
            cloudSyncManager.backupToCloud()
        }
    }

    fun updateSets(sets: List<ExerciseSet>) {
        viewModelScope.launch {
            sets.forEach {
                repository.insertExerciseSet(it)
            }
            cloudSyncManager.backupToCloud()
        }
    }
    
    fun deleteWeightRecord(record: WeightRecord) {
        viewModelScope.launch { 
            repository.deleteWeightRecord(record) 
            cloudSyncManager.backupToCloud()
        }
    }

    val allMeals: StateFlow<List<Meal>> = repository.allMeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExerciseMappings: StateFlow<List<ExerciseMapping>> = repository.allExerciseMappings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutinesWithExercises: StateFlow<List<RoutineWithExercises>> = repository.allRoutinesWithExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRoutine(
        routine: Routine,
        exercises: List<RoutineExerciseWithTargets>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = try {
                repository.saveRoutineWithExercises(routine, exercises)
                cloudSyncManager.backupToCloud()
                true
            } catch (e: Exception) {
                Log.e("FitnessViewModel", "Failed to save routine", e)
                false
            }
            onComplete(success)
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            try {
                repository.deleteRoutine(routine)
                cloudSyncManager.backupToCloud()
            } catch (e: Exception) {
                Log.e("FitnessViewModel", "Failed to delete routine", e)
            }
        }
    }

    fun createSessionFromRoutineAsync(routineWithExercises: RoutineWithExercises, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val sessionName = routineWithExercises.routine.name
                val sId = repository.insertWorkoutSession(WorkoutSession(timestamp = System.currentTimeMillis(), startTimeStamp = System.currentTimeMillis(), name = sessionName))
                routineWithExercises.exercises.forEach { ex ->
                    val exId = repository.insertWorkoutExercise(WorkoutExercise(sessionId = sId, exerciseName = ex.exercise.exerciseName))
                    ex.targets.forEach { target ->
                        repository.insertExerciseSet(
                            ExerciseSet(
                                exerciseId = exId,
                                setNumber = target.orderIndex + 1,
                                reps = target.targetReps,
                                weightKg = target.targetWeight,
                                isDropSet = target.setType == "Drop"
                            )
                        )
                    }
                }
                cloudSyncManager.backupToCloud()
                onCreated(sId)
            } catch (e: Exception) {
                Log.e("FitnessViewModel", "Failed to create session from routine", e)
            }
        }
    }

    val allMealLogs: StateFlow<List<MealLog>> = repository.allMealLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProgressPhotos: StateFlow<List<ProgressPhoto>> = repository.allProgressPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetWeight: StateFlow<TargetWeight?> = repository.targetWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val targetNutrition: StateFlow<TargetNutrition?> = repository.targetNutrition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateTargetWeight(weightKg: Float, targetDateStamp: Long, startWeightKg: Float) {
        viewModelScope.launch {
            val target = TargetWeight(id = 1, targetWeightKg = weightKg, targetDateStamp = targetDateStamp, startWeightKg = startWeightKg)
            repository.insertTargetWeight(target)
            cloudSyncManager.backupToCloud()
        }
    }

    fun updateTargetNutrition(calories: Int, protein: Float) {
        viewModelScope.launch {
            val target = TargetNutrition(id = 1, targetCalories = calories, targetProtein = protein)
            repository.insertTargetNutrition(target)
            cloudSyncManager.backupToCloud()
        }
    }

    private val _selectedDate = MutableStateFlow(getTodayMidnight())
    val selectedDate: StateFlow<Long> = _selectedDate
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val mealLogsForSelectedDate: StateFlow<List<MealLog>> = _selectedDate
        .flatMapLatest { date -> repository.getMealLogsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiLoading = MutableStateFlow(false)
    val aiError = MutableStateFlow<String?>(null)

    fun logMealWithAI(query: String, mealType: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            aiLoading.value = true
            aiError.value = null
            
            val info = com.example.data.GeminiService.extractMealInfo(query)
            if (info != null) {
                val mealId = repository.insertMeal(Meal(name = info.name, calories = info.calories, protein = info.protein, isTemplate = false))
                repository.insertMealLog(MealLog(mealId = mealId, dateStamp = _selectedDate.value, mealType = mealType))
                onSuccess()
            } else {
                aiError.value = "Failed to process text. Check your API key or ensure the AI can identify the food."
            }
            aiLoading.value = false
            cloudSyncManager.backupToCloud()
        }
    }

    fun logMealWithAIFromImage(base64Image: String, mimeType: String, query: String, mealType: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            aiLoading.value = true
            aiError.value = null
            
            val info = com.example.data.GeminiService.extractMealInfoFromImage(base64Image, mimeType, query)
            if (info != null) {
                val mealId = repository.insertMeal(Meal(name = info.name, calories = info.calories, protein = info.protein, isTemplate = false))
                repository.insertMealLog(MealLog(mealId = mealId, dateStamp = _selectedDate.value, mealType = mealType))
                onSuccess()
            } else {
                aiError.value = "Failed to process image. Make sure the API key is valid and the image clearly shows food."
            }
            aiLoading.value = false
            cloudSyncManager.backupToCloud()
        }
    }

    fun setSelectedDate(timestamp: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        _selectedDate.value = cal.timeInMillis
    }

    fun addMeal(name: String, calories: Int, protein: Float, reminderHour: Int? = null, reminderMinute: Int? = null, context: android.content.Context? = null) {
        viewModelScope.launch {
            val mealId = repository.insertMeal(Meal(name = name, calories = calories, protein = protein, reminderHour = reminderHour, reminderMinute = reminderMinute))
            if (mealId > 0L && reminderHour != null && reminderMinute != null && context != null) {
                com.example.notifications.ReminderScheduler.scheduleMealReminder(context, mealId, name, reminderHour, reminderMinute)
            }
            cloudSyncManager.backupToCloud()
        }
    }

    fun updateMeal(meal: Meal, context: android.content.Context? = null) {
        viewModelScope.launch {
            repository.updateMeal(meal)
            if (meal.reminderHour != null && meal.reminderMinute != null && context != null) {
                com.example.notifications.ReminderScheduler.scheduleMealReminder(context, meal.id, meal.name, meal.reminderHour, meal.reminderMinute)
            } else if (context != null) {
                com.example.notifications.ReminderScheduler.cancelMealReminder(context, meal.id)
            }
            cloudSyncManager.backupToCloud()
        }
    }

    fun deleteMeal(meal: Meal, context: android.content.Context? = null) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
            if (context != null) {
                com.example.notifications.ReminderScheduler.cancelMealReminder(context, meal.id)
            }
            cloudSyncManager.backupToCloud()
        }
    }

    fun logMeal(mealId: Long, mealType: String) {
        viewModelScope.launch {
            repository.insertMealLog(MealLog(mealId = mealId, dateStamp = _selectedDate.value, mealType = mealType))
            cloudSyncManager.backupToCloud()
        }
    }

    fun deleteMealLog(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMealLog(mealId, _selectedDate.value)
            cloudSyncManager.backupToCloud()
        }
    }
    
    fun deleteMealLogById(id: Long) {
        viewModelScope.launch {
            repository.deleteMealLogById(id)
            cloudSyncManager.backupToCloud()
        }
    }

    fun toggleMealLog(mealId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                repository.insertMealLog(MealLog(mealId = mealId, dateStamp = _selectedDate.value))
            } else {
                repository.deleteMealLog(mealId, _selectedDate.value)
            }
            cloudSyncManager.backupToCloud()
        }
    }

    private fun getTodayMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError

    fun clearUploadError() {
        _uploadError.value = null
    }

    fun addProgressPhoto(dateStamp: Long, imagePath: String, viewType: String, weightKg: Float? = null, notes: String = "") {
        viewModelScope.launch {
            val photo = ProgressPhoto(dateStamp = dateStamp, imagePath = imagePath, viewType = viewType, weightKg = weightKg, notes = notes)
            val photoId = repository.insertProgressPhoto(photo)
            
            // Sync in background
            val remoteUrl = cloudSyncManager.uploadPhotoToCloudinary(imagePath) { errorMsg ->
                _uploadError.value = errorMsg
            }

            if (remoteUrl != null && photoId > 0L) {
                repository.updateProgressPhoto(photo.copy(id = photoId, remoteUrl = remoteUrl))
            }

            cloudSyncManager.backupToCloud()
        }
    }

    fun deleteProgressPhoto(photo: ProgressPhoto) {
        viewModelScope.launch {
            repository.deleteProgressPhoto(photo)
            
            if (photo.remoteUrl != null) {
                cloudSyncManager.deletePhotoFromCloudinary(photo.remoteUrl) { errorMsg ->
                    _uploadError.value = errorMsg
                }
            }
            
            cloudSyncManager.backupToCloud()
        }
    }
}

class FitnessViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FitnessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitnessViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
