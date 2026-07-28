package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.example.data.entity.DownloadEntity
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.reactiveUser.collectAsState()
    val downloads by viewModel.userDownloads.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App bar
        TopAppBar(
            title = { Text("الملف الشخصي والحساب", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateTo("HOME") }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        )

        val currentUser = user
        if (currentUser == null) {
            AuthTabSection(viewModel = viewModel)
        } else {
            LoggedInUserProfile(viewModel = viewModel, user = currentUser, downloads = downloads)
        }
    }
}

@Composable
fun LoggedInUserProfile(
    viewModel: StoreViewModel,
    user: com.example.data.entity.UserEntity,
    downloads: List<DownloadEntity>
) {
    val context = LocalContext.current
    var refillAmountInput by remember { mutableStateOf("") }
    var isWalletExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Header Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.username.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = user.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = user.email,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (user.role) {
                                    "ADMIN" -> "مدير عام النظام"
                                    "DEVELOPER" -> "حساب مطور: ${user.developerName}"
                                    else -> "حساب مستخدم عادي"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 2. Simulated E-wallet Section (Wallet refilling)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("المحفظة الإلكترونية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "${user.balance} $",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { isWalletExpanded = !isWalletExpanded },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = if (isWalletExpanded) "إغلاق الشحن السريع" else "شحن الرصيد بالبطاقة الإلكترونية")
                    }

                    if (isWalletExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("اختر أو اكتب مبلغ الشحن:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            
                            // Amount chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("10", "20", "50", "100").forEach { amount ->
                                    ElevatedButton(
                                        onClick = { refillAmountInput = amount }
                                    ) {
                                        Text("$amount $")
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = refillAmountInput,
                                onValueChange = { refillAmountInput = it },
                                label = { Text("مبلغ الشحن المخصص ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = { Icon(imageVector = Icons.Default.AddCircle, contentDescription = null) }
                            )

                            Button(
                                onClick = {
                                    val deposit = refillAmountInput.toDoubleOrNull()
                                    if (deposit == null || deposit <= 0) {
                                        Toast.makeText(context, "الرجاء إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.addBalance(deposit)
                                    refillAmountInput = ""
                                    isWalletExpanded = false
                                    Toast.makeText(context, "تم شحن رصيد المحفظة بمبلغ $deposit $ بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تأكيد الدفع الإلكتروني الآمن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. User Dashboard Action button
        if (user.role == "DEVELOPER" || user.role == "ADMIN") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo("DEV_DASHBOARD") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (user.role == "ADMIN") "فتح لوحة تحكم الإدارة" else "فتح استوديو المطورين الخاص بك",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (user.role == "ADMIN") "مراجعة واعتماد التطبيقات وإدارة الحسابات." else "ارفع تطبيقات وألعاب جديدة، وأصدر تحديثات حية لمستخدميك.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Download / Purchased apps library
        item {
            Text(
                text = "مكتبتك وتنزيلاتك",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val installedDownloads = downloads.filter { it.isInstalled }
        if (installedDownloads.isEmpty()) {
            item {
                Text(
                    text = "لم تقم بتثبيت أي تطبيقات أو ألعاب بعد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(installedDownloads) { download ->
                InstalledAppRow(download = download, viewModel = viewModel)
            }
        }

        // 5. Logout Button
        item {
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج من الحساب", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InstalledAppRow(download: DownloadEntity, viewModel: StoreViewModel) {
    val allApps by viewModel.filteredApps.collectAsState()
    val matchingApp = allApps.find { it.id == download.appId } ?: return

    val colorHex = matchingApp.bannerColor
    val parsedColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectApp(matchingApp.id) },
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(parsedColor),
                contentAlignment = Alignment.Center
            ) {
                Text(matchingApp.title.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(matchingApp.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("تاريخ التثبيت: ${formatDate(download.installedAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F5E9))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("مثبت", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AuthTabSection(viewModel: StoreViewModel) {
    var isLoginTab by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "مرحباً بك في سوق متجر بلاي",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "قم بالدخول لتتمكن من تحميل التطبيقات، التقييم، وشحن المحفظة.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tab selection row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { isLoginTab = true },
                modifier = Modifier
                    .weight(1f)
                    .background(if (isLoginTab) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .testTag("login_tab_button")
            ) {
                Text("تسجيل الدخول", fontWeight = FontWeight.Bold, color = if (isLoginTab) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }

            TextButton(
                onClick = { isLoginTab = false },
                modifier = Modifier
                    .weight(1f)
                    .background(if (!isLoginTab) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .testTag("register_tab_button")
            ) {
                Text("إنشاء حساب جديد", fontWeight = FontWeight.Bold, color = if (!isLoginTab) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoginTab) {
            LoginForm(viewModel = viewModel)
        } else {
            RegisterForm(viewModel = viewModel)
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoogleBrandIcon(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "المتابعة باستخدام Google",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun GoogleBrandIcon(modifier: Modifier = Modifier.size(22.dp)) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.width * 0.18f
            // Red arc (top)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 220f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            // Yellow arc (left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 130f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            // Green arc (bottom)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 40f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            // Blue arc (right)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -50f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }
        Text(
            text = "G",
            color = Color(0xFF4285F4),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
    }
}

@Composable
fun rememberGoogleSignInLauncher(
    context: android.content.Context,
    viewModel: StoreViewModel
): androidx.activity.result.ActivityResultLauncher<android.content.Intent> {
    return androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account?.idToken
                val email = account?.email ?: ""
                val displayName = account?.displayName ?: account?.givenName ?: email.substringBefore("@")

                if (!idToken.isNullOrBlank() || email.isNotBlank()) {
                    viewModel.loginWithGoogle(
                        idToken = idToken,
                        googleEmail = email,
                        googleDisplayName = displayName,
                        onSuccess = { u ->
                            android.widget.Toast.makeText(context, "تم تسجيل الدخول بنجاح كـ ${u.username}", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.navigateTo("HOME")
                        },
                        onError = { err ->
                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    context,
                    "خطأ تسجيل الدخول بـ Google (${e.statusCode}): ${e.localizedMessage ?: "إلغاء أو فشل الاتصال"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "فشل تسجيل الدخول بواسطة Google: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            android.widget.Toast.makeText(context, "تم إلغاء تسجيل الدخول", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

fun launchLegacyGoogleSignIn(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
) {
    android.util.Log.d("AUTH_DEBUG", "Launching legacy GoogleSignInClient fallback")
    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
    )
        .requestIdToken("423299705791-06m9b1stog47pel10dj49hikfinf4n3k.apps.googleusercontent.com")
        .requestEmail()
        .build()

    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    googleSignInClient.signOut().addOnCompleteListener {
        android.util.Log.d("AUTH_DEBUG", "GoogleSignInClient signed out, launching intent")
        launcher.launch(googleSignInClient.signInIntent)
    }
}

@Composable
fun LoginForm(viewModel: StoreViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val errorState by viewModel.loginError.collectAsState()
    val legacyLauncher = rememberGoogleSignInLauncher(context = context, viewModel = viewModel)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Google Sign-In Option
        GoogleSignInButton(
            onClick = {
                triggerGoogleSignInFlow(
                    context = context,
                    coroutineScope = coroutineScope,
                    viewModel = viewModel,
                    legacyLauncher = legacyLauncher
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = " أو باستخدام البريد ",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني") },
            placeholder = { Text("مثال: user@play.com") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة المرور") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input"),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        val currentErr = errorState
        if (currentErr != null) {
            Text(
                text = currentErr,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "الرجاء تعبئة جميع الحقول", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.login(
                    email = email,
                    passwordText = password,
                    onSuccess = {
                        Toast.makeText(context, "تم تسجيل الدخول بنجاح!", Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo("HOME")
                        email = ""
                        password = ""
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_login_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("دخول", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Help hint
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("حسابات تجريبية سريعة ومحملة مسبقاً:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("• مستخدم عادي: user@play.com | كلمة المرور: user", fontSize = 11.sp)
                Text("• مطور ألعاب وتطبيقات: dev@play.com | كلمة المرور: dev", fontSize = 11.sp)
                Text("• مدير عام النظام: admin@play.com | كلمة المرور: admin", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun RegisterForm(viewModel: StoreViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("USER") } // USER or DEVELOPER
    var developerName by remember { mutableStateOf("") }

    val legacyLauncher = rememberGoogleSignInLauncher(context = context, viewModel = viewModel)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Google Sign-In Option
        GoogleSignInButton(
            onClick = {
                triggerGoogleSignInFlow(
                    context = context,
                    coroutineScope = coroutineScope,
                    viewModel = viewModel,
                    legacyLauncher = legacyLauncher
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = " أو التسجيل بالبيانات ",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("الاسم الكامل") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_username_input"),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_email_input"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة المرور") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_password_input"),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // Account role select - Modern Interactive Cards
        Text(
            text = "اختر نوع الحساب قبل التسجيل:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // User Card Option
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { role = "USER" }
                    .testTag("role_user_option"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (role == "USER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (role == "USER") androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👤", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "حساب مستخدم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (role == "USER") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "تصفح وتحميل التطبيقات",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Developer Card Option
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { role = "DEVELOPER" }
                    .testTag("role_dev_option"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (role == "DEVELOPER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (role == "DEVELOPER") androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛠️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "حساب مطوّر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (role == "DEVELOPER") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "رفع التطبيقات والتقييمات",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (role == "DEVELOPER") {
            OutlinedTextField(
                value = developerName,
                onValueChange = { developerName = it },
                label = { Text("اسم الاستوديو أو المطور") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_dev_name_input"),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (username.isBlank() || email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "الرجاء تعبئة جميع الحقول المطلوبة", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (role == "DEVELOPER" && developerName.isBlank()) {
                    Toast.makeText(context, "يرجى تعبئة اسم المطور أو الاستوديو الخاص بك", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.register(
                    username = username,
                    email = email,
                    passwordText = password,
                    role = role,
                    developerName = developerName,
                    onSuccess = {
                        Toast.makeText(context, "تم إنشاء الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo("HOME")
                        username = ""
                        email = ""
                        password = ""
                        developerName = ""
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_register_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("إنشاء الحساب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun triggerGoogleSignInFlow(
    context: android.content.Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    viewModel: StoreViewModel,
    legacyLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
) {
    val activity = context.findActivity()
    if (activity == null) {
        launchLegacyGoogleSignIn(context, legacyLauncher)
        return
    }

    coroutineScope.launch {
        try {
            val credentialManager = androidx.credentials.CredentialManager.create(activity)
            
            val rawNonce = java.util.UUID.randomUUID().toString()
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("423299705791-06m9b1stog47pel10dj49hikfinf4n3k.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential) {
                viewModel.loginWithGoogle(
                    idToken = credential.idToken,
                    googleEmail = credential.id,
                    googleDisplayName = credential.displayName ?: credential.id.substringBefore("@"),
                    onSuccess = { u ->
                        android.widget.Toast.makeText(context, "تم تسجيل الدخول بنجاح كـ ${u.username}", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo("HOME")
                    },
                    onError = { err ->
                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Fallback to GoogleSignInClient
                launchLegacyGoogleSignIn(context, legacyLauncher)
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            android.widget.Toast.makeText(context, "تم إغلاق نافذة تسجيل الدخول", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback immediately to GoogleSignInClient for Infinix Smart 10 / devices with CredentialManager issues
            launchLegacyGoogleSignIn(context, legacyLauncher)
        }
    }
}

// Helper date formatter
fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd | hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
