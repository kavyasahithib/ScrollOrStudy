package com.example.scrollorstudy.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scrollorstudy.ui.components.ConfigurableBackground
import com.example.scrollorstudy.ui.components.BackgroundStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDeleteAccount() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ConfigurableBackground(style = BackgroundStyle.ORBS)
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (uiState.userRole == "parent") "Monitoring Profile" else "Student Identity", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Avatar Badge
                Box(
                    modifier = Modifier.size(110.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.2f), CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape), 
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.userName.isNotBlank()) uiState.userName.first().uppercase() else "S", 
                        fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = uiState.userName, 
                    onValueChange = { viewModel.updateName(it) }, 
                    label = { Text(if (uiState.userRole == "parent") "Student Name" else "Full Name") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    readOnly = uiState.userRole == "parent",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, 
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.userEmail, 
                    onValueChange = {}, 
                    label = { Text("Registered Email") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    readOnly = true, 
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }, 
                    shape = RoundedCornerShape(16.dp)
                )

                // Stats Banner
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "Consistency", fontSize = 14.sp, color = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${uiState.streak} Day Streak \uD83D\uDD25", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                
                if (uiState.userRole == "student" && uiState.userId.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Parent Hub Connectivity", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.userId, 
                        onValueChange = {}, 
                        readOnly = true, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(16.dp), 
                        trailingIcon = { 
                            IconButton(onClick = { 
                                clipboardManager.setText(AnnotatedString(uiState.userId))
                                Toast.makeText(context, "Parent ID Copied!", Toast.LENGTH_SHORT).show() 
                            }) { Icon(Icons.Default.Share, contentDescription = "Copy") } 
                        }
                    )
                    Text(text = "Forward this ID to your guardian to grant dashboard tracking access.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Environment Dark Mode", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Switch(checked = uiState.isDarkMode, onCheckedChange = { viewModel.toggleDarkMode(it) })
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = onLogout, 
                    modifier = Modifier.fillMaxWidth().height(56.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer), 
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Securely Sign Out", fontWeight = FontWeight.Bold)
                }
                
                if (uiState.userRole == "student") {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account Permanently")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
