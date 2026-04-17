package com.example.scrollorstudy.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onProfileClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentDate = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())

    Box(modifier = Modifier.fillMaxSize()) {
        com.example.scrollorstudy.ui.components.ConfigurableBackground(style = com.example.scrollorstudy.ui.components.BackgroundStyle.MESH)
        
        // Render 3D Focus Orb behind the cards!
        com.example.scrollorstudy.ui.components.FocusOrb3D(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Scroll or Study", 
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                currentDate, 
                                fontSize = 12.sp, 
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = onProfileClick) {
                            Icon(
                                Icons.Default.AccountCircle, 
                                contentDescription = "Profile", 
                                modifier = Modifier.size(32.dp), 
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // WELCOME MESSAGE
                val displayName = uiState.userName.ifEmpty { "Student" }
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = "👋 Welcome, $displayName",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // AI INSIGHT CARD (Combined)
                DashboardGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info, 
                                contentDescription = null, 
                                tint = Color(0xFF5B8CFF), 
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Insight", 
                                fontSize = 15.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = uiState.aiMessage, 
                            fontSize = 14.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Recommendation section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.Star, 
                                    contentDescription = null, 
                                    tint = Color(0xFF00E5FF), 
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.aiCoachMessage, 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // STATS ROW
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Study Time
                    DashboardGlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Edit, 
                                    contentDescription = null, 
                                    tint = Color(0xFF2DD4BF), 
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Study", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Medium, 
                                    color = Color(0xFF2DD4BF)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val studyMins = uiState.studyTimeToday / 60
                            val studyHrs = studyMins / 60
                            val formatStudy = if (studyHrs > 0) "${studyHrs}h ${studyMins % 60}m" else "${studyMins}m"
                            Text(
                                text = formatStudy, 
                                fontSize = 22.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White
                            )
                        }
                    }
                    
                    // Scroll Time
                    DashboardGlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning, 
                                    contentDescription = null, 
                                    tint = Color(0xFFF43F5E), 
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scroll", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Medium, 
                                    color = Color(0xFFF43F5E)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val scrollMins = uiState.scrollTimeToday / 60
                            val scrollHrs = scrollMins / 60
                            val formatScroll = if (scrollHrs > 0) "${scrollHrs}h ${scrollMins % 60}m" else "${scrollMins}m"
                            Text(
                                text = formatScroll, 
                                fontSize = 22.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // STREAK CARD
                DashboardGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${uiState.currentStreak} Day Streak", 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // PRIMARY ACTION BUTTON
                PremiumActionButton(
                    text = if (uiState.isStudyModeActive) "Stop Study Session" else "Start Study Session",
                    isActive = uiState.isStudyModeActive,
                    onClick = { viewModel.toggleStudyMode() }
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // SETTINGS SECTION (Hardcore Mode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hardcore Mode", 
                            fontSize = 15.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color.White
                        )
                        Text(
                            text = "Lock phone if you scroll too much", 
                            fontSize = 11.sp, 
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = uiState.isHardcoreModeActive,
                        onCheckedChange = { viewModel.toggleHardcoreMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DashboardGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        content()
    }
}

@Composable
fun PremiumActionButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFFF43F5E) else Color(0xFF5B8CFF)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
