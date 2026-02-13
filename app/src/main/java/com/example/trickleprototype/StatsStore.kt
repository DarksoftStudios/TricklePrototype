package com.example.trickleprototype

import android.content.Context

data class PlayerStats(
    var totalGames: Int = 0,
    var totalWins: Int = 0,

    // Can grow large over time -> keep Long to be safe
    var totalMarblesAcrossGames: Long = 0L,

    var totalGuesses: Int = 0,
    var correctGuesses: Int = 0,
    var timesTrickedByZero: Int = 0,
    var perfectGames: Int = 0,

    var easyGames: Int = 0,
    var normalGames: Int = 0,
    var hardGames: Int = 0,

    var easyWins: Int = 0,
    var normalWins: Int = 0,
    var hardWins: Int = 0
)

/**
 * Simple persistent stats store using SharedPreferences.
 */
class StatsStore(context: Context) {
    private val prefs = context.getSharedPreferences("trickle_stats", Context.MODE_PRIVATE)

    fun load(): PlayerStats {
        return PlayerStats(
            totalGames = prefs.getInt("totalGames", 0),
            totalWins = prefs.getInt("totalWins", 0),
            totalMarblesAcrossGames = prefs.getLong("totalMarblesAcrossGames", 0L),

            totalGuesses = prefs.getInt("totalGuesses", 0),
            correctGuesses = prefs.getInt("correctGuesses", 0),
            timesTrickedByZero = prefs.getInt("timesTrickedByZero", 0),
            perfectGames = prefs.getInt("perfectGames", 0),

            easyGames = prefs.getInt("easyGames", 0),
            normalGames = prefs.getInt("normalGames", 0),
            hardGames = prefs.getInt("hardGames", 0),

            easyWins = prefs.getInt("easyWins", 0),
            normalWins = prefs.getInt("normalWins", 0),
            hardWins = prefs.getInt("hardWins", 0)
        )
    }

    fun save(stats: PlayerStats) {
        prefs.edit()
            .putInt("totalGames", stats.totalGames)
            .putInt("totalWins", stats.totalWins)
            .putLong("totalMarblesAcrossGames", stats.totalMarblesAcrossGames)

            .putInt("totalGuesses", stats.totalGuesses)
            .putInt("correctGuesses", stats.correctGuesses)
            .putInt("timesTrickedByZero", stats.timesTrickedByZero)
            .putInt("perfectGames", stats.perfectGames)

            .putInt("easyGames", stats.easyGames)
            .putInt("normalGames", stats.normalGames)
            .putInt("hardGames", stats.hardGames)

            .putInt("easyWins", stats.easyWins)
            .putInt("normalWins", stats.normalWins)
            .putInt("hardWins", stats.hardWins)
            .apply()
    }
}
