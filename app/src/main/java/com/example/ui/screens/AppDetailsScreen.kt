package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AppEntity
import com.example.data.entity.ReviewEntity
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    viewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app by viewModel.selectedApp.collectAsState()
    val reviews by viewModel.currentAppReviews.collectAsState()
    val downloads by viewModel.userDownloads.collectAsState()
    val user by viewModel.reactiveUser.collectAsState()

    val currentApp = app ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val downloadState = downloads.find { it.appId == currentApp.id }
    val colorHex = currentApp.bannerColor
    val parsedColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

    // State for leaving a review
    var ratingInput by remember { mutableStateOf(5f) }
    var commentInput by remember { mutableStateOf("") }
    var isReviewFormVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. App Header/Banner (Google Play style)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(parsedColor, parsedColor.copy(alpha = 0.5f))
                        )
                    )
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("HOME") },
                    modifier = Modifier
                        .padding(top = 40.dp, start = 16.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = currentApp.category,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Main Title, Icon, and Package Info
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon Large
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    parsedColor,
                                    parsedColor.copy(alpha = 0.75f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentApp.iconUri != null) {
                        coil.compose.AsyncImage(
                            model = currentApp.iconUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = currentApp.title.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 38.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentApp.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Text(
                        text = currentApp.developerName,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = currentApp.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 3. Stats Section (Rating, Downloads, Size)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currentApp.rating}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${currentApp.ratingsCount} تقييم",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Size Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentApp.size,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "الحجم",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Downloads Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentApp.downloadsCount > 1000) "${currentApp.downloadsCount / 1000}K+" else "${currentApp.downloadsCount}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "تنزيل",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Download / Purchase Button with Direct Real DownloadManager execution
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (downloadState != null && downloadState.isInstalled) {
                    // Installed State - Option to Open, Re-download APK, or Uninstall
                    Button(
                        onClick = {
                            Toast.makeText(context, "بدأ التنزيل الآن...", Toast.LENGTH_SHORT).show()
                            viewModel.purchaseAndDownload(currentApp) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parsedColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("download_apk_again_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تنزيل ملف APK الجديد وتثبيته", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "تم فتح التطبيق ${currentApp.title}!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_app_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فتح التطبيق", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                viewModel.uninstallApp(currentApp) {
                                    Toast.makeText(context, "تم إزالة التطبيق من القائمة المثبتة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إلغاء التثبيت", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    // Buy or Download Button
                    val isPremium = currentApp.price > 0.0
                    Button(
                        onClick = {
                            Toast.makeText(context, "بدأ التنزيل الآن...", Toast.LENGTH_SHORT).show()
                            viewModel.purchaseAndDownload(currentApp) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("install_app_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPremium) "شراء وتنزيل APK (${currentApp.price} $)" else "تنزيل وتثبيت APK مجاناً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // 4.5 Screenshots / Previews Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "لقطات الشاشة والمعاينة",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )

                val screenshotsList = if (currentApp.screenshotUris.isNotBlank()) currentApp.screenshotUris.split(",").filter { it.isNotBlank() } else emptyList()

                if (screenshotsList.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(screenshotsList) { fileName ->
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(280.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                coil.compose.AsyncImage(
                                    model = fileName,
                                    contentDescription = "لقطة شاشة",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم يتم رفع لقطات شاشة لهذا التطبيق بعد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }

                if (currentApp.videoUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(currentApp.videoUrl))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لا يمكن فتح الرابط", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاهدة الفيديو الترويجي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 5. App Description
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "حول هذا التطبيق",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = currentApp.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الإصدار الحالي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentApp.version, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الملف البرمجي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (currentApp.apkFileName.isNotBlank()) currentApp.apkFileName else "لا يوجد",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. Verification Status (Programmatic Trust Checklist)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "تطبيق موثوق ومباشر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "تم التحقق المباشر ونشر التطبيق فوراً وبنجاح في المتجر.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 7. Feedback & Ratings Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التقييمات والآراء",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (user != null) {
                        TextButton(
                            onClick = { isReviewFormVisible = !isReviewFormVisible },
                            modifier = Modifier.testTag("write_review_button")
                        ) {
                            Text(
                                text = if (isReviewFormVisible) "إلغاء التقييم" else "كتابة مراجعة",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "سجل دخولك لكتابة تقييم",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Leave Review Form
                AnimatedVisibility(visible = isReviewFormVisible) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ما هو تقييمك للتطبيق؟",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom Interactive Star Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                (1..5).forEach { index ->
                                    val isStarSelected = ratingInput >= index
                                    Icon(
                                        imageVector = if (isStarSelected) Icons.Default.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = if (isStarSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { ratingInput = index.toFloat() }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                placeholder = { Text("اكتب رأيك بصراحة عن التطبيق هنا...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("review_comment_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (commentInput.isBlank()) {
                                        Toast.makeText(context, "الرجاء كتابة تعليق أولاً", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.postReview(currentApp.id, ratingInput, commentInput) {
                                        commentInput = ""
                                        isReviewFormVisible = false
                                        Toast.makeText(context, "تم نشر مراجعتك بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("submit_review_button")
                            ) {
                                Text("نشر التقييم")
                            }
                        }
                    }
                }
            }
        }

        // Reviews List
        if (reviews.isEmpty()) {
            item {
                Text(
                    text = "لا توجد مراجعات بعد لهذا التطبيق. كن أول من يقيمه!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(reviews) { review ->
                ReviewItemRow(review = review)
            }
        }
    }
}

@Composable
fun ReviewItemRow(review: ReviewEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.username.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = review.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..5).forEach { starIndex ->
                        val isStarFilled = review.rating >= starIndex
                        Icon(
                            imageVector = if (isStarFilled) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (isStarFilled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.comment,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}
