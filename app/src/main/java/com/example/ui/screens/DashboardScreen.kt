package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AppEntity
import com.example.data.entity.UserEntity
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.reactiveUser.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App bar
        TopAppBar(
            title = { 
                Text(
                    text = if (user?.role == "ADMIN") "لوحة تحكم المدير العام" else "استوديو منشئي المحتوى",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ) 
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateTo("HOME") }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        val currentUser = user
        if (currentUser == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("يرجى تسجيل الدخول للوصول إلى هذه اللوحة.")
            }
            return
        }

        if (currentUser.role == "ADMIN") {
            AdminDashboard(viewModel = viewModel)
        } else if (currentUser.role == "DEVELOPER") {
            DeveloperDashboard(viewModel = viewModel, user = currentUser)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("عذراً، هذا القسم مخصص للمطورين والمدراء فقط.")
            }
        }
    }
}

@Composable
fun DeveloperDashboard(viewModel: StoreViewModel, user: UserEntity) {
    val context = LocalContext.current
    val devApps by viewModel.developerApps.collectAsState()

    // Form inputs for uploading new app
    var showUploadForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("com.epic.") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ألغاز") }
    var type by remember { mutableStateOf("GAME") } // GAME or APP
    var price by remember { mutableStateOf("0.0") }
    var size by remember { mutableStateOf("45 MB") }
    var version by remember { mutableStateOf("1.0.0") }
    var apkUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var iconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var screenshotUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var videoUrl by remember { mutableStateOf("") }
    var bannerColor by remember { mutableStateOf("#FF6200EE") }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        apkUri = uri
    }

    val iconPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        iconUri = uri
    }

    val screenshotPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris: List<android.net.Uri> ->
        screenshotUris = uris
    }

    val categoriesList = listOf("مغامرات", "سباق", "أكشن", "ألغاز", "الصحة والرشاقة", "المالية", "الإنتاجية", "التواصل الاجتماعي", "فوتوغرافيا")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dev Info & Simulated Balance (Earnings)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "أهلاً بك، ${user.username}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "اسم المطور: ${user.developerName}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "أرباح مبيعات التطبيقات:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${user.balance} $",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Add New App Button
        item {
            Button(
                onClick = { showUploadForm = !showUploadForm },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("show_upload_form_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (showUploadForm) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showUploadForm) "إغلاق النموذج" else "نشر لعبة أو تطبيق جديد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // App Upload Form
        if (showUploadForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تفاصيل التطبيق الجديد (نشر مباشر وفوري في المتجر)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("اسم التطبيق / اللعبة") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_title_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it },
                            label = { Text("اسم الحزمة البرمجية (Package Name)") },
                            placeholder = { Text("مثال: com.epic.mygame") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_package_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("الوصف الكامل") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("app_description_input")
                        )

                        // Selector for Category and Type
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var categoryExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { categoryExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("الفئة: $category")
                                }
                                DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                    categoriesList.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                category = cat
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(onClick = { type = "GAME" }) {
                                    Text("لعبة", fontWeight = if (type == "GAME") FontWeight.Bold else FontWeight.Normal)
                                }
                                Divider(modifier = Modifier.fillMaxHeight().width(1.dp).padding(vertical = 8.dp))
                                TextButton(onClick = { type = "APP" }) {
                                    Text("تطبيق", fontWeight = if (type == "APP") FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        // Price, Size, Version
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("السعر ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = size,
                                onValueChange = { size = it },
                                label = { Text("الحجم") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = version,
                                onValueChange = { version = it },
                                label = { Text("الإصدار") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("application/vnd.android.package-archive") },
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) {
                                Text(if (apkUri != null) "تم اختيار APK" else "اختر ملف APK")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { iconPickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) {
                                Text(if (iconUri != null) "تم اختيار الأيقونة" else "اختر أيقونة التطبيق")
                            }
                            OutlinedButton(
                                onClick = { screenshotPickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) {
                                Text(if (screenshotUris.isNotEmpty()) "تم اختيار ${screenshotUris.size} صور" else "اختر لقطات الشاشة")
                            }
                        }

                        OutlinedTextField(
                            value = videoUrl,
                            onValueChange = { videoUrl = it },
                            label = { Text("رابط يوتيوب ترويجي (اختياري)") },
                            placeholder = { Text("https://www.youtube.com/...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Color picker simple simulation
                        Text("اختر لون المظهر الجمالي:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        val colors = listOf("#FF107C41", "#FFE81123", "#FF0078D7", "#FFB900F7", "#FF6200EE", "#FFEC008C", "#FF002050")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(colors) { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { bannerColor = hex }
                                        .padding(4.dp)
                                ) {
                                    if (bannerColor == hex) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit with Verification Checklist info
                        Button(
                            onClick = {
                                val priceVal = price.toDoubleOrNull() ?: 0.0
                                viewModel.uploadApp(
                                    title = title,
                                    packageName = packageName,
                                    description = description,
                                    category = category,
                                    type = type,
                                    price = priceVal,
                                    size = size,
                                    version = version,
                                    apkUri = apkUri,
                                    bannerColor = bannerColor,
                                    iconUri = iconUri,
                                    screenshotUris = screenshotUris,
                                    videoUrl = videoUrl,
                                    onSuccess = {
                                        Toast.makeText(context, "تم نشر اللعبة/التطبيق مباشرة وبنجاح في المتجر!", Toast.LENGTH_LONG).show()
                                        showUploadForm = false
                                        // Reset
                                        title = ""
                                        packageName = "com.epic."
                                        description = ""
                                        price = "0.0"
                                        size = "45 MB"
                                        version = "1.0.0"
                                        apkUri = null
                                        iconUri = null
                                        screenshotUris = emptyList()
                                        videoUrl = ""
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_app_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("نشر مباشر في المتجر", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live apps header
        item {
            Text(
                text = "تطبيقاتك المنشورة",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (devApps.isEmpty()) {
            item {
                Text(
                    text = "لم تقم برفع أي تطبيقات بعد. اضغط على الزر أعلاه لنشر تطبيقك الأول!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(devApps) { app ->
                DevAppItemRow(app = app, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DevAppItemRow(app: AppEntity, viewModel: StoreViewModel) {
    val context = LocalContext.current
    var showUpdateSection by remember { mutableStateOf(false) }
    var updateVerInput by remember { mutableStateOf(app.version) }
    var updateDescInput by remember { mutableStateOf(app.description) }
    var updateApkUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var updateIconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var updateScreenshotUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    val updateApkPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        updateApkUri = uri
    }

    val updateIconPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        updateIconUri = uri
    }

    val updateScreenshotPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris: List<android.net.Uri> ->
        updateScreenshotUris = uris
    }

    val colorHex = app.bannerColor
    val parsedColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

    val appReviews by viewModel.getReviewsForApp(app.id).collectAsState(initial = emptyList())
    var showReviewsSection by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(parsedColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!app.iconUri.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = app.iconUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(app.title.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(app.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Live status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "منشور ومباشر",
                        color = Color(0xFF2E7D32),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الإصدار: ${app.version} | التنزيلات: ${app.downloadsCount}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    TextButton(onClick = { showReviewsSection = !showReviewsSection }) {
                        Text(
                            text = if (showReviewsSection) "إغلاق التقييمات" else "التقييمات (${appReviews.size}) ⭐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showUpdateSection = !showUpdateSection }) {
                        Text(
                            text = if (showUpdateSection) "إلغاء التحديث" else "إصدار تحديث",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // User Reviews & Feedback Dashboard Section
            AnimatedVisibility(visible = showReviewsSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ملاحظات وتعليقات المستخدمين:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${app.rating} ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Text(" (${app.ratingsCount})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Divider()

                    if (appReviews.isEmpty()) {
                        Text(
                            text = "لا توجد تقييمات أو تعليقات من المستخدمين حتى الآن.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            appReviews.forEach { review ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(review.username, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                (1..5).forEach { index ->
                                                    Icon(
                                                        imageVector = if (review.rating >= index) Icons.Default.Star else Icons.Outlined.Star,
                                                        contentDescription = null,
                                                        tint = if (review.rating >= index) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (review.comment.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(review.comment, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showUpdateSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider()
                    Text("أدخل تفاصيل التحديث وارفع البيانات الجديدة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    
                    OutlinedTextField(
                        value = updateVerInput,
                        onValueChange = { updateVerInput = it },
                        label = { Text("رقم الإصدار الجديد") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = updateDescInput,
                        onValueChange = { updateDescInput = it },
                        label = { Text("ما الجديد في هذا الإصدار") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = { updateApkPickerLauncher.launch("application/vnd.android.package-archive") },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (updateApkUri != null) "تم اختيار ملف APK الجديد" else "اختر ملف APK الجديد للتحديث (مطلوب)")
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { updateIconPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text(if (updateIconUri != null) "تم اختيار الأيقونة" else "تحديث الأيقونة (اختياري)", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { updateScreenshotPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text(if (updateScreenshotUris.isNotEmpty()) "تم اختيار ${updateScreenshotUris.size} صور" else "تحديث اللقطات (اختياري)", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.updateLiveApp(
                                appId = app.id,
                                newVersion = updateVerInput,
                                newDescription = updateDescInput,
                                newApkUri = updateApkUri,
                                newIconUri = updateIconUri,
                                newScreenshotUris = updateScreenshotUris,
                                onSuccess = {
                                    showUpdateSection = false
                                    updateApkUri = null
                                    updateIconUri = null
                                    updateScreenshotUris = emptyList()
                                    Toast.makeText(context, "تم نشر التحديث المباشر وتحديث الصور والأيقونة بنجاح!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("رفع APK ونشر التحديث فوراً")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboard(viewModel: StoreViewModel) {
    val unverified by viewModel.unverifiedApps.collectAsState()
    val usersList by viewModel.allUsers.collectAsState()

    var activeAdminTab by remember { mutableStateOf("PENDING") } // PENDING or USERS

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle tabs
        TabRow(
            selectedTabIndex = if (activeAdminTab == "PENDING") 0 else 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeAdminTab == "PENDING",
                onClick = { activeAdminTab = "PENDING" },
                text = { Text("المراجعة البرمجية للملفات (${unverified.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeAdminTab == "USERS",
                onClick = { activeAdminTab = "USERS" },
                text = { Text("إدارة المستخدمين", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeAdminTab == "PENDING") {
            if (unverified.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("رائع! لا توجد تطبيقات معلقة بانتظار المراجعة حالياً.", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(unverified) { app ->
                        AdminAppReviewCard(app = app, viewModel = viewModel)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(usersList) { user ->
                    AdminUserCard(user = user, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AdminAppReviewCard(app: AppEntity, viewModel: StoreViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "طلب مراجعة من المطور: ${app.developerName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = app.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "اسم الحزمة: ${app.packageName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "ملف البرمجي المرفوع: ${app.apkFileName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "السعر المقترح: ${app.price} $ | الحجم: ${app.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "الوصف المقدم: ${app.description}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.approveApp(app.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("موافقة ونشر")
                }

                Button(
                    onClick = { viewModel.rejectApp(app.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("رفض وحذف")
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(user: UserEntity, viewModel: StoreViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = user.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "البريد الإلكتروني: ${user.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "الدور: " + when(user.role) {
                        "ADMIN" -> "مدير عام"
                        "DEVELOPER" -> "مطور تطبيقات (${user.developerName})"
                        else -> "مستخدم"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (user.role != "ADMIN") {
                IconButton(
                    onClick = { viewModel.deleteUser(user) },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المستخدم")
                }
            }
        }
    }
}
