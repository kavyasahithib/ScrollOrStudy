package com.example.scrollorstudy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.scrollorstudy.data.local.PreferencesManager
import com.example.scrollorstudy.ui.screens.dashboard.DashboardScreen
import com.example.scrollorstudy.ui.screens.parent.ParentDashboardScreen
import com.example.scrollorstudy.ui.screens.profile.ProfileScreen
import com.example.scrollorstudy.ui.theme.ScrollOrStudyTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val preferencesManager: PreferencesManager by lazy { (application as ScrollOrStudyApplication).container.preferencesManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestNotificationPermission()
        checkPermissions()
        
        setContent {
            val isDarkMode by preferencesManager.isDarkMode.collectAsState(initial = false)
            val currentUserRole by preferencesManager.userRole.collectAsState(initial = null)
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            
            LaunchedEffect(firebaseUser, currentUserRole) {
                if (currentUserRole != null && firebaseUser == null && currentUserRole != "parent") {
                    navigateToLogin()
                }
            }

            ScrollOrStudyTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUserRole == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "dashboard") {
                            composable("dashboard") {
                                if (currentUserRole == "parent") {
                                    ParentDashboardScreen(
                                        onProfileClick = { navController.navigate("profile") }
                                    )
                                } else {
                                    DashboardScreen(
                                        onProfileClick = { navController.navigate("profile") }
                                    )
                                }
                            }
                            composable("profile") {
                                ProfileScreen(
                                    onBack = { navController.popBackStack() },
                                    onLogout = { logout() },
                                    onDeleteAccount = { deleteAccount() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            preferencesManager.setUserRole("student")
            preferencesManager.setStudentUidForParent("")
            FirebaseAuth.getInstance().signOut()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                logout()
            } else {
                Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val stats = usageStatsManager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - 1000 * 10, System.currentTimeMillis())
            if (stats == null || stats.isEmpty()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        } catch (e: Exception) {
            Log.e("PERMISSION", "Error checking usage stats permission", e)
        }
    }
}
