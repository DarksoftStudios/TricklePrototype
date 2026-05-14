package com.example.trickleprototype

import android.content.Context

data class PlayerStats(
    var totalGames: Int = 0,
    var totalWins: Int = 0,
    // Can grow large over time -> keep Long to be safe
    var totalMarblesAcrossGames: Long = 0L,
    var lifetimeMarblesEarned: Long = 0L,
    var vaultMarbles: Long = 0L,

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
    var dieting: Boolean = false,              // correctly guess the Glutton's 3 three times in one game
    var chaosTheory: Boolean = false,          // have Chaos correctly guess your choice
    var shadowStep: Boolean = false,           // have Lurker correctly guess your 3
    var vendetta: Boolean = false,             // get targeted by Avenger and the bot being avenged
    var wrongGuyPal: Boolean = false,          // win after attacking Nemesis in round 1
    var taxPaid: Boolean = false,              // get correctly guessed by Auditor
    var anointed: Boolean = false,             // win after being selected by Cabal
    var slowAndSteady: Boolean = false,        // win after correctly guessing Limper's 1
    var firstFlood: Boolean = false,           // Scout guesses your round 1 choice and you still win
    var altComedy: Boolean = false,            // Jester guesses you wrong when you did not choose 0
    var pickOnSomeoneYourSize: Boolean = false,// witness Bully target someone for 3
    var badFaith: Boolean = false,             // guess Cynic wrong and have Cynic guess you correctly
    var karma: Boolean = false,                // lose after correctly guessing Pacifist's 3 three times
    var copycat: Boolean = false,              // lose while Echo wins
    var beatHunter: Boolean = false,           // win against Hunter
    var beatSeer: Boolean = false,             // win against Seer
    var beatMirror: Boolean = false,           // win against Mirror
    var bossSlayer: Boolean = false,           // beat every boss

    // Weather achievements
    var unlockedWeatherAchievements: Set<String> = emptySet(),
    var seenWeatherIds: Set<String> = emptySet(),
    var stormChaser: Boolean = false
)


data class ArchetypeAvatarUnlockDef(
    val label: String,
    val resourceName: String,
    val achievementUnlocked: (PlayerStats) -> Boolean
)

object ArchetypeAvatarUnlocks {
    const val COST: Long = 111L

    val all: List<ArchetypeAvatarUnlockDef> = listOf(
        ArchetypeAvatarUnlockDef("Auditor", "auditor") { it.taxPaid },
        ArchetypeAvatarUnlockDef("Avenger", "avenger") { it.vendetta },
        ArchetypeAvatarUnlockDef("Bully", "bully") { it.pickOnSomeoneYourSize },
        ArchetypeAvatarUnlockDef("Cabal", "cabal") { it.anointed },
        ArchetypeAvatarUnlockDef("Chaos", "chaos") { it.chaosTheory },
        ArchetypeAvatarUnlockDef("Cynic", "cynic") { it.badFaith },
        ArchetypeAvatarUnlockDef("Echo", "echo") { it.copycat },
        ArchetypeAvatarUnlockDef("Glutton", "glutton") { it.dieting },
        ArchetypeAvatarUnlockDef("Hunter", "hunter") { it.beatHunter },
        ArchetypeAvatarUnlockDef("Jester", "jester") { it.altComedy },
        ArchetypeAvatarUnlockDef("Juliet", "juliet") { it.shakespeareWin },
        ArchetypeAvatarUnlockDef("Limper", "limper") { it.slowAndSteady },
        ArchetypeAvatarUnlockDef("Lurker", "lurker") { it.shadowStep },
        ArchetypeAvatarUnlockDef("Mirror", "mirror") { it.beatMirror },
        ArchetypeAvatarUnlockDef("Nemesis", "nemesis") { it.wrongGuyPal },
        ArchetypeAvatarUnlockDef("Pacifist", "pacifist") { it.pacifistWin || it.pacifistGame },
        ArchetypeAvatarUnlockDef("Pitfall", "pitfall") { it.firstZeroTrap },
        ArchetypeAvatarUnlockDef("Romeo", "romeo") { it.shakespeareWin },
        ArchetypeAvatarUnlockDef("Scout", "scout") { it.firstFlood },
        ArchetypeAvatarUnlockDef("Seer", "seer") { it.beatSeer },
        ArchetypeAvatarUnlockDef("Strobe", "strobe") { it.caughtTheStrobe }
    )

