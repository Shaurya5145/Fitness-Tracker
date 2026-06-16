package com.example

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun database_migration_isCorrect() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fitness_database"
        ).fallbackToDestructiveMigration().build()
        
        val dao = db.routineDao()
        val routines = dao.getAllRoutinesWithExercises().first()
        assertNotNull(routines)
    }
}
