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
    var won1113thGame: Boolean = false,

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
    var reachedRound6: Boolean = false,
    var wonWith18Marbles: Boolean = false,

    // Difficulty-specific
    var wonEasy: Boolean = false,
    var wonNormal: Boolean = false,
    var wonHard: Boolean = false,
    var playedAllDifficulties: Boolean = false,  // Tourist

    // Zero chain
    var firstTheFool: Boolean = false,         // guessed wrong on bot's zero
    var firstZeroTrap: Boolean = false,         // tricked bot with your zero
    var zeroHeroUnlocked: Boolean = false,      // both above true

    // Variety/Style
    var pacifistWin: Boolean = false,           // win without guessing/targeting
    var pacifistGame: Boolean = false,          // COMPLETE a game without guessing/targeting (new)
    var shakespeareWin: Boolean = false,        // won after correctly guessing both R&J
    var justPressEverything: Boolean = false,// won using every action type at least once

    // === NEW ACHIEVEMENTS (you approved these) ===
    var drySeasonWin: Boolean = false,          // win without ever choosing 3
    var ghostCupWin: Boolean = false,           // win without being targeted
    var onARoll: Boolean = false,               // 3 correct guesses in a row (in one game)
    var dumbLuck: Boolean = false,              // correctly guess a 3 in round 1
    var hatFinisher: Boolean = false,           // win in a round where you start because you have the Hat
    var caughtTheStrobe: Boolean = false,       // correctly guess Strobe's 3 twice in one game
    var pushover: Boolean = false,              // correctly guess the Three-Pusher's 3 four times in one game

    // Weather achievements
    var unlockedWeatherAchievements: Set<String> = emptySet(),
    var seenWeatherIds: Set<String> = emptySet(),
    var stormChaser: Boolean = false
)

data class WeatherAchievementDef(
    val id: String,
    val title: String,
    val desc: String
)

object WeatherAchievements {
    val perCard: List<WeatherAchievementDef> = listOf(
        WeatherAchievementDef("drizzle_light_rain", "Light Rain", "Complete a Drizzle round"),
        WeatherAchievementDef("downpour_soaking_it_in", "Soaking It In", "Score with a 1 during Downpour"),
        WeatherAchievementDef("fog_hidden_in_plain_sight", "Hidden in Plain Sight", "Be revealed early and protected during Fog"),
        WeatherAchievementDef("sunny_day_bright_strategy", "Bright Strategy", "Score with a 3 during Sunny Day"),
        WeatherAchievementDef("low_pressure_set_the_pressure", "Set the Pressure", "Be the first guesser during Low Pressure"),
        WeatherAchievementDef("windshear_against_the_wind", "Against the Wind", "Be the first guesser during Windshear"),
        WeatherAchievementDef("static_charge_first_strike", "First Strike", "Be the first guesser during Static Charge"),
        WeatherAchievementDef("crosswinds_two_birds_one_guess", "Two Birds, One Guess", "Correctly guess both targets during Crosswinds"),
        WeatherAchievementDef("sleet_cold_exchange", "Cold Exchange", "Gain marbles from another player during Sleet"),
        WeatherAchievementDef("thunderstorm_shock_therapy", "Shock Therapy", "Be hit by a failed guess on a 0 during Thunderstorm"),
        WeatherAchievementDef("drought_dry_spell", "Dry Spell", "Acquire the Hat during Drought"),
        WeatherAchievementDef("tornado_eye_of_the_storm", "Eye of the Storm", "Successfully attack during Tornado"),
        WeatherAchievementDef("hail_ice_storm", "Ice Storm", "Score with a 3 during Hail"),
        WeatherAchievementDef("hurricane_storm_surge", "Storm Surge", "Score with a 0 during Hurricane"),
        WeatherAchievementDef("rainbow_silver_lining", "Silver Lining", "Gain points from a failed guess during Rainbow"),
        WeatherAchievementDef("perfect_storm_twelve", "Perfect Storm", "Score 12 points in one round during Perfect Storm"),
        WeatherAchievementDef("lightning_storm_strike_twice", "Strike Twice", "Gain points from a failed guess during Lightning Storm"),
        WeatherAchievementDef("heat_mirage_what_just_happened", "What Just Happened?", "Score after your number rotates during Heat Mirage"),
        WeatherAchievementDef("smog_hidden_moves", "Hidden Moves", "Gain marbles from a trickle obscured by Smog"),
        WeatherAchievementDef("high_pressure_one_shot", "One Shot", "Gain your single allowed positive score during High Pressure"),
        WeatherAchievementDef("stormfront_cut_off", "Cut Off", "Trigger the target limit during Stormfront"),
        WeatherAchievementDef("cold_rain_shared_storm", "Shared Storm", "Receive marbles from the Cold Rain redistribution"),
        WeatherAchievementDef("thunderhead_top_of_the_storm", "Top of the Storm", "Be among the top scorers during Thunderhead"),
        WeatherAchievementDef("cool_breeze_quiet_advantage", "Quiet Advantage", "Be among the lowest positive scorers during Cool Breeze")
    )

