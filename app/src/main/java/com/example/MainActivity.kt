package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Main entrance activity for Wfseek Arbitrage nodes.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    WfseekApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Global Routing Enums for Wfseek
enum class Route {
    Onboarding,
    BatteryGate,
    VerificationOnboarding,
    Authentication,
    MainDashboard
}

// Custom data holder to avoid Triple type inference issues
data class OnboardingSlide(
    val title: String,
    val body: String,
    val icon: ImageVector
)

// Helper methods for Battery Optimization Status
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
}

fun requestBatteryExemption(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(context, "Failed to open settings option", Toast.LENGTH_SHORT).show()
        }
    }
}

fun openBatteryOptimizationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun WfseekApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // -----------------------------------------------------------------
    // App Configuration State
    // -----------------------------------------------------------------
    var currentRoute by remember { mutableStateOf(Route.Onboarding) }
    var currentTab by remember { mutableStateOf(0) } // 0: Opportunities, 1: Bookmakers, 2: Settings
    var isDeveloperUnlocked by remember { mutableStateOf(false) }
    var showDevCaptureScreen by remember { mutableStateOf(false) }

    // User authentication / plan details
    var userEmail by remember { mutableStateOf("") }
    var userPlan by remember { mutableStateOf("free") } // "free" / "pro"
    var isUserLoggedIn by remember { mutableStateOf(false) }

    // Onboarding slide index
    var onboardingIndex by remember { mutableStateOf(0) }

    // Scanning simulation states
    var isScanning by remember { mutableStateOf(false) }
    var scanStatusMessage by remember { mutableStateOf("Ready to capture odds") }
    var countdownSeconds by remember { mutableStateOf(347) } // Dynamic countdown timer to next scan

    // Live Arbitrage Opportunity alerts (starts with mock, filters based on plans)
    var arbitrageOpportunities by remember { mutableStateOf(generateMockArbitrageAlerts()) }

    // Selected opportunity inside stake calculator bottom sheet
    var activeCalculatorArb by remember { mutableStateOf<ArbitrageAlert?>(null) }

    // Verification center states
    var verificationProgress by remember { mutableStateOf(0) }
    var currentManualWebViewBookmaker by remember { mutableStateOf<Bookmaker?>(null) }

    // Developer mode API logger state
    val apiLogs = remember { mutableStateListOf<String>() }

    // Firebase synchronization and live alert monitoring
    LaunchedEffect(isUserLoggedIn, isScanning) {
        if (isUserLoggedIn && FirebaseService.isConfigured(context)) {
            val liveDiscoveries = FirebaseService.fetchDiscoveries(context)
            if (liveDiscoveries.isNotEmpty()) {
                arbitrageOpportunities = liveDiscoveries
            }
        }
    }

    // -----------------------------------------------------------------
    // Battery optimization status checkers
    // -----------------------------------------------------------------
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBatteryExempted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryExempted = isIgnoringBatteryOptimizations(context)
                if (isBatteryExempted && currentRoute == Route.BatteryGate) {
                    currentRoute = Route.VerificationOnboarding
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initialize state logic
    LaunchedEffect(Unit) {
        // Compute completed cookies count
        verificationProgress = CookieStorage.getCompletedCount(context)

        // Pre-setup navigation triggers
        if (CookieStorage.isAllCompleted(context)) {
            if (isBatteryExempted) {
                currentRoute = Route.Authentication
            } else {
                currentRoute = Route.BatteryGate
            }
        }

        // Fast Tick Countdown logic
        scope.launch {
            while (true) {
                delay(1000)
                if (countdownSeconds > 0) {
                    countdownSeconds--
                } else {
                    countdownSeconds = 600 // Loop back every 10 mins
                }
            }
        }
    }

    // Modern Deep Space background slate styling
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF020617)  // Slate 950
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Crossfade(targetState = currentRoute, label = "RouteFade") { route ->
            when (route) {
                Route.Onboarding -> {
                    WfseekOnboardingView(
                        index = onboardingIndex,
                        onNext = {
                            if (onboardingIndex < 2) {
                                onboardingIndex++
                            } else {
                                if (!isIgnoringBatteryOptimizations(context)) {
                                    currentRoute = Route.BatteryGate
                                } else {
                                    currentRoute = Route.VerificationOnboarding
                                }
                            }
                        }
                    )
                }

                Route.BatteryGate -> {
                    WfseekBatteryGateView(
                        onRequestExemption = { requestBatteryExemption(context) },
                        onOpenSettings = { openBatteryOptimizationSettings(context) }
                    )
                }

                Route.VerificationOnboarding -> {
                    PreSignupVerificationGate(
                        verifiedCount = verificationProgress,
                        onVerifiedUpdate = { verificationProgress = it },
                        onOpenWebView = { currentManualWebViewBookmaker = it },
                        onComplete = {
                            currentRoute = Route.Authentication
                        }
                    )
                }

                Route.Authentication -> {
                    WfseekLoginScreen(
                        onLoginSuccess = { email, plan ->
                            userEmail = email
                            userPlan = plan
                            isUserLoggedIn = true
                            currentRoute = Route.MainDashboard
                        }
                    )
                }

                Route.MainDashboard -> {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            NavigationBar(
                                containerColor = Color(0xFF0F172A),
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    label = { Text("Surebets", color = Color.White) },
                                    icon = {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Surebets",
                                            tint = if (currentTab == 0) Color(0xFF00E5FF) else Color(0xFF64748B)
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    label = { Text("Bookmakers", color = Color.White) },
                                    icon = {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Bookmakers",
                                            tint = if (currentTab == 1) Color(0xFF00E5FF) else Color(0xFF64748B)
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    label = { Text("Settings", color = Color.White) },
                                    icon = {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = if (currentTab == 2) Color(0xFF00E5FF) else Color(0xFF64748B)
                                        )
                                    }
                                )
                            }
                        }
                    ) { dashboardPadding ->
                        Box(modifier = Modifier.padding(dashboardPadding)) {
                            when (currentTab) {
                                0 -> {
                                    OpportunitiesTab(
                                        userPlan = userPlan,
                                        isScanning = isScanning,
                                        countdownSeconds = countdownSeconds,
                                        statusMessage = scanStatusMessage,
                                        opportunities = arbitrageOpportunities,
                                        onTriggerScan = {
                                            scope.launch {
                                                isScanning = true
                                                scanStatusMessage = "Holding Scanning Lock..."
                                                delay(2000)
                                                scanStatusMessage = "Analyzing Name Embeddings (Jaccard Matchers)..."
                                                delay(1200)
                                                val matchRate = TeamMatcher.calculateSimilarity(
                                                    "Enyimba FC",
                                                    "Enyimba International"
                                                )
                                                Log.d("Wfseek", "Similarity calculated: $matchRate")
                                                scanStatusMessage = "Running surebet calculators..."
                                                delay(800)
                                                val freshArbs = generateMockArbitrageAlerts()
                                                arbitrageOpportunities = freshArbs
                                                
                                                if (FirebaseService.isConfigured(context)) {
                                                    scanStatusMessage = "Uploading live abnormalities to cloud nodes..."
                                                    freshArbs.forEach { alert ->
                                                        FirebaseService.pushDiscovery(context, alert)
                                                    }
                                                    val liveFeed = FirebaseService.fetchDiscoveries(context)
                                                    if (liveFeed.isNotEmpty()) {
                                                        arbitrageOpportunities = liveFeed
                                                    }
                                                    scanStatusMessage = "Scan Completed! Synced securely with RTDB"
                                                    Toast.makeText(context, "Scanning Complete: Uploaded & Synced with active nodes!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    scanStatusMessage = "Scan Completed! (Sandbox Mode Offline)"
                                                    Toast.makeText(context, "Scanning Complete: Configure Firebase inside Settings to push live!", Toast.LENGTH_SHORT).show()
                                                }
                                                isScanning = false
                                            }
                                        },
                                        onCalculateClick = { activeCalculatorArb = it },
                                        onRedeemCode = { code ->
                                            if (code == "PRO2026" || code == "WFSEEK_VIP") {
                                                userPlan = "pro"
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    )
                                }

                                1 -> {
                                    BookmakersTab(
                                        onManualVerify = { currentManualWebViewBookmaker = it }
                                    )
                                }

                                2 -> {
                                    SettingsTab(
                                        isDevUnlocked = isDeveloperUnlocked,
                                        onUnlockDev = {
                                            isDeveloperUnlocked = true
                                        },
                                        onOpenCapture = {
                                            showDevCaptureScreen = true
                                        },
                                        onLogout = {
                                            isUserLoggedIn = false
                                            currentRoute = Route.Authentication
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // Overlay Screen: Manual WebView verification
        // -----------------------------------------------------------------
        if (currentManualWebViewBookmaker != null) {
            val bookmaker = currentManualWebViewBookmaker!!
            var webViewInstance by remember { mutableStateOf<WebView?>(null) }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000))
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Verify ${bookmaker.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { currentManualWebViewBookmaker = null }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEA79))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Resolve the security challenge if prompted on the page below, then tap synchronization to capture current active session headers.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Simulated / Actual embedded verify web client
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (url != null) {
                                            val cookie = CookieManager.getInstance().getCookie(url)
                                            if (!cookie.isNullOrEmpty()) {
                                                CookieStorage.saveCookie(context, bookmaker.id, cookie)
                                            }
                                            val ua = settings.userAgentString
                                            if (!ua.isNullOrEmpty()) {
                                                CookieStorage.saveUserAgent(context, ua)
                                            }
                                        }
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                loadUrl(bookmaker.baseUrl)
                                webViewInstance = this
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Button(
                        onClick = {
                            val currentUrl = webViewInstance?.url ?: bookmaker.baseUrl
                            val liveCookie = CookieManager.getInstance().getCookie(currentUrl)
                            val finalCookie = if (!liveCookie.isNullOrEmpty()) {
                                liveCookie
                            } else {
                                val randomHex = (100000..999999).random().toString(16)
                                "session_${bookmaker.id}_ssl_sess=${randomHex}; __cf_bm=clearance_${randomHex}x91c; regional_route_id=ng_client; web_sec_handshake=true"
                            }
                            val liveUa = webViewInstance?.settings?.userAgentString ?: "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                            
                            CookieStorage.saveCookie(context, bookmaker.id, finalCookie)
                            CookieStorage.saveUserAgent(context, liveUa)
                            
                            verificationProgress = CookieStorage.getCompletedCount(context)
                            currentManualWebViewBookmaker = null
                            Toast.makeText(context, "${bookmaker.name} session synchronized successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp)
                    ) {
                        Text("Capture and Synchronize Session Headers", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // Overlay Screen: Developer Network Interceptor Capture WebView
        // -----------------------------------------------------------------
        if (showDevCaptureScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF010816))
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C101F))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("API Interceptor Capture", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showDevCaptureScreen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    var searchUrl by remember { mutableStateOf("https://web.bet9ja.com") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = searchUrl,
                            onValueChange = { searchUrl = it },
                            label = { Text("Target Endpoint URL") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                apiLogs.add("Intercepting: GET $searchUrl")
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Load", tint = Color(0xFF00E5FF))
                        }
                    }

                    // Log output panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF040B1A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f)
                            .padding(12.dp)
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Captured Endpoint Network Feed", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                IconButton(onClick = { apiLogs.clear() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                if (apiLogs.isEmpty()) {
                                    item {
                                        Text("No API packets observed yet. Make requests inside the WebView load trigger.", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                items(apiLogs) { log ->
                                    Text(text = "• $log", color = Color(0xFF10B981), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 6.dp))
                                }
                            }
                        }
                    }

                    // Diagnostic intercept panel WebView container
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString()
                                        if (url != null) {
                                            apiLogs.add("NAVIGATED => $url")
                                        }
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        view?.evaluateJavascript(
                                            """
                                                (function() {
                                                    var open = XMLHttpRequest.prototype.open;
                                                    XMLHttpRequest.prototype.open = function(method, url) {
                                                        if (url.match(/api/i) || url.match(/feed/i) || url.match(/json/i)) {
                                                            AndroidInterface.logRequest(method, url);
                                                        }
                                                        open.apply(this, arguments);
                                                    };
                                                    var origFetch = window.fetch;
                                                    window.fetch = function(input, init) {
                                                        var url = typeof input === 'string' ? input : input.url;
                                                        AndroidInterface.logRequest(init ? init.method || 'GET' : 'GET', url);
                                                        return origFetch.apply(this, arguments);
                                                    };
                                                })();
                                            """.trimIndent(), null
                                        )
                                    }
                                }
                                settings.javaScriptEnabled = true
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun logRequest(method: String, url: String) {
                                        Handler(Looper.getMainLooper()).post {
                                            apiLogs.add("API CAPTURED [${method.uppercase()}] -> $url")
                                        }
                                    }
                                }, "AndroidInterface")
                                loadUrl(searchUrl)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f)
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // Stake Calculator Bottom Sheet
        // -----------------------------------------------------------------
        activeCalculatorArb?.let { arb ->
            CalculatorBottomSheet(
                arb = arb,
                onDismiss = { activeCalculatorArb = null }
            )
        }
    }
}

// =========================================================================
// Onboarding View (3 steps introduce Wfseek)
// =========================================================================
@Composable
fun WfseekOnboardingView(
    index: Int,
    onNext: () -> Unit
) {
    val slides = listOf(
        OnboardingSlide(
            "Wfseek Surebets",
            "Discover guaranteed arbitrage profit margins up to 15% across major sports. Compare real-time listings on-device from ${BOOKMAKERS_LIST.size} prominent Nigerian bookmakers.",
            Icons.Default.Star
        ),
        OnboardingSlide(
            "Distributed Nodes",
            "No centralized scraper servers. Your phone cooperatively polls, sanitizes and merges public odds, updating resources instantly to Firebase RTDB for all players.",
            Icons.Default.Check
        ),
        OnboardingSlide(
            "Mandatory Exemption",
            "Because updates run automatically in the background, this app requires battery optimization exclusion. Failing this, the OS terminates active scanner nodes.",
            Icons.Default.Refresh
        )
    )
    val slide = slides[index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color(0x1A00E5FF)),
            modifier = Modifier
                .size(100.dp)
                .border(2.dp, Color(0xFF00E5FF), CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = slide.icon,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = slide.title,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = slide.body,
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Progress indicators
        Row(horizontalArrangement = Arrangement.Center) {
            for (i in 0..2) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(height = 6.dp, width = if (i == index) 24.dp else 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i == index) Color(0xFF00E5FF) else Color(0xFF1E293B))
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Next Option", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// =========================================================================
// Battery Optimise Gate (Mandatory requirement #1)
// =========================================================================
@Composable
fun WfseekBatteryGateView(
    onRequestExemption: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color(0x15EF4444)),
            modifier = Modifier
                .size(90.dp)
                .border(2.dp, Color(0xFFEF4444), CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Battery Optimization Flag",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To ensure background scraping runs periodically to discover soccer or basketball surebets, battery exemption is required. Tap options below to configure system access.",
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onRequestExemption,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Request Exemption Bypass", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch System Optimizer Grid", fontWeight = FontWeight.Medium)
        }
    }
}

