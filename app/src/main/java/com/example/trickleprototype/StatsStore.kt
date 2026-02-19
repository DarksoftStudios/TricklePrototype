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
    var hardWins: Int = 0,

    // === ACHIEVEMENT FLAGS (all one-time unlocks) ===

    // First milestones
    var firstGameCompleted: Boolean = false,
    var firstWin: Boolean = false,

    // Win milestones
    var won13thGame: Boolean = false,
    var won113thGame: Boolean = false,

    // Play milestones
    var played13Games: Boolean = false,
    var played113Games: Boolean = false,
    var played1113Games: Boolean = false,

    // Marble bucket milestones
    var has113MarblesTotal: Boolean = false,
    var has1113MarblesTotal: Boolean = false,
    var has11113MarblesTotal: Boolean = false,

    // Special wins & events
    var firstPerfectWin: Boolean = false,
    var reachedRound7: Boolean = false,
    var wonWith18Marbles: Boolean = false,

    // Difficulty-specific
    var wonEasy: Boolean = false,
    var wonNormal: Boolean = false,
    var wonHard: Boolean = false,
    var playedAllDifficulties: Boolean = false,  // Tourist

    // Zero chain
    var firstTheFool: Boolean = false,          // guessed wrong on bot's zero
    var firstZeroTrap: Boolean = false,         // tricked bot with your zero
    var zeroHeroUnlocked: Boolean = false,      // both above true

    // Variety/Style
    var pacifistWin: Boolean = false,           // win without guessing/targeting
    var pacifistGame: Boolean = false,          // COMPLETE a game without guessing/targeting (new)
    var shakespeareWin: Boolean = false,        // won after correctly guessing both R&J
    var justPressEverythingWin: Boolean = false,// won using every action type at least once

    // === NEW ACHIEVEMENTS (you approved these) ===
    var drySeasonWin: Boolean = false,          // win without ever choosing 3
    var ghostCupWin: Boolean = false,           // win without being targeted
    var onARoll: Boolean = false,               // 3 correct guesses in a row (in one game)
    var dumbLuck: Boolean = false,              // correctly guess a 3 in round 1
    var hatFinisher: Boolean = false,           // win in a round where you start because you have the Hat
    var caughtTheStrobe: Boolean = false,       // correctly guess Strobe's 3 twice in one game
    var pushover: Boolean = false               // correctly guess the Three-Pusher's 3 four times in one game
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
            hardWins = prefs.getInt("hardWins", 0),

            // Achievements
            firstGameCompleted = prefs.getBoolean("firstGameCompleted", false),
            firstWin = prefs.getBoolean("firstWin", false),

            won13thGame = prefs.getBoolean("won13thGame", false),
            won113thGame = prefs.getBoolean("won113thGame", false),

            played13Games = prefs.getBoolean("played13Games", false),
            played113Games = prefs.getBoolean("played113Games", false),
            played1113Games = prefs.getBoolean("played1113Games", false),

            has113MarblesTotal = prefs.getBoolean("has113MarblesTotal", false),
            has1113MarblesTotal = prefs.getBoolean("has1113MarblesTotal", false),
            has11113MarblesTotal = prefs.getBoolean("has11113MarblesTotal", false),

            firstPerfectWin = prefs.getBoolean("firstPerfectWin", false),
            reachedRound7 = prefs.getBoolean("reachedRound7", false),
            wonWith18Marbles = prefs.getBoolean("wonWith18Marbles", false),

            wonEasy = prefs.getBoolean("wonEasy", false),
            wonNormal = prefs.getBoolean("wonNormal", false),
            wonHard = prefs.getBoolean("wonHard", false),
            playedAllDifficulties = prefs.getBoolean("playedAllDifficulties", false),

            firstTheFool = prefs.getBoolean("firstTheFool", false),
            firstZeroTrap = prefs.getBoolean("firstZeroTrap", false),
            zeroHeroUnlocked = prefs.getBoolean("zeroHeroUnlocked", false),

            pacifistWin = prefs.getBoolean("pacifistWin", false),
            pacifistGame = prefs.getBoolean("pacifistGame", false),
            shakespeareWin = prefs.getBoolean("shakespeareWin", false),
            justPressEverythingWin = prefs.getBoolean("justPressEverythingWin", false),

            // New achievements
            drySeasonWin = prefs.getBoolean("drySeasonWin", false),
            ghostCupWin = prefs.getBoolean("ghostCupWin", false),
            onARoll = prefs.getBoolean("onARoll", false),
            dumbLuck = prefs.getBoolean("dumbLuck", false),
            hatFinisher = prefs.getBoolean("hatFinisher", false),
            caughtTheStrobe = prefs.getBoolean("caughtTheStrobe", false),
            pushover = prefs.getBoolean("pushover", false),
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

            // Achievements
            .putBoolean("firstGameCompleted", stats.firstGameCompleted)
            .putBoolean("firstWin", stats.firstWin)

            .putBoolean("won13thGame", stats.won13thGame)
            .putBoolean("won113thGame", stats.won113thGame)

            .putBoolean("played13Games", stats.played13Games)
            .putBoolean("played113Games", stats.played113Games)
            .putBoolean("played1113Games", stats.played1113Games)

            .putBoolean("has113MarblesTotal", stats.has113MarblesTotal)
            .putBoolean("has1113MarblesTotal", stats.has1113MarblesTotal)
            .putBoolean("has11113MarblesTotal", stats.has11113MarblesTotal)

            .putBoolean("firstPerfectWin", stats.firstPerfectWin)
            .putBoolean("reachedRound7", stats.reachedRound7)
            .putBoolean("wonWith18Marbles", stats.wonWith18Marbles)

            .putBoolean("wonEasy", stats.wonEasy)
            .putBoolean("wonNormal", stats.wonNormal)
            .putBoolean("wonHard", stats.wonHard)
            .putBoolean("playedAllDifficulties", stats.playedAllDifficulties)

            .putBoolean("firstTheFool", stats.firstTheFool)
            .putBoolean("firstZeroTrap", stats.firstZeroTrap)
            .putBoolean("zeroHeroUnlocked", stats.zeroHeroUnlocked)

            .putBoolean("pacifistWin", stats.pacifistWin)
            .putBoolean("pacifistGame", stats.pacifistGame)
            .putBoolean("shakespeareWin", stats.shakespeareWin)
            .putBoolean("justPressEverythingWin", stats.justPressEverythingWin)

            // New achievements
            .putBoolean("drySeasonWin", stats.drySeasonWin)
            .putBoolean("ghostCupWin", stats.ghostCupWin)
            .putBoolean("onARoll", stats.onARoll)
            .putBoolean("dumbLuck", stats.dumbLuck)
            .putBoolean("hatFinisher", stats.hatFinisher)
            .putBoolean("caughtTheStrobe", stats.caughtTheStrobe)
            .putBoolean("pushover", stats.pushover)

            .apply()
    }
}
