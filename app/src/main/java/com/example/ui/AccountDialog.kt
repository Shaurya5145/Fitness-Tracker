package com.example.ui

import android.util.Log
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class AuthMode {
    SignUp,
    Login
}

private enum class AuthAction {
    None,
    Email,
    Google,
    Reset,
    SignOut
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDialog(viewModel: FitnessViewModel, onDismiss: () -> Unit) {
    val currentUser by viewModel.authManager.currentUser.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var mode by remember { mutableStateOf(AuthMode.SignUp) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var touchedEmail by remember { mutableStateOf(false) }
    var touchedPassword by remember { mutableStateOf(false) }
    var activeAction by remember { mutableStateOf(AuthAction.None) }
    var bannerText by remember { mutableStateOf<String?>(null) }
    var successText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mode) {
        bannerText = null
        successText = null
        touchedEmail = false
        touchedPassword = false
    }

    val trimmedEmail = email.trim()
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val isPasswordValid = if (mode == AuthMode.SignUp) password.length >= 8 else password.isNotBlank()
    val emailError = when {
        !touchedEmail || trimmedEmail.isEmpty() -> null
        !isEmailValid -> "Enter a valid email address"
        else -> null
    }
    val passwordError = when {
        !touchedPassword || password.isEmpty() -> null
        mode == AuthMode.SignUp && password.length < 8 -> "Use at least 8 characters"
        else -> null
    }
    val canSubmit = isEmailValid && isPasswordValid && activeAction == AuthAction.None

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken == null) {
                activeAction = AuthAction.None
                bannerText = "Google did not return a sign-in token. Try again."
                return@rememberLauncherForActivityResult
            }

