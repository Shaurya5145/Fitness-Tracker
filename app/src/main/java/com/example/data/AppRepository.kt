package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    private val dao = db.appDao()
    private val routineDao = db.routineDao()

    suspend fun replaceAllDataForRestore(restore: suspend AppRepository.() -> Unit = {}) {
        db.withTransaction {
            clearTablesForRestore()
            this@AppRepository.restore()
        }
    }

    suspend fun saveRoutineWithExercises(
        routine: Routine,
        exercises: List<RoutineExerciseWithTargets>
    ): Long {
        return db.withTransaction {
            val routineId = if (routine.id == 0L) {
                routineDao.insertRoutine(routine)
            } else {
                routineDao.updateRoutine(routine)
                routineDao.deleteExercisesForRoutine(routine.id)
                routine.id
            }

            exercises.forEachIndexed { exIndex, exWithTargets ->
                val routineExerciseId = routineDao.insertRoutineExercise(
                    exWithTargets.exercise.copy(
                        id = 0L,
                        routineId = routineId,
                        orderIndex = exIndex
                    )
                )
                exWithTargets.targets.forEachIndexed { setIndex, setTarget ->
                    routineDao.insertSetTarget(
                        setTarget.copy(
                            id = 0L,
                            routineExerciseId = routineExerciseId,
                            orderIndex = setIndex
                        )
                    )
                }
            }

            routineId
        }
    }

    private suspend fun clearTablesForRestore() {
        routineDao.deleteAllSetTargets()
        routineDao.deleteAllRoutineExercises()
        routineDao.deleteAllRoutines()

        dao.deleteAllExerciseSets()
        dao.deleteAllWorkoutExercises()
        dao.deleteAllWorkoutSessions()
        dao.deleteAllMealLogs()
        dao.deleteAllMeals()
        dao.deleteAllProgressPhotos()
        dao.deleteAllWeightRecords()
        dao.deleteAllExerciseMappings()
        dao.deleteAllTargetWeight()
        dao.deleteAllTargetNutrition()
    }

    val allWeightRecords: Flow<List<WeightRecord>> = dao.getAllWeightRecords()
    val fullWorkoutSessions: Flow<List<SessionWithExercises>> = dao.getFullWorkoutSessions()
    val allMeals: Flow<List<Meal>> = dao.getAllMeals()
    val allMealLogs: Flow<List<MealLog>> = dao.getAllMealLogs()
    val allProgressPhotos: Flow<List<ProgressPhoto>> = dao.getAllProgressPhotos()
    val allExerciseMappings: Flow<List<ExerciseMapping>> = dao.getAllExerciseMappings()
    val targetWeight: Flow<TargetWeight?> = dao.getTargetWeight()
    val targetNutrition: Flow<TargetNutrition?> = dao.getTargetNutrition()

    val allRoutinesWithExercises: Flow<List<RoutineWithExercises>> = routineDao.getAllRoutinesWithExercises()

    suspend fun getRoutineWithExercises(routineId: Long) = routineDao.getRoutineWithExercises(routineId)
    suspend fun insertRoutine(routine: Routine) = routineDao.insertRoutine(routine)
    suspend fun updateRoutine(routine: Routine) = routineDao.updateRoutine(routine)
    suspend fun deleteRoutine(routine: Routine) = routineDao.deleteRoutine(routine)
    
    suspend fun insertRoutineExercise(routineExercise: RoutineExercise) = routineDao.insertRoutineExercise(routineExercise)
    suspend fun updateRoutineExercise(routineExercise: RoutineExercise) = routineDao.updateRoutineExercise(routineExercise)
    suspend fun insertSetTarget(setTarget: SetTarget) = routineDao.insertSetTarget(setTarget)
    suspend fun deleteSetTargetsForExercise(routineExerciseId: Long) = routineDao.deleteSetTargetsForExercise(routineExerciseId)


    suspend fun getAllWeightRecordsSnapshot() = dao.getAllWeightRecordsSnapshot()
    suspend fun getFullWorkoutSessionsSnapshot() = dao.getFullWorkoutSessionsSnapshot()
    suspend fun getAllMealsSnapshot() = dao.getAllMealsSnapshot()
    suspend fun getAllMealLogsSnapshot() = dao.getAllMealLogsSnapshot()
    suspend fun getAllProgressPhotosSnapshot() = dao.getAllProgressPhotosSnapshot()
    suspend fun getAllExerciseMappingsSnapshot() = dao.getAllExerciseMappingsSnapshot()
    suspend fun getTargetWeightSnapshot() = dao.getTargetWeightSnapshot()
    suspend fun getTargetNutritionSnapshot() = dao.getTargetNutritionSnapshot()
    suspend fun getAllRoutinesWithExercisesSnapshot() = routineDao.getAllRoutinesWithExercisesSnapshot()

    fun getMealLogsForDate(date: Long) = dao.getMealLogsForDate(date)

    suspend fun insertWeightRecord(record: WeightRecord) = dao.insertWeightRecord(record)
    suspend fun deleteWeightRecord(record: WeightRecord) = dao.deleteWeightRecord(record)

    suspend fun insertWorkoutSession(session: WorkoutSession): Long = dao.insertWorkoutSession(session)
    suspend fun updateWorkoutSession(session: WorkoutSession) = dao.updateWorkoutSession(session)
    suspend fun deleteWorkoutSession(session: WorkoutSession) = dao.deleteWorkoutSession(session)

    suspend fun insertWorkoutExercise(exercise: WorkoutExercise): Long = dao.insertWorkoutExercise(exercise)
    suspend fun updateWorkoutExercise(exercise: WorkoutExercise) = dao.updateWorkoutExercise(exercise)
    suspend fun deleteWorkoutExercise(exercise: WorkoutExercise) = dao.deleteWorkoutExercise(exercise)

    suspend fun insertExerciseSet(set: ExerciseSet) = dao.insertExerciseSet(set)
    suspend fun deleteExerciseSet(set: ExerciseSet) = dao.deleteExerciseSet(set)

    suspend fun insertMeal(meal: Meal): Long = dao.insertMeal(meal)
    suspend fun updateMeal(meal: Meal) = dao.updateMeal(meal)
    suspend fun deleteMeal(meal: Meal) = dao.deleteMeal(meal)
    suspend fun insertMealLog(log: MealLog) = dao.insertMealLog(log)
    suspend fun deleteMealLog(mealId: Long, dateStamp: Long) = dao.deleteMealLog(mealId, dateStamp)
    suspend fun deleteMealLogById(id: Long) = dao.deleteMealLogById(id)

    suspend fun insertProgressPhoto(photo: ProgressPhoto): Long = dao.insertProgressPhoto(photo)
    suspend fun deleteProgressPhoto(photo: ProgressPhoto) = dao.deleteProgressPhoto(photo)
    suspend fun updateProgressPhoto(photo: ProgressPhoto) = dao.updateProgressPhoto(photo)

    suspend fun insertExerciseMappings(mappings: List<ExerciseMapping>) = dao.insertExerciseMappings(mappings)

    suspend fun insertTargetWeight(target: TargetWeight) = dao.insertTargetWeight(target)

    suspend fun insertTargetNutrition(target: TargetNutrition) = dao.insertTargetNutrition(target)
}
