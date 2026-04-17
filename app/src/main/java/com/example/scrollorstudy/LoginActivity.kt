package com.example.scrollorstudy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.scrollorstudy.data.local.PreferencesManager
import com.example.scrollorstudy.ui.components.BackgroundStyle
import com.example.scrollorstudy.ui.components.ConfigurableBackground
import com.example.scrollorstudy.ui.theme.ScrollOrStudyTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.example.scrollorstudy.utils.EmailSender
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val preferencesManager: PreferencesManager by lazy { (application as ScrollOrStudyApplication).container.preferencesManager }

    private var pendingStudentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        setupGoogleSignIn()
        setupBiometrics()

        setContent {
            val userRole by preferencesManager.userRole.collectAsState(initial = null)
            val studentUid by preferencesManager.studentUidForParent.collectAsState(initial = null)
            
            var firebaseUser by remember { mutableStateOf(auth.currentUser) }
            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener {
                    firebaseUser = it.currentUser
                }
                auth.addAuthStateListener(listener)
                onDispose { auth.removeAuthStateListener(listener) }
            }

            LaunchedEffect(userRole, studentUid, firebaseUser) {
                if (userRole != null && studentUid != null) {
                    val isEmailProvider = firebaseUser?.providerData?.any { it.providerId == "password" } == true
                    val isVerified = !isEmailProvider || firebaseUser?.displayName == "verified"

                    val isStudentLoggedIn = firebaseUser != null && userRole == "student" && isVerified
                    val isParentLoggedIn = userRole == "parent" && studentUid!!.isNotEmpty()
                    
                    if (isStudentLoggedIn || isParentLoggedIn) {
                        startMainActivity()
                    }
                }
            }

            ScrollOrStudyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ConfigurableBackground(style = BackgroundStyle.ORBS)
                        
                        if (userRole == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        } else {
                            LoginScreen()
                        }
                    }
                }
            }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val proceed = {
                        lifecycleScope.launch {
                            preferencesManager.setStudentUidForParent(pendingStudentId)
                            preferencesManager.setUserRole("parent")
                            startMainActivity()
                        }
                    }
                    if (auth.currentUser != null) {
                        proceed()
                    } else {
                        lifecycleScope.launch {
                            auth.signInAnonymously().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    proceed()
                                } else {
                                    Log.e("LoginActivity", "Anonymous sign in failed: ${task.exception?.message}")
                                    Toast.makeText(this@LoginActivity, "Firebase Anonymous Sign-In Disabled. Attempting Public Access...", Toast.LENGTH_LONG).show()
                                    proceed() // Fallback to public access
                                }
                            }
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parent Vault Access")
            .setSubtitle("Verify identity to monitor student data")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                    lifecycleScope.launch {
                        if (isNewUser && user?.email != null) {
                            EmailSender.sendWelcomeEmail(user.email!!, user.uid)
                            val finalName = user.displayName ?: "Student"
                            preferencesManager.setUserName(finalName)
                            
                            val database = FirebaseDatabase.getInstance("https://scrollorstudy-default-rtdb.asia-southeast1.firebasedatabase.app")
                            val userStatsRef = database.getReference("user_stats").child(user.uid)
                            userStatsRef.child("userName").setValue(finalName)
                            userStatsRef.child("userEmail").setValue(user.email)
                            userStatsRef.child("role").setValue("student")
                        } else {
                            user?.displayName?.let { preferencesManager.setUserName(it) }
                        }
                        preferencesManager.setUserRole("student")
                    }
                } else {
                    Toast.makeText(this, "Google login failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    enum class AuthTab { LOGIN, REGISTER, OTP, PARENT }

    @Composable
    fun LoginScreen() {
        var currentTab by remember { mutableStateOf(AuthTab.LOGIN) }
        var visible by remember { mutableStateOf(false) }
        
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var otpInput by remember { mutableStateOf("") }
        var generatedOtp by remember { mutableStateOf("") }

        var parentId by remember { mutableStateOf("") }
        var parentPassword by remember { mutableStateOf("") }
        var parentPasswordVisible by remember { mutableStateOf(false) }

        var isLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        
        LaunchedEffect(Unit) { visible = true }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1200))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AvatarsGroup()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (currentTab == AuthTab.PARENT) "Parent Access" else "Let’s get you\nsigned in!",
                            fontSize = if (currentTab == AuthTab.PARENT) 28.sp else 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 38.sp
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color(0xFF1E1E1E))
                                Spacer(modifier = Modifier.height(16.dp))
                            } else {
                                when (currentTab) {
                                    AuthTab.LOGIN -> LoginTabContent(
                                        email = email,
                                        onEmailChange = { email = it },
                                        password = password,
                                        onPasswordChange = { password = it },
                                        passwordVisible = passwordVisible,
                                        onPasswordVisibilityChange = { passwordVisible = it },
                                        onLoginClick = {
                                            if (email.isNotBlank() && password.isNotBlank()) {
                                                isLoading = true
                                                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val isEmailProvider = auth.currentUser?.providerData?.any { provider -> provider.providerId == "password" } == true
                                                        if (isEmailProvider && auth.currentUser?.displayName != "verified") {
                                                            val uid = auth.currentUser?.uid ?: ""
                                                            generatedOtp = Random.nextInt(100000, 999999).toString()
                                                            scope.launch {
                                                                EmailSender.sendOtpEmail(email, generatedOtp, uid)
                                                                isLoading = false
                                                                Toast.makeText(this@LoginActivity, "Please complete OTP verification. New code sent.", Toast.LENGTH_LONG).show()
                                                                currentTab = AuthTab.OTP
                                                            }
                                                        } else {
                                                            isLoading = false
                                                            scope.launch { preferencesManager.setUserRole("student") }
                                                        }
                                                    } else {
                                                        isLoading = false
                                                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        onRegisterClick = { currentTab = AuthTab.REGISTER },
                                        onGoogleLoginClick = {
                                            googleSignInClient.signOut().addOnCompleteListener { launcher.launch(googleSignInClient.signInIntent) }
                                        },
                                        onParentAccessClick = { currentTab = AuthTab.PARENT }
                                    )
                                    AuthTab.REGISTER -> RegisterTabContent(
                                        name = name,
                                        onNameChange = { name = it },
                                        email = email,
                                        onEmailChange = { email = it },
                                        password = password,
                                        onPasswordChange = { password = it },
                                        passwordVisible = passwordVisible,
                                        onPasswordVisibilityChange = { passwordVisible = it },
                                        onRegisterClick = {
                                            if (name.isNotBlank() && email.isNotBlank() && password.length >= 6) {
                                                isLoading = true
                                                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val uid = task.result?.user?.uid ?: ""
                                                        generatedOtp = Random.nextInt(100000, 999999).toString()
                                                        scope.launch {
                                                            val sent = EmailSender.sendOtpEmail(email, generatedOtp, uid)
                                                            isLoading = false
                                                            if (sent) {
                                                                Toast.makeText(this@LoginActivity, "OTP sent to email", Toast.LENGTH_SHORT).show()
                                                                currentTab = AuthTab.OTP
                                                            } else {
                                                                Toast.makeText(this@LoginActivity, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                                                                auth.currentUser?.delete()
                                                            }
                                                        }
                                                    } else {
                                                        isLoading = false
                                                        Toast.makeText(this@LoginActivity, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(this@LoginActivity, "Enter valid details", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onLoginClick = { currentTab = AuthTab.LOGIN }
                                    )
                                    AuthTab.OTP -> OtpTabContent(
                                        otpInput = otpInput,
                                        onOtpChange = { otpInput = it },
                                        onVerifyClick = {
                                            if (otpInput == generatedOtp) {
                                                isLoading = true
                                                val profileUpdates = userProfileChangeRequest {
                                                    displayName = "verified"
                                                }
                                                auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                                    isLoading = false
                                                    val uid = auth.currentUser?.uid ?: ""
                                                    val database = FirebaseDatabase.getInstance("https://scrollorstudy-default-rtdb.asia-southeast1.firebasedatabase.app")
                                                    val userStatsRef = database.getReference("user_stats").child(uid)
                                                    userStatsRef.child("userName").setValue(name.trim())
                                                    userStatsRef.child("userEmail").setValue(email.trim())
                                                    userStatsRef.child("role").setValue("student")

                                                    scope.launch {
                                                        preferencesManager.setUserName(name.trim())
                                                        preferencesManager.setUserRole("student")
                                                        startMainActivity()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(this@LoginActivity, "Incorrect OTP", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                    AuthTab.PARENT -> ParentTabContent(
                                        parentId = parentId,
                                        onParentIdChange = { parentId = it },
                                        parentPassword = parentPassword,
                                        onParentPasswordChange = { parentPassword = it },
                                        passwordVisible = parentPasswordVisible,
                                        onPasswordVisibilityChange = { parentPasswordVisible = it },
                                        onLoginClick = {
                                            if (parentPassword == "welcome" && parentId.isNotBlank()) {
                                                pendingStudentId = parentId.trim()
                                                biometricPrompt.authenticate(promptInfo)
                                            } else {
                                                Toast.makeText(this@LoginActivity, "Invalid login", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onBackClick = { currentTab = AuthTab.LOGIN }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LoginTabContent(email: String, onEmailChange: (String) -> Unit, password: String, onPasswordChange: (String) -> Unit, passwordVisible: Boolean, onPasswordVisibilityChange: (Boolean) -> Unit, onLoginClick: () -> Unit, onRegisterClick: () -> Unit, onGoogleLoginClick: () -> Unit, onParentAccessClick: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CustomTextField(value = email, onValueChange = onEmailChange, hint = "Email Address")
            Spacer(modifier = Modifier.height(12.dp))
            CustomPasswordField(value = password, onValueChange = onPasswordChange, hint = "Password", isVisible = passwordVisible, onVisibilityChange = onPasswordVisibilityChange)

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Forgot password?", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { /* Handle forgot password */ })
                Text("Sign Up", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onRegisterClick() })
            }

            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton("Sign In", onClick = onLoginClick)

            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFEEEEEE)))
                Text("or", modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray, fontSize = 14.sp)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFEEEEEE)))
            }
            Spacer(modifier = Modifier.height(24.dp))

            SecondaryButton("Continue with Google", onClick = onGoogleLoginClick)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Parent? Access monitoring here", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onParentAccessClick() }.padding(8.dp))
        }
    }

    @Composable
    fun RegisterTabContent(name: String, onNameChange: (String) -> Unit, email: String, onEmailChange: (String) -> Unit, password: String, onPasswordChange: (String) -> Unit, passwordVisible: Boolean, onPasswordVisibilityChange: (Boolean) -> Unit, onRegisterClick: () -> Unit, onLoginClick: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Already have an account?", color = Color.Gray, fontSize = 14.sp)
            Text("Sign In", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onLoginClick() }.padding(vertical = 4.dp))
            Spacer(modifier = Modifier.height(24.dp))

            CustomTextField(value = name, onValueChange = onNameChange, hint = "Full Name")
            Spacer(modifier = Modifier.height(12.dp))
            CustomTextField(value = email, onValueChange = onEmailChange, hint = "Email Address")
            Spacer(modifier = Modifier.height(12.dp))
            CustomPasswordField(value = password, onValueChange = onPasswordChange, hint = "Password (min 6 chars)", isVisible = passwordVisible, onVisibilityChange = onPasswordVisibilityChange)

            Spacer(modifier = Modifier.height(32.dp))
            PrimaryButton("Create Account", onClick = onRegisterClick)
        }
    }

    @Composable
    fun OtpTabContent(otpInput: String, onOtpChange: (String) -> Unit, onVerifyClick: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Enter the 6-digit OTP sent to your email", color = Color.Black, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
            CustomTextField(value = otpInput, onValueChange = onOtpChange, hint = "6-digit OTP", isNumber = true)
            Spacer(modifier = Modifier.height(32.dp))
            PrimaryButton("Verify OTP", onClick = onVerifyClick)
        }
    }

    @Composable
    fun ParentTabContent(parentId: String, onParentIdChange: (String) -> Unit, parentPassword: String, onParentPasswordChange: (String) -> Unit, passwordVisible: Boolean, onPasswordVisibilityChange: (Boolean) -> Unit, onLoginClick: () -> Unit, onBackClick: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Monitor your student's progress", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
            CustomTextField(value = parentId, onValueChange = onParentIdChange, hint = "Student ID")
            Spacer(modifier = Modifier.height(12.dp))
            CustomPasswordField(value = parentPassword, onValueChange = onParentPasswordChange, hint = "Password", isVisible = passwordVisible, onVisibilityChange = onPasswordVisibilityChange)
            Spacer(modifier = Modifier.height(32.dp))
            PrimaryButton("Login as Parent", onClick = onLoginClick)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Back to Student Login", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onBackClick() }.padding(8.dp))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomTextField(value: String, onValueChange: (String) -> Unit, hint: String, isNumber: Boolean = false) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint, color = Color.Gray, fontSize = 15.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5)
            )
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomPasswordField(value: String, onValueChange: (String) -> Unit, hint: String, isVisible: Boolean, onVisibilityChange: (Boolean) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint, color = Color.Gray, fontSize = 15.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Text(
                    text = if (isVisible) "Hide" else "Show",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(end = 16.dp).clickable { onVisibilityChange(!isVisible) }
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5)
            )
        )
    }

    @Composable
    fun PrimaryButton(text: String, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    fun SecondaryButton(text: String, onClick: () -> Unit) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Text(text, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }

    @Composable
    fun AvatarsGroup() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            AvatarPlaceholder(
                color = Color(0xFFFFCCB6),
                modifier = Modifier.offset(x = (-45).dp, y = (-20).dp).size(70.dp),
                emoji = "🎒"
            )
            AvatarPlaceholder(
                color = Color(0xFFB5EAEA),
                modifier = Modifier.offset(x = 45.dp, y = (-20).dp).size(70.dp),
                emoji = "📚"
            )
            AvatarPlaceholder(
                color = Color(0xFFC1E1C1),
                modifier = Modifier.offset(y = 35.dp).size(70.dp),
                emoji = "🎓"
            )
        }
    }

    @Composable
    fun AvatarPlaceholder(color: Color, modifier: Modifier = Modifier, emoji: String) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Color.Black.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 28.sp)
        }
    }
}
