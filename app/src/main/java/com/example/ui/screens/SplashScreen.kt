package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onStartApp: () -> Unit
) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF064E3B),
                        Color(0xFF047857),
                        Color(0xFF0F766E),
                        Color(0xFF134E4A)
                    )
                )
            )
    ) {
        // Decorative background illustration with elegant soft opacity
        Image(
            painter = painterResource(id = R.drawable.img_splash_bg_1789151418394),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.22f)
        )

        // Main Splash Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Government / Health Dept Label
            Row(
                modifier = Modifier
                    .alpha(alpha.value)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MintAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ระบบฐานข้อมูล อสม. ดิจิทัล",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Center Logo & Title Block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
            ) {
                // High-End 3D App Logo
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(24.dp, RoundedCornerShape(36.dp), spotColor = Color(0xFF022C22))
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo_1789151402503),
                        contentDescription = "Smart OSM Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Smart OSM",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ระบบบริหารจัดการข้อมูลประชากรและครัวเรือน",
                    style = MaterialTheme.typography.titleSmall,
                    color = MintAccent,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "รพ.สต.บ้านกร่างประดู่วัง • ม.8 ต.ป่าขะ อ.บ้านนา จ.นครนายก",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            // Bottom CTA Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onStartApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintAccent,
                        contentColor = Color(0xFF064E3B)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFF022C22))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "เข้าสู่ระบบสำรวจ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "พัฒนาโดย วัชรพงษ์ พวงแจ่ม (อสม.) • เวอร์ชัน 1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
