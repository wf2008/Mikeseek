package com.example

import android.content.Context

/**
 * Data model for a Bookmaker configuration.
 */
data class Bookmaker(
    val id: String,
    val name: String,
    val baseUrl: String,
    val endpoints: List<String>,
    val isProtected: Boolean,
    val providesLeagueCategory: Boolean = true,
    val sportsMarkets: Map<String, List<String>>
)

/**
 * Represent sports and their localized tags.
 */
object Sports {
    const val SOCCER = "soccer"
    const val BASKETBALL = "basketball"
    const val TENNIS = "tennis"
    const val VOLLEYBALL = "volleyball"
    const val HOCKEY = "hockey"

    val ALL_SPORTS = listOf(SOCCER, BASKETBALL, TENNIS, VOLLEYBALL, HOCKEY)
}

/**
 * Predefined list of 30 Nigerian / major bookmakers as specified in requirements.
 */
val BOOKMAKERS_LIST = listOf(
    Bookmaker(
        id = "bet9ja",
        name = "Bet9ja",
        baseUrl = "https://web.bet9ja.com",
        endpoints = listOf("/api/odds", "/sports/soccer"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "sportybet",
        name = "SportyBet",
        baseUrl = "https://www.sportybet.com/ng/",
        endpoints = listOf("/api/v1/sports", "/market/active"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "bangbet",
        name = "Bangbet",
        baseUrl = "https://www.bangbet.com",
        endpoints = listOf("/sport/match/list"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "msport",
        name = "MSport",
        baseUrl = "https://www.msport.com/ng/",
        endpoints = listOf("/api/odds/feed"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "maxbet",
        name = "MaxBet",
        baseUrl = "https://www.maxbet.ng",
        endpoints = listOf("/odds/live"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "betpawa",
        name = "BetPawa",
        baseUrl = "https://www.betpawa.ng",
        endpoints = listOf("/api/bets/v3", "/market/all"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under")
        )
    ),
    Bookmaker(
        id = "cloudbet",
        name = "Cloudbet",
        baseUrl = "https://www.cloudbet.com",
        endpoints = listOf("/sports/feed"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "bcgame",
        name = "BC.Game",
        baseUrl = "https://bc.game",
        endpoints = listOf("/odds/football"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "nairabet",
        name = "NairaBet",
        baseUrl = "https://www.nairabet.com",
        endpoints = listOf("/api/prematch", "/api/live"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "merrybet",
        name = "Merrybet",
        baseUrl = "https://www.merrybet.com",
        endpoints = listOf("/sportsbook/sports"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "accessbet",
        name = "AccessBET",
        baseUrl = "https://www.accessbet.com",
        endpoints = listOf("/odds/prematch"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "winnerbet",
        name = "WinnerBet",
        baseUrl = "https://winnerbet.ng",
        endpoints = listOf("/sports/matches"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.VOLLEYBALL to listOf("winner")
        )
    ),
    Bookmaker(
        id = "betwinner",
        name = "BetWinner",
        baseUrl = "https://betwinner.ng",
        endpoints = listOf("/api/feed"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "22bet",
        name = "22Bet",
        baseUrl = "https://22bet.ng",
        endpoints = listOf("/sportsbook/live"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "1xbet",
        name = "1xBet",
        baseUrl = "https://1xbet.ng",
        endpoints = listOf("/api/v1/prematch"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "betking",
        name = "BetKing",
        baseUrl = "https://www.betking.com",
        endpoints = listOf("/sports/odds/direct"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "betano",
        name = "Betano",
        baseUrl = "https://www.betano.ng",
        endpoints = listOf("/api/sports/soccer/matches"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "betway",
        name = "Betway",
        baseUrl = "https://www.betway.com.ng",
        endpoints = listOf("/api/feeds/odds"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "bet365",
        name = "Bet365",
        baseUrl = "https://www.bet365.com",
        endpoints = listOf("/sports/interactive"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "livescorebet",
        name = "LiveScore Bet",
        baseUrl = "https://www.livescorebet.com/ng/",
        endpoints = listOf("/sports/livedatastream"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "betfair",
        name = "Betfair",
        baseUrl = "https://www.betfair.com",
        endpoints = listOf("/exchange/football"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "betfred",
        name = "Betfred",
        baseUrl = "https://www.betfred.com",
        endpoints = listOf("/api/prematch/odds"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "betsson",
        name = "Betsson",
        baseUrl = "https://www.betsson.com",
        endpoints = listOf("/en/sportsbook/v1"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "unibet",
        name = "Unibet",
        baseUrl = "https://www.unibet.com",
        endpoints = listOf("/sportsbook/v2"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "pinnacle",
        name = "Pinnacle",
        baseUrl = "https://www.pinnacle.com",
        endpoints = listOf("/v3/fixtures", "/v3/odds"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "williamhill",
        name = "William Hill",
        baseUrl = "https://www.williamhill.com",
        endpoints = listOf("/api/odds/feed"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline"),
            Sports.TENNIS to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "betonline",
        name = "BetOnline",
        baseUrl = "https://www.betonline.ag",
        endpoints = listOf("/sports/prematch"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "betus",
        name = "BetUS",
        baseUrl = "https://www.betus.com.pa",
        endpoints = listOf("/sportsbook/odds/json"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "betcris",
        name = "Betcris",
        baseUrl = "https://www.betcris.com",
        endpoints = listOf("/v3/live/lines"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under"),
            Sports.TENNIS to listOf("winner")
        )
    ),
    Bookmaker(
        id = "stake",
        name = "Stake",
        baseUrl = "https://stake.com",
        endpoints = listOf("/api/sportsbook/v1"),
        isProtected = true,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    ),
    Bookmaker(
        id = "onewin",
        name = "1win",
        baseUrl = "https://1win.pro",
        endpoints = listOf("/api/v1/line"),
        isProtected = false,
        sportsMarkets = mapOf(
            Sports.SOCCER to listOf("1x2", "over_under", "double_chance", "btts"),
            Sports.BASKETBALL to listOf("moneyline", "over_under"),
            Sports.TENNIS to listOf("winner"),
            Sports.VOLLEYBALL to listOf("winner"),
            Sports.HOCKEY to listOf("moneyline")
        )
    )
)

/**
 * Represent an active, real-time Arbitrage Opportunity alert.
 */
data class ArbitrageAlert(
    val id: String,
    val sport: String,
    val matchName: String,
    val leagueName: String,
    val bookmakerA: String,
    val bookmakerB: String,
    val outcomeA: String,
    val outcomeB: String,
    val oddsA: Double,
    val oddsB: Double,
    val profitPercent: Double,
    val timestamp: Long
)

/**
 * Cookie Storage simulation helper targeting Local SharedPreferences.
 */
object CookieStorage {
    private const val PREFS_NAME = "cookie_manager_prefs"

    fun saveCookie(context: Context, domain: String, cookies: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString("cookies_$domain", cookies).apply()
    }

    fun getCookieHeader(context: Context, domain: String): String? {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString("cookies_$domain", null)
    }

    fun clearCookiesForDomain(context: Context, domain: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().remove("cookies_$domain").apply()
    }

    fun saveUserAgent(context: Context, ua: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString("user_agent", ua).apply()
    }

    fun getUserAgent(context: Context): String? {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString("user_agent", null)
    }

    fun isAllCompleted(context: Context): Boolean {
        // Checking if at least some cookies are retrieved for simulated progress check
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keys = sp.all.keys
        val count = keys.count { it.startsWith("cookies_") }
        return count >= BOOKMAKERS_LIST.size
    }

    fun getCompletedCount(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.all.keys.count { it.startsWith("cookies_") }
    }

    fun saveTelegramToken(context: Context, token: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString("tg_bot_token", token).apply()
    }

    fun getTelegramToken(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString("tg_bot_token", "") ?: ""
    }

    fun saveTelegramChatId(context: Context, chatId: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString("tg_chat_id", chatId).apply()
    }

    fun getTelegramChatId(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString("tg_chat_id", "") ?: ""
    }

    fun saveTelegramEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean("tg_enabled", enabled).apply()
    }

    fun isTelegramEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean("tg_enabled", false)
    }
}

/**
 * Mock generator for Arbitrage opportunities.
 * Returns dynamic, highly realistic arbitrage math configurations.
 */
fun generateMockArbitrageAlerts(): List<ArbitrageAlert> {
    return listOf(
        ArbitrageAlert(
            id = "arb_1",
            sport = Sports.SOCCER,
            matchName = "Chelsea vs Arsenal",
            leagueName = "English Premier League",
            bookmakerA = "SportyBet",
            bookmakerB = "Bet9ja",
            outcomeA = "Home (1)",
            outcomeB = "Away or Draw (X2)",
            oddsA = 2.15,
            oddsB = 2.05,
            profitPercent = 4.88,
            timestamp = System.currentTimeMillis()
        ),
        ArbitrageAlert(
            id = "arb_2",
            sport = Sports.BASKETBALL,
            matchName = "LA Lakers vs Boston Celtics",
            leagueName = "NBA",
            bookmakerA = "BetKing",
            bookmakerB = "1xBet",
            outcomeA = "LA Lakers (ML)",
            outcomeB = "Boston Celtics (ML)",
            oddsA = 2.22,
            oddsB = 1.95,
            profitPercent = 3.83,
            timestamp = System.currentTimeMillis() - 30000
        ),
        ArbitrageAlert(
            id = "arb_3",
            sport = Sports.TENNIS,
            matchName = "Iga Swiatek vs Aryna Sabalenka",
            leagueName = "WTA French Open",
            bookmakerA = "Stake",
            bookmakerB = "Betway",
            outcomeA = "Swiatek to win",
            outcomeB = "Sabalenka to win",
            oddsA = 1.68,
            oddsB = 2.75,
            profitPercent = 4.31,
            timestamp = System.currentTimeMillis() - 120000
        ),
        ArbitrageAlert(
            id = "arb_4",
            sport = Sports.SOCCER,
            matchName = "Real Madrid vs Borussia Dortmund",
            leagueName = "UEFA Champions League",
            bookmakerA = "MSport",
            bookmakerB = "Bangbet",
            outcomeA = "Over 2.5 Goals",
            outcomeB = "Under 2.5 Goals",
            oddsA = 2.10,
            oddsB = 2.08,
            profitPercent = 4.49,
            timestamp = System.currentTimeMillis() - 300000
        ),
        ArbitrageAlert(
            id = "arb_5",
            sport = Sports.HOCKEY,
            matchName = "Edmonton Oilers vs Florida Panthers",
            leagueName = "NHL Stanley Cup",
            bookmakerA = "Pinnacle",
            bookmakerB = "Betano",
            outcomeA = "Oilers to win",
            outcomeB = "Panthers to win",
            oddsA = 2.35,
            oddsB = 1.85,
            profitPercent = 3.63,
            timestamp = System.currentTimeMillis() - 450000
        ),
        ArbitrageAlert(
            id = "arb_6",
            sport = Sports.VOLLEYBALL,
            matchName = "Poland vs Italy",
            leagueName = "FIVB Nations League",
            bookmakerA = "22Bet",
            bookmakerB = "Bet365",
            outcomeA = "Poland to win",
            outcomeB = "Italy to win",
            oddsA = 2.12,
            oddsB = 2.06,
            profitPercent = 4.45,
            timestamp = System.currentTimeMillis() - 600000
        )
    )
}

suspend fun sendTelegramMessageAsync(token: String, chatId: String, text: String): Pair<Boolean, String> {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val urlString = "https://api.telegram.org/bot${token}/sendMessage"
            val url = java.net.URL(urlString)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            
            // Safe JSON escaping in Kotlin without external libraries
            val escapedText = text.replace("\\", "\\\\")
                                  .replace("\"", "\\\"")
                                  .replace("\n", "\\n")
                                  .replace("\r", "\\r")
                                  .replace("\t", "\\t")
            
            val payload = """{"chat_id": "$chatId", "text": "$escapedText", "parse_mode": "Markdown"}"""
            
            conn.outputStream.use { os ->
                val input = payload.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                true to "Success"
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown Telegram Error"
                false to "HTTP $responseCode: $err"
            }
        } catch (e: Exception) {
            false to (e.message ?: "Network error occurred")
        }
    }
}

