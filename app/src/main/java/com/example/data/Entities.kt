package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey val dateStamp: Long,
    val weightKg: Float
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val startTimeStamp: Long = 0L,
    val endTimeStamp: Long = 0L,
    val name: String,
    val notes: String = ""
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val isDropSet: Boolean = false,
    val durationMinutes: Int = 0,
    val distance: Float = 0f
)

@Entity(tableName = "exercise_mappings")
data class ExerciseMapping(
    @PrimaryKey val exerciseName: String,
    val muscleGroup: String
)

@Entity(tableName = "target_weight")
data class TargetWeight(
    @PrimaryKey val id: Int = 1,
    val targetWeightKg: Float,
    val targetDateStamp: Long,
    val startWeightKg: Float = 0f
)

@Entity(tableName = "target_nutrition")
data class TargetNutrition(
    @PrimaryKey val id: Int = 1,
    val targetCalories: Int,
    val targetProtein: Float
)

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val calories: Int = 0,
    val protein: Float = 0f,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val isTemplate: Boolean = true
)

@Entity(
    tableName = "meal_logs",
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId")]
)
data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val dateStamp: Long,
    val mealType: String = "Snacks" // e.g. Early Morning, Breakfast, Post Workout, Lunch, Snacks, Dinner
)

@Entity(tableName = "progress_photos")
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateStamp: Long,
    val imagePath: String,
    val viewType: String, // "Front", "Side", "Back"
    val remoteUrl: String? = null,
    val weightKg: Float? = null,
    val notes: String = ""
)
