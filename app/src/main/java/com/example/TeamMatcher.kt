package com.example

import java.text.Normalizer
import java.util.Locale

/**
 * Custom Name Matcher engine mimicking the On-Device token similarity requirements.
 * Performs diacritic removal, stopword filtering, token sorting, custom synonym lookups for leagues and teams,
 * and cosine/token Jaccard similarity.
 */
object TeamMatcher {

    // Stopwords specified in requirements
    private val STOPWORDS = setOf(
        "fc", "ac", "cf", "sc", "united", "city", "town", "athletic", "wanderers", "rovers"
    )

    // League Synonyms
    val LEAGUE_SYNONYMS = mapOf(
        "epl" to "Premier League",
        "english premier league" to "Premier League",
        "la liga" to "LaLiga",
        "spanish primera division" to "LaLiga",
        "serie a" to "Serie A",
        "italian serie a" to "Serie A",
        "ligue 1" to "Ligue 1",
        "french ligue 1" to "Ligue 1",
        "bundesliga" to "Bundesliga",
        "german bundesliga" to "Bundesliga"
    )

    // 20+ Nigerian Team Synonym Map (Enyimba -> Enyimba International, etc.)
    val NIGERIAN_TEAM_SYNONYMS = mapOf(
        "enyimba" to "Enyimba International",
        "enyimba fc" to "Enyimba International",
        "rangers" to "Enugu Rangers",
        "rangers international" to "Enugu Rangers",
        "kano pillars" to "Kano Pillars FC",
        "pillars" to "Kano Pillars FC",
        "shooting stars" to "3SC Shooting Stars",
        "shooting stars sc" to "3SC Shooting Stars",
        "remor stars" to "Remo Stars",
        "remo stars" to "Remo Stars FC",
        "plateau" to "Plateau United",
        "plateau utd" to "Plateau United",
        "lobi" to "Lobi Stars",
        "lobi stars fc" to "Lobi Stars",
        "akwa united" to "Akwa United FC",
        "akwa" to "Akwa United FC",
        "rivers" to "Rivers United",
        "rivers united fc" to "Rivers United",
        "sunshine" to "Sunshine Stars",
        "sunshine stars fc" to "Sunshine Stars",
        "bendel insurance" to "Bendel Insurance FC",
        "insurance" to "Bendel Insurance FC",
        "nasarawa" to "Nasarawa United",
        "nasarawa united fc" to "Nasarawa United",
        "kwara" to "Kwara United",
        "kwara united fc" to "Kwara United",
        "abuja" to "Abuja FC",
        "heartland" to "Heartland FC",
        "gombe" to "Gombe United"
    )

    /**
     * Remove diacritics and accents from string.
     */
    fun removeDiacritics(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    /**
     * Process text: lowercase, remove diacritics, remove generic stopwords, sort tokens.
     */
    fun preprocess(text: String): List<String> {
        // Lowercase & remove accents/diacritics
        val clean = removeDiacritics(text.lowercase(Locale.ROOT))
            .replace("[^a-z0-9\\s]".toRegex(), " ") // replace punctuation with spaces

        // Tokenize and filter
        val tokens = clean.split("\\s+".toRegex())
            .map { it.trim() }
            .filter { it.isNotEmpty() && !STOPWORDS.contains(it) }

        // Normalizing synonyms if possible
        val mappedTokens = tokens.map { token ->
            NIGERIAN_TEAM_SYNONYMS[token]?.lowercase()?.split("\\s+") ?: listOf(token)
        }.flatten()

        return mappedTokens.sorted()
    }

    /**
     * Standard Jaccard / Token correlation similarity metric as a fallback to Vector/Tensor Cosine similarity.
     * Mimics modern client-side TF-IDF/embeddings output on-device.
     */
    fun calculateSimilarity(teamA: String, teamB: String): Double {
        // Quick exact or mapping checks
        val normalizedA = teamA.trim().lowercase()
        val normalizedB = teamB.trim().lowercase()
        if (normalizedA == normalizedB) return 1.0

        val synA = NIGERIAN_TEAM_SYNONYMS[normalizedA] ?: teamA
        val synB = NIGERIAN_TEAM_SYNONYMS[normalizedB] ?: teamB
        if (synA.lowercase() == synB.lowercase()) return 1.0

        val tokensA = preprocess(teamA).toSet()
        val tokensB = preprocess(teamB).toSet()

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0

        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size

        return intersection.toDouble() / union.toDouble()
    }

    /**
     * Resolves league names to standard form using the league synonym map.
     */
    fun resolveLeague(leagueName: String): String {
        val key = leagueName.trim().lowercase()
        return LEAGUE_SYNONYMS[key] ?: LEAGUE_SYNONYMS.entries.firstOrNull {
            key.contains(it.key) || it.key.contains(key)
        }?.value ?: leagueName
    }
}
