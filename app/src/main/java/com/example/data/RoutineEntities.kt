package com.example.data

import androidx.room.*

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val folderId: String = "My Routines",
    val estimatedDuration: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val exerciseOrder: String = ""
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId")]
)
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseName: String,
    val orderIndex: Int,
    val note: String = "",
    val restDurationSeconds: Int = 0,
    val supersetGroupId: String = ""
)

@Entity(
    tableName = "set_targets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExercise::class,
            parentColumns = ["id"],
            childColumns = ["routineExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineExerciseId")]
)
data class SetTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineExerciseId: Long,
    val orderIndex: Int,
    val setType: String = "Normal", // Normal, Warm-up, Drop, Failure
    val targetWeight: Float = 0f,
    val targetReps: Int = 0,
    val minimumReps: Int = 0,
    val maximumReps: Int = 0,
    val targetDuration: Int = 0,
    val targetDistance: Float = 0f
)

data class RoutineWithExercises(
    @Embedded val routine: Routine,
    @Relation(
        entity = RoutineExercise::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercises: List<RoutineExerciseWithTargets>
)

data class RoutineExerciseWithTargets(
    @Embedded val exercise: RoutineExercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineExerciseId"
    )
    val targets: List<SetTarget>
)
