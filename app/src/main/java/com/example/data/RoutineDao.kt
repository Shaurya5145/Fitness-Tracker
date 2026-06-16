package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Transaction
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun getAllRoutinesWithExercises(): Flow<List<RoutineWithExercises>>
    
    @Transaction
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    suspend fun getAllRoutinesWithExercisesSnapshot(): List<RoutineWithExercises>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineWithExercises(routineId: Long): RoutineWithExercises?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(routineExercise: RoutineExercise): Long

    @Update
    suspend fun updateRoutineExercise(routineExercise: RoutineExercise)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesForRoutine(routineId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetTarget(setTarget: SetTarget): Long

    @Query("DELETE FROM set_targets WHERE routineExerciseId = :routineExerciseId")
    suspend fun deleteSetTargetsForExercise(routineExerciseId: Long)
}