    fun resourceNames(): Set<String> = all.map { it.resourceName }.toSet()

    fun achievementUnlocked(stats: PlayerStats, resourceName: String): Boolean {
        return all.firstOrNull { it.resourceName == resourceName }?.achievementUnlocked?.invoke(stats) ?: false
    }
}

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
        WeatherAchievementDef("whiteout_hidden_moves", "Hidden Moves", "Gain marbles from a trickle obscured by Whiteout"),
        WeatherAchievementDef("high_pressure_one_shot", "One Shot", "Gain your single allowed positive score during High Pressure"),
        WeatherAchievementDef("stormfront_cut_off", "Cut Off", "Trigger the target limit during Stormfront"),
        WeatherAchievementDef("cold_rain_shared_storm", "Shared Storm", "Receive marbles from the Cold Rain redistribution"),
        WeatherAchievementDef("thunderhead_top_of_the_storm", "Top of the Storm", "Be among the top scorers during Thunderhead"),
        WeatherAchievementDef("cool_breeze_quiet_advantage", "Quiet Advantage", "Be among the lowest positive scorers during Cool Breeze"),
        WeatherAchievementDef("snow_fresh_powder", "Fresh Powder", "Complete a Snow round"),
        WeatherAchievementDef("smog_smoke_screen", "Smoke Screen", "Be revealed by Smog")
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
        const val UPGRADE_FAUCET = "faucet"
        const val UPGRADE_HOSE = "hose"
        const val UPGRADE_ROOSTER = "rooster"
        const val UPGRADE_WEATHER_VANE = "weather_vane"
        const val UPGRADE_FOUNTAIN = "fountain"
        const val UPGRADE_WATERSLIDE = "waterslide"
        const val UPGRADE_COMMUNITY_BOARD = "community_board"
        const val UPGRADE_ONE_POUND_WEIGHT = "one_pound_weight"
        const val UPGRADE_THREE_POUND_WEIGHT = "three_pound_weight"
        const val UPGRADE_PATINAD_COIN = "patinad_coin"
        const val UPGRADE_NORMAL_COIN = "normal_coin"
        const val UPGRADE_POLISHED_COIN = "polished_coin"
        const val UPGRADE_SEALED_COIN = "sealed_coin"
        const val KEY_UNLOCKED_SHOP_UPGRADE_IDS = "unlocked_shop_upgrade_ids"
        const val KEY_LAST_ROOSTER_BONUS_AT = "last_rooster_bonus_at"
        const val KEY_LAST_WEATHER_VANE_BONUS_AT = "last_weather_vane_bonus_at"
        const val DAILY_BONUS_WINDOW_MS = 24L * 60L * 60L * 1000L

        const val KEY_PLAYER_NAME = "player_name"
        const val KEY_PLAYER_AVATAR_RESOURCE_NAME = "player_avatar_resource_name"
        const val DEFAULT_PLAYER_AVATAR_RESOURCE_NAME = "player"
        const val KEY_PLAYER_NAME_COLOR_ID = "player_name_color_id"
        const val KEY_PLAYER_AVATAR_OUTLINE_COLOR_ID = "player_avatar_outline_color_id"
        const val KEY_UNLOCKED_PLAYER_NAME_COLOR_IDS = "unlocked_player_name_color_ids"
        const val KEY_UNLOCKED_PLAYER_AVATAR_OUTLINE_COLOR_IDS = "unlocked_player_avatar_outline_color_ids"
        const val KEY_UNLOCKED_ARCHETYPE_AVATAR_RESOURCE_NAMES = "unlocked_archetype_avatar_resource_names"
    }

    fun getPlayerNameColorId(): String {
        return prefs.getString(KEY_PLAYER_NAME_COLOR_ID, null)?.trim().orEmpty()
    }

    fun setPlayerNameColorId(colorId: String) {
        prefs.edit().putString(KEY_PLAYER_NAME_COLOR_ID, colorId.trim()).apply()
    }

    fun getPlayerAvatarOutlineColorId(): String {
        return prefs.getString(KEY_PLAYER_AVATAR_OUTLINE_COLOR_ID, null)?.trim().orEmpty()
    }

    fun setPlayerAvatarOutlineColorId(colorId: String) {
        prefs.edit().putString(KEY_PLAYER_AVATAR_OUTLINE_COLOR_ID, colorId.trim()).apply()
    }

    fun getUnlockedPlayerNameColorIds(): Set<String> {
        return prefs.getStringSet(KEY_UNLOCKED_PLAYER_NAME_COLOR_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun getUnlockedPlayerAvatarOutlineColorIds(): Set<String> {
        return prefs.getStringSet(KEY_UNLOCKED_PLAYER_AVATAR_OUTLINE_COLOR_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun getUnlockedArchetypeAvatarResourceNames(): Set<String> {
        return prefs.getStringSet(KEY_UNLOCKED_ARCHETYPE_AVATAR_RESOURCE_NAMES, emptySet())?.toSet() ?: emptySet()
    }

    fun getUnlockedShopUpgradeIds(): Set<String> {
        return prefs.getStringSet(KEY_UNLOCKED_SHOP_UPGRADE_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun isShopUpgradeUnlocked(upgradeId: String): Boolean {
        return upgradeId.trim() in getUnlockedShopUpgradeIds()
    }

    fun buyShopUpgrade(upgradeId: String, cost: Long): Boolean {
        val cleaned = upgradeId.trim()
        if (cleaned.isBlank() || isShopUpgradeUnlocked(cleaned)) return false
        val stats = load()
        if (stats.vaultMarbles < cost) return false

        stats.vaultMarbles -= cost
        save(stats)

        val unlocked = getUnlockedShopUpgradeIds() + cleaned
        prefs.edit()
            .putStringSet(KEY_UNLOCKED_SHOP_UPGRADE_IDS, unlocked)
            .apply()
        return true
    }

    fun claimRoosterDailyBonus(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isShopUpgradeUnlocked(UPGRADE_ROOSTER)) return false
        val lastClaimed = prefs.getLong(KEY_LAST_ROOSTER_BONUS_AT, 0L)
        if (lastClaimed > 0L && nowMs - lastClaimed < DAILY_BONUS_WINDOW_MS) return false
        prefs.edit().putLong(KEY_LAST_ROOSTER_BONUS_AT, nowMs).apply()
        return true
    }

    fun claimWeatherVaneDailyBonus(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isShopUpgradeUnlocked(UPGRADE_WEATHER_VANE)) return false
        val lastClaimed = prefs.getLong(KEY_LAST_WEATHER_VANE_BONUS_AT, 0L)
        if (lastClaimed > 0L && nowMs - lastClaimed < DAILY_BONUS_WINDOW_MS) return false
        prefs.edit().putLong(KEY_LAST_WEATHER_VANE_BONUS_AT, nowMs).apply()
        return true
    }

    fun isPlayerNameColorUnlocked(colorId: String): Boolean {
        val cleaned = colorId.trim()
        return cleaned.isBlank() || cleaned in getUnlockedPlayerNameColorIds()
    }

    fun isPlayerAvatarOutlineColorUnlocked(colorId: String): Boolean {
        val cleaned = colorId.trim()
        return cleaned.isBlank() || cleaned in getUnlockedPlayerAvatarOutlineColorIds()
    }

    fun buyPlayerNameColor(colorId: String, cost: Long): Boolean {
        val cleaned = colorId.trim()
        if (cleaned.isBlank() || isPlayerNameColorUnlocked(cleaned)) return false
        val stats = load()
        if (stats.vaultMarbles < cost) return false
        stats.vaultMarbles -= cost
        save(stats)
        val unlocked = getUnlockedPlayerNameColorIds() + cleaned
        prefs.edit()
            .putStringSet(KEY_UNLOCKED_PLAYER_NAME_COLOR_IDS, unlocked)
            .putString(KEY_PLAYER_NAME_COLOR_ID, cleaned)
            .apply()
        return true
    }

    fun buyPlayerAvatarOutlineColor(colorId: String, cost: Long): Boolean {
        val cleaned = colorId.trim()
        if (cleaned.isBlank() || isPlayerAvatarOutlineColorUnlocked(cleaned)) return false
        val stats = load()
        if (stats.vaultMarbles < cost) return false
        stats.vaultMarbles -= cost
        save(stats)
        val unlocked = getUnlockedPlayerAvatarOutlineColorIds() + cleaned
        prefs.edit()
            .putStringSet(KEY_UNLOCKED_PLAYER_AVATAR_OUTLINE_COLOR_IDS, unlocked)
            .putString(KEY_PLAYER_AVATAR_OUTLINE_COLOR_ID, cleaned)
            .apply()
        return true
    }

    fun isArchetypeAvatarUnlocked(resourceName: String): Boolean {
        val cleaned = resourceName.trim()
        if (cleaned.isBlank()) return false
        return cleaned in getUnlockedArchetypeAvatarResourceNames()
    }

    fun canBuyArchetypeAvatar(resourceName: String): Boolean {
        val cleaned = resourceName.trim()
        if (cleaned.isBlank() || isArchetypeAvatarUnlocked(cleaned)) return false
        return ArchetypeAvatarUnlocks.achievementUnlocked(load(), cleaned)
    }

    fun buyArchetypeAvatar(resourceName: String, cost: Long): Boolean {
        val cleaned = resourceName.trim()
        if (cleaned.isBlank() || isArchetypeAvatarUnlocked(cleaned)) return false
        val stats = load()
        if (!ArchetypeAvatarUnlocks.achievementUnlocked(stats, cleaned)) return false
        if (stats.vaultMarbles < cost) return false

        stats.vaultMarbles -= cost
        save(stats)

        val unlocked = getUnlockedArchetypeAvatarResourceNames() + cleaned
        prefs.edit()
            .putStringSet(KEY_UNLOCKED_ARCHETYPE_AVATAR_RESOURCE_NAMES, unlocked)
            .putString(KEY_PLAYER_AVATAR_RESOURCE_NAME, cleaned)
            .apply()
        return true
    }

    fun isPlayerAvatarAvailable(resourceName: String): Boolean {
        val cleaned = resourceName.trim()
        if (cleaned.isBlank()) return false
        if (cleaned !in ArchetypeAvatarUnlocks.resourceNames()) return true
        return isArchetypeAvatarUnlocked(cleaned)
    }

    fun getPlayerAvatarResourceName(): String {
        val raw = prefs.getString(KEY_PLAYER_AVATAR_RESOURCE_NAME, null)?.trim().orEmpty()
        return if (raw.isBlank()) DEFAULT_PLAYER_AVATAR_RESOURCE_NAME else raw
    }

    fun setPlayerAvatarResourceName(resourceName: String) {
        val cleaned = resourceName.trim()
        val selected = if (cleaned.isBlank()) DEFAULT_PLAYER_AVATAR_RESOURCE_NAME else cleaned
        if (!isPlayerAvatarAvailable(selected)) return
        prefs.edit().putString(KEY_PLAYER_AVATAR_RESOURCE_NAME, selected).apply()
    }

    fun getPlayerName(): String {
        val raw = prefs.getString(KEY_PLAYER_NAME, null)?.trim().orEmpty()
        return if (raw.isBlank()) "Player" else raw
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
            lifetimeMarblesEarned = prefs.getLong("lifetimeMarblesEarned", prefs.getLong("totalMarblesAcrossGames", 0L)),
            vaultMarbles = prefs.getLong("vaultMarbles", 0L),

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
            dieting = prefs.getBoolean("dieting", false),
            chaosTheory = prefs.getBoolean("chaosTheory", false),
            shadowStep = prefs.getBoolean("shadowStep", false),
            vendetta = prefs.getBoolean("vendetta", false),
            wrongGuyPal = prefs.getBoolean("wrongGuyPal", false),
            taxPaid = prefs.getBoolean("taxPaid", false),
            anointed = prefs.getBoolean("anointed", false),
            slowAndSteady = prefs.getBoolean("slowAndSteady", false),
            firstFlood = prefs.getBoolean("firstFlood", false),
            altComedy = prefs.getBoolean("altComedy", false),
            pickOnSomeoneYourSize = prefs.getBoolean("pickOnSomeoneYourSize", false),
            badFaith = prefs.getBoolean("badFaith", false),
            karma = prefs.getBoolean("karma", false),
            copycat = prefs.getBoolean("copycat", false),
            beatHunter = prefs.getBoolean("beatHunter", false),
            beatSeer = prefs.getBoolean("beatSeer", false),
            beatMirror = prefs.getBoolean("beatMirror", false),
            bossSlayer = prefs.getBoolean("bossSlayer", false),

            unlockedWeatherAchievements = prefs.getStringSet("unlockedWeatherAchievements", emptySet())?.toSet() ?: emptySet(),
            seenWeatherIds = prefs.getStringSet("seenWeatherIds", emptySet())?.toSet() ?: emptySet(),
            stormChaser = prefs.getBoolean("stormChaser", false),
        )
    }

    fun addEarnedMarbles(amount: Long, totalMarblesAcrossGamesAmount: Long = 0L) {
        if (amount <= 0L && totalMarblesAcrossGamesAmount <= 0L) return
        val s = load()
        if (amount > 0L) {
            s.lifetimeMarblesEarned += amount
            s.vaultMarbles += amount
        }
        if (totalMarblesAcrossGamesAmount > 0L) {
            s.totalMarblesAcrossGames += totalMarblesAcrossGamesAmount
        }
        save(s)
    }

    fun save(stats: PlayerStats) {
        prefs.edit()
            .putInt("totalGames", stats.totalGames)
            .putInt("totalWins", stats.totalWins)
            .putLong("totalMarblesAcrossGames", stats.totalMarblesAcrossGames)
            .putLong("lifetimeMarblesEarned", stats.lifetimeMarblesEarned)
            .putLong("vaultMarbles", stats.vaultMarbles)

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
            .putBoolean("dieting", stats.dieting)
            .putBoolean("chaosTheory", stats.chaosTheory)
            .putBoolean("shadowStep", stats.shadowStep)
            .putBoolean("vendetta", stats.vendetta)
            .putBoolean("wrongGuyPal", stats.wrongGuyPal)
            .putBoolean("taxPaid", stats.taxPaid)
            .putBoolean("anointed", stats.anointed)
            .putBoolean("slowAndSteady", stats.slowAndSteady)
            .putBoolean("firstFlood", stats.firstFlood)
            .putBoolean("altComedy", stats.altComedy)
            .putBoolean("pickOnSomeoneYourSize", stats.pickOnSomeoneYourSize)
            .putBoolean("badFaith", stats.badFaith)
            .putBoolean("karma", stats.karma)
            .putBoolean("copycat", stats.copycat)
            .putBoolean("beatHunter", stats.beatHunter)
            .putBoolean("beatSeer", stats.beatSeer)
            .putBoolean("beatMirror", stats.beatMirror)
            .putBoolean("bossSlayer", stats.bossSlayer)

            .putStringSet("unlockedWeatherAchievements", stats.unlockedWeatherAchievements)
            .putStringSet("seenWeatherIds", stats.seenWeatherIds)
            .putBoolean("stormChaser", stats.stormChaser)

            .apply()
    }
}