package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class FitnessApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database) }

    override fun onCreate() {
        super.onCreate()
        if (
            FirebaseApp.getApps(this).isEmpty() &&
            BuildConfig.FIREBASE_API_KEY.isNotEmpty() &&
            BuildConfig.FIREBASE_API_KEY != "LOCAL_API_KEY"
        ) {
            val options = FirebaseOptions.Builder()
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .build()
            FirebaseApp.initializeApp(this, options)
        }
    }
}
