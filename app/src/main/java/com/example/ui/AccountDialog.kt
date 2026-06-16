package com.example.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FitnessViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ApiException
import android.util.Log

@Composable
fun AccountDialog(viewModel: FitnessViewModel, onDismiss: () -> Unit) {
    val currentUser by viewModel.authManager.currentUser.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken) { success ->
                        if (success) {
                            onDismiss()
                        } else {
                            errorText = "Google Sign in failed."
                        }
                    }
                } else {
                    errorText = "Failed to get ID token."
                }
            } catch (e: Exception) {
                Log.e("AccountDialog", "Google sign in error", e)
                errorText = "Error: ${e.localizedMessage ?: e.message ?: "Failed"}"
            }
        }
    }
    
    val handleGoogleSignIn: () -> Unit = {
        try {
            val googleClientId = try {
                com.example.BuildConfig::class.java.getField("GOOGLE_WEB_CLIENT_ID").get(null) as? String
            } catch (e: Exception) {
                null
            }
            
            if (googleClientId.isNullOrEmpty() || googleClientId == "MY_GOOGLE_WEB_CLIENT_ID") {
                 errorText = "Please configure GOOGLE_WEB_CLIENT_ID in Secrets."
            } else {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(googleClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                launcher.launch(googleSignInClient.signInIntent)
            }
        } catch (e: Exception) {
            errorText = "Init failed: ${e.message}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (currentUser?.isAnonymous == false) "Account" else (if (isLogin) "Log In" else "Sign Up / Link Account"))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentUser?.isAnonymous == false) {
                    Text("Logged in as ${currentUser?.email}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your data is automatically synced to the cloud.")
                } else {
                    if (currentUser?.isAnonymous == true) {
                        Text("You are using a Guest Account. Sign up to save your data.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (errorText.isNotEmpty()) {
                        Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = handleGoogleSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Sign in with Google")
                    }

                    TextButton(
                        onClick = { isLogin = !isLogin },
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                    ) {
                        Text(if (isLogin) "Need an account? Sign Up" else "Already have an account? Log In")
                    }
                }
            }
        },
        confirmButton = {
            if (currentUser?.isAnonymous == false) {
                TextButton(onClick = { 
                    viewModel.signOutAndClearData {
                        onDismiss()
                    }
                }) {
                    Text("Sign Out")
                }
            } else {
                Button(onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorText = "Please fill in all fields"
                        return@Button
                    }
                    if (isLogin) {
                        viewModel.signInWithEmail(email, password) { success ->
                            if (success) {
                                onDismiss()
                            } else {
                                errorText = "Authentication failed. Try again."
                            }
                        }
                    } else {
                        viewModel.signUpWithEmail(email, password) { success ->
                            if (success) {
                                onDismiss()
                            } else {
                                errorText = "Authentication failed. Try again."
                            }
                        }
                    }
                }) {
                    Text(if (isLogin) "Log In" else "Sign Up")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