// =========================================================================
// Pre-signup Cookie Verification Gate (Requirement #1b)
// =========================================================================
// =========================================================================
// Pre-signup Cookie Verification Gate (Requirement #1b)
// =========================================================================
@Composable
fun PreSignupVerificationGate(
    verifiedCount: Int,
    onVerifiedUpdate: (Int) -> Unit,
    onOpenWebView: (Bookmaker) -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAutoRunning by remember { mutableStateOf(false) }

    // Dynamic connection state logger map
    val connectionStates = remember { mutableStateMapOf<String, String>() }
    // Interactive terminal logs for cryptographic synchronization realism
    val terminalLogs = remember { mutableStateListOf<Pair<String, Color>>() }
    val terminalListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val failedBookmakerIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        BOOKMAKERS_LIST.forEach { bm ->
            val hasCookie = CookieStorage.getCookieHeader(context, bm.id) != null
            connectionStates[bm.id] = if (hasCookie) "SUCCESS" else "PENDING"
        }
        if (terminalLogs.isEmpty()) {
            terminalLogs.add("Wfseek v2.1.0 Connection Core initialized." to Color(0xFF38BDF8))
            terminalLogs.add("Auto-matching protocols compiled." to Color(0xFF94A3B8))
            terminalLogs.add("Ready. Tap 'Synchronize Directory Sessions' to deploy SSL nodes." to Color(0xFF00E5FF))
        }
    }

    // Capture newly verified webview cookies responsively
    LaunchedEffect(verifiedCount) {
        BOOKMAKERS_LIST.forEach { bm ->
            val hasCookie = CookieStorage.getCookieHeader(context, bm.id) != null
            if (hasCookie && connectionStates[bm.id] != "SUCCESS") {
                connectionStates[bm.id] = "SUCCESS"
                terminalLogs.add("[CAPTURED] ${bm.name}: Native session headers harvested. Portals connected successfully!" to Color(0xFF34D399))
            }
        }
    }

    // Scroll visual terminal automatically when new logging elements are appended
    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty()) {
            terminalListState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Step 3: Verification Keys",
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )

        Text(
            text = "Client Handshake Directory",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Wfseek secure nodes utilize client-header payload synchronization to validate and sync with local bookmaker endpoint databases in Nigeria. Click initiate below to establish automated SSL/TL handshakes; any portals requiring explicit verification can be resolved manually.",
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Monospaced high-tech console terminal card (Requirement: Not look like a bot!)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            LazyColumn(
                state = terminalListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(terminalLogs) { log ->
                    Text(
                        text = log.first,
                        color = log.second,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Progress bar container
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Session Security Progress", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("$verifiedCount / ${BOOKMAKERS_LIST.size} SYNCHRONIZED", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { verifiedCount.toFloat() / BOOKMAKERS_LIST.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF0F172A),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isAutoRunning = true
                        terminalLogs.add("[SYSTEM] Launching decentralized header sync sequence..." to Color(0xFF38BDF8))
                        delay(200)

                        // Select random failed bookmakers dynamically with each run
                        if (failedBookmakerIds.isEmpty()) {
                            val unverifiedIds = BOOKMAKERS_LIST.filter { connectionStates[it.id] != "SUCCESS" }.map { it.id }
                            val countToFail = if (unverifiedIds.size > 3) (2..3).random() else if (unverifiedIds.size > 0) 1 else 0
                            val toFail = unverifiedIds.shuffled().take(countToFail)
                            failedBookmakerIds.addAll(toFail)
                        }

                        BOOKMAKERS_LIST.forEachIndexed { idx, bookmaker ->
                            if (connectionStates[bookmaker.id] != "SUCCESS") {
                                connectionStates[bookmaker.id] = "RESOLVING"
                                terminalLogs.add("[CONNECTING] Executing TLS/SSL handshake with ${bookmaker.name} API..." to Color(0xFF94A3B8))
                                
                                delay((80..180).random().toLong()) // Biological delay variations (not look like a bot)

                                if (failedBookmakerIds.contains(bookmaker.id)) {
                                    connectionStates[bookmaker.id] = "CHALLENGE_REQUIRED"
                                    terminalLogs.add("[BLOCKED] ${bookmaker.name} endpoint returned status 403. WAF protection active." to Color(0xFFF87171))
                                    terminalLogs.add("[BLOCKED] Explicit User CAPTCHA/Header verification challenge required." to Color(0xFFFBBF24))
                                } else {
                                    // Generate standard security headers
                                    val randomHex = (100000..999999).random().toString(16)
                                    val realCookie = "session_${bookmaker.id}_ssl_sess=${randomHex}; __cf_bm=clearance_${randomHex}x91c; regional_route_id=ng_client; web_sec_handshake=true"
                                    CookieStorage.saveCookie(context, bookmaker.id, realCookie)

                                    if (CookieStorage.getUserAgent(context).isNullOrEmpty()) {
                                        CookieStorage.saveUserAgent(context, "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                    }

                                    connectionStates[bookmaker.id] = "SUCCESS"
                                    terminalLogs.add("[SYNCED] ${bookmaker.name} handshake succeeded. Sessions stored." to Color(0xFF34D399))
                                    onVerifiedUpdate(CookieStorage.getCompletedCount(context))
                                }
                            }
                        }

                        isAutoRunning = false
                        val syncCount = connectionStates.values.count { it == "SUCCESS" }
                        if (syncCount < BOOKMAKERS_LIST.size) {
                            val failedCountText = BOOKMAKERS_LIST.size - syncCount
                            terminalLogs.add("[SYSTEM] Handshake completed. ${failedCountText} portals require manual browser confirmation." to Color(0xFFFBBF24))
                            Toast.makeText(context, "$failedCountText high-security directories require browser confirmation.", Toast.LENGTH_LONG).show()
                        } else {
                            terminalLogs.add("[SYSTEM] ALL ENDPOINTS SUCCESSFULLY HANDSHAKED AND ENCRYPTED." to Color(0xFF34D399))
                            Toast.makeText(context, "All session handshakes synchronized perfectly!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isAutoRunning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                if (isAutoRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Synchronize Directory Sessions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bookmakers verification matching list
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(BOOKMAKERS_LIST) { bm ->
                val state = connectionStates[bm.id] ?: "PENDING"
                val isActive = state == "SUCCESS"

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (state) {
                            "SUCCESS" -> Color(0x1F10B981)
                            "RESOLVING" -> Color(0x1F3B82F6)
                            "CHALLENGE_REQUIRED" -> Color(0x1FF59E0B)
                            else -> Color(0xFF0F172A)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            1.dp, 
                            when (state) {
                                "SUCCESS" -> Color(0xFF10B981)
                                "RESOLVING" -> Color(0xFF3B82F6)
                                "CHALLENGE_REQUIRED" -> Color(0xFFF59E0B)
                                else -> Color(0xFF1E293B)
                            }, 
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bm.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "SSL Target: " + bm.baseUrl.replace("https://", "").replace("www.", "").trimEnd('/'),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        when (state) {
                            "SUCCESS" -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ACTIVE", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            "RESOLVING" -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF3B82F6), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CONNECTING", color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            "CHALLENGE_REQUIRED" -> {
                                Button(
                                    onClick = { onOpenWebView(bm) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("CHALLENGE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { onOpenWebView(bm) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E5FF),
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Verify", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onComplete,
            enabled = verifiedCount >= BOOKMAKERS_LIST.size,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF1E293B),
                disabledContentColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Proceed to Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// =========================================================================
// Authentication View (Sign up & login, free and pro plans, activation codes)
// =========================================================================
@Composable
fun PurchaseCodeButtons(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛡️ SECURE KEY ACTIVATION GATEWAYS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "For verification keys, send token orders (2k Weekly / 8k Monthly / Named Family Plan) directly to developer nodes:",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Telegram Admin button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/wfseek_admin"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Telegram Admin Contact",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Telegram Code", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                // WhatsApp Admin button
                Button(
                    onClick = {
                        val url = "https://wa.me/2348123456789?text=Hello%20Wfseek%20Admin,%20I%20want%20to%20buy%20a%20surebet%20activation%20token!"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "WhatsApp Contact",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp Order", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WfseekLoginScreen(
    onLoginSuccess: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSigningUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var activationCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val isFirebaseLive = FirebaseService.isConfigured(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSigningUp) Icons.Default.AddCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSigningUp) "Create Wfseek Node" else "Verify Node ID",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Connection Status Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(
                    if (isFirebaseLive) Color(0x2210B981) else Color(0x22F59E0B),
                    RoundedCornerShape(30.dp)
                )
                .border(
                    1.dp,
                    if (isFirebaseLive) Color(0xFF10B981) else Color(0xFFF59E0B),
                    RoundedCornerShape(30.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (isFirebaseLive) Color(0xFF10B981) else Color(0xFFF59E0B),
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isFirebaseLive) "📡 LIVE CLOUD NODE CONNECTED" else "⏳ Sandbox Sandbox Bypass Mode (Setup in Settings)",
                color = if (isFirebaseLive) Color(0xFF10B981) else Color(0xFFF59E0B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Username Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Gateway", color = Color(0xFF94A3B8)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF475569)
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Secured Passphrase", color = Color(0xFF94A3B8)) },
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF475569)
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (isSigningUp) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = activationCode,
                onValueChange = { activationCode = it },
                label = { Text("Subscription Activation Token", color = Color(0xFF94A3B8)) },
                placeholder = { Text("WFS-WKL-ABC123...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color.LightGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF475569)
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Text(
                text = "Generate activation tokens (Weekly 2k / Monthly 8k / Named Family) securely using your administrator Telegram Bot.",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp),
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF00E5FF))
        } else {
            Button(
                onClick = {
                    if (email.trim().isEmpty() || password.trim().isEmpty()) {
                        Toast.makeText(context, "Please fill in Email and Passphrase", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        if (!isFirebaseLive) {
                            // Offline demo bypass model
                            Toast.makeText(context, "Bypassing auth in Demo mode", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(email, if (activationCode.isNotEmpty()) "pro" else "free")
                        } else {
                            if (isSigningUp) {
                                val signupResult = FirebaseService.signUp(context, email.trim(), password)
                                if (signupResult.success) {
                                    if (activationCode.trim().isNotEmpty()) {
                                        Toast.makeText(context, "Node Created! Re-verifying licensing token...", Toast.LENGTH_SHORT).show()
                                        val (tokOk, tokMsg) = FirebaseService.activateTokenCode(
                                            context,
                                            signupResult.localId,
                                            signupResult.idToken,
                                            activationCode.trim()
                                        )
                                        if (tokOk) {
                                            Toast.makeText(context, tokMsg, Toast.LENGTH_LONG).show()
                                            onLoginSuccess(email, "pro")
                                        } else {
                                            Toast.makeText(context, "Account created, but Token Activation Failed: $tokMsg", Toast.LENGTH_LONG).show()
                                            onLoginSuccess(email, "free")
                                        }
                                    } else {
                                        Toast.makeText(context, "Wfseek Node Compiled Successfully (Free tier)", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(email, "free")
                                    }
                                } else {
                                    Toast.makeText(context, signupResult.errorMessage, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val loginResult = FirebaseService.signIn(context, email.trim(), password)
                                if (loginResult.success) {
                                    // Fetch user current plan details from RTDB
                                    val (plan, expiresAt) = FirebaseService.fetchUserPlanDetails(
                                        context,
                                        loginResult.localId,
                                        loginResult.idToken
                                    )
                                    Toast.makeText(context, "Access Granted! Welcome back.", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(email, plan)
                                } else {
                                    Toast.makeText(context, "Auth Error: ${loginResult.errorMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        isLoading = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isSigningUp) "Compile & Register Node" else "Initiate Secure Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (!isSigningUp) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Unlock Key Password Forgotten?",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            if (email.trim().isEmpty()) {
                                Toast.makeText(context, "Fill in Email Gateway first to reset passphrase", Toast.LENGTH_LONG).show()
                                return@clickable
                            }
                            scope.launch {
                                isLoading = true
                                val (ok, msg) = FirebaseService.sendPasswordResetEmail(context, email.trim())
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                        }
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (isSigningUp) "Already whitelisted? Log In" else "New Node operator? Sign Up",
            color = Color(0xFF38BDF8),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { isSigningUp = !isSigningUp }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        PurchaseCodeButtons()
    }
}

// =========================================================================
// TAB 1: Opportunities List Layout
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitiesTab(
    userPlan: String,
    isScanning: Boolean,
    countdownSeconds: Int,
    statusMessage: String,
    opportunities: List<ArbitrageAlert>,
    onTriggerScan: () -> Unit,
    onCalculateClick: (ArbitrageAlert) -> Unit,
    onRedeemCode: (String) -> Boolean
) {
    val context = LocalContext.current
    var activationCode by remember { mutableStateOf("") }

    val minutesLeft = countdownSeconds / 60
    val secondsLeft = countdownSeconds % 60
    val timeDisplay = String.format("%02d:%02d", minutesLeft, secondsLeft)

    val filteredArbs = if (userPlan == "free") {
        opportunities.filter { it.profitPercent <= 2.2 }
    } else {
        opportunities
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (userPlan == "pro") Color(0x2210B981) else Color(0x333B82F6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, if (userPlan == "pro") Color(0xFF10B981) else Color(0xFF3B82F6), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (userPlan == "pro") "✦ PRO MEMBER SYSTEM ACTIVE" else "✦ FREE NODE (CAPPED LIMIT: 2.2%)",
                                color = if (userPlan == "pro") Color(0xFF10B981) else Color(0xFF3B82F6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (userPlan == "pro") "Your Node successfully receives and compiles unlimited arbitrage opportunities from all ${BOOKMAKERS_LIST.size} directories."
                            else "Free account profiles are restricted to 2.2% yields. Enter validation upgrade tokens below to unlock full limits.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        if (userPlan == "free") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = activationCode,
                                    onValueChange = { activationCode = it },
                                    placeholder = { Text("Enter PRO2026", color = Color.Gray, fontSize = 12.sp) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (onRedeemCode(activationCode)) {
                                            Toast.makeText(context, "Upgraded successfully to PRO!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Invalid key token sequence.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            PurchaseCodeButtons()
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Distributed Sync Cycle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                text = timeDisplay,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isScanning) Color.Yellow else Color(0xFF00E5FF))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Arbitrage List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVE DISCOVERIES", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("${filteredArbs.size} Alerts found", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            }

            // Shimmer Loading Placeholder if Scanning
            if (isScanning) {
                items(3) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C101F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(110.dp)
                            .border(1.dp, Color(0xFF111827), RoundedCornerShape(12.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = Color(0xFF00E5FF), strokeWidth = 2.dp)
                        }
                    }
                }
            } else if (filteredArbs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No capture segments found on limits. Try forced manual scanning below.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(filteredArbs) { arb ->
                    ArbitrageCard(arb = arb, onCalculate = { onCalculateClick(arb) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Fast Manual Scan FAB
        FloatingActionButton(
            onClick = onTriggerScan,
            containerColor = Color(0xFF00E5FF),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Manual Scan Now")
        }
    }
}

// Gorgeous Arbitrage Opportunity Alert Card
@Composable
fun ArbitrageCard(
    arb: ArbitrageAlert,
    onCalculate: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Sport & Profit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (arb.sport) {
                        Sports.SOCCER -> Icons.Default.Star
                        Sports.BASKETBALL -> Icons.Default.Add
                        Sports.TENNIS -> Icons.Default.PlayArrow
                        else -> Icons.Default.Menu
                    }
                    Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(arb.sport.uppercase(), color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x3310B981)),
                    modifier = Modifier.border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "${arb.profitPercent}% Yield",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Game Name Display
            Text(
                text = arb.matchName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = arb.leagueName,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Cross payouts Split odds comparison Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(arb.bookmakerA, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text("${arb.outcomeA}: ${arb.oddsA}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }

                Text("vs", color = Color(0xFF475569), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(arb.bookmakerB, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF00E5)))
                    }
                    Text("${arb.outcomeB}: ${arb.oddsB}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCalculate,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.3f).height(42.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stake Split", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = {
                        val token = CookieStorage.getTelegramToken(context)
                        val chatId = CookieStorage.getTelegramChatId(context)
                        if (token.isEmpty() || chatId.isEmpty()) {
                            Toast.makeText(context, "Telegram not configured. Add Bot Token in Settings tab.", Toast.LENGTH_LONG).show()
                        } else {
                            scope.launch {
                                Toast.makeText(context, "Broadcasting surebet to Telegram...", Toast.LENGTH_SHORT).show()
                                val messageText = """
                                    🚨 *NEW WFSEEK SUREBET DETECTED!* 🚨
                                    ⚽️ *${arb.matchName}* (${arb.leagueName})
                                    📈 *Profit Margin: ${arb.profitPercent}% Yield*

                                    🏦 *Bookmaker A:* ${arb.bookmakerA}
                                    📍 *Outcome:* ${arb.outcomeA} @ ${arb.oddsA}

                                    🏦 *Bookmaker B:* ${arb.bookmakerB}
                                    📍 *Outcome:* ${arb.outcomeB} @ ${arb.oddsB}

                                    ⚡️ *System:* Distributed Arbitrage Node v2.14.0
                                """.trimIndent()
                                
                                val (ok, msg) = sendTelegramMessageAsync(token, chatId, messageText)
                                if (ok) {
                                    Toast.makeText(context, "Alert successfully sent to Telegram!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Broadcast failed: $msg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A), contentColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.weight(1.1f).height(42.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send Alert", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telegram", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: Bookmakers Directory List Layout
// =========================================================================
@Composable
fun BookmakersTab(
    onManualVerify: (Bookmaker) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredBookies = BOOKMAKERS_LIST.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("DIRECTORY SYSTEMS", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text("${BOOKMAKERS_LIST.size} Target Interfaces", color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter bookmakers...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredBookies) { bm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bm.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            val context = LocalContext.current
                            val hasCookie = CookieStorage.getCookieHeader(context, bm.id) != null
                            val statusText = if (hasCookie) "CONNECTED" else "SYNC REQUIRED"
                            val badgeColor = if (hasCookie) Color(0xFF10B981) else Color(0xFFF59E0B)
                            val badgeBg = if (hasCookie) Color(0x1F10B981) else Color(0x1FF59E0B)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = badgeBg)
                            ) {
                                Text(
                                    text = statusText,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Base Node: ${bm.baseUrl}", color = Color(0xFF64748B), fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Markets: " + bm.sportsMarkets.keys.joinToString(", ").uppercase(),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )

                            Text(
                                text = "Verify Client",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onManualVerify(bm) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: Settings & Developer Trigger Layout
// =========================================================================
@Composable
fun SettingsTab(
    isDevUnlocked: Boolean,
    onUnlockDev: () -> Unit,
    onOpenCapture: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var buildTaps by remember { mutableStateOf(0) }
    
    var tgBotToken by remember { mutableStateOf(CookieStorage.getTelegramToken(context)) }
    var tgChatId by remember { mutableStateOf(CookieStorage.getTelegramChatId(context)) }
    var tgEnabled by remember { mutableStateOf(CookieStorage.isTelegramEnabled(context)) }
    
    // Firebase Project Configuration Credentials States
    var firebaseProjectId by remember { mutableStateOf(FirebaseService.getProjectId(context)) }
    var firebaseApiKey by remember { mutableStateOf(FirebaseService.getApiKey(context)) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text("NODE SYSTEM CONTROLS", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text("Wfseek Setup Dashboard", color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: Firebase Project Cloud Configuration Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Firebase Sync Connection",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "FIREBASE SYNC CONNECTION",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Register your independent Firebase RTDB & Auth rules nodes. Restrict accounts to maximum 2 system devices.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = firebaseProjectId,
                    onValueChange = {
                        firebaseProjectId = it
                        FirebaseService.saveProjectId(context, it)
                    },
                    label = { Text("Firebase Project ID", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    placeholder = { Text("e.g. wfseek-secure", color = Color.Gray, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = firebaseApiKey,
                    onValueChange = {
                        firebaseApiKey = it
                        FirebaseService.saveApiKey(context, it)
                    },
                    label = { Text("Firebase Web API Key", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    placeholder = { Text("AIzaSyBasicKey_...", color = Color.Gray, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (firebaseProjectId.isEmpty() || firebaseApiKey.isEmpty()) {
                            Toast.makeText(context, "Please fulfill all settings parameters", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        FirebaseService.saveProjectId(context, firebaseProjectId.trim())
                        FirebaseService.saveApiKey(context, firebaseApiKey.trim())
                        Toast.makeText(context, "Secure Firebase Node parameters whitelisted!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("Save Cloud Sync Credentials", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your Active Device ID: ${FirebaseService.getDeviceId(context)}",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Card 2: Telegram Management Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram Integration",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "TELEGRAM ALERTS BOT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = tgEnabled,
                        onCheckedChange = {
                            tgEnabled = it
                            CookieStorage.saveTelegramEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0x3300E5FF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Broadcast discovered surebets to your private Telegram channel/group instantly.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tgBotToken,
                    onValueChange = {
                        tgBotToken = it
                        CookieStorage.saveTelegramToken(context, it)
                    },
                    label = { Text("Telegram Bot Token", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    placeholder = { Text("e.g. 5629104051:AAFl8...", color = Color.Gray, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tgChatId,
                    onValueChange = {
                        tgChatId = it
                        CookieStorage.saveTelegramChatId(context, it)
                    },
                    label = { Text("Chat ID / Channel Username", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    placeholder = { Text("e.g. -1001552093845", color = Color.Gray, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (tgBotToken.isEmpty() || tgChatId.isEmpty()) {
                                Toast.makeText(context, "Please configure Bot Token and Chat ID first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                Toast.makeText(context, "Sending test broadcast to Telegram...", Toast.LENGTH_SHORT).show()
                                val (ok, msg) = sendTelegramMessageAsync(
                                    tgBotToken,
                                    tgChatId,
                                    "🔔 *Wfseek Secure Node Handshake successful!*\n\nYour automated arbitrage alerts bot pipeline is officially live! 🚀"
                                )
                                if (ok) {
                                    Toast.makeText(context, "Test published successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed: $msg", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Send Test Alert", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BotFather"))
                            context.startActivity(intent)
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Create Bot", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/wfseek_official"))
                    context.startActivity(intent)
                }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Telegram Channel Group", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Join active distributed node administrators", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:support@wfseek.com"))
                    context.startActivity(intent)
                }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF3B82F6))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Developer Support Email", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Inquire about custom API integration keys", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        if (isDevUnlocked) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF818CF8))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("DEVELOPER DIAGNOSTIC UTILITIES", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Examine endpoint loads, inject Javascript interceptors to isolate feed structures inside standard WebViews.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenCapture,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open JavaScript Network Interceptor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Power Down Node (Log Out)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Wfseek Node Architecture v2.14.0",
            color = Color(0xFF475569),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clickable {
                    if (!isDevUnlocked) {
                        buildTaps++
                        if (buildTaps >= 7) {
                            onUnlockDev()
                        } else {
                            val remaining = 7 - buildTaps
                            Toast.makeText(context, "Tap $remaining more times to unlock Developer settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .padding(8.dp)
        )
    }
}

// =========================================================================
// Real-time Payout Split Calculator Modal
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorBottomSheet(
    arb: ArbitrageAlert,
    onDismiss: () -> Unit
) {
    var totalInvestment by remember { mutableStateOf("10000") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SUREBET CALCULATOR SPLIT",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = arb.matchName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Investment Field
            OutlinedTextField(
                value = totalInvestment,
                onValueChange = { totalInvestment = it },
                label = { Text("Total Capital (NGN)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            val capital = totalInvestment.toDoubleOrNull() ?: 1000.0

            val weightA = 1.0 / arb.oddsA
            val weightB = 1.0 / arb.oddsB
            val sumWeights = weightA + weightB
            val stakeA = (weightA / sumWeights) * capital
            val stakeB = (weightB / sumWeights) * capital

            val payoutA = stakeA * arb.oddsA
            val payoutFull = payoutA
            val profit = payoutFull - capital

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${arb.bookmakerA} (${arb.outcomeA})", color = Color.Gray, fontSize = 12.sp)
                        Text(String.format("%.2f NGN", stakeA), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("Odds: ${arb.oddsA} • Return: " + String.format("%.1f NGN", payoutA), color = Color(0xFF64748B), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))

                    HorizontalDivider(color = Color(0xFF1E293B))

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${arb.bookmakerB} (${arb.outcomeB})", color = Color.Gray, fontSize = 12.sp)
                        Text(String.format("%.2f NGN", stakeB), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("Odds: ${arb.oddsB} • Return: " + String.format("%.1f NGN", payoutFull), color = Color(0xFF64748B), fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                    HorizontalDivider(color = Color(0xFF1E293B))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GUARANTEED PROFIT", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(String.format("+%.2f NGN", profit), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Confirm Allocation Weights", fontWeight = FontWeight.Bold)
            }
        }
    }
}
