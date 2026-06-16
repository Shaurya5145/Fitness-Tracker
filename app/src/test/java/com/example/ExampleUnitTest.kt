package com.example

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.runBlocking
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun saveRoutineWithExercises_replacesExistingChildren() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val repository = AppRepository(db)
            val routineId = repository.saveRoutineWithExercises(
                routine = Routine(name = "Push Day"),
                exercises = listOf(
                    RoutineExerciseWithTargets(
                        exercise = RoutineExercise(
                            routineId = 0L,
                            exerciseName = "Bench Press",
                            orderIndex = 0
                        ),
                        targets = listOf(
                            SetTarget(
                                routineExerciseId = 0L,
                                orderIndex = 0,
                                targetReps = 8,
                                targetWeight = 60f
                            )
                        )
                    )
                )
            )

            repository.saveRoutineWithExercises(
                routine = Routine(
                    id = routineId,
                    name = "Updated Push Day",
                    createdAt = 123L,
                    updatedAt = 456L
                ),
                exercises = listOf(
                    RoutineExerciseWithTargets(
                        exercise = RoutineExercise(
                            routineId = routineId,
                            exerciseName = "Incline Bench",
                            orderIndex = 0
                        ),
                        targets = listOf(
                            SetTarget(
                                routineExerciseId = 0L,
                                orderIndex = 0,
                                targetReps = 10,
                                targetWeight = 50f
                            ),
                            SetTarget(
                                routineExerciseId = 0L,
                                orderIndex = 1,
                                targetReps = 12,
                                targetWeight = 45f
                            )
                        )
                    )
                )
            )

            val savedRoutine = repository.getRoutineWithExercises(routineId)

            assertNotNull(savedRoutine)
            assertEquals("Updated Push Day", savedRoutine!!.routine.name)
            assertEquals(1, savedRoutine.exercises.size)
            assertEquals("Incline Bench", savedRoutine.exercises.single().exercise.exerciseName)
            assertEquals(2, savedRoutine.exercises.single().targets.size)
        } finally {
            db.close()
        }
    }
}
