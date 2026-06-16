package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WeightRecord::class,
        WorkoutSession::class,
        WorkoutExercise::class,
        ExerciseSet::class,
        Meal::class,
        MealLog::class,
        ProgressPhoto::class,
        ExerciseMapping::class,
        TargetWeight::class,
        TargetNutrition::class,
        Routine::class,
        RoutineExercise::class,
        SetTarget::class
    ],
    version = 22,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE exercise_sets ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE exercise_sets ADD COLUMN distance REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_mappings` (`exerciseName` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, PRIMARY KEY(`exerciseName`))")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `target_weight` (`id` INTEGER NOT NULL, `targetWeightKg` REAL NOT NULL, `targetDateStamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE workout_sessions ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE meal_logs ADD COLUMN mealType TEXT NOT NULL DEFAULT 'Snack'")
                } catch (e: Exception) {}
                db.execSQL("CREATE TABLE IF NOT EXISTS `water_logs` (`dateStamp` INTEGER NOT NULL, `amountMl` INTEGER NOT NULL, PRIMARY KEY(`dateStamp`))")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE progress_photos ADD COLUMN weightKg REAL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE progress_photos ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE target_weight ADD COLUMN startWeightKg REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS water_logs")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE workout_sessions ADD COLUMN endTimeStamp INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    
                }
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE workout_sessions ADD COLUMN startTimeStamp INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Ignore duplicate
                }
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE meals ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {
                    // Ignore duplicate column exception
                }
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `target_nutrition` (`id` INTEGER NOT NULL, `targetCalories` INTEGER NOT NULL, `targetProtein` REAL NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `routines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `folderId` TEXT NOT NULL, `estimatedDuration` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `exerciseOrder` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `routine_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routineId` INTEGER NOT NULL, `exerciseName` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `note` TEXT NOT NULL, `restDurationSeconds` INTEGER NOT NULL, `supersetGroupId` TEXT NOT NULL, FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_exercises_routineId` ON `routine_exercises` (`routineId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `set_targets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routineExerciseId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `setType` TEXT NOT NULL, `targetWeight` REAL NOT NULL, `targetReps` INTEGER NOT NULL, `minimumReps` INTEGER NOT NULL, `maximumReps` INTEGER NOT NULL, `targetDuration` INTEGER NOT NULL, `targetDistance` REAL NOT NULL, FOREIGN KEY(`routineExerciseId`) REFERENCES `routine_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_set_targets_routineExerciseId` ON `set_targets` (`routineExerciseId`)")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE exercise_sets ADD COLUMN isDropSet INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE exercise_sets ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE exercise_sets ADD COLUMN distance REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) { }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_database"
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
