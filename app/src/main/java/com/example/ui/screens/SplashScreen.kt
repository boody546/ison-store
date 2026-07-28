package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    // Elegant entrance animation states
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(alphaAnim)
                .scale(scaleAnim)
        ) {
            // Logo Icon Container with dynamic glow & elevation appearance
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "ISON Store Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(72.dp)
                            .offset(x = 4.dp) // Shift slightly to perfectly center the play triangle
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Application Brand Name
            Text(
                text = "ISON Store",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "بوابتك لكل التطبيقات والألعاب",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Modern indicator with white track
            CircularProgressIndicator(
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeWidth = 4.dp,
                modifier = Modifier.size(36.dp)
            )
        }

        // Footer Branding
        Text(
            text = "منصة ISON • حقيقي وآمن بالكامل",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )
    }
}
