package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class AuthOperationResult(
    val success: Boolean,
    val message: String
)

class AuthManager {
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    suspend fun trySignInAnonymously() {
        if (auth != null && auth?.currentUser == null) {
            try {
                auth?.signInAnonymously()?.await()
                Log.d("AuthManager", "Signed in anonymously")
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed to sign in anonymously", e)
            }
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Boolean {
        return try {
            auth?.signInWithEmailAndPassword(email, pass)?.await()
            true
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to sign in", e)
            false
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Boolean {
         return try {
             val user = auth?.currentUser
             if (user != null && user.isAnonymous) {
                 val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, pass)
                 user.linkWithCredential(credential).await()
             } else {
                 auth?.createUserWithEmailAndPassword(email, pass)?.await()
             }
             true
         } catch(e: Exception) {
             Log.e("AuthManager", "Failed to sign up", e)
             false
         }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Boolean {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = auth?.currentUser
            if (user != null && user.isAnonymous) {
                try {
                    user.linkWithCredential(credential).await()
                } catch (e: Exception) {
                    // If linking fails (e.g., account already exists), just sign in
                    auth?.signInWithCredential(credential)?.await()
                }
            } else {
                auth?.signInWithCredential(credential)?.await()
            }
            true
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to sign in with Google", e)
            false
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthOperationResult {
        val firebaseAuth = auth ?: return AuthOperationResult(
            success = false,
            message = "Firebase Auth is not available in this build."
        )

        return try {
            val methods = firebaseAuth.fetchSignInMethodsForEmail(email).await().signInMethods.orEmpty()
            if (methods.isNotEmpty() && EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD !in methods) {
                return AuthOperationResult(
                    success = false,
                    message = "This email uses Google sign-in. Continue with Google instead."
                )
            }

            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthOperationResult(
                success = true,
                message = "Password reset email sent. Check your inbox and spam folder."
            )
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.e("AuthManager", "No password account found for reset email", e)
            AuthOperationResult(
                success = false,
                message = "No password account was found for this email."
            )
        } catch (e: FirebaseNetworkException) {
            Log.e("AuthManager", "Network failed while sending password reset email", e)
            AuthOperationResult(
                success = false,
                message = "No internet connection. Try again when you are online."
            )
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to send password reset email", e)
            AuthOperationResult(
                success = false,
                message = e.localizedMessage ?: "Could not send reset email. Try again."
            )
        }
    }

    fun signOut() {
        auth?.signOut()
    }
}