            viewModel.signInWithGoogle(idToken) { success ->
                activeAction = AuthAction.None
                if (success) {
                    successText = "Signed in successfully"
                    onDismiss()
                } else {
                    bannerText = "Google sign-in failed. Check your connection and try again."
                }
            }
        } catch (e: ApiException) {
            activeAction = AuthAction.None
            bannerText = if (e.statusCode == 12501) {
                "Google sign-in was cancelled."
            } else {
                "Google sign-in failed. Error ${e.statusCode}."
            }
        } catch (e: Exception) {
            activeAction = AuthAction.None
            Log.e("AccountDialog", "Google sign in error", e)
            bannerText = "Google sign-in failed. Try again."
        }
    }

    fun startGoogleSignIn() {
        if (activeAction != AuthAction.None) return
        val googleClientId = try {
            com.example.BuildConfig::class.java.getField("GOOGLE_WEB_CLIENT_ID").get(null) as? String
        } catch (e: Exception) {
            null
        }

        if (googleClientId.isNullOrEmpty() || googleClientId == "MY_GOOGLE_WEB_CLIENT_ID") {
            bannerText = "Google sign-in is not configured for this build."
            return
        }

        activeAction = AuthAction.Google
        bannerText = null
        successText = null
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(googleClientId)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        launcher.launch(googleSignInClient.signInIntent)
    }

    fun submitEmailAuth() {
        touchedEmail = true
        touchedPassword = true
        bannerText = null
        successText = null
        if (!canSubmit) return

        activeAction = AuthAction.Email
        if (mode == AuthMode.Login) {
            viewModel.signInWithEmail(trimmedEmail, password) { success ->
                activeAction = AuthAction.None
                if (success) {
                    successText = "Logged in successfully"
                    onDismiss()
                } else {
                    bannerText = "Login failed. Check your email and password."
                }
            }
        } else {
            viewModel.signUpWithEmail(trimmedEmail, password) { success ->
                activeAction = AuthAction.None
                if (success) {
                    successText = "Account created successfully"
                    onDismiss()
                } else {
                    bannerText = "Account creation failed. This email may already be registered."
                }
            }
        }
    }

    fun sendPasswordReset() {
        touchedEmail = true
        bannerText = null
        successText = null
        if (!isEmailValid || activeAction != AuthAction.None) return

        activeAction = AuthAction.Reset
        viewModel.sendPasswordResetEmail(trimmedEmail) { result ->
            activeAction = AuthAction.None
            if (result.success) {
                successText = result.message
            } else {
                bannerText = result.message
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (activeAction == AuthAction.None) onDismiss()
        },
        sheetState = sheetState,
        containerColor = AuthColors.Surface,
        contentColor = AuthColors.PrimaryText,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { AuthDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp)
                .animateContentSize(animationSpec = tween(220)),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SheetHeader(
                title = if (currentUser?.isAnonymous == false) "Account" else if (mode == AuthMode.Login) "Welcome back" else "Save your progress",
                description = if (currentUser?.isAnonymous == false) {
                    "Your data is synced securely to your account."
                } else if (mode == AuthMode.Login) {
                    "Log in to restore your fitness data and continue where you left off."
                } else {
                    "Create an account to securely save your fitness data and continue across devices. Your current guest data will be preserved."
                },
                onDismiss = { if (activeAction == AuthAction.None) onDismiss() }
            )

            BannerMessage(text = bannerText, isError = true)
            BannerMessage(text = successText, isError = false)

            if (currentUser?.isAnonymous == false) {
                AccountContent(
                    email = currentUser?.email ?: "Signed in",
                    isLoading = activeAction == AuthAction.SignOut,
                    onSignOut = {
                        if (activeAction != AuthAction.None) return@AccountContent
                        activeAction = AuthAction.SignOut
                        viewModel.signOutAndClearData {
                            activeAction = AuthAction.None
                            onDismiss()
                        }
                    }
                )
            } else {
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "auth-mode"
                ) { targetMode ->
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        GoogleAuthButton(
                            isLoading = activeAction == AuthAction.Google,
                            enabled = activeAction == AuthAction.None,
                            onClick = ::startGoogleSignIn
                        )

                        DividerWithText("or continue with email")

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AuthTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    if (touchedEmail) bannerText = null
                                },
                                label = "Email address",
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                                isError = emailError != null,
                                errorText = emailError,
                                enabled = activeAction == AuthAction.None,
                                onFocusLost = { touchedEmail = true }
                            )

                            AuthTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    if (touchedPassword) bannerText = null
                                },
                                label = "Password",
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { passwordVisible = !passwordVisible },
                                        modifier = Modifier.semantics {
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                                isError = passwordError != null,
                                errorText = passwordError,
                                enabled = activeAction == AuthAction.None,
                                onFocusLost = { touchedPassword = true }
                            )

                            if (targetMode == AuthMode.SignUp) {
                                PasswordRequirement(password)
                            } else {
                                TextButton(
                                    onClick = ::sendPasswordReset,
                                    enabled = activeAction == AuthAction.None,
                                    modifier = Modifier.align(Alignment.End).height(36.dp)
                                ) {
                                    if (activeAction == AuthAction.Reset) {
                                        SmallSpinner()
                                        Spacer(Modifier.width(8.dp))
                                        Text("Sending...", color = AuthColors.Blue)
                                    } else {
                                        Text("Forgot password?", color = AuthColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        PrimaryAuthButton(
                            text = if (targetMode == AuthMode.Login) "Log in" else "Create account",
                            loadingText = if (targetMode == AuthMode.Login) "Logging in..." else "Creating account...",
                            isLoading = activeAction == AuthAction.Email,
                            enabled = canSubmit,
                            onClick = ::submitEmailAuth
                        )

                        GuestDataMessage()

                        AuthModeSwitch(
                            text = if (targetMode == AuthMode.Login) "New here? " else "Already have an account? ",
                            actionText = if (targetMode == AuthMode.Login) "Create an account" else "Log in",
                            onClick = {
                                if (activeAction == AuthAction.None) {
                                    mode = if (targetMode == AuthMode.Login) AuthMode.SignUp else AuthMode.Login
                                }
                            }
                        )

                        if (targetMode == AuthMode.SignUp) {
                            TermsText()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 38.dp, height = 4.dp)
            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
    )
}

@Composable
private fun SheetHeader(title: String, description: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                color = AuthColors.PrimaryText,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = AuthColors.SecondaryText,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(44.dp)
                .background(AuthColors.InputSurface, CircleShape)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = AuthColors.SecondaryText)
        }
    }
}

