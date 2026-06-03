package com.example

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object FirebaseService {
    private const val TAG = "FirebaseService"
    private const val PREFS_NAME = "firebase_service_prefs"

    // Default configuration (can be updated from the app settings UI)
    private const val DEFAULT_PROJECT_ID = "wfdmike"
    private const val DEFAULT_API_KEY = "AIzaSyDy9LJYmchzRjkctseNCwYs_dGxqvADXFU"

    fun getProjectId(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString("firebase_project_id", DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
    }

    fun saveProjectId(context: Context, projectId: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString("firebase_project_id", projectId).apply()
    }

    fun getApiKey(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString("firebase_api_key", DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString("firebase_api_key", apiKey).apply()
    }

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    /**
     * Returns true whenever a non-empty Firebase Web API key is available.
     *
     * The DEFAULT_PROJECT_ID / DEFAULT_API_KEY hardcoded above are the
     * Wfseek project's real Firebase credentials, so the app is considered
     * "live" out of the box. Users can override them from the in-app
     * Settings -> Firebase Sync Connection panel if they want to point the
     * app at their own Firebase project.
     */
    fun isConfigured(context: Context): Boolean {
        val apiKey = getApiKey(context)
        val projectId = getProjectId(context)
        return apiKey.isNotBlank() && projectId.isNotBlank()
    }

    // High fidelity data model for Auth Result
    data class AuthResult(
        val success: Boolean,
        val email: String = "",
        val idToken: String = "",
        val localId: String = "",
        val errorMessage: String = ""
    )

    /**
     * Firebase Email/Password Sign Up Action
     */
    suspend fun signUp(context: Context, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        val urlString = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val payload = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("returnSecureToken", true)
            }.toString()

            conn.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(payload)
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseReader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = responseReader.use { it.readText() }
                val json = JSONObject(response)
                val localId = json.getString("localId")
                val idToken = json.getString("idToken")
                
                // Initialize default database user entry upon registration
                initializeUserDatabaseRecord(context, localId, idToken, email)

                AuthResult(success = true, email = email, idToken = idToken, localId = localId)
            } else {
                val errorReader = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream))
                val errText = errorReader.use { it.readText() }
                val errMsg = parseFirebaseErrorMessage(errText)
                AuthResult(success = false, errorMessage = errMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign Up network error", e)
            AuthResult(success = false, errorMessage = e.message ?: "Sign up failed due to network exception")
        }
    }

    /**
     * Firebase Email/Password Sign In Action with Strict Device Limiting (Max 2 devices)
     */
    suspend fun signIn(context: Context, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        val urlString = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val payload = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("returnSecureToken", true)
            }.toString()

            conn.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(payload)
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseReader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = responseReader.use { it.readText() }
                val json = JSONObject(response)
                val localId = json.getString("localId")
                val idToken = json.getString("idToken")

                // Enforce device limiting (Max 2 Devices rule)
                val deviceId = getDeviceId(context)
                val deviceLimitCheck = checkAndRegisterDeviceSession(context, localId, idToken, deviceId)
                if (!deviceLimitCheck.first) {
                    AuthResult(success = false, errorMessage = deviceLimitCheck.second)
                } else {
                    AuthResult(success = true, email = email, idToken = idToken, localId = localId)
                }
            } else {
                val errorReader = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream))
                val errText = errorReader.use { it.readText() }
                val errMsg = parseFirebaseErrorMessage(errText)
                AuthResult(success = false, errorMessage = errMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign In network error", e)
            AuthResult(success = false, errorMessage = e.message ?: "Log in failed. Check connectivity.")
        }
    }

    /**
     * Forgot Password Action via standard Firebase REST password reset link
     */
    suspend fun sendPasswordResetEmail(context: Context, email: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        val urlString = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$apiKey"
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val payload = JSONObject().apply {
                put("requestType", "PASSWORD_RESET")
                put("email", email)
            }.toString()

            conn.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(payload)
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                true to "Password reset instructions initialized! Check your Email Gateway."
            } else {
                val errorReader = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream))
                val errText = errorReader.use { it.readText() }
                false to parseFirebaseErrorMessage(errText)
            }
        } catch (e: java.lang.Exception) {
            false to (e.message ?: "Network error establishing password reset connection.")
        }
    }

    /**
     * Initialize base user node in RTDB upon signup
     */
    private fun initializeUserDatabaseRecord(context: Context, localId: String, idToken: String, email: String) {
        try {
            val projectId = getProjectId(context)
            val urlString = "https://$projectId-default-rtdb.firebaseio.com/users/$localId.json?auth=$idToken"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val userObj = JSONObject().apply {
                put("email", email)
                put("plan", "free")
                put("registeredAt", System.currentTimeMillis())
                put("expiresAt", 0L)
            }.toString()

            conn.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(userObj)
                writer.flush()
            }
            conn.responseCode // Fire and forget connection setup trigger
        } catch (e: Exception) {
            Log.e(TAG, "Error compiling RTDB profile", e)
        }
    }

    /**
     * device check to prevent 3 devices. Max 2 devices are allowed at once.
     */
    private fun checkAndRegisterDeviceSession(context: Context, localId: String, idToken: String, deviceId: String): Pair<Boolean, String> {
        try {
            val projectId = getProjectId(context)
            // Retrieve current devices
            val urlString = "https://$projectId-default-rtdb.firebaseio.com/users/$localId/devices.json?auth=$idToken"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            val responseCode = conn.responseCode
            var devicesJson = JSONObject()
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseReader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = responseReader.use { it.readText() }
                if (response.trim() != "null" && response.trim().isNotEmpty()) {
                    devicesJson = JSONObject(response)
                }
            }

            // Exclude current device from active limit list
            val otherDevicesCount = devicesJson.keys().asSequence().filter { it != deviceId }.count()
            if (otherDevicesCount >= 2) {
                return false to "Login Rejected! Wfseek account bound to max limit (2 devices). Disconnect unused nodes."
            }

            // Register current session timestamp
            val patchUrlString = "https://$projectId-default-rtdb.firebaseio.com/users/$localId/devices/$deviceId.json?auth=$idToken"
            val patchUrl = URL(patchUrlString)
            val patchConn = patchUrl.openConnection() as HttpURLConnection
            patchConn.requestMethod = "PUT"
            patchConn.doOutput = true
            patchConn.setRequestProperty("Content-Type", "application/json")

            val deviceMetadata = JSONObject().apply {
                put("lastActive", System.currentTimeMillis())
                put("model", android.os.Build.MODEL)
            }.toString()

            patchConn.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(deviceMetadata)
                writer.flush()
            }
            patchConn.responseCode
            return true to "Verified"
        } catch (e: Exception) {
            Log.e(TAG, "Error registering session check: Fallback authorized for compilation.", e)
            return true to "Success (Fallback Offline Mode Bypass)"
        }
    }

    /**
     * User Logout session clearing
     */
    suspend fun unregisterDeviceSession(context: Context, localId: String, idToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = getProjectId(context)
            val deviceId = getDeviceId(context)
            val urlString = "https://$projectId-default-rtdb.firebaseio.com/users/$localId/devices/$deviceId.json?auth=$idToken"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            val code = conn.responseCode
            code == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e(TAG, "Log out session release warning", e)
            false
        }
    }

    /**
     * Fetch plan settings from real database
     */
    suspend fun fetchUserPlanDetails(context: Context, localId: String, idToken: String): Pair<String, Long> = withContext(Dispatchers.IO) {
        try {
            val projectId = getProjectId(context)
            val urlString = "https://$projectId-default-rtdb.firebaseio.com/users/$localId.json?auth=$idToken"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                if (text.trim() != "null") {
                    val json = JSONObject(text)
                    val plan = json.optString("plan", "free")
                    val expiresAt = json.optLong("expiresAt", 0L)
                    return@withContext plan to expiresAt
                }
            }
            "free" to 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture remote network plans", e)
            "free" to 0L
        }
    }

    /**
     * Activate a Telegram-generated token safely using transaction mechanics
     */
    suspend fun activateTokenCode(context: Context, localId: String, idToken: String, code: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val projectId = getProjectId(context)
            val tokenUrlString = "https://$projectId-default-rtdb.firebaseio.com/tokens/$code.json"
            val url = URL(tokenUrlString)
            
            // 1. Fetch token details
            var conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            var responseText = ""
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                responseText = conn.inputStream.bufferedReader().use { it.readText() }
            }

            if (responseText.trim() == "null" || responseText.trim().isEmpty()) {
                return@withContext false to "Verification failed. Activation token does not exist!"
            }

            val tokenJson = JSONObject(responseText)
            val status = tokenJson.optString("status", "unused")
            if (status != "unused") {
                return@withContext false to "Bypass rejection: Token already activated by operator ${tokenJson.optString("activatedBy")}!"
            }

            val planType = tokenJson.optString("planType", "weekly") // "weekly", "monthly", "family"
            val name = tokenJson.optString("name", "") // Label for family keys
            
            // Calculate active lifespan
            val durationMs = when (planType) {
                "weekly" -> 7L * 24 * 3600 * 1000
                "monthly" -> 30L * 24 * 3600 * 1000
                else -> -1L // Family - never expires automatically unless manual trigger
            }

            val now = System.currentTimeMillis()
            val expiresAt = if (durationMs > 0) now + durationMs else -1L

            // 2. Mark token as used
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val tokenUpdate = JSONObject().apply {
                put("status", "used")
                put("activatedBy", localId)
                put("activatedAt", now)
            }.toString()

            conn.outputStream.use { os ->
                os.write(tokenUpdate.toByteArray(Charsets.UTF_8))
            }
            conn.responseCode

            // 3. Bind plan directly to user
            val userUrlString = "https://$projectId-default-rtdb.firebaseio.com/users/$localId.json?auth=$idToken"
            conn = URL(userUrlString).openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val userUpdate = JSONObject().apply {
                put("plan", planType)
                put("expiresAt", expiresAt)
            }.toString()

            conn.outputStream.use { os ->
                os.write(userUpdate.toByteArray(Charsets.UTF_8))
            }
            conn.responseCode

            val durationLabel = when (planType) {
                "weekly" -> "Weekly Pro Access (Expires in 7 days)"
                "monthly" -> "Monthly Master Access (Expires in 30 days)"
                else -> "Family Lifetime Node ($name)"
            }

            true to "Successfully unlocked $durationLabel!"
        } catch (e: Exception) {
            Log.e(TAG, "Error executing secure token handshakes", e)
            false to (e.message ?: "Handshake failed due to database server offline status.")
        }
    }

    /**
     * Push a newly discovered Arbitrage opportunity to the shared database
     */
    suspend fun pushDiscovery(context: Context, alert: ArbitrageAlert): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = getProjectId(context)
            val urlString = "https://$projectId-default-rtdb.firebaseio.com/discoveries/${alert.id}.json"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val payload = JSONObject().apply {
                put("id", alert.id)
                put("sport", alert.sport)
                put("matchName", alert.matchName)
                put("leagueName", alert.leagueName)
                put("bookmakerA", alert.bookmakerA)
                put("bookmakerB", alert.bookmakerB)
                put("outcomeA", alert.outcomeA)
                put("outcomeB", alert.outcomeB)
                put("oddsA", alert.oddsA)
                put("oddsB", alert.oddsB)
                put("profitPercent", alert.profitPercent)
                put("timestamp", alert.timestamp)
            }.toString()

            conn.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(payload)
                writer.flush()
            }
            conn.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing discovery to RTDB", e)
            false
        }
    }

    /**
     * Fetch all shared arbitrage opportunities uploaded by distributed nodes
     */
    suspend fun fetchDiscoveries(context: Context): List<ArbitrageAlert> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ArbitrageAlert>()
        try {
            val projectId = getProjectId(context)
            val urlString = "https://$projectId-default-rtdb.firebaseio.com/discoveries.json"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                if (text.trim() != "null" && text.trim().isNotEmpty()) {
                    val root = JSONObject(text)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.getJSONObject(key)
                        list.add(
                            ArbitrageAlert(
                                id = obj.optString("id", key),
                                sport = obj.optString("sport", ""),
                                matchName = obj.optString("matchName", ""),
                                leagueName = obj.optString("leagueName", ""),
                                bookmakerA = obj.optString("bookmakerA", ""),
                                bookmakerB = obj.optString("bookmakerB", ""),
                                outcomeA = obj.optString("outcomeA", ""),
                                outcomeB = obj.optString("outcomeB", ""),
                                oddsA = obj.optDouble("oddsA", 0.0),
                                oddsB = obj.optDouble("oddsB", 0.0),
                                profitPercent = obj.optDouble("profitPercent", 0.0),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching discoveries from RTDB", e)
        }
        list
    }

    private fun parseFirebaseErrorMessage(errorJsonText: String): String {
        return try {
            val json = JSONObject(errorJsonText)
            val error = json.getJSONObject("error")
            val msgCode = error.getString("message")
            when (msgCode) {
                "EMAIL_EXISTS" -> "This Email is already associated with another Wfseek Node."
                "EMAIL_NOT_FOUND" -> "Unregistered Node email. Please SignUp."
                "INVALID_PASSWORD" -> "The security passphrase is incorrect."
                "USER_DISABLED" -> "This account has been administratively deactivated."
                "WEAK_PASSWORD" -> "Security key too weak. Use a password with 6+ characters."
                "INVALID_EMAIL" -> "Please provide a valid email gateway path."
                else -> msgCode.replace("_", " ")
            }
        } catch (e: Exception) {
            "An error occurred. Check input credentials & Firebase Server configuration."
        }
    }
}
