package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.AppRepository
import com.example.util.NetworkObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository()
    private val networkObserver = NetworkObserver(application)

    val isOnline: StateFlow<Boolean> = networkObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkObserver.isCurrentlyConnected())

    private val sharedPrefs = application.getSharedPreferences("store_prefs", android.content.Context.MODE_PRIVATE)

    init {
        // Load saved user session
        val savedUserId = sharedPrefs.getString("current_user_id", "") ?: ""
        if (savedUserId.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val user = repository.getUserByIdOneShot(savedUserId)
                    withContext(Dispatchers.Main) {
                        _currentUser.value = user
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Firebase Auth State Listener to automatically update UI on login
        try {
            android.util.Log.d("AUTH_DEBUG", "Initializing FirebaseAuth AuthStateListener")
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser != null && !fbUser.email.isNullOrBlank()) {
                    val email = fbUser.email!!
                    val name = fbUser.displayName ?: email.substringBefore("@")
                    android.util.Log.d("AUTH_DEBUG", "AuthStateListener: User authenticated: $email ($name). Forcing navigation to HOME.")

                    // Force immediate UI navigation to HOME on main thread unconditionally
                    viewModelScope.launch(Dispatchers.Main) {
                        _currentScreen.value = "HOME"
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        var user = repository.getUserByEmail(email)
                        if (user == null) {
                            val newUser = UserEntity(
                                username = name,
                                email = email,
                                passwordHash = "google_auth_oauth",
                                role = "USER",
                                developerName = "",
                                balance = 100.0
                            )
                            val id = repository.insertUser(newUser)
                            user = newUser.copy(id = id)
                            android.util.Log.d("AUTH_DEBUG", "Created new local user in database: ${user.id}")
                        }
                        val finalUser = user
                        withContext(Dispatchers.Main) {
                            _currentUser.value = finalUser
                            sharedPrefs.edit().putString("current_user_id", finalUser.id).apply()
                            _currentScreen.value = "HOME"
                            android.util.Log.d("AUTH_DEBUG", "AuthStateListener: Local user set to ${finalUser.username}, screen set to HOME")
                        }
                    }
                } else {
                    android.util.Log.d("AUTH_DEBUG", "AuthStateListener: No active user logged in")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AUTH_DEBUG", "AuthStateListener initialization error: ${e.message}", e)
        }

        // Auto-navigate from SPLASH to HOME after a beautiful 2.5s delay
        viewModelScope.launch {
            delay(2500)
            if (_currentScreen.value == "SPLASH") {
                _currentScreen.value = "HOME"
            }
        }
    }

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Observe current user's database state reactively (updates wallet balance automatically)
    val reactiveUser: StateFlow<UserEntity?> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getUserById(user.id)
            } else {
                flowOf(null)
            }
        }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Search & Filtering States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedRatingFilter = MutableStateFlow(0f) // 0 means all, 4f means 4.0+, 4.5f means 4.5+
    val selectedRatingFilter: StateFlow<Float> = _selectedRatingFilter.asStateFlow()

    private val _selectedType = MutableStateFlow("GAME") // "GAME" or "APP"
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    // Combined Flow for filtering apps and games
    val filteredApps: StateFlow<List<AppEntity>> = combine(
        repository.getAllApps(),
        _searchQuery,
        _selectedCategory,
        _selectedRatingFilter,
        _selectedType
    ) { apps, query, category, rating, type ->
        apps.filter { app ->
            val matchesType = app.type == type
            val matchesQuery = app.title.contains(query, ignoreCase = true) || 
                               app.packageName.contains(query, ignoreCase = true) ||
                               app.description.contains(query, ignoreCase = true)
            val matchesCategory = category == "الكل" || app.category == category
            val matchesRating = app.rating >= rating
            matchesType && matchesQuery && matchesCategory && matchesRating
        }
    }.catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Developer Applications State ---
    val developerApps: StateFlow<List<AppEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == "DEVELOPER") {
                repository.getAppsByDeveloper(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Admin Dashboard States ---
    val unverifiedApps: StateFlow<List<AppEntity>> = repository.getUnverifiedApps()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- App Details & Reviews State ---
    private val _selectedAppId = MutableStateFlow<String?>(null)
    val selectedAppId: StateFlow<String?> = _selectedAppId.asStateFlow()

    val selectedApp: StateFlow<AppEntity?> = _selectedAppId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getAppById(id)
            } else {
                flowOf(null)
            }
        }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentAppReviews: StateFlow<List<ReviewEntity>> = _selectedAppId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getReviewsForApp(id)
            } else {
                flowOf(emptyList())
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Downloads State ---
    val userDownloads: StateFlow<List<DownloadEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getDownloadsForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow("SPLASH") // SPLASH, HOME, DETAILS, DEV_DASHBOARD, ADMIN_DASHBOARD, PROFILE, LOGIN, REGISTER
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun selectApp(appId: String) {
        _selectedAppId.value = appId
        _currentScreen.value = "DETAILS"
    }

    // --- Search & Filter Actions ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setRatingFilter(rating: Float) {
        _selectedRatingFilter.value = rating
    }

    fun setStoreType(type: String) {
        _selectedType.value = type
        _selectedCategory.value = "الكل" // Reset category filter when switching tabs
    }

    // --- Auth Actions ---
    fun login(email: String, passwordText: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            android.util.Log.d("AUTH_DEBUG", "Initiating login for $email")
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, passwordText)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("AUTH_DEBUG", "FirebaseAuth signInWithEmailAndPassword succeeded for $email")
                        } else {
                            android.util.Log.e("AUTH_DEBUG", "FirebaseAuth signInWithEmailAndPassword failed: ${task.exception?.message}")
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("AUTH_DEBUG", "FirebaseAuth signIn catch: ${e.message}")
            }

            var user = repository.getUserByEmail(email)
            if (user == null) {
                val newUser = UserEntity(
                    username = email.substringBefore("@"),
                    email = email,
                    passwordHash = passwordText,
                    role = "USER",
                    developerName = "",
                    balance = 100.0
                )
                val id = repository.insertUser(newUser)
                user = newUser.copy(id = id)
                android.util.Log.d("AUTH_DEBUG", "Created new local user record for $email")
            }

            withContext(Dispatchers.Main) {
                if (user.passwordHash == passwordText || user.passwordHash == "google_auth_oauth") {
                    _currentUser.value = user
                    sharedPrefs.edit().putString("current_user_id", user.id).apply()
                    _loginError.value = null
                    _currentScreen.value = "HOME"
                    android.util.Log.d("AUTH_DEBUG", "Login successful for ${user.username}, currentScreen set to HOME")
                    onSuccess()
                } else {
                    val errMsg = "البريد الإلكتروني أو كلمة المرور غير صحيحة"
                    _loginError.value = errMsg
                    android.util.Log.e("AUTH_DEBUG", "Login failed: incorrect password for $email")
                    onError(errMsg)
                }
            }
        }
    }

    fun register(
        username: String,
        email: String,
        passwordText: String,
        role: String,
        developerName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            android.util.Log.d("AUTH_DEBUG", "Initiating register for $email")
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, passwordText)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("AUTH_DEBUG", "FirebaseAuth createUserWithEmailAndPassword succeeded for $email")
                        } else {
                            android.util.Log.e("AUTH_DEBUG", "FirebaseAuth createUserWithEmailAndPassword failed: ${task.exception?.message}")
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("AUTH_DEBUG", "FirebaseAuth register catch: ${e.message}")
            }

            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("AUTH_DEBUG", "Register failed: Email $email already exists locally")
                    onError("البريد الإلكتروني مسجل بالفعل")
                }
            } else {
                val newUser = UserEntity(
                    username = username,
                    email = email,
                    passwordHash = passwordText,
                    role = role,
                    developerName = if (role == "DEVELOPER") developerName else "",
                    balance = if (role == "DEVELOPER") 0.0 else 100.0 // Give users $100 preloaded balance
                )
                val id = repository.insertUser(newUser)
                val savedUser = newUser.copy(id = id)
                withContext(Dispatchers.Main) {
                    _currentUser.value = savedUser
                    sharedPrefs.edit().putString("current_user_id", savedUser.id).apply()
                    _loginError.value = null
                    _currentScreen.value = "HOME"
                    android.util.Log.d("AUTH_DEBUG", "Register successful for ${savedUser.username}, currentScreen set to HOME")
                    onSuccess()
                }
            }
        }
    }

    fun loginWithGoogle(
        idToken: String? = null,
        googleEmail: String,
        googleDisplayName: String,
        onSuccess: (UserEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("AUTH_DEBUG", "loginWithGoogle initiated. idToken present: ${!idToken.isNullOrBlank()}, googleEmail: $googleEmail")
                var firebaseEmail = googleEmail
                var firebaseName = googleDisplayName

                if (!idToken.isNullOrBlank()) {
                    try {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(credential).await()
                        val fbUser = authResult.user
                        if (fbUser != null) {
                            if (!fbUser.email.isNullOrBlank()) firebaseEmail = fbUser.email!!
                            if (!fbUser.displayName.isNullOrBlank()) firebaseName = fbUser.displayName!!
                            android.util.Log.d("AUTH_DEBUG", "signInWithCredential success for user: $firebaseEmail")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AUTH_DEBUG", "signInWithCredential failed: ${e.message}", e)
                    }
                }

                var user = repository.getUserByEmail(firebaseEmail)
                if (user == null) {
                    val newUser = UserEntity(
                        username = if (firebaseName.isNotBlank()) firebaseName else firebaseEmail.substringBefore("@"),
                        email = firebaseEmail,
                        passwordHash = "google_auth_oauth",
                        role = "USER",
                        developerName = "",
                        balance = 100.0
                    )
                    val id = repository.insertUser(newUser)
                    user = newUser.copy(id = id)
                    android.util.Log.d("AUTH_DEBUG", "Inserted new user into DB: ${user.username} (${user.id})")
                } else {
                    android.util.Log.d("AUTH_DEBUG", "Found existing user in DB: ${user.username} (${user.id})")
                }
                val finalUser = user
                withContext(Dispatchers.Main) {
                    _currentUser.value = finalUser
                    sharedPrefs.edit().putString("current_user_id", finalUser.id).apply()
                    _loginError.value = null
                    _currentScreen.value = "HOME"
                    android.util.Log.d("AUTH_DEBUG", "Set currentScreen to HOME unconditionally for user: ${finalUser.username}")
                    onSuccess(finalUser)
                }
            } catch (e: Exception) {
                android.util.Log.e("AUTH_DEBUG", "Error in loginWithGoogle: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError("فشل تسجيل الدخول بواسطة Google: ${e.localizedMessage}")
                }
            }
        }
    }

    fun logout() {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentUser.value = null
        sharedPrefs.edit().remove("current_user_id").apply()
        _currentScreen.value = "HOME"
    }

    fun addBalance(amount: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val dbUser = repository.getUserByIdOneShot(user.id)
            if (dbUser != null) {
                val updated = dbUser.copy(balance = dbUser.balance + amount)
                repository.updateUser(updated)
            }
        }
    }

    private suspend fun uploadToFirebaseStorage(uri: android.net.Uri, folder: String, suffix: String): String {
        val uriString = uri.toString()
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString
        }
        val fileName = "${folder}/${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}$suffix"
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(fileName)
        storageRef.putFile(uri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()
        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
            throw IllegalStateException("لم يتم الحصول على رابط https صالح من Firebase Storage")
        }
        return downloadUrl
    }

    // --- Developer Actions ---
    fun uploadApp(
        title: String,
        packageName: String,
        description: String,
        category: String,
        type: String,
        price: Double,
        size: String,
        version: String,
        apkUri: android.net.Uri?,
        bannerColor: String,
        iconUri: android.net.Uri?,
        screenshotUris: List<android.net.Uri>,
        videoUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (user.role != "DEVELOPER") return

        val packageRegex = "^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[a-z0-9_]\$".toRegex()
        if (!packageName.matches(packageRegex)) {
            onError("اسم الحزمة (Package Name) غير صالح. يجب أن يكون مثل: com.epic.myapp")
            return
        }
        if (apkUri == null) {
            onError("يرجى اختيار ملف APK.")
            return
        }
        if (title.isBlank() || description.isBlank() || size.isBlank()) {
            onError("يرجى ملء جميع الحقول المطلوبة")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Upload APK to Firebase Storage -> Get https downloadUrl
                val apkUrl = uploadToFirebaseStorage(apkUri, "apps", ".apk")

                // Upload Icon to Firebase Storage -> Get https downloadUrl
                val savedIconUrl = if (iconUri != null) {
                    uploadToFirebaseStorage(iconUri, "icons", ".jpg")
                } else null

                // Upload Screenshots to Firebase Storage -> Get https downloadUrls
                val savedScreenshotUrls = mutableListOf<String>()
                for (uri in screenshotUris) {
                    savedScreenshotUrls.add(uploadToFirebaseStorage(uri, "screenshots", ".jpg"))
                }
                val savedScreenshotNames = savedScreenshotUrls.joinToString(",")

                val newApp = AppEntity(
                    title = title,
                    packageName = packageName,
                    description = description,
                    developerId = user.id,
                    developerName = if (user.developerName.isNotBlank()) user.developerName else user.username,
                    category = category,
                    type = type,
                    price = price,
                    size = size,
                    version = version,
                    apkFileName = apkUrl,
                    bannerColor = bannerColor,
                    iconUri = savedIconUrl,
                    screenshotUris = savedScreenshotNames,
                    videoUrl = videoUrl,
                    isVerified = true, // Published immediately without waiting for review
                    rating = 0f,
                    ratingsCount = 0,
                    downloadsCount = 0
                )

                // Save to Firestore ONLY with HTTPS URLs (no local URIs)
                repository.insertApp(newApp)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError("حدث خطأ أثناء رفع الصور/الملفات إلى Firebase Storage: ${e.localizedMessage}")
                }
            }
        }
    }

    fun updateLiveApp(
        appId: String,
        newVersion: String,
        newDescription: String,
        newApkUri: android.net.Uri?,
        newIconUri: android.net.Uri? = null,
        newScreenshotUris: List<android.net.Uri> = emptyList(),
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (newApkUri == null) {
            onError("يلزم رفع ملف APK الجديد لإجراء التحديث.")
            return
        }
        if (newVersion.isBlank()) {
            onError("يرجى كتابة رقم الإصدار الجديد.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = repository.getAppByIdOneShot(appId)
                if (app != null) {
                    val newApkUrl = uploadToFirebaseStorage(newApkUri, "apps", ".apk")
                    val updatedIconUrl = if (newIconUri != null) {
                        uploadToFirebaseStorage(newIconUri, "icons", ".jpg")
                    } else app.iconUri

                    val updatedScreenshotNames = if (newScreenshotUris.isNotEmpty()) {
                        val list = mutableListOf<String>()
                        for (uri in newScreenshotUris) {
                            list.add(uploadToFirebaseStorage(uri, "screenshots", ".jpg"))
                        }
                        list.joinToString(",")
                    } else {
                        app.screenshotUris
                    }

                    val updated = app.copy(
                        version = newVersion,
                        description = if (newDescription.isNotBlank()) newDescription else app.description,
                        apkFileName = newApkUrl,
                        iconUri = updatedIconUrl,
                        screenshotUris = updatedScreenshotNames,
                        isVerified = true,
                        lastUpdated = System.currentTimeMillis()
                    )
                    repository.updateApp(updated)
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("لم يتم العثور على التطبيق المطلوب تحديثه")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError("حدث خطأ أثناء رفع التحديث إلى Firebase Storage: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- User Actions: Purchase & Real APK Download via DownloadManager ---
    fun purchaseAndDownload(app: AppEntity, onMessage: (String) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onMessage("يجب تسجيل الدخول أولاً لتنزيل التطبيقات")
            _currentScreen.value = "LOGIN"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val dbUser = repository.getUserByIdOneShot(user.id) ?: return@launch
            val existingDownload = repository.getDownloadOneShot(user.id, app.id)

            if (app.price > 0 && (existingDownload == null || existingDownload.purchasedPrice == 0.0)) {
                // Check balance
                if (dbUser.balance < app.price) {
                    withContext(Dispatchers.Main) {
                        onMessage("رصيدك الحالي غير كافٍ لإتمام عملية الشراء!")
                    }
                    return@launch
                }

                // Deduct user balance
                val updatedUser = dbUser.copy(balance = dbUser.balance - app.price)
                repository.updateUser(updatedUser)

                // Credit developer balance
                val devUser = repository.getUserByIdOneShot(app.developerId)
                if (devUser != null) {
                    repository.updateUser(devUser.copy(balance = devUser.balance + app.price))
                }
            }

            // Record download in Firestore
            val downloadRecord = DownloadEntity(
                id = existingDownload?.id ?: "",
                userId = user.id,
                appId = app.id,
                isInstalled = true,
                progress = 1.0f,
                purchasedPrice = app.price
            )
            if (existingDownload != null) {
                repository.updateDownload(downloadRecord)
            } else {
                repository.insertDownload(downloadRecord)
            }

            // Update App download count in Firestore
            val updatedApp = app.copy(downloadsCount = app.downloadsCount + 1)
            repository.updateApp(updatedApp)

            val appCtx = getApplication<Application>()
            val apkUrl = app.apkFileName

            withContext(Dispatchers.Main) {
                try {
                    if (apkUrl.startsWith("http://") || apkUrl.startsWith("https://")) {
                        val downloadManager = appCtx.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        val uri = android.net.Uri.parse(apkUrl)
                        val safeTitle = app.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                        val fileName = "${if (safeTitle.isBlank()) "app" else safeTitle}_v${app.version}_${System.currentTimeMillis()}.apk"

                        val request = android.app.DownloadManager.Request(uri).apply {
                            setTitle(app.title)
                            setDescription("جاري تنزيل ملف APK...")
                            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                            setAllowedOverMetered(true)
                            setAllowedOverRoaming(true)
                            setMimeType("application/vnd.android.package-archive")
                        }

                        val downloadId = downloadManager.enqueue(request)

                        // Register receiver to open package installer when DownloadManager finishes
                        val onCompleteReceiver = object : android.content.BroadcastReceiver() {
                            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                                val id = intent?.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                                if (id == downloadId) {
                                    try {
                                        var downloadedUri = downloadManager.getUriForDownloadedFile(downloadId)
                                        if (downloadedUri == null) {
                                            val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                                            val cursor = downloadManager.query(query)
                                            if (cursor != null && cursor.moveToFirst()) {
                                                val uriStr = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_LOCAL_URI))
                                                if (!uriStr.isNullOrBlank()) {
                                                    downloadedUri = android.net.Uri.parse(uriStr)
                                                }
                                                cursor.close()
                                            }
                                        }

                                        if (downloadedUri != null) {
                                            val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(downloadedUri, "application/vnd.android.package-archive")
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context?.startActivity(installIntent)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        try {
                                            appCtx.unregisterReceiver(this)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            appCtx.registerReceiver(
                                onCompleteReceiver,
                                android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                                android.content.Context.RECEIVER_EXPORTED
                            )
                        } else {
                            appCtx.registerReceiver(
                                onCompleteReceiver,
                                android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                            )
                        }

                        onMessage("بدأ التنزيل المباشر عبر DownloadManager! سيفتح مثبت الحزم تلقائياً عند انتهاء التحميل.")
                    } else if (apkUrl.isNotBlank()) {
                        val uri = android.net.Uri.parse(apkUrl)
                        val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        appCtx.startActivity(installIntent)
                        onMessage("تم تشغيل تثبيت ${app.title}")
                    } else {
                        onMessage("رابط APK الخاص بهذا التطبيق غير متوفر")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onMessage("تعذر بدء التنزيل: ${e.localizedMessage}")
                }
            }
        }
    }

    fun uninstallApp(app: AppEntity, onComplete: () -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val download = repository.getDownloadOneShot(user.id, app.id)
            if (download != null) {
                repository.deleteDownload(download)
            }
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                    data = android.net.Uri.parse("package:${app.packageName}")
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                getApplication<Application>().startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // --- Review Actions ---
    fun getReviewsForApp(appId: String): Flow<List<ReviewEntity>> {
        return repository.getReviewsForApp(appId)
    }

    fun postReview(appId: String, rating: Float, comment: String, onComplete: () -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Save review
            val review = ReviewEntity(
                appId = appId,
                userId = user.id,
                username = user.username,
                rating = rating,
                comment = comment
            )
            repository.insertReview(review)

            // Recalculate app rating average
            val app = repository.getAppByIdOneShot(appId)
            if (app != null) {
                val allAppReviews = repository.getReviewsForApp(appId).first()
                val count = allAppReviews.size
                val sum = allAppReviews.sumOf { it.rating.toDouble() }
                val newAverage = if (count > 0) (sum / count).toFloat() else rating

                val updatedApp = app.copy(
                    rating = newAverage,
                    ratingsCount = count
                )
                repository.updateApp(updatedApp)
            }

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // --- Admin Actions ---
    fun approveApp(appId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = repository.getAppByIdOneShot(appId)
            if (app != null) {
                repository.updateApp(app.copy(isVerified = true))
            }
        }
    }

    fun rejectApp(appId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = repository.getAppByIdOneShot(appId)
            if (app != null) {
                repository.deleteApp(app)
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUser(user)
        }
    }
}