@Composable
private fun AccountContent(email: String, isLoading: Boolean, onSignOut: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = AuthColors.InputSurface,
            border = BorderStroke(1.dp, AuthColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Signed in as", color = AuthColors.SecondaryText, fontSize = 13.sp)
                Text(email, color = AuthColors.PrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
        PrimaryAuthButton(
            text = "Sign out",
            loadingText = "Signing out...",
            isLoading = isLoading,
            enabled = !isLoading,
            icon = Icons.Filled.Logout,
            onClick = onSignOut
        )
    }
}

@Composable
private fun GoogleAuthButton(isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "google-press")

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White.copy(alpha = 0.7f),
            disabledContentColor = Color(0xFF1F1F1F).copy(alpha = 0.55f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1F1F1F))
            Spacer(Modifier.width(10.dp))
            Text("Connecting...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        } else {
            GoogleMark()
            Spacer(Modifier.width(10.dp))
            Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GoogleMark() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color.White, CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            color = Color(0xFF4285F4),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DividerWithText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = AuthColors.Border)
        Text(text, color = AuthColors.SecondaryText, fontSize = 13.sp)
        HorizontalDivider(modifier = Modifier.weight(1f), color = AuthColors.Border)
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable () -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isError: Boolean,
    errorText: String?,
    enabled: Boolean,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AuthColors.PrimaryText,
                unfocusedTextColor = AuthColors.PrimaryText,
                disabledTextColor = AuthColors.DisabledText,
                focusedContainerColor = AuthColors.InputSurface,
                unfocusedContainerColor = AuthColors.InputSurface,
                disabledContainerColor = AuthColors.InputSurface.copy(alpha = 0.55f),
                focusedBorderColor = AuthColors.Blue,
                unfocusedBorderColor = AuthColors.Border,
                errorBorderColor = AuthColors.Error,
                focusedLabelColor = AuthColors.Blue,
                unfocusedLabelColor = AuthColors.SecondaryText,
                errorLabelColor = AuthColors.Error,
                focusedLeadingIconColor = AuthColors.Blue,
                unfocusedLeadingIconColor = AuthColors.SecondaryText,
                errorLeadingIconColor = AuthColors.Error,
                focusedTrailingIconColor = AuthColors.SecondaryText,
                unfocusedTrailingIconColor = AuthColors.SecondaryText,
                cursorColor = AuthColors.Blue,
                errorCursorColor = AuthColors.Error
            )
        )
        LaunchedEffect(value) {
            if (value.isNotEmpty()) onFocusLost()
        }
        if (errorText != null) {
            Text(errorText, color = AuthColors.Error, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun PasswordRequirement(password: String) {
    val valid = password.length >= 8
    Text(
        text = if (valid) "8 or more characters" else "Use at least 8 characters",
        color = if (valid) AuthColors.Success else AuthColors.SecondaryText,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun PrimaryAuthButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "primary-press")

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthColors.Blue,
            contentColor = Color.White,
            disabledContainerColor = AuthColors.Blue.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.55f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(loadingText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GuestDataMessage() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AuthColors.Blue.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AuthColors.Blue.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.CloudDone, contentDescription = null, tint = AuthColors.Blue, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Your current data is safe", color = AuthColors.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Your workouts, meals, weight entries and progress photos will stay with your account.",
                    color = AuthColors.SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun AuthModeSwitch(text: String, actionText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = AuthColors.SecondaryText, fontSize = 14.sp)
        TextButton(onClick = onClick, modifier = Modifier.height(40.dp)) {
            Text(actionText, color = AuthColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TermsText() {
    Text(
        text = buildAnnotatedString {
            append("By creating an account, you agree to the ")
            withStyle(SpanStyle(color = AuthColors.Blue, fontWeight = FontWeight.Medium)) {
                append("Terms of Service")
            }
            append(" and ")
            withStyle(SpanStyle(color = AuthColors.Blue, fontWeight = FontWeight.Medium)) {
                append("Privacy Policy")
            }
            append(".")
        },
        color = AuthColors.SecondaryText,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun BannerMessage(text: String?, isError: Boolean) {
    if (text == null) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) AuthColors.Error.copy(alpha = 0.12f) else AuthColors.Success.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isError) AuthColors.Error.copy(alpha = 0.28f) else AuthColors.Success.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = if (isError) "Error" else "Success",
                tint = if (isError) AuthColors.Error else AuthColors.Success,
                modifier = Modifier.size(20.dp)
            )
            Text(text, color = AuthColors.PrimaryText, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun SmallSpinner() {
    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AuthColors.Blue)
}

private object AuthColors {
    val Surface = Color(0xFF1C1C1E)
    val InputSurface = Color(0xFF242428)
    val Border = Color.White.copy(alpha = 0.10f)
    val PrimaryText = Color(0xFFF5F5F7)
    val SecondaryText = Color(0xFFA1A1AA)
    val DisabledText = Color(0xFF66666E)
    val Blue = Color(0xFF0A84FF)
    val Error = Color(0xFFFF453A)
    val Success = Color(0xFF34C759)
}
