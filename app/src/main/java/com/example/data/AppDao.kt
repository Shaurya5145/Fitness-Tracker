package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Weight
    @Query("SELECT * FROM weight_records ORDER BY dateStamp DESC")
    fun getAllWeightRecords(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records ORDER BY dateStamp DESC")
    suspend fun getAllWeightRecordsSnapshot(): List<WeightRecord>

    @Query("DELETE FROM weight_records")
    suspend fun deleteAllWeightRecords()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightRecord(record: WeightRecord)

    @Delete
    suspend fun deleteWeightRecord(record: WeightRecord)

    // Workout Sessions
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession): Long

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllWorkoutSessions()

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSession)

    @Delete
    suspend fun deleteWorkoutSession(session: WorkoutSession)
    
    // Workout Exercises
    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId")
    fun getExercisesForSession(sessionId: Long): Flow<List<WorkoutExercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(exercise: WorkoutExercise): Long

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAllWorkoutExercises()
    
    @Delete
    suspend fun deleteWorkoutExercise(exercise: WorkoutExercise)

    @Update
    suspend fun updateWorkoutExercise(exercise: WorkoutExercise)

    // Exercise Sets
    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId ORDER BY setNumber ASC")
    fun getSetsForExercise(exerciseId: Long): Flow<List<ExerciseSet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSet(set: ExerciseSet)

    @Query("DELETE FROM exercise_sets")
    suspend fun deleteAllExerciseSets()
    
    @Delete
    suspend fun deleteExerciseSet(set: ExerciseSet)
    
    // Complex queries for UI convenience?
    // We can just rely on collecting Flow or simple relational mapping.
    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getFullWorkoutSessions(): Flow<List<SessionWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    suspend fun getFullWorkoutSessionsSnapshot(): List<SessionWithExercises>

    // Meals
    @Query("SELECT * FROM meals ORDER BY id ASC")
    fun getAllMeals(): Flow<List<Meal>>

    @Query("SELECT * FROM meals ORDER BY id ASC")
    suspend fun getAllMealsSnapshot(): List<Meal>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeal(meal: Meal): Long

    @Query("DELETE FROM meals")
    suspend fun deleteAllMeals()

    @Update
    suspend fun updateMeal(meal: Meal)

    @Delete
    suspend fun deleteMeal(meal: Meal)
    
    // Meal Logs
    @Query("SELECT * FROM meal_logs WHERE dateStamp = :dateStamp")
    fun getMealLogsForDate(dateStamp: Long): Flow<List<MealLog>>

    @Query("SELECT * FROM meal_logs ORDER BY dateStamp DESC")
    fun getAllMealLogs(): Flow<List<MealLog>>

    @Query("SELECT * FROM meal_logs ORDER BY dateStamp DESC")
    suspend fun getAllMealLogsSnapshot(): List<MealLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(log: MealLog)

    @Query("DELETE FROM meal_logs")
    suspend fun deleteAllMealLogs()

    @Query("DELETE FROM meal_logs WHERE mealId = :mealId AND dateStamp = :dateStamp")
    suspend fun deleteMealLog(mealId: Long, dateStamp: Long)

    @Query("DELETE FROM meal_logs WHERE id = :id")
    suspend fun deleteMealLogById(id: Long)

    // Progress Photos
    @Query("SELECT * FROM progress_photos ORDER BY dateStamp DESC")
    fun getAllProgressPhotos(): Flow<List<ProgressPhoto>>

    @Query("SELECT * FROM progress_photos ORDER BY dateStamp DESC")
    suspend fun getAllProgressPhotosSnapshot(): List<ProgressPhoto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressPhoto(photo: ProgressPhoto)

    @Query("DELETE FROM progress_photos")
    suspend fun deleteAllProgressPhotos()

    @Delete
    suspend fun deleteProgressPhoto(photo: ProgressPhoto)

    @Update
    suspend fun updateProgressPhoto(photo: ProgressPhoto)

    // Exercise Mappings
    @Query("SELECT * FROM exercise_mappings")
    fun getAllExerciseMappings(): Flow<List<ExerciseMapping>>

    @Query("SELECT * FROM exercise_mappings")
    suspend fun getAllExerciseMappingsSnapshot(): List<ExerciseMapping>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseMappings(mappings: List<ExerciseMapping>)

    @Query("DELETE FROM exercise_mappings")
    suspend fun deleteAllExerciseMappings()

    // Target Weight
    @Query("SELECT * FROM target_weight WHERE id = 1")
    fun getTargetWeight(): Flow<TargetWeight?>

    @Query("SELECT * FROM target_weight WHERE id = 1")
    suspend fun getTargetWeightSnapshot(): TargetWeight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetWeight(targetWeight: TargetWeight)

    @Query("DELETE FROM target_weight")
    suspend fun deleteAllTargetWeight()

    // Target Nutrition
    @Query("SELECT * FROM target_nutrition WHERE id = 1")
    fun getTargetNutrition(): Flow<TargetNutrition?>

    @Query("SELECT * FROM target_nutrition WHERE id = 1")
    suspend fun getTargetNutritionSnapshot(): TargetNutrition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetNutrition(targetNutrition: TargetNutrition)

    @Query("DELETE FROM target_nutrition")
    suspend fun deleteAllTargetNutrition()

}

data class SessionWithExercises(
    @Embedded val session: WorkoutSession,
    @Relation(
        entity = WorkoutExercise::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exercises: List<ExerciseWithSets>
)

data class ExerciseWithSets(
    @Embedded val exercise: WorkoutExercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<ExerciseSet>
)