    const val STORM_CHASER_ID = "storm_chaser"

    val allWeatherIds: Set<String> =
        Weather.allCards.filter { it.enabled && it.includedInDeck }.map { it.id }.toSet()

    fun unlocked(ids: Set<String>, defId: String): Boolean = ids.contains(defId)
}

/**
 * Simple persistent stats store using SharedPreferences.
 */
class StatsStore(context: Context) {
    private val prefs = context.getSharedPreferences("trickle_stats", Context.MODE_PRIVATE)


    private companion object {
        const val KEY_PLAYER_NAME = "player_name"
    }

    fun getPlayerName(): String {
        val raw = prefs.getString(KEY_PLAYER_NAME, null)?.trim().orEmpty()
        return if (raw.isBlank()) "You" else raw
    }

    fun setPlayerName(name: String) {
        val cleaned = name.trim()
        prefs.edit().putString(KEY_PLAYER_NAME, cleaned).apply()
    }

    fun resetAll() {
        // Clears stats + any saved name (name will fall back to "You")
        prefs.edit().clear().apply()
    }

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
            won1113thGame = prefs.getBoolean("won1113thGame", false),

            played13Games = prefs.getBoolean("played13Games", false),
            played113Games = prefs.getBoolean("played113Games", false),
            played1113Games = prefs.getBoolean("played1113Games", false),

            has113MarblesTotal = prefs.getBoolean("has113MarblesTotal", false),
            has1113MarblesTotal = prefs.getBoolean("has1113MarblesTotal", false),
            has11113MarblesTotal = prefs.getBoolean("has11113MarblesTotal", false),

            firstPerfectWin = prefs.getBoolean("firstPerfectWin", false),
            reachedRound6 = prefs.getBoolean("reachedRound6", false),
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
            justPressEverything = prefs.getBoolean("justPressEverything", false),

            // New achievements
            drySeasonWin = prefs.getBoolean("drySeasonWin", false),
            ghostCupWin = prefs.getBoolean("ghostCupWin", false),
            onARoll = prefs.getBoolean("onARoll", false),
            dumbLuck = prefs.getBoolean("dumbLuck", false),
            hatFinisher = prefs.getBoolean("hatFinisher", false),
            caughtTheStrobe = prefs.getBoolean("caughtTheStrobe", false),
            pushover = prefs.getBoolean("pushover", false),

            unlockedWeatherAchievements = prefs.getStringSet("unlockedWeatherAchievements", emptySet())?.toSet() ?: emptySet(),
            seenWeatherIds = prefs.getStringSet("seenWeatherIds", emptySet())?.toSet() ?: emptySet(),
            stormChaser = prefs.getBoolean("stormChaser", false),
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
            .putBoolean("won1113thGame", stats.won1113thGame)

            .putBoolean("played13Games", stats.played13Games)
            .putBoolean("played113Games", stats.played113Games)
            .putBoolean("played1113Games", stats.played1113Games)

            .putBoolean("has113MarblesTotal", stats.has113MarblesTotal)
            .putBoolean("has1113MarblesTotal", stats.has1113MarblesTotal)
            .putBoolean("has11113MarblesTotal", stats.has11113MarblesTotal)

            .putBoolean("firstPerfectWin", stats.firstPerfectWin)
            .putBoolean("reachedRound6", stats.reachedRound6)
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
            .putBoolean("justPressEverything", stats.justPressEverything)

            // New achievements
            .putBoolean("drySeasonWin", stats.drySeasonWin)
            .putBoolean("ghostCupWin", stats.ghostCupWin)
            .putBoolean("onARoll", stats.onARoll)
            .putBoolean("dumbLuck", stats.dumbLuck)
            .putBoolean("hatFinisher", stats.hatFinisher)
            .putBoolean("caughtTheStrobe", stats.caughtTheStrobe)
            .putBoolean("pushover", stats.pushover)

            .putStringSet("unlockedWeatherAchievements", stats.unlockedWeatherAchievements)
            .putStringSet("seenWeatherIds", stats.seenWeatherIds)
            .putBoolean("stormChaser", stats.stormChaser)

            .apply()
    }
}