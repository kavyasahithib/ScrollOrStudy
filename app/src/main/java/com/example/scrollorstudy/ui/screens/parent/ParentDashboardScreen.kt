package com.example.scrollorstudy.ui.screens.parent

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scrollorstudy.ui.components.RadarChart
import com.example.scrollorstudy.ui.components.RadarData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    onProfileClick: () -> Unit,
    viewModel: ParentDashboardViewModel = viewModel(factory = ParentDashboardViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        com.example.scrollorstudy.ui.components.LegendaryParentBackground()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Parent Monitoring", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = onProfileClick) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Text(text = "Currently Monitoring", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 16.dp))
            Text(text = uiState.studentName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(24.dp)) {
                    StatRowPro(icon = Icons.Default.Edit, label = "Study Time", seconds = uiState.studentStudyTime, color = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(20.dp))
                    StatRowPro(icon = Icons.Default.PlayArrow, label = "Scroll Time", seconds = uiState.studentScrollTime, color = Color(0xFFF44336))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "${uiState.studentStreak} Day Streak", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔥 Holistic Analytics Web", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val studyMax = 7200f // Dynamically scaled to 2 hours for massive visible spikes!
                    val scrollPenaltyThreshold = 10800f // 3 hours curve to allow wider range of visual change 
                    val efficiency = if (uiState.studentStudyTime + uiState.studentScrollTime > 0) {
                        (uiState.studentStudyTime.toFloat() / (uiState.studentStudyTime + uiState.studentScrollTime)) * 100f
                    } else 0f
                    
                    val radarData = listOf(
                        RadarData("Study Volume", uiState.studentStudyTime.toFloat(), studyMax),
                        RadarData("Consistency", uiState.studentStreak.toFloat(), 30f),
                        RadarData("Constraint Bias", maxOf(0f, scrollPenaltyThreshold - uiState.studentScrollTime), scrollPenaltyThreshold),
                        RadarData("Focus Yield", efficiency, 100f)
                    )
                    
                    RadarChart(
                        data = radarData,
                        modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 8.dp),
                        polyColor = Color(0xFF00C853),
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Live monitoring active. Data updates in real-time.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), textAlign = TextAlign.Center)
        }
    }
    }
}

@Composable
private fun StatRowPro(icon: ImageVector, label: String, seconds: Long, color: Color) {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    val timeString = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${secs}s"
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(text = timeString, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
