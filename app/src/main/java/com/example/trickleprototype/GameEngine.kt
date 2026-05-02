package com.example.trickleprototype

import kotlin.random.Random

data class PlayerState(
    val id: Int,
    var baseName: String,
    var marbles: Int = 0,
    var revealedChoice: Int? = null
)

enum class Difficulty { EASY, NORMAL, HARD }

data class RoundLogEvent(val text: String)

enum class MarbleTransferEndpointType {
    BOWL,
    PLAYER
}

data class MarbleTransferEvent(
    val fromType: MarbleTransferEndpointType,
    val fromPlayerId: Int? = null,
    val toType: MarbleTransferEndpointType,
    val toPlayerId: Int? = null,
    val amount: Int
)


enum class LogEventKind { PASS, OTHER }

enum class EnginePhase {
    SETUP, SELECT, BOT_TURN, PLAYER_TURN, TIEBREAKER_CHOICE, ROUND_END, GAME_OVER
}

data class RoundResult(
    val phase: EnginePhase,
    val roundNumber: Int,
    val log: List<RoundLogEvent>,
    val players: List<PlayerState>,
    val winnerIds: List<Int>,
    val bannerText: String?,
    val targetableIdsForHuman: List<Int>,
    val currentActorId: Int?,
    val hatHolderId: Int?,
    val currentStarterId: Int?,
    val lastEventKind: LogEventKind?,
    val currentWeatherName: String?,
    val currentWeatherEffect: String?,
    val forcedGuessForHuman: Int?,
    val mustTargetForHuman: Boolean,
    val requiresSecondTargetForHuman: Boolean,
    val activeTargetArrowActorId: Int?,
    val activeTargetArrowTargetIds: List<Int>,
    val marbleTransfers: List<MarbleTransferEvent>,
    val botArchetypeNamesByPlayerId: Map<Int, String>,
    val smogRevealedPlayerIds: Set<Int>,
    val humanHeldHatThisGame: Boolean,
    val humanCorrectGuessesThisGame: Int,
    val humanSubmittedTargetThisGame: Boolean,
    val humanStartedAnyRoundThisGame: Boolean,
    val humanPerfectBonusIntact: Boolean,
    val humanHadUntargetedOneTrickleThisGame: Boolean
)

class GameEngine(
    private val rng: Random = Random.Default
) {
    companion object {
        const val HUMAN_ID = 1
        const val WIN_SCORE = 13
        private var lastBossKind: BossKind? = null
    }

    private var difficulty: Difficulty = Difficulty.NORMAL
    private var weatherEnabled: Boolean = false
    private var currentBossId: Int? = null

    private var currentWeatherCard: WeatherCard? = null
    private var firstNonZeroGuessThisRound: Int? = null
    private var firstTargetingActorId: Int? = null
    private var firstWrongZeroResolvedThisRound: Boolean = false
    private var targetingActionsTakenThisRound: Int = 0
    private val playersWithPositiveScoringEventThisRound = mutableSetOf<Int>()
    private val roundStartMarbles = mutableMapOf<Int, Int>()
    private val latestMarbleTransfers = mutableListOf<MarbleTransferEvent>()

    private val botNames = listOf(
        "Al", "Barbara", "Clark", "David", "Erika", "Fred",
        "Graham", "Harry", "Ian", "Josh", "Kelly", "Lois"
    )

    private val players: MutableList<PlayerState> = mutableListOf<PlayerState>().apply {
        add(PlayerState(HUMAN_ID, "Player", marbles = 0))
        for (i in botNames.indices) add(PlayerState(i + 2, botNames[i], marbles = 0))
    }

    private fun ensureBotRoster() {
        val human = players.firstOrNull { it.id == HUMAN_ID }
            ?: PlayerState(HUMAN_ID, "Player", marbles = 0)

        val rebuilt = mutableListOf(human)
        botNames.forEachIndexed { index, botName ->
            val botId = index + 2
            val existing = players.firstOrNull { it.id == botId }
            if (existing != null) {
                existing.baseName = botName
                rebuilt.add(existing)
            } else {
                rebuilt.add(PlayerState(botId, botName, marbles = 0))
            }
        }

        players.clear()
        players.addAll(rebuilt)
    }


    fun setHumanName(name: String) {
        val cleaned = name.trim().ifBlank { "Player" }
        players.firstOrNull { it.id == HUMAN_ID }?.baseName = cleaned
    }

    private var roundNumber: Int = 1
    private var starterIndex: Int = 0
    private var phase: EnginePhase = EnginePhase.SETUP

    private val selectionsThisRound = mutableMapOf<Int, Int>()
    private val revealedThisRound = mutableMapOf<Int, Int>()
    private val revealedScoresThisRound = mutableMapOf<Int, Int>()
    private val targetedThisRound = mutableSetOf<Int>()
    private val attacksThisRound = mutableMapOf<Int, Int>()
    private val guessesThisRound = mutableMapOf<Int, Int>()
    private val weatherProtectedThisRound = mutableSetOf<Int>()
    private val smogRevealedPlayerIds = mutableSetOf<Int>()

    private var turnOrder: List<Int> = emptyList()
    private var turnCursor: Int = 0

    private var hatHolderId: Int? = null
    private var hatStartOfRoundHolderId: Int? = null

    private var tiebreakerParticipantIds: Set<Int>? = null
    private var tiebreakerStageIndex: Int = -1
    private var tiebreakerRoundsRemaining: Int = 0
    private var gameEndedInBotTie: Boolean = false

    private var pendingTiebreakerChooserId: Int? = null
    private var pendingTiebreakerGuesserId: Int? = null
    private var pendingTiebreakerHumanIsChooser: Boolean = false
    private var pendingTiebreakerBotValue: Int? = null

    // stats: per-game trackers
    private var gameWrongGuesses: Int = 0
    private var humanMadeWrongGuessThisGame: Boolean = false

    private var gameRoundReached: Int = 1
    private var gameHumanWasTrickedByZero: Boolean = false
    private var gameHumanTrickedBotWithZero: Boolean = false

    // achievements: per-game trackers
    private var gameHumanMadeGuess: Boolean = false
    private var gameHumanPassedAtLeastOnce: Boolean = false
    private val gameHumanChoicesUsed = mutableSetOf<Int>()   // track 0/1/3 used across rounds
    private val gameHumanGuessesUsed = mutableSetOf<Int>()   // track guessed 1/3 across game

    private var gameHumanCorrectRomeo: Boolean = false
    private var gameHumanCorrectJuliet: Boolean = false

    // NEW per-game trackers for new achievements
    private var gameHumanWasTargeted: Boolean = false
    private var gameHumanEverChose3: Boolean = false
    private var gameHumanHeldHat: Boolean = false
    private var gameHumanCorrectGuesses: Int = 0
    private var gameHumanSubmittedTarget: Boolean = false
    private var gameHumanStartedAnyRound: Boolean = false
    private var gameHumanPerfectBonusBroken: Boolean = false
    private var gameHumanHadUntargetedOneTrickle: Boolean = false

    private var humanCorrectGuessStreak: Int = 0
    private var unlockedOnARollThisGame: Boolean = false
    private var unlockedDumbLuckThisGame: Boolean = false

    private var startedRoundBecauseHat: Boolean = false // computed at startRound
    private var strobeCorrect3Count: Int = 0
    private var gluttonCorrect3Count: Int = 0
    private var pacifistCorrect3Count: Int = 0

    private var gameChaosTheory: Boolean = false
    private var gameShadowStep: Boolean = false
    private var gameVendetta: Boolean = false
    private var gameWrongGuyPal: Boolean = false
    private var gameTaxPaid: Boolean = false
    private var gameHumanSelectedByCabal: Boolean = false
    private var gameSlowAndSteady: Boolean = false
    private var gameFirstFlood: Boolean = false
    private var gameAltComedy: Boolean = false
    private var gamePickOnSomeoneYourSize: Boolean = false
    private var gameHumanGuessedCynicWrong: Boolean = false
    private var gameCynicGuessedHumanCorrectly: Boolean = false
    private val gameBotsThatTargetedHuman = mutableSetOf<Int>()
    private val gameAvengedBotIdsAgainstHuman = mutableSetOf<Int>()

    // weather achievements
    private val gameSeenWeatherIds = mutableSetOf<String>()
    private val gameUnlockedWeatherAchievementIds = mutableSetOf<String>()
    private var humanOriginalChoiceThisRound: Int? = null

    private var lastRoundChoices: Map<Int, Int> = emptyMap()
    private var lastRoundAttacks: Map<Int, Int> = emptyMap()
    private var lastRoundGuesses: Map<Int, Int> = emptyMap()
    private var lastRoundCorrectlyGuessedTargetIds: Set<Int> = emptySet()
    private val currentRoundCorrectlyGuessedTargetIds = mutableSetOf<Int>()
    private val passStreaks = mutableMapOf<Int, Int>()

    private val archetypeById: MutableMap<Int, Archetype> = mutableMapOf()
    private val memById: MutableMap<Int, BotMemory> = mutableMapOf()

    private val log = mutableListOf<RoundLogEvent>()
    private var bannerText: String? = null
    private var winnerIds: List<Int> = emptyList()

    private data class PendingLog(
        val kind: LogEventKind,
        val apply: () -> Unit
    )

    private val pending = ArrayDeque<PendingLog>()
    private var lastEventKind: LogEventKind? = null
    private var activeTargetArrowActorId: Int? = null
    private var activeTargetArrowTargetIds: List<Int> = emptyList()
    private var humanTargetIdThisRound: Int? = null
    private var humanGuessThisRound: Int? = null

    private var statsStore: StatsStore? = null

    init {
        reset()
    }

    fun attachStatsStore(store: StatsStore) {
        statsStore = store
    }

    fun setDifficulty(d: Difficulty) {
        difficulty = d
        if (phase == EnginePhase.SETUP) {
            assignRandomArchetypesToBots()
            phase = EnginePhase.SELECT
        }
    }

    fun setWeatherEnabled(enabled: Boolean) {
        weatherEnabled = enabled
    }

    fun isWeatherEnabled(): Boolean = weatherEnabled
    fun getDifficulty(): Difficulty = difficulty
    fun getPhase(): EnginePhase = phase
    fun getRoundNumber(): Int = roundNumber

    fun getPlayersSnapshot(): List<PlayerState> {
        ensureBotRoster()
        return players.map { p -> p.copy(baseName = displayNameFor(p.id)) }
    }

    fun reset() {
        ensureBotRoster()
        clearLatestMarbleTransfers()
        players.forEach { it.marbles = 0; it.revealedChoice = null }
        roundNumber = 1
        starterIndex = rng.nextInt(players.size)
        phase = EnginePhase.SETUP

        selectionsThisRound.clear()
        revealedThisRound.clear()
        revealedScoresThisRound.clear()
        targetedThisRound.clear()
        attacksThisRound.clear()
        guessesThisRound.clear()
        weatherProtectedThisRound.clear()
        smogRevealedPlayerIds.clear()
        currentWeatherCard = null
        activeTargetArrowActorId = null
        activeTargetArrowTargetIds = emptyList()
        humanTargetIdThisRound = null
        humanGuessThisRound = null
        firstNonZeroGuessThisRound = null
        firstTargetingActorId = null
        firstWrongZeroResolvedThisRound = false
        targetingActionsTakenThisRound = 0
        playersWithPositiveScoringEventThisRound.clear()
        roundStartMarbles.clear()

        turnOrder = emptyList()
        turnCursor = 0

        setHatHolder(null)
        hatStartOfRoundHolderId = null
        tiebreakerParticipantIds = null
        tiebreakerStageIndex = -1
        tiebreakerRoundsRemaining = 0
        gameEndedInBotTie = false

        lastRoundChoices = emptyMap()
        lastRoundAttacks = emptyMap()
        lastRoundGuesses = emptyMap()
        lastRoundCorrectlyGuessedTargetIds = emptySet()
        currentRoundCorrectlyGuessedTargetIds.clear()
        passStreaks.clear()

        log.clear()
        bannerText = null
        winnerIds = emptyList()
        clearPendingTiebreakerDuel()

        pending.clear()
        lastEventKind = null

        gameWrongGuesses = 0
        humanMadeWrongGuessThisGame = false

        gameRoundReached = 1
        gameHumanWasTrickedByZero = false
        gameHumanTrickedBotWithZero = false

        gameHumanMadeGuess = false
        gameHumanPassedAtLeastOnce = false
        gameHumanChoicesUsed.clear()
        gameHumanGuessesUsed.clear()
        gameHumanCorrectRomeo = false
        gameHumanCorrectJuliet = false

        gameHumanWasTargeted = false
        gameHumanEverChose3 = false
        gameHumanHeldHat = false
        gameHumanCorrectGuesses = 0
        gameHumanSubmittedTarget = false
        gameHumanStartedAnyRound = false
        gameHumanPerfectBonusBroken = false
        gameHumanHadUntargetedOneTrickle = false
        humanCorrectGuessStreak = 0
        unlockedOnARollThisGame = false
        unlockedDumbLuckThisGame = false
        startedRoundBecauseHat = false
        strobeCorrect3Count = 0
        gluttonCorrect3Count = 0
        pacifistCorrect3Count = 0
        gameChaosTheory = false
        gameShadowStep = false
        gameVendetta = false
        gameWrongGuyPal = false
        gameTaxPaid = false
        gameHumanSelectedByCabal = false
        gameSlowAndSteady = false
        gameFirstFlood = false
        gameAltComedy = false
        gamePickOnSomeoneYourSize = false
        gameHumanGuessedCynicWrong = false
        gameCynicGuessedHumanCorrectly = false
        gameBotsThatTargetedHuman.clear()
        gameAvengedBotIdsAgainstHuman.clear()
        gameSeenWeatherIds.clear()
        gameUnlockedWeatherAchievementIds.clear()
        humanOriginalChoiceThisRound = null
    }


    private fun drawWeatherForRound() {
        if (!weatherEnabled) {
            currentWeatherCard = null
            return
        }

        currentWeatherCard = if (roundNumber == 1) {
            Weather.firstRoundWeather()
        } else {
            Weather.drawRandomWeather(
                rng = rng,
                includeDigitalCards = true,
                excludeIds = setOf(Weather.FIRST_ROUND_WEATHER_ID)
            )
        }
    }


    private fun currentWeatherId(): String? = currentWeatherCard?.id

    private fun unlockWeatherAchievement(id: String) {
        gameUnlockedWeatherAchievementIds += id
    }

    private fun setHatHolder(id: Int?) {
        hatHolderId = id
        if (id == HUMAN_ID) {
            gameHumanHeldHat = true
        }
    }

    private fun unlockCurrentWeatherIf(id: String) {
        if (currentWeatherId() == id) {
            gameUnlockedWeatherAchievementIds += when (id) {
                "drizzle" -> "drizzle_light_rain"
                "downpour" -> "downpour_soaking_it_in"
                "fog" -> "fog_hidden_in_plain_sight"
                "sunny_day" -> "sunny_day_bright_strategy"
                "low_pressure" -> "low_pressure_set_the_pressure"
                "windshear" -> "windshear_against_the_wind"
                "static_charge" -> "static_charge_first_strike"
                "crosswinds" -> "crosswinds_two_birds_one_guess"
                "sleet" -> "sleet_cold_exchange"
                "thunderstorm" -> "thunderstorm_shock_therapy"
                "drought" -> "drought_dry_spell"
                "tornado" -> "tornado_eye_of_the_storm"
                "hail" -> "hail_ice_storm"
                "hurricane" -> "hurricane_storm_surge"
                "rainbow" -> "rainbow_silver_lining"
                "perfect_storm" -> "perfect_storm_twelve"
                "lightning_storm" -> "lightning_storm_strike_twice"
                "heat_mirage" -> "heat_mirage_what_just_happened"
                "whiteout" -> "whiteout_hidden_moves"
                "high_pressure" -> "high_pressure_one_shot"
                "stormfront" -> "stormfront_cut_off"
                "cold_rain" -> "cold_rain_shared_storm"
                "thunderhead" -> "thunderhead_top_of_the_storm"
                "cool_breeze" -> "cool_breeze_quiet_advantage"
                "snow" -> "snow_fresh_powder"
                "smog" -> "smog_smoke_screen"
                else -> return
            }
        }
    }

    private fun markHumanScoredFromOwnSelection(actualSelection: Int, gained: Int) {
        if (gained <= 0) return

        if (actualSelection == 1 && HUMAN_ID !in targetedThisRound) {
            gameHumanHadUntargetedOneTrickle = true
        }

        when (currentWeatherId()) {
            "downpour" -> if (actualSelection == 1) unlockWeatherAchievement("downpour_soaking_it_in")
            "sunny_day" -> if (actualSelection == 3) unlockWeatherAchievement("sunny_day_bright_strategy")
            "hail" -> if (actualSelection == 3) unlockWeatherAchievement("hail_ice_storm")
            "hurricane" -> if (actualSelection == 0) unlockWeatherAchievement("hurricane_storm_surge")
            "heat_mirage" -> {
                val original = humanOriginalChoiceThisRound
                if (original != null && original != actualSelection) {
                    unlockWeatherAchievement("heat_mirage_what_just_happened")
                }
            }

            "high_pressure" -> unlockWeatherAchievement("high_pressure_one_shot")
        }
    }

    private fun markHumanScoredFromOwnSelection(
        actualSelection: Int,
        moved: Int,
        obscured: Boolean
    ) {
        if (obscured && moved > 0 && currentWeatherId() == "whiteout") {
            unlockWeatherAchievement("whiteout_hidden_moves")
        }
        markHumanScoredFromOwnSelection(actualSelection = actualSelection, gained = moved)
    }

    private fun finalizeWeatherAchievementsAtGameEnd() {
        if (difficulty == Difficulty.EASY) return

        statsStore?.let { store ->
            val s = store.load()

            val combinedSeen = (s.seenWeatherIds + gameSeenWeatherIds).toSet()
            s.seenWeatherIds = combinedSeen

            val newWeatherIds = gameUnlockedWeatherAchievementIds - s.unlockedWeatherAchievements
            if (newWeatherIds.isNotEmpty()) {
                s.unlockedWeatherAchievements =
                    (s.unlockedWeatherAchievements + newWeatherIds).toSet()
                newWeatherIds.forEach { newId ->
                    val def =
                        WeatherAchievements.perCard.firstOrNull { it.id == newId } ?: return@forEach
                    log += RoundLogEvent("*** Achievement Unlocked: ${def.title} - ${def.desc}! ***")
                }
            }

            if (!s.stormChaser && combinedSeen.containsAll(WeatherAchievements.allWeatherIds)) {
                s.stormChaser = true
                log += RoundLogEvent("*** Achievement Unlocked: Storm Chaser - Experience every unique weather across completed games! ***")
            }

            store.save(s)
        }
    }

    private fun weatherHas(tag: WeatherEffectTag): Boolean =
        currentWeatherCard?.effectTags?.contains(tag) == true

    private fun noMarblesMoveThisRound(): Boolean =
        weatherHas(WeatherEffectTag.NO_MARBLES_MOVE)

    private fun hasReachedStormfrontTargetingLimit(): Boolean {
        if (!weatherHas(WeatherEffectTag.LIMIT_TARGETING_ACTIONS_TO_HALF_ROUNDED_DOWN)) return false
        val maxTargetingActions = activePlayerIds().size / 2
        return targetingActionsTakenThisRound >= maxTargetingActions
    }

    private fun actorMustPassBecauseAlreadyScored(actorId: Int): Boolean =
        weatherHas(WeatherEffectTag.ONE_POSITIVE_SCORING_EVENT_PER_PLAYER) &&
                actorId in playersWithPositiveScoringEventThisRound


    private fun clearLatestMarbleTransfers() {
        latestMarbleTransfers.clear()
    }

    private fun recordMarbleTransfer(
        fromType: MarbleTransferEndpointType,
        fromPlayerId: Int? = null,
        toType: MarbleTransferEndpointType,
        toPlayerId: Int? = null,
        amount: Int
    ) {
        if (amount <= 0) return
        latestMarbleTransfers += MarbleTransferEvent(
            fromType = fromType,
            fromPlayerId = fromPlayerId,
            toType = toType,
            toPlayerId = toPlayerId,
            amount = amount
        )
    }

    private fun removeMarblesToBowl(player: PlayerState, amount: Int): Int {
        if (amount <= 0) return 0
        if (noMarblesMoveThisRound()) return 0
        val moved = minOf(amount, player.marbles)
        if (moved <= 0) return 0
        player.marbles -= moved
        recordMarbleTransfer(
            fromType = MarbleTransferEndpointType.PLAYER,
            fromPlayerId = player.id,
            toType = MarbleTransferEndpointType.BOWL,
            amount = moved
        )
        return moved
    }

    private fun applyPositiveGain(player: PlayerState, amount: Int): Int {
        return applyPositiveGainInternal(
            player = player,
            amount = amount,
            allowDuringDrought = false
        )
    }

    private fun applyTrickleGain(player: PlayerState, amount: Int): Int {
        return applyPositiveGainInternal(
            player = player,
            amount = amount,
            allowDuringDrought = true
        )
    }

    private fun applyPositiveGainInternal(
        player: PlayerState,
        amount: Int,
        allowDuringDrought: Boolean
    ): Int {
        if (amount <= 0) return 0
        if (noMarblesMoveThisRound() && !(allowDuringDrought && currentWeatherId() == "drought")) return 0
        if (weatherHas(WeatherEffectTag.ONE_POSITIVE_SCORING_EVENT_PER_PLAYER) &&
            player.id in playersWithPositiveScoringEventThisRound
        ) {
            return 0
        }

        player.marbles += amount
        recordMarbleTransfer(
            fromType = MarbleTransferEndpointType.BOWL,
            toType = MarbleTransferEndpointType.PLAYER,
            toPlayerId = player.id,
            amount = amount
        )
        if (weatherHas(WeatherEffectTag.ONE_POSITIVE_SCORING_EVENT_PER_PLAYER)) {
            playersWithPositiveScoringEventThisRound += player.id
        }
        return amount
    }

    private fun transferMarbles(from: PlayerState, to: PlayerState, amount: Int): Int {
        if (amount <= 0) return 0
        val moved = minOf(amount, from.marbles)
        if (moved <= 0) return 0
        from.marbles -= moved
        to.marbles += moved
        recordMarbleTransfer(
            fromType = MarbleTransferEndpointType.PLAYER,
            fromPlayerId = from.id,
            toType = MarbleTransferEndpointType.PLAYER,
            toPlayerId = to.id,
            amount = moved
        )
        return moved
    }

    private fun reduceTargetedGain(amount: Int): Int {
        if (amount <= 0) return 0
        if (!weatherHas(WeatherEffectTag.REDUCE_TARGETED_GAINS_BY_ONE)) return amount
        return maxOf(0, amount - 1)
    }

    private fun increaseTrickleGain(amount: Int): Int {
        if (amount <= 0) return 0
        if (!weatherHas(WeatherEffectTag.INCREASE_TRICKLE_GAINS_BY_TWO)) return amount
        return amount + 2
    }

    private fun roundScoreForSelection(value: Int): Int {
        if (noMarblesMoveThisRound()) return 0
        return baseScoreForSelection(value)
    }

    private fun trickleScoreForSelection(value: Int): Int {
        val base = if (currentWeatherId() == "drought") {
            baseScoreForSelection(value)
        } else {
            roundScoreForSelection(value)
        }
        return increaseTrickleGain(base)
    }

    private fun baseScoreForSelection(value: Int): Int {
        var result = when {
            value == 1 && weatherHas(WeatherEffectTag.SCORE_ONES_AS_TWO) -> 2
            value == 3 && weatherHas(WeatherEffectTag.SCORE_THREES_AS_FOUR) -> 4
            value == 0 && weatherHas(WeatherEffectTag.UNTARGETED_ZERO_TRICKLES_FOR_ONE) -> 1
            else -> value
        }

        if (weatherHas(WeatherEffectTag.DOUBLE_ALL_MARBLE_GAINS)) {
            result *= 2
        }

        return result
    }

    private fun legalTargetIdsForActor(actorId: Int): List<Int> {
        val activeSet = activePlayerIds().toSet()
        return players.asSequence()
            .map { it.id }
            .filter { it != actorId }
            .filter { it in activeSet }
            .filter { it !in targetedThisRound }
            .filter { !isUntargetableFromWeather(it) }
            .toList()
    }

    private fun actorNeedsTwoTargetsIfTargeting(): Boolean =
        weatherHas(WeatherEffectTag.MUST_TARGET_TWO_WITH_SAME_GUESS_IF_TARGETING)

    private fun actorHasEnoughLegalTargetsToTarget(actorId: Int): Boolean {
        val legalCount = legalTargetIdsForActor(actorId).size
        val requiredCount = if (actorNeedsTwoTargetsIfTargeting()) 2 else 1
        return legalCount >= requiredCount
    }

    private fun actorMustTarget(actorId: Int): Boolean =
        !actorMustPassBecauseAlreadyScored(actorId) &&
                !hasReachedStormfrontTargetingLimit() &&
                weatherHas(WeatherEffectTag.MUST_TARGET_IF_LEGAL) &&
                actorHasEnoughLegalTargetsToTarget(actorId)

    private fun humanMustAutoPassBecauseWeather(): Boolean =
        phase == EnginePhase.PLAYER_TURN &&
                !actorMustPassBecauseAlreadyScored(HUMAN_ID) &&
                !hasReachedStormfrontTargetingLimit() &&
                weatherHas(WeatherEffectTag.MUST_TARGET_IF_LEGAL) &&
                !actorHasEnoughLegalTargetsToTarget(HUMAN_ID)

    private fun wrongZeroPenaltyForActor(actorId: Int): Int {
        if (noMarblesMoveThisRound()) return 0
        if (weatherHas(WeatherEffectTag.FIRST_WRONG_ON_ZERO_NO_LOSS_BUT_HAT_MOVES) && firstTargetingActorId == actorId) {
            return 0
        }

        var penalty = when {
            weatherHas(WeatherEffectTag.FIRST_WRONG_ZERO_GIVES_THREE) && !firstWrongZeroResolvedThisRound -> {
                firstWrongZeroResolvedThisRound = true
                3
            }

            weatherHas(WeatherEffectTag.WRONG_ZERO_COSTS_THREE) -> -3
            weatherHas(WeatherEffectTag.WRONG_ZERO_COSTS_TWO) -> -2
            weatherHas(WeatherEffectTag.WRONG_ZERO_GIVES_ONE) -> +1
            else -> -1
        }

        if (penalty > 0 && weatherHas(WeatherEffectTag.DOUBLE_ALL_MARBLE_GAINS)) {
            penalty *= 2
        } else if (penalty < 0 && weatherHas(WeatherEffectTag.DOUBLE_ALL_MARBLE_LOSSES)) {
            penalty *= 2
        }

        return penalty
    }

    private fun rewardForCorrectGuess(actorId: Int, guess: Int): Int {
        if (noMarblesMoveThisRound()) return 0
        if (weatherHas(WeatherEffectTag.FIRST_GUESSER_NO_REWARD_IF_CORRECT) && firstTargetingActorId == actorId) {
            return 0
        }
        return reduceTargetedGain(roundScoreForSelection(guess))
    }

    private fun lockedGuessForRound(): Int? {
        val first = firstNonZeroGuessThisRound ?: return null
        return when {
            weatherHas(WeatherEffectTag.LOCK_GUESSES_SAME_AS_FIRST) -> first
            weatherHas(WeatherEffectTag.LOCK_GUESSES_OPPOSITE_OF_FIRST) -> if (first == 1) 3 else 1
            else -> null
        }
    }

    private fun highestScorePlayerIds(activeIds: Collection<Int>): Set<Int> {
        if (activeIds.isEmpty()) return emptySet()
        val highestScore = players
            .asSequence()
            .filter { it.id in activeIds }
            .maxOfOrNull { it.marbles }
            ?: return emptySet()
        return players
            .asSequence()
            .filter { it.id in activeIds && it.marbles == highestScore }
            .map { it.id }
            .toSet()
    }

    private fun isUntargetableFromWeather(playerId: Int): Boolean {
        if (playerId in weatherProtectedThisRound) return true
        val choice = selectionsThisRound[playerId] ?: return false
        return when {
            weatherHas(WeatherEffectTag.REVEAL_ONES) && choice == 1 -> true
            weatherHas(WeatherEffectTag.REVEAL_THREES) && choice == 3 -> true
            else -> false
        }
    }

    private fun queueWeatherReveal(activeIds: List<Int>) {
        val weather = currentWeatherCard ?: return

        enqueueOther {
            log += RoundLogEvent("Weather: ${weather.displayName} - ${weather.effectText}")
        }

        if (weatherHas(WeatherEffectTag.REVEAL_ONES) || weatherHas(WeatherEffectTag.REVEAL_THREES)) {
            val revealValue = when {
                weatherHas(WeatherEffectTag.REVEAL_ONES) -> 1
                weatherHas(WeatherEffectTag.REVEAL_THREES) -> 3
                else -> null
            }

            if (revealValue != null) {
                enqueueOther {
                    for (pid in activeIds) {
                        if (selectionsThisRound[pid] == revealValue) {
                            revealedThisRound[pid] = revealValue

                            val player = players.firstOrNull { it.id == pid }
                            if (player != null) {
                                player.revealedChoice = revealValue
                                val awarded = applyPositiveGain(player, revealValue)

                                if (pid == HUMAN_ID) {
                                    markHumanScoredFromOwnSelection(
                                        actualSelection = revealValue,
                                        moved = awarded,
                                        obscured = false
                                    )
                                }

                                if (pid == HUMAN_ID && currentWeatherId() == "fog" && revealValue == 1) {
                                    unlockWeatherAchievement("fog_hidden_in_plain_sight")
                                }

                                log += RoundLogEvent("${displayNameFor(pid)} is revealed early by weather: $revealValue and gains $awarded.")
                            }
                        }
                    }
                }
            }
        }

        if (weatherHas(WeatherEffectTag.REVEAL_HIGHEST_SCORE_CHOICES_AND_PROTECT)) {
            enqueueOther {
                val leaderIds = highestScorePlayerIds(activeIds)
                weatherProtectedThisRound.clear()
                weatherProtectedThisRound += leaderIds
                smogRevealedPlayerIds.clear()
                smogRevealedPlayerIds += leaderIds

                for (pid in leaderIds) {
                    val choice = selectionsThisRound[pid] ?: continue
                    val player = players.firstOrNull { it.id == pid } ?: continue

                    revealedScoresThisRound[pid] = player.marbles
                    revealedThisRound[pid] = choice
                    player.revealedChoice = choice

                    val awarded = applyPositiveGain(player, choice)

                    if (pid == HUMAN_ID) {
                        markHumanScoredFromOwnSelection(
                            actualSelection = choice,
                            moved = awarded,
                            obscured = false
                        )
                    }

                    log += RoundLogEvent("${displayNameFor(pid)} is revealed by Smog: score ${player.marbles - awarded}, choice $choice, gains $awarded, and cannot be targeted this round.")
                }
            }
        }
    }


    fun submitTiebreakerChoice(choice: Int): RoundResult {
        clearLatestMarbleTransfers()
        if (phase != EnginePhase.TIEBREAKER_CHOICE) return snapshot()
        if (choice != 1 && choice != 3) {
            bannerText = "Choose 1 or 3 for the tiebreaker."
            return snapshot()
        }

        val chooserId = pendingTiebreakerChooserId ?: return snapshot()
        val guesserId = pendingTiebreakerGuesserId ?: return snapshot()
        val botValue = pendingTiebreakerBotValue ?: return snapshot()

        val chooserPick: Int
        val guesserGuess: Int

        if (pendingTiebreakerHumanIsChooser) {
            chooserPick = choice
            guesserGuess = botValue
        } else {
            chooserPick = botValue
            guesserGuess = choice
        }

        finishTwoPlayerTiebreaker(
            chooserId = chooserId,
            guesserId = guesserId,
            chooserPick = chooserPick,
            guesserGuess = guesserGuess
        )
        clearPendingTiebreakerDuel()
        phase = EnginePhase.GAME_OVER
        finalizeGameOverIfNeeded()
        return snapshot()
    }

    fun startRound(humanChoice: Int): RoundResult {
        clearLatestMarbleTransfers()
        if (phase == EnginePhase.GAME_OVER) return snapshot()
        if (phase != EnginePhase.SELECT && phase != EnginePhase.ROUND_END) return snapshot()

        gameRoundReached = maxOf(gameRoundReached, roundNumber)

        selectionsThisRound.clear()
        revealedThisRound.clear()
        revealedScoresThisRound.clear()
        targetedThisRound.clear()
        attacksThisRound.clear()
        guessesThisRound.clear()
        weatherProtectedThisRound.clear()
        smogRevealedPlayerIds.clear()
        bannerText = null
        winnerIds = emptyList()
        pending.clear()
        lastEventKind = null
        activeTargetArrowActorId = null
        activeTargetArrowTargetIds = emptyList()
        humanTargetIdThisRound = null
        humanGuessThisRound = null
        currentRoundCorrectlyGuessedTargetIds.clear()
        firstNonZeroGuessThisRound = null
        firstTargetingActorId = null
        firstWrongZeroResolvedThisRound = false
        targetingActionsTakenThisRound = 0
        playersWithPositiveScoringEventThisRound.clear()
        roundStartMarbles.clear()
        players.forEach { it.revealedChoice = null }

        if (players.getOrNull(starterIndex)?.id == HUMAN_ID) {
            gameHumanStartedAnyRound = true
        }

        drawWeatherForRound()
        currentWeatherId()?.let {
            gameSeenWeatherIds += it
            when (it) {
                "drizzle", "snow" -> unlockCurrentWeatherIf(it)
            }
        }

        hatStartOfRoundHolderId = hatHolderId

        val starterId = players[starterIndex].id
        // Start because you have the Hat means starter == hatHolder AND hatHolder exists
        startedRoundBecauseHat = (hatHolderId != null && starterId == hatHolderId)

        val activeIds = activePlayerIds()
        turnOrder = buildTurnOrderFromStarter(activeIds)
        turnCursor = 0

        if (HUMAN_ID in activeIds) {
            selectionsThisRound[HUMAN_ID] = humanChoice
            humanOriginalChoiceThisRound = humanChoice
            gameHumanChoicesUsed += humanChoice
            if (humanChoice == 3) gameHumanEverChose3 = true
        } else {
            humanOriginalChoiceThisRound = null
        }

        for (pid in players.map { it.id }.filter { it != HUMAN_ID }) {
            if (pid !in activeIds) continue

            val arch = archetypeById[pid]!!
            val mem = memById.getOrPut(pid) { BotMemory() }

            val pub = PublicRoundInfo(
                roundIndex = roundNumber,
                startingPlayerId = starterId,
                hatHolderId = hatHolderId,
                revealedThisRound = emptyMap(),
                revealedScoresThisRound = emptyMap(),
                targetedThisRound = emptySet(),
                lastRoundChoices = lastRoundChoices,
                lastRoundAttacks = lastRoundAttacks,
                lastRoundGuesses = lastRoundGuesses,
                passStreaks = passStreaks.toMap(),
                myId = pid,
                playersAlive = activeIds,
                rng = rng,
                humanMarbles = if (difficulty == Difficulty.HARD) players.first { it.id == HUMAN_ID }.marbles else null,
                currentRoundChoices = selectionsThisRound.toMap(),
                currentScores = players.associate { it.id to it.marbles }
            )
            selectionsThisRound[pid] = arch.chooseDie(pub, mem).v
        }

        if (weatherHas(WeatherEffectTag.ROTATE_SELECTED_VALUES_0_TO_1_TO_3_TO_0)) {
            for (pid in activeIds) {
                val original = selectionsThisRound[pid] ?: continue
                selectionsThisRound[pid] = when (original) {
                    0 -> 1
                    1 -> 3
                    else -> 0
                }
            }
        }

        for (pid in activeIds) {
            roundStartMarbles[pid] = players.first { it.id == pid }.marbles
        }

        enqueueOther {
            log += RoundLogEvent("")
            log += RoundLogEvent("=== ROUND $roundNumber | Starter: ${displayNameFor(starterId)} ===")
        }
        queueWeatherReveal(activeIds)

        phase = EnginePhase.BOT_TURN
        return stepInternalOrSnapshot()
    }

    fun step(): RoundResult {
        clearLatestMarbleTransfers()
        if (phase != EnginePhase.BOT_TURN) return snapshot()
        return stepInternalOrSnapshot()
    }

    fun submitHumanTurn(targetId: Int?, guess: Int?, secondTargetId: Int? = null): RoundResult {
        clearLatestMarbleTransfers()
        if (phase != EnginePhase.PLAYER_TURN) return snapshot()
        val actorId = turnOrder.getOrNull(turnCursor) ?: return snapshot()
        if (actorId != HUMAN_ID) return snapshot()

        val validTargets = targetableIdsForHuman()

        if (actorMustPassBecauseAlreadyScored(HUMAN_ID) || humanMustAutoPassBecauseWeather()) {
            humanTargetIdThisRound = 0
            humanGuessThisRound = 0
            queuePass(actorId = HUMAN_ID)
            turnCursor++
            phase = EnginePhase.BOT_TURN
            return stepInternalOrSnapshot()
        }

        if (targetId == null) {
            if (actorMustTarget(HUMAN_ID)) {
                bannerText = "Tornado is active. You must target if any legal target exists."
                return snapshot()
            }
            if (validTargets.isNotEmpty()) {
                gameHumanPerfectBonusBroken = true
            }
            gameHumanPassedAtLeastOnce = true
            humanTargetIdThisRound = 0
            humanGuessThisRound = 0
            queuePass(actorId = HUMAN_ID)
        } else {
            if (targetId !in validTargets) {
                bannerText = "That target is no longer available. Pick someone else."
                return snapshot()
            }

            val selectedTargets = if (actorNeedsTwoTargetsIfTargeting()) {
                val second = secondTargetId
                if (second == null || second == targetId || second !in validTargets) {
                    bannerText = "Crosswinds requires two different legal targets."
                    return snapshot()
                }
                listOf(targetId, second)
            } else {
                listOf(targetId)
            }

            val g = guess ?: 1
            val forcedGuess = lockedGuessForRound()
            if (g != 0 && forcedGuess != null && g != forcedGuess) {
                bannerText = "Weather locks your guess to $forcedGuess this round."
                return snapshot()
            }

            gameHumanMadeGuess = true
            gameHumanSubmittedTarget = true
            gameHumanGuessesUsed += g
            humanTargetIdThisRound = targetId
            humanGuessThisRound = g

            queueGuessAction(actorId = HUMAN_ID, targetIds = selectedTargets, guess = g)
        }

        turnCursor++
        phase = EnginePhase.BOT_TURN
        return stepInternalOrSnapshot()
    }

    private fun stepInternalOrSnapshot(): RoundResult {
        if (pending.isNotEmpty()) {
            val p = pending.removeFirst()
            p.apply.invoke()
            lastEventKind = p.kind
            return snapshot()
        }

        if (turnCursor >= turnOrder.size) {
            queueTrickleAndEndRound()
            return stepInternalOrSnapshot()
        }

        val actorId = turnOrder[turnCursor]

        activeTargetArrowActorId = null
        activeTargetArrowTargetIds = emptyList()

        if (archetypeById[actorId] is Mirror &&
            humanTargetIdThisRound == null &&
            HUMAN_ID in turnOrder.drop(turnCursor + 1)
        ) {
            val reordered = turnOrder.toMutableList()
            reordered.removeAt(turnCursor)
            val humanIndex = reordered.indexOf(HUMAN_ID)
            val insertIndex = if (humanIndex >= 0) humanIndex + 1 else reordered.size
            reordered.add(insertIndex, actorId)
            turnOrder = reordered
            return stepInternalOrSnapshot()
        }

        if (actorId == HUMAN_ID) {
            phase = EnginePhase.PLAYER_TURN
            if (humanMustAutoPassBecauseWeather()) {
                humanTargetIdThisRound = 0
                humanGuessThisRound = 0
                queuePass(actorId = HUMAN_ID)
                turnCursor++
                phase = EnginePhase.BOT_TURN
                return stepInternalOrSnapshot()
            }
            return snapshot()
        }

        queueBotTurn(actorId)
        turnCursor++
        return stepInternalOrSnapshot()
    }

    private fun visibleScoresThisRoundForBots(): Map<Int, Int> {
        return if (difficulty == Difficulty.HARD) revealedScoresThisRound.toMap() else emptyMap()
    }

    private fun pickHardModePressureTarget(
        arch: Archetype,
        pub: PublicRoundInfo,
        legalTargets: List<Int>
    ): Pair<Int, Int>? {
        if (difficulty != Difficulty.HARD) return null
        if (arch is Hunter || arch is Seer || arch is Mirror) return null
        if (!arch.isAggressiveInHardMode) return null
        if (actorNeedsTwoTargetsIfTargeting()) return null

        val highestVisibleThreat = pub.revealedScoresThisRound
            .filterKeys { it in legalTargets }
            .filterValues { it >= 10 }

        if (highestVisibleThreat.isEmpty()) return null

        val bestScore = highestVisibleThreat.values.maxOrNull() ?: return null
        val candidates = highestVisibleThreat
            .filterValues { it == bestScore }
            .keys
            .toList()

        if (candidates.isEmpty()) return null

        val targetId = candidates.random(rng)
        val guess =
            guessFromVisibleReveal(pub, targetId, defaultGuess = DieChoice.THREE)?.v ?: return null
        return targetId to guess
    }

    private fun queueBotTurn(actorId: Int) {
        val starterId = players[starterIndex].id
        val arch = archetypeById[actorId]!!
        val mem = memById.getOrPut(actorId) { BotMemory() }

        val legalTargets = legalTargetIdsForActor(actorId)

        if (currentWeatherId() == "drought") {
            if (arch !is Jester) {
                queuePass(actorId)
                return
            }

            val droughtTargets = legalTargets.filter { it in lastRoundCorrectlyGuessedTargetIds }
            if (droughtTargets.isNotEmpty()) {
                queueGuessAction(
                    actorId = actorId,
                    targetIds = listOf(droughtTargets.random(rng)),
                    guess = 3
                )
                passStreaks[actorId] = 0
            } else {
                queuePass(actorId)
            }
            return
        }

        if (actorMustPassBecauseAlreadyScored(actorId) || hasReachedStormfrontTargetingLimit()) {
            queuePass(actorId)
            return
        }

        val weatherBlockedTargets = activePlayerIds()
            .filter { it != actorId }
            .filter { isUntargetableFromWeather(it) }
            .toSet()

        val pub = PublicRoundInfo(
            roundIndex = roundNumber,
            startingPlayerId = starterId,
            hatHolderId = hatHolderId,
            revealedThisRound = revealedThisRound.toMap(),
            revealedScoresThisRound = visibleScoresThisRoundForBots(),
            targetedThisRound = (targetedThisRound + weatherBlockedTargets).toSet(),
            lastRoundChoices = lastRoundChoices,
            lastRoundAttacks = if (roundNumber == 1) attacksThisRound.toMap() else lastRoundAttacks,
            lastRoundGuesses = if (roundNumber == 1) guessesThisRound.toMap() else lastRoundGuesses,
            passStreaks = passStreaks.toMap(),
            myId = actorId,
            playersAlive = activePlayerIds(),
            rng = rng,
            humanMarbles = if (difficulty == Difficulty.HARD) players.first { it.id == HUMAN_ID }.marbles else null,
            currentRoundChoices = selectionsThisRound.toMap(),
            currentScores = players.associate { it.id to it.marbles },
            humanTargetIdThisRound = humanTargetIdThisRound,
            humanGuessThisRound = humanGuessThisRound
        )

        pickHardModePressureTarget(arch, pub, legalTargets)?.let { (targetId, guess) ->
            queueGuessAction(actorId = actorId, targetIds = listOf(targetId), guess = guess)
            passStreaks[actorId] = 0
            return
        }

        val decision = arch.takeTurn(pub, mem)
        if (arch is Cabal && mem.protectedId == HUMAN_ID) {
            gameHumanSelectedByCabal = true
        }

        when (decision) {
            is TurnDecision.Pass -> {
                if (actorMustTarget(actorId)) {
                    val fallbackTarget = legalTargets.firstOrNull()
                    if (fallbackTarget != null) {
                        val forcedGuess = lockedGuessForRound()
                        queueGuessAction(actorId, listOf(fallbackTarget), forcedGuess ?: 1)
                    } else {
                        queuePass(actorId)
                    }
                } else {
                    queuePass(actorId)
                }
            }

            is TurnDecision.Guess -> {
                val forcedGuess = lockedGuessForRound()
                val botGuess = decision.guess.v
                val finalGuess = when {
                    forcedGuess == null -> botGuess
                    botGuess == 0 -> 0
                    botGuess == forcedGuess -> botGuess
                    else -> forcedGuess
                }

                if (actorNeedsTwoTargetsIfTargeting()) {
                    val firstTarget = decision.targetId.takeIf { it in legalTargets }
                        ?: legalTargets.firstOrNull()
                    val secondTarget = legalTargets.firstOrNull { it != firstTarget }

                    if (firstTarget != null && secondTarget != null) {
                        queueGuessAction(actorId, listOf(firstTarget, secondTarget), finalGuess)
                    } else {
                        queuePass(actorId)
                    }
                } else {
                    val finalTarget = decision.targetId.takeIf { it in legalTargets }
                        ?: legalTargets.firstOrNull()
                    if (finalTarget != null) {
                        queueGuessAction(actorId, listOf(finalTarget), finalGuess)
                    } else {
                        queuePass(actorId)
                    }
                }
            }
        }
    }

    private fun logNameForAction(actorId: Int): String {
        val base = displayNameFor(actorId)
        return if (archetypeById[actorId] is Mirror) "$base copies Player," else base
    }

    private fun queuePass(actorId: Int) {
        pending.addLast(
            PendingLog(kind = LogEventKind.PASS) {
                log += RoundLogEvent("${logNameForAction(actorId)} passes.")
                attacksThisRound[actorId] = 0
                guessesThisRound[actorId] = 0
                passStreaks[actorId] = (passStreaks[actorId] ?: 0) + 1
            }
        )
    }

    private fun queueGuessAction(actorId: Int, targetIds: List<Int>, guess: Int) {
        if (targetIds.isEmpty()) {
            queuePass(actorId)
            return
        }

        if (hasReachedStormfrontTargetingLimit()) {
            queuePass(actorId)
            return
        }

        if (targetIds.any { isUntargetableFromWeather(it) }) {
            val blockedTarget = targetIds.first { isUntargetableFromWeather(it) }
            pending.addLast(
                PendingLog(kind = LogEventKind.PASS) {
                    log += RoundLogEvent(
                        "${displayNameFor(actorId)} tried to target ${
                            displayNameFor(
                                blockedTarget
                            )
                        }, but weather protects them this round. (pass)"
                    )
                    attacksThisRound[actorId] = 0
                    guessesThisRound[actorId] = 0
                    passStreaks[actorId] = (passStreaks[actorId] ?: 0) + 1
                }
            )
            return
        }

        if (targetIds.distinct().size != targetIds.size) {
            pending.addLast(
                PendingLog(kind = LogEventKind.PASS) {
                    log += RoundLogEvent("${displayNameFor(actorId)} made an invalid targeting action and must pass.")
                    attacksThisRound[actorId] = 0
                    guessesThisRound[actorId] = 0
                    passStreaks[actorId] = (passStreaks[actorId] ?: 0) + 1
                }
            )
            return
        }

        if (targetIds.any { it in targetedThisRound }) {
            val takenTarget = targetIds.first { it in targetedThisRound }
            pending.addLast(
                PendingLog(kind = LogEventKind.PASS) {
                    log += RoundLogEvent(
                        "${displayNameFor(actorId)} tried to target ${
                            displayNameFor(
                                takenTarget
                            )
                        }, but they were already targeted. (pass)"
                    )
                    attacksThisRound[actorId] = 0
                    guessesThisRound[actorId] = 0
                    passStreaks[actorId] = (passStreaks[actorId] ?: 0) + 1
                }
            )
            return
        }

        if (guess != 0 && firstNonZeroGuessThisRound == null) {
            firstNonZeroGuessThisRound = guess
        }
        if (firstTargetingActorId == null) {
            firstTargetingActorId = actorId
            if (actorId == HUMAN_ID) {
                when (currentWeatherId()) {
                    "low_pressure" -> unlockWeatherAchievement("low_pressure_set_the_pressure")
                    "windshear" -> unlockWeatherAchievement("windshear_against_the_wind")
                    "static_charge" -> unlockWeatherAchievement("static_charge_first_strike")
                }
            }
        }

        if (actorId == HUMAN_ID) {
            if (currentWeatherId() == "tornado") {
                unlockWeatherAchievement("tornado_eye_of_the_storm")
            }
            if (currentWeatherId() == "crosswinds" &&
                targetIds.size == 2 &&
                targetIds.all { selectionsThisRound[it] == guess }
            ) {
                unlockWeatherAchievement("crosswinds_two_birds_one_guess")
            }
        }

        targetIds.forEach { targetId ->
            targetedThisRound += targetId
            if (targetId == HUMAN_ID) {
                gameHumanWasTargeted = true
                gameBotsThatTargetedHuman += actorId
                if (actorId in gameAvengedBotIdsAgainstHuman) {
                    gameVendetta = true
                }
            }
            if (actorId == HUMAN_ID && roundNumber == 1 && archetypeById[targetId] is Nemesis) {
                gameWrongGuyPal = true
            }
        }

        if (archetypeById[actorId] is Avenger && targetIds.contains(HUMAN_ID)) {
            val avengedId = lastRoundAttacks[HUMAN_ID]?.takeIf { it != 0 && it != actorId } ?: actorId
            gameAvengedBotIdsAgainstHuman += avengedId
            if (avengedId in gameBotsThatTargetedHuman) {
                gameVendetta = true
            }
        }

        if (archetypeById[actorId] is Bully && guess == 3) {
            gamePickOnSomeoneYourSize = true
        }

        attacksThisRound[actorId] = targetIds.first()
        guessesThisRound[actorId] = guess
        passStreaks[actorId] = 0
        targetingActionsTakenThisRound += 1
        activeTargetArrowActorId = actorId
        activeTargetArrowTargetIds = targetIds

        if (actorId == HUMAN_ID &&
            currentWeatherId() == "stormfront" &&
            weatherHas(WeatherEffectTag.LIMIT_TARGETING_ACTIONS_TO_HALF_ROUNDED_DOWN) &&
            hasReachedStormfrontTargetingLimit()
        ) {
            unlockWeatherAchievement("stormfront_cut_off")
        }

        val targetNames = targetIds.joinToString(" and ") { displayNameFor(it) }
        enqueueOther {
            log += RoundLogEvent("${logNameForAction(actorId)} targets $targetNames and guesses $guess.")
        }

        targetIds.forEach { targetId ->
            enqueueOther {
                if (!revealedThisRound.containsKey(targetId)) {
                    val c = selectionsThisRound[targetId]!!
                    revealedThisRound[targetId] = c
                    players.firstOrNull { it.id == targetId }?.revealedChoice = c
                }
                val actual = revealedThisRound[targetId]!!
                players.firstOrNull { it.id == targetId }?.revealedChoice = actual
                log += RoundLogEvent("${displayNameFor(targetId)} is revealed: $actual")
            }

            enqueueOther {
                resolveGuessOutcome(actorId = actorId, targetId = targetId, guess = guess)
            }
        }
    }

    private fun resolveGuessOutcome(actorId: Int, targetId: Int, guess: Int) {
        val actor = players.first { it.id == actorId }
        val target = players.first { it.id == targetId }
        val actual = revealedThisRound[targetId]!!
        val actorArch = archetypeById[actorId]
        val targetArch = archetypeById[targetId]

        if (actorId == HUMAN_ID && difficulty != Difficulty.EASY) {
            statsStore?.let { store ->
                val s = store.load()
                s.totalGuesses += 1
                if (guess == actual) s.correctGuesses += 1
                if (actual == 0 && guess != 0) s.timesTrickedByZero += 1
                store.save(s)
            }
        }

        if (targetId == HUMAN_ID && actual == 0 && guess != 0) {
            gameHumanTrickedBotWithZero = true
        }

        if (guess == actual) {
            currentRoundCorrectlyGuessedTargetIds += targetId

            if (targetId == HUMAN_ID) {
                if (actorArch is Chaos) gameChaosTheory = true
                if (actorArch is Lurker && actual == 3) gameShadowStep = true
                if (actorArch is Auditor) gameTaxPaid = true
                if (actorArch is Scout && roundNumber == 1) gameFirstFlood = true
                if (actorArch is Cynic) gameCynicGuessedHumanCorrectly = true
            }

            if (guess == 0) {
                setHatHolder(actorId)
                if (actorId == HUMAN_ID && currentWeatherId() == "drought") {
                    unlockWeatherAchievement("drought_dry_spell")
                }
                log += RoundLogEvent("${displayNameFor(actorId)} nailed a 0 and takes the Hat.")
            } else {
                val reward = rewardForCorrectGuess(actorId, guess)
                if (weatherHas(WeatherEffectTag.USE_CUP_TO_CUP_TRANSFERS)) {
                    val moved = transferMarbles(from = target, to = actor, amount = reward)
                    if (actorId == HUMAN_ID) {
                        when (currentWeatherId()) {
                            "downpour" -> if (guess == 1 && moved > 0) unlockWeatherAchievement("downpour_soaking_it_in")
                            "sunny_day" -> if (guess == 3 && moved > 0) unlockWeatherAchievement("sunny_day_bright_strategy")
                            "hail" -> if (guess == 3 && moved > 0) unlockWeatherAchievement("hail_ice_storm")
                            "sleet" -> if (moved > 0) unlockWeatherAchievement("sleet_cold_exchange")
                            "high_pressure" -> if (moved > 0) unlockWeatherAchievement("high_pressure_one_shot")
                        }
                    }
                    log += RoundLogEvent(
                        "${displayNameFor(actorId)} was correct and takes $moved from ${
                            displayNameFor(
                                targetId
                            )
                        }."
                    )
                } else {
                    val awarded = applyPositiveGain(actor, reward)
                    if (actorId == HUMAN_ID) {
                        when (currentWeatherId()) {
                            "downpour" -> if (guess == 1 && awarded > 0) unlockWeatherAchievement("downpour_soaking_it_in")
                            "sunny_day" -> if (guess == 3 && awarded > 0) unlockWeatherAchievement("sunny_day_bright_strategy")
                            "hail" -> if (guess == 3 && awarded > 0) unlockWeatherAchievement("hail_ice_storm")
                            "high_pressure" -> if (awarded > 0) unlockWeatherAchievement("high_pressure_one_shot")
                        }
                    }
                    log += RoundLogEvent("${displayNameFor(actorId)} was correct and gains $awarded.")
                }
            }

            if (actorId == HUMAN_ID) {
                gameHumanCorrectGuesses += 1
                humanCorrectGuessStreak += 1

                if (!unlockedDumbLuckThisGame && roundNumber == 1 && guess == 3) {
                    unlockedDumbLuckThisGame = true
                }

                if (!unlockedOnARollThisGame && humanCorrectGuessStreak >= 3) {
                    unlockedOnARollThisGame = true
                }

                val arch = archetypeById[targetId]
                if (arch is Colluder) {
                    when (arch.displayName) {
                        "Romeo" -> gameHumanCorrectRomeo = true
                        "Juliet" -> gameHumanCorrectJuliet = true
                    }
                }

                if (guess == 1 && actual == 1 && targetArch is Limper) {
                    gameSlowAndSteady = true
                }

                if (guess == 3 && actual == 3) {
                    if (targetArch is Strobe) strobeCorrect3Count += 1
                    if (targetArch is Glutton) gluttonCorrect3Count += 1
                    if (targetArch is Pacifist) pacifistCorrect3Count += 1
                }
            }

            return
        }

        if (actorId == HUMAN_ID) {
            humanCorrectGuessStreak = 0
            gameHumanPerfectBonusBroken = true
            if (targetArch is Cynic) {
                gameHumanGuessedCynicWrong = true
            }
        }

        if (targetId == HUMAN_ID && actorArch is Jester && actual != 0) {
            gameAltComedy = true
        }

        if (actual == 0) {
            val penalty = wrongZeroPenaltyForActor(actorId)
            when {
                penalty < 0 -> {
                    if (weatherHas(WeatherEffectTag.USE_CUP_TO_CUP_TRANSFERS)) {
                        val moved = transferMarbles(from = actor, to = target, amount = -penalty)
                        setHatHolder(actorId)
                        if (actorId == HUMAN_ID && currentWeatherId() == "drought") {
                            unlockWeatherAchievement("drought_dry_spell")
                        }
                        log += RoundLogEvent(
                            "${displayNameFor(actorId)} was wrong on a 0 and gives $moved to ${
                                displayNameFor(
                                    targetId
                                )
                            } (HAT moves to ${displayNameFor(actorId)})."
                        )
                    } else {
                        val lost = removeMarblesToBowl(actor, -penalty)
                        setHatHolder(actorId)
                        if (actorId == HUMAN_ID && currentWeatherId() == "drought") {
                            unlockWeatherAchievement("drought_dry_spell")
                        }
                        log += RoundLogEvent(
                            "${displayNameFor(actorId)} was wrong on a 0, loses $lost (HAT moves to ${
                                displayNameFor(
                                    actorId
                                )
                            })."
                        )
                    }
                }

                penalty > 0 -> {
                    val awarded = applyPositiveGain(actor, reduceTargetedGain(penalty))
                    setHatHolder(actorId)
                    if (actorId == HUMAN_ID) {
                        when (currentWeatherId()) {
                            "rainbow" -> if (awarded > 0) unlockWeatherAchievement("rainbow_silver_lining")
                            "lightning_storm" -> if (awarded > 0) unlockWeatherAchievement("lightning_storm_strike_twice")
                            "high_pressure" -> if (awarded > 0) unlockWeatherAchievement("high_pressure_one_shot")
                        }
                        if (currentWeatherId() == "drought") {
                            unlockWeatherAchievement("drought_dry_spell")
                        }
                    }
                    log += RoundLogEvent(
                        "${displayNameFor(actorId)} was wrong on a 0, gains $awarded (HAT moves to ${
                            displayNameFor(
                                actorId
                            )
                        })."
                    )
                }

                else -> {
                    setHatHolder(actorId)
                    if (actorId == HUMAN_ID && currentWeatherId() == "drought") {
                        unlockWeatherAchievement("drought_dry_spell")
                    }
                    log += RoundLogEvent(
                        "${displayNameFor(actorId)} was wrong on a 0, loses 0 (HAT moves to ${
                            displayNameFor(
                                actorId
                            )
                        })."
                    )
                }
            }

            if (actorId == HUMAN_ID && guess != 0) {
                gameWrongGuesses += 1
                humanMadeWrongGuessThisGame = true
                gameHumanWasTrickedByZero = true
            }
        } else {
            val consolation = reduceTargetedGain(roundScoreForSelection(actual))
            if (weatherHas(WeatherEffectTag.USE_CUP_TO_CUP_TRANSFERS)) {
                val moved = transferMarbles(from = actor, to = target, amount = consolation)
                if (targetId == HUMAN_ID) {
                    if (currentWeatherId() == "sleet" && moved > 0) {
                        unlockWeatherAchievement("sleet_cold_exchange")
                    }
                    markHumanScoredFromOwnSelection(
                        actualSelection = actual,
                        moved = moved,
                        obscured = false
                    )
                }
                log += RoundLogEvent(
                    "${displayNameFor(actorId)} was wrong, ${
                        displayNameFor(
                            targetId
                        )
                    } takes $moved from them."
                )
            } else {
                val awarded = applyPositiveGain(target, consolation)
                if (targetId == HUMAN_ID) {
                    markHumanScoredFromOwnSelection(
                        actualSelection = actual,
                        moved = awarded,
                        obscured = false
                    )
                }
                log += RoundLogEvent(
                    "${displayNameFor(actorId)} was wrong, ${
                        displayNameFor(
                            targetId
                        )
                    } gains $awarded."
                )
            }
            if (targetId == HUMAN_ID &&
                actual == 0 &&
                guess != 0 &&
                currentWeatherId() == "thunderstorm"
            ) {
                unlockWeatherAchievement("thunderstorm_shock_therapy")
            }
            if (actorId == HUMAN_ID) {
                gameWrongGuesses += 1
                humanMadeWrongGuessThisGame = true
            }
        }
    }

    private fun queueTrickleAndEndRound() {
        enqueueOther {
            log += RoundLogEvent("")
            log += RoundLogEvent("TRICKLE ----------------------------------------")
        }

        val activeIds = activePlayerIds().toSet()
        val hideTrickles = weatherHas(WeatherEffectTag.HIDE_UNTARGETED_TRICKLE_REVEALS)
        val hasUntargetedActivePlayers = players.any { p ->
            p.id in activeIds && !revealedThisRound.containsKey(p.id)
        }

        if (hideTrickles && hasUntargetedActivePlayers) {
            enqueueOther {
                log += RoundLogEvent("Trickle obscured by the Whiteout.")
            }
        }

        for (p in players) {
            if (p.id !in activeIds) continue
            if (!revealedThisRound.containsKey(p.id)) {
                enqueueOther {
                    val c = selectionsThisRound[p.id]!!
                    val trickle = trickleScoreForSelection(c)
                    revealedThisRound[p.id] = c
                    p.revealedChoice = c
                    val awarded = applyTrickleGain(p, trickle)

                    if (p.id == HUMAN_ID) {
                        markHumanScoredFromOwnSelection(
                            actualSelection = c,
                            moved = awarded,
                            obscured = hideTrickles
                        )
                    }

                    if (!hideTrickles) {
                        log += RoundLogEvent("${displayNameFor(p.id)} wasn't targeted, trickles $awarded.")
                    }
                }
            }
        }

        enqueueOther {
            if (currentWeatherId() == "drizzle" && HUMAN_ID in activeIds) {
                unlockWeatherAchievement("drizzle_light_rain")
            }

            applyPostRoundWeatherResolution()

            val humanRoundGain = if (HUMAN_ID in activeIds) {
                val current = players.first { it.id == HUMAN_ID }.marbles
                current - (roundStartMarbles[HUMAN_ID] ?: current)
            } else {
                0
            }
            if (currentWeatherId() == "perfect_storm" && humanRoundGain >= 12) {
                unlockWeatherAchievement("perfect_storm_twelve")
            }

            lastRoundChoices = selectionsThisRound.toMap()
            lastRoundAttacks = attacksThisRound.toMap()
            lastRoundGuesses = guessesThisRound.toMap()
            lastRoundCorrectlyGuessedTargetIds = currentRoundCorrectlyGuessedTargetIds.toSet()

            winnerIds = resolveWinnersAfterRound()

            finalizeGameOverIfNeeded()

            val startHolder = hatStartOfRoundHolderId
            val endHolder = hatHolderId
            val overrideEligible = (startHolder != endHolder) && (endHolder != null)

            if (phase != EnginePhase.GAME_OVER && phase != EnginePhase.TIEBREAKER_CHOICE) {
                roundNumber += 1
                starterIndex = if (overrideEligible) {
                    indexOfId(endHolder!!)
                } else {
                    nextStarterIndex(activePlayerIds())
                }
            }

            selectionsThisRound.clear()
            revealedThisRound.clear()
            targetedThisRound.clear()
            attacksThisRound.clear()
            guessesThisRound.clear()
            currentRoundCorrectlyGuessedTargetIds.clear()
            turnOrder = emptyList()
            turnCursor = 0
            hatStartOfRoundHolderId = null
            playersWithPositiveScoringEventThisRound.clear()
            roundStartMarbles.clear()

            if (phase != EnginePhase.GAME_OVER && phase != EnginePhase.TIEBREAKER_CHOICE) {
                phase = EnginePhase.SELECT
            }

            if (phase == EnginePhase.GAME_OVER) {
                gameWrongGuesses = 0
            }
        }
    }



    private fun finalizeGameOverIfNeeded() {
        if (phase == EnginePhase.GAME_OVER) {
            if (gameEndedInBotTie) {
                log += RoundLogEvent("")
                log += RoundLogEvent("GAME OVER --------------------------------------")
                log += RoundLogEvent("TIE GAME BETWEEN BOTS")
                log += RoundLogEvent("-----------------------------------------------")
                log += RoundLogEvent("")
            } else if (winnerIds.isNotEmpty()) {
                val winnersText = winnerIds.joinToString(", ") { displayNameFor(it) }
                val top = players.first { it.id == winnerIds.first() }.marbles

                log += RoundLogEvent("")
                log += RoundLogEvent("GAME OVER --------------------------------------")
                log += RoundLogEvent("WINNER: $winnersText with $top")
                log += RoundLogEvent("-----------------------------------------------")
                log += RoundLogEvent("")
            }

            finalizeWeatherAchievementsAtGameEnd()

            statsStore?.let { store ->
                val s = store.load()
                val humanFinal = players.first { it.id == HUMAN_ID }.marbles
                val humanWon = winnerIds.contains(HUMAN_ID)
                val winningArchetypes = winnerIds.mapNotNull { archetypeById[it]?.displayName }
                val bossArchetypeName = currentBossId?.let { archetypeById[it]?.displayName }

                // Always count Easy games so Normal mode unlocks after playing Easy
                when (difficulty) {
                    Difficulty.EASY -> {
                        s.easyGames += 1
                        if (humanWon) s.easyWins += 1
                    }

                    Difficulty.NORMAL -> {
                        s.normalGames += 1
                        if (humanWon) s.normalWins += 1
                    }

                    Difficulty.HARD -> {
                        s.hardGames += 1
                        if (humanWon) s.hardWins += 1
                    }
                }

// Easy mode only gets these two achievements
                if (difficulty == Difficulty.EASY) {
                    if (!s.firstGameCompleted) {
                        s.firstGameCompleted = true
                        log += RoundLogEvent("*** Achievement Unlocked: First Drip - Completed a game! ***")
                    }
                    if (humanWon && !s.wonEasy) {
                        s.wonEasy = true
                        log += RoundLogEvent("*** Achievement Unlocked: Comp Stomp - Won on Easy! ***")
                    }
                }
// Normal and Hard get all other achievements
                else {
                    s.totalGames += 1
                    s.totalMarblesAcrossGames += humanFinal.toLong()
                    if (humanWon) {
                        s.totalWins += 1
                    }

                    if (!s.played13Games && s.totalGames >= 13) {
                        s.played13Games = true
                        log += RoundLogEvent("*** Achievement Unlocked: Amateur - Play 13 games! ***")
                    }

                    if (!s.played113Games && s.totalGames >= 113) {
                        s.played113Games = true
                        log += RoundLogEvent("*** Achievement Unlocked: Professional - Play 113 games! ***")
                    }

                    if (!s.played1113Games && s.totalGames >= 1113) {
                        s.played1113Games = true
                        log += RoundLogEvent("*** Achievement Unlocked: Expert - Play 1,113 games! ***")
                    }

                    if (!s.has113MarblesTotal && s.totalMarblesAcrossGames >= 113L) {
                        s.has113MarblesTotal = true
                        log += RoundLogEvent("*** Achievement Unlocked: Bucket Filler - Gain 113 marbles across games! ***")
                    }

                    if (!s.has1113MarblesTotal && s.totalMarblesAcrossGames >= 1113L) {
                        s.has1113MarblesTotal = true
                        log += RoundLogEvent("*** Achievement Unlocked: Tub Filler - Gain 1,113 marbles across games! ***")
                    }

                    if (!s.has11113MarblesTotal && s.totalMarblesAcrossGames >= 11113L) {
                        s.has11113MarblesTotal = true
                        log += RoundLogEvent("*** Achievement Unlocked: Pool Filler - Gain 11,113 marbles across games! ***")
                    }

                    // All other achievements (Perfect Puddler, Dumb Luck, On a Roll, etc.) stay here
                    if (!s.pacifistGame && !gameHumanMadeGuess) {
                        s.pacifistGame = true
                        log += RoundLogEvent("*** Achievement Unlocked: Pacifist (Game) - Finished a game without guessing! ***")
                    }

                    if (!s.reachedRound6 && gameRoundReached >= 6) {
                        s.reachedRound6 = true
                        log += RoundLogEvent("*** Achievement Unlocked: Idle Hands - Reached round 6! ***")
                    }

                    if (!s.firstTheFool && gameHumanWasTrickedByZero) {
                        s.firstTheFool = true
                        log += RoundLogEvent("*** Achievement Unlocked: The Fool - Get tricked by a 0! ***")
                    }

                    if (!s.firstZeroTrap && gameHumanTrickedBotWithZero) {
                        s.firstZeroTrap = true
                        log += RoundLogEvent("*** Achievement Unlocked: Zero Trap - Trick someone with a 0! ***")
                    }

                    if (!s.zeroHeroUnlocked && s.firstTheFool && s.firstZeroTrap) {
                        s.zeroHeroUnlocked = true
                        log += RoundLogEvent("*** Achievement Unlocked: Zero Hero - Experience both sides of the 0! ***")
                    }

                    if (!s.dumbLuck && unlockedDumbLuckThisGame) {
                        s.dumbLuck = true
                        log += RoundLogEvent("*** Achievement Unlocked: Dumb Luck - Correctly guessed a 3 in round 1! ***")
                    }

                    if (!s.onARoll && unlockedOnARollThisGame) {
                        s.onARoll = true
                        log += RoundLogEvent("*** Achievement Unlocked: On a Roll - Get 3 correct guesses in a row in one game! ***")
                    }

                    if (!s.justPressEverything &&
                        gameHumanPassedAtLeastOnce &&
                        gameHumanChoicesUsed.containsAll(setOf(0, 1, 3)) &&
                        gameHumanGuessesUsed.containsAll(setOf(1, 3))
                    ) {
                        s.justPressEverything = true
                        log += RoundLogEvent("*** Achievement Unlocked: Just Press Everything - Do every action possible in a game ***")
                    }

                    if (!s.playedAllDifficulties && s.normalGames > 0 && s.hardGames > 0) {
                        s.playedAllDifficulties = true
                        log += RoundLogEvent("*** Achievement Unlocked: Tourist - Played on Normal and Hard! ***")
                    }

                    if (!s.chaosTheory && gameChaosTheory) {
                        s.chaosTheory = true
                        log += RoundLogEvent("*** Achievement Unlocked: Chaos Theory - Have your choice guessed correctly by Chaos! ***")
                    }

                    if (!s.shadowStep && gameShadowStep) {
                        s.shadowStep = true
                        log += RoundLogEvent("*** Achievement Unlocked: Shadow Step - Have Lurker correctly guess your 3! ***")
                    }

                    if (!s.vendetta && gameVendetta) {
                        s.vendetta = true
                        log += RoundLogEvent("*** Achievement Unlocked: Vendetta - Get targeted by Avenger and the bot being avenged in one game! ***")
                    }

                    if (!s.taxPaid && gameTaxPaid) {
                        s.taxPaid = true
                        log += RoundLogEvent("*** Achievement Unlocked: Tax Paid - Get correctly guessed by Auditor! ***")
                    }

                    if (!s.altComedy && gameAltComedy) {
                        s.altComedy = true
                        log += RoundLogEvent("*** Achievement Unlocked: Alt Comedy - Get guessed wrong by Jester without choosing 0! ***")
                    }

                    if (!s.pickOnSomeoneYourSize && gamePickOnSomeoneYourSize) {
                        s.pickOnSomeoneYourSize = true
                        log += RoundLogEvent("*** Achievement Unlocked: Pick on Someone Your Size - Witness Bully target someone for 3! ***")
                    }

                    if (!s.badFaith && gameHumanGuessedCynicWrong && gameCynicGuessedHumanCorrectly) {
                        s.badFaith = true
                        log += RoundLogEvent("*** Achievement Unlocked: Bad Faith - Guess wrong on Cynic and have Cynic guess you correctly! ***")
                    }

                    if (!humanWon && !s.copycat && winningArchetypes.contains("Echo")) {
                        s.copycat = true
                        log += RoundLogEvent("*** Achievement Unlocked: Copycat - Lose when Echo wins! ***")
                    }

                    if (!humanWon && !s.karma && pacifistCorrect3Count >= 3) {
                        s.karma = true
                        log += RoundLogEvent("*** Achievement Unlocked: Karma - Lose after correctly guessing Pacifist's 3 three times! ***")
                    }

                    if (humanWon) {

                        if (!humanMadeWrongGuessThisGame) {
                            s.perfectGames += 1
                            if (!s.firstPerfectWin) {
                                s.firstPerfectWin = true
                                log += RoundLogEvent("*** Achievement Unlocked: Perfect Puddler - Win without a wrong guess! ***")
                            }
                        }

                        if (!s.wonWith18Marbles && humanFinal >= 18) {
                            s.wonWith18Marbles = true
                            log += RoundLogEvent("*** Achievement Unlocked: Is That Legal? - Win with 18+ marbles! ***")
                        }

                        if (!s.pacifistWin && !gameHumanMadeGuess) {
                            s.pacifistWin = true
                            log += RoundLogEvent("*** Achievement Unlocked: Pacifist - Win without guessing! ***")
                        }

                        if (!s.shakespeareWin && gameHumanCorrectRomeo && gameHumanCorrectJuliet) {
                            s.shakespeareWin = true
                            log += RoundLogEvent("*** Achievement Unlocked: Wherefore Art Thou - Win after correctly guessing Romeo and Juliet! ***")
                        }

                        if (!s.drySeasonWin && !gameHumanEverChose3) {
                            s.drySeasonWin = true
                            log += RoundLogEvent("*** Achievement Unlocked: Dry Season - Win without ever choosing 3! ***")
                        }

                        if (!s.ghostCupWin && !gameHumanWasTargeted) {
                            s.ghostCupWin = true
                            log += RoundLogEvent("*** Achievement Unlocked: Ghost Cup - Win without being targeted! ***")
                        }

                        if (!s.hatFinisher && startedRoundBecauseHat) {
                            s.hatFinisher = true
                            log += RoundLogEvent("*** Achievement Unlocked: Hat Trick - Win after starting because you had the Hat! ***")
                        }

                        if (!s.caughtTheStrobe && strobeCorrect3Count >= 2) {
                            s.caughtTheStrobe = true
                            log += RoundLogEvent("*** Achievement Unlocked: Caught the Strobe - Correctly guess Strobe's 3 twice in one game! ***")
                        }

                        if (!s.dieting && gluttonCorrect3Count >= 3) {
                            s.dieting = true
                            log += RoundLogEvent("*** Achievement Unlocked: Dieting - Correctly guess Glutton's 3 three times in one game! ***")
                        }

                        if (!s.wrongGuyPal && gameWrongGuyPal) {
                            s.wrongGuyPal = true
                            log += RoundLogEvent("*** Achievement Unlocked: Wrong Guy, Pal - Win after attacking Nemesis in round 1! ***")
                        }

                        if (!s.anointed && gameHumanSelectedByCabal) {
                            s.anointed = true
                            log += RoundLogEvent("*** Achievement Unlocked: Anointed - Win after being selected by Cabal! ***")
                        }

                        if (!s.slowAndSteady && gameSlowAndSteady) {
                            s.slowAndSteady = true
                            log += RoundLogEvent("*** Achievement Unlocked: Slow and Steady - Win after correctly guessing Limper's 1! ***")
                        }

                        if (!s.firstFlood && gameFirstFlood) {
                            s.firstFlood = true
                            log += RoundLogEvent("*** Achievement Unlocked: First Flood - Have Scout guess your round 1 choice and still win! ***")
                        }

                        if (difficulty == Difficulty.HARD) {
                            when (bossArchetypeName) {
                                "Hunter" -> if (!s.beatHunter) {
                                    s.beatHunter = true
                                    log += RoundLogEvent("*** Achievement Unlocked: Marked Prey - Win against Hunter! ***")
                                }
                                "Seer" -> if (!s.beatSeer) {
                                    s.beatSeer = true
                                    log += RoundLogEvent("*** Achievement Unlocked: Blind Prophet - Win against Seer! ***")
                                }
                                "Mirror" -> if (!s.beatMirror) {
                                    s.beatMirror = true
                                    log += RoundLogEvent("*** Achievement Unlocked: Broken Reflection - Win against Mirror! ***")
                                }
                            }

                            if (!s.bossSlayer && s.beatHunter && s.beatSeer && s.beatMirror) {
                                s.bossSlayer = true
                                log += RoundLogEvent("*** Achievement Unlocked: Boss Slayer - Win against every boss! ***")
                            }
                        }

                        when (difficulty) {
                            Difficulty.NORMAL -> {
                                if (!s.wonNormal) {
                                    s.wonNormal = true
                                    log += RoundLogEvent("*** Achievement Unlocked: Pattern Finder - Won on Normal! ***")
                                }
                                s.normalWins += 1
                            }

                            Difficulty.HARD -> {
                                if (!s.wonHard) {
                                    s.wonHard = true
                                    log += RoundLogEvent("*** Achievement Unlocked: TR1CKL3! - Won on Hard and unlocked Weather! ***")
                                }
                                s.hardWins += 1
                            }

                            Difficulty.EASY -> Unit
                        }

                        if (!s.won13thGame && s.totalWins >= 13) {
                            s.won13thGame = true
                            log += RoundLogEvent("*** Achievement Unlocked: Trickle Pro - Won 13 games! ***")
                        }

                        if (!s.won113thGame && s.totalWins >= 113) {
                            s.won113thGame = true
                            log += RoundLogEvent("*** Achievement Unlocked: Trickle Champion - Won 113 games! ***")
                        }
                        if (!s.won1113thGame && s.totalWins >= 1113) {
                            s.won1113thGame = true
                            log += RoundLogEvent("*** Achievement Unlocked: Trickle God - Won 1113 games! ***")
                        }
                    }
                }

                store.save(s)
            }
        }

    }

    private fun applyPostRoundWeatherResolution() {
        val activeIds = activePlayerIds()
        if (activeIds.isEmpty()) return

        val positiveGains = activeIds.associateWith { pid ->
            val player = players.first { it.id == pid }
            maxOf(0, player.marbles - (roundStartMarbles[pid] ?: player.marbles))
        }

        when {
            weatherHas(WeatherEffectTag.REDISTRIBUTE_ALL_POSITIVE_NET_GAINS_TO_ALL_PLAYERS) -> {
                val pool = positiveGains.values.sum()
                if (pool <= 0) return

                activeIds.forEach { pid ->
                    val player = players.first { it.id == pid }
                    removeMarblesToBowl(player, positiveGains[pid] ?: 0)
                }

                val eachShare = pool / activeIds.size
                if (eachShare > 0) {
                    activeIds.forEach { pid ->
                        val player = players.first { it.id == pid }
                        applyPositiveGain(player, eachShare)
                    }
                }

                val remainder = pool % activeIds.size
                if (HUMAN_ID in activeIds && eachShare > 0) {
                    unlockWeatherAchievement("cold_rain_shared_storm")
                }
                log += RoundLogEvent("Cold Rain redistributes $pool total marbles. Each active player receives $eachShare, with $remainder left in the bowl.")
            }

            weatherHas(WeatherEffectTag.ONLY_TOP_ROUND_SCORERS_KEEP_POINTS) -> {
                val bestGain = positiveGains.values.maxOrNull() ?: 0
                if (bestGain <= 0) return

                val keepers = positiveGains.filterValues { it == bestGain }.keys
                activeIds.forEach { pid ->
                    if (pid !in keepers) {
                        val player = players.first { it.id == pid }
                        removeMarblesToBowl(player, positiveGains[pid] ?: 0)
                    }
                }

                if (HUMAN_ID in keepers) {
                    unlockWeatherAchievement("thunderhead_top_of_the_storm")
                }
                val keeperNames = keepers.joinToString(", ") { displayNameFor(it) }
                log += RoundLogEvent("Thunderhead keeps round gains only for: $keeperNames.")
            }

            weatherHas(WeatherEffectTag.ONLY_LOWEST_POSITIVE_ROUND_SCORERS_KEEP_POINTS) -> {
                val qualifyingGain = positiveGains.values.filter { it > 0 }.minOrNull() ?: return
                val keepers = positiveGains.filterValues { it == qualifyingGain }.keys
                activeIds.forEach { pid ->
                    if (pid !in keepers) {
                        val player = players.first { it.id == pid }
                        removeMarblesToBowl(player, positiveGains[pid] ?: 0)
                    }
                }

                if (HUMAN_ID in keepers) {
                    unlockWeatherAchievement("cool_breeze_quiet_advantage")
                }
                val keeperNames = keepers.joinToString(", ") { displayNameFor(it) }
                log += RoundLogEvent("Cool Breeze keeps round gains only for: $keeperNames.")
            }
        }
    }

    private fun activePlayerIds(): List<Int> {
        val restricted = tiebreakerParticipantIds
        return if (restricted == null) players.map { it.id } else players.map { it.id }
            .filter { it in restricted }
    }

    private fun nextStarterIndex(activeIds: List<Int>): Int {
        if (activeIds.isEmpty()) return (starterIndex + 1) % players.size
        for (offset in 1..players.size) {
            val idx = (starterIndex + offset) % players.size
            if (players[idx].id in activeIds) return idx
        }
        return starterIndex
    }

    private fun resolveWinnersAfterRound(): List<Int> {
        gameEndedInBotTie = false
        return if (tiebreakerParticipantIds != null) {
            resolveActiveTiebreakerState()
        } else {
            resolveThresholdWinners()
        }
    }

    private fun resolveThresholdWinners(): List<Int> {
        val winners = players.filter { it.marbles >= WIN_SCORE }
        if (winners.isEmpty()) return emptyList()

        val top = winners.maxOf { it.marbles }
        val leaders = winners.filter { it.marbles == top }
        return resolveTieGroup(leaders, isFreshTie = true)
    }

    private fun resolveActiveTiebreakerState(): List<Int> {
        if (tiebreakerRoundsRemaining > 1) {
            tiebreakerRoundsRemaining -= 1
            log += RoundLogEvent("TIEBREAKER CONTINUES: $tiebreakerRoundsRemaining round(s) remain in this phase.")
            phase = EnginePhase.SELECT
            return emptyList()
        }

        val participantIds = tiebreakerParticipantIds ?: return emptyList()
        val contenders = players.filter { it.id in participantIds }
        val top = contenders.maxOf { it.marbles }
        val leaders = contenders.filter { it.marbles == top }

        return if (leaders.size == 1) {
            clearTiebreakerState()
            phase = EnginePhase.GAME_OVER
            listOf(leaders.first().id)
        } else {
            val oldStageIndex = tiebreakerStageIndex
            clearTiebreakerState()
            resolveTieGroup(leaders, isFreshTie = false, stageJustCompleted = oldStageIndex)
        }
    }

    private fun resolveTieGroup(
        leaders: List<PlayerState>,
        isFreshTie: Boolean,
        stageJustCompleted: Int = -1
    ): List<Int> {
        if (leaders.isEmpty()) return emptyList()
        if (leaders.size == 1) {
            phase = EnginePhase.GAME_OVER
            return listOf(leaders.first().id)
        }

        val includesHuman = leaders.any { it.id == HUMAN_ID }
        if (!includesHuman) {
            phase = EnginePhase.GAME_OVER
            gameEndedInBotTie = true
            return emptyList()
        }

        if (leaders.size == 2) {
            return resolveTwoPlayerTiebreaker(leaders[0].id, leaders[1].id)
        }

        val nextStageRounds = when {
            isFreshTie -> 3
            stageJustCompleted == 0 -> 2
            stageJustCompleted == 1 -> 1
            else -> 0
        }

        if (nextStageRounds <= 0) {
            phase = EnginePhase.GAME_OVER
            return listOf(HUMAN_ID)
        }

        tiebreakerParticipantIds = leaders.map { it.id }.toSet()
        tiebreakerStageIndex = when {
            isFreshTie -> 0
            stageJustCompleted == 0 -> 1
            else -> 2
        }
        tiebreakerRoundsRemaining = nextStageRounds

        val names = leaders.joinToString(", ") { displayNameFor(it.id) }
        log += RoundLogEvent("TIEBREAKER: $names remain tied for the lead.")
        log += RoundLogEvent("Only those players act for the next $nextStageRounds round(s).")
        phase = EnginePhase.SELECT
        return emptyList()
    }

    private fun resolveTwoPlayerTiebreaker(firstId: Int, secondId: Int): List<Int> {
        log += RoundLogEvent("TIEBREAKER: ${displayNameFor(firstId)} and ${displayNameFor(secondId)} enter the final duel.")

        var firstRoll: Int
        var secondRoll: Int
        do {
            firstRoll = listOf(0, 1, 3)[rng.nextInt(3)]
            secondRoll = listOf(0, 1, 3)[rng.nextInt(3)]
            log += RoundLogEvent(
                "${displayNameFor(firstId)} rolls $firstRoll. ${
                    displayNameFor(
                        secondId
                    )
                } rolls $secondRoll."
            )
        } while (firstRoll == secondRoll)

        val chooserId: Int
        val guesserId: Int
        if (firstRoll < secondRoll) {
            chooserId = firstId
            guesserId = secondId
        } else {
            chooserId = secondId
            guesserId = firstId
        }

        if (chooserId == HUMAN_ID || guesserId == HUMAN_ID) {
            pendingTiebreakerChooserId = chooserId
            pendingTiebreakerGuesserId = guesserId
            pendingTiebreakerHumanIsChooser = chooserId == HUMAN_ID
            pendingTiebreakerBotValue = if (rng.nextBoolean()) 1 else 3
            phase = EnginePhase.TIEBREAKER_CHOICE
            log += RoundLogEvent("${displayNameFor(chooserId)} had the lower roll and secretly chooses between 1 and 3.")
            return emptyList()
        }

        val chooserPick = if (rng.nextBoolean()) 1 else 3
        val guesserGuess = if (rng.nextBoolean()) 1 else 3

        finishTwoPlayerTiebreaker(
            chooserId = chooserId,
            guesserId = guesserId,
            chooserPick = chooserPick,
            guesserGuess = guesserGuess
        )
        phase = EnginePhase.GAME_OVER
        return winnerIds
    }

    private fun finishTwoPlayerTiebreaker(
        chooserId: Int,
        guesserId: Int,
        chooserPick: Int,
        guesserGuess: Int
    ) {
        log += RoundLogEvent("${displayNameFor(guesserId)} guesses $guesserGuess.")
        log += RoundLogEvent("${displayNameFor(chooserId)} chose $chooserPick.")

        winnerIds = if (guesserGuess == chooserPick) {
            log += RoundLogEvent("${displayNameFor(guesserId)} is correct and wins the tiebreaker.")
            listOf(guesserId)
        } else {
            log += RoundLogEvent(
                "${displayNameFor(guesserId)} is wrong, so ${
                    displayNameFor(
                        chooserId
                    )
                } wins the tiebreaker."
            )
            listOf(chooserId)
        }
    }

    private fun clearPendingTiebreakerDuel() {
        pendingTiebreakerChooserId = null
        pendingTiebreakerGuesserId = null
        pendingTiebreakerHumanIsChooser = false
        pendingTiebreakerBotValue = null
    }

    private fun clearTiebreakerState() {
        tiebreakerParticipantIds = null
        tiebreakerStageIndex = -1
        tiebreakerRoundsRemaining = 0
        clearPendingTiebreakerDuel()
    }

    private fun enqueueOther(block: () -> Unit) {
        pending.addLast(PendingLog(LogEventKind.OTHER, block))
    }

    private fun chooseBossKind(): BossKind {
        val choices = BossKind.values().filter { it != lastBossKind }
        val chosen = choices.random(rng)
        lastBossKind = chosen
        return chosen
    }

    private fun archetypeForBossKind(kind: BossKind): Archetype =
        when (kind) {
            BossKind.HUNTER -> Hunter()
            BossKind.SEER -> Seer()
            BossKind.MIRROR -> Mirror()
        }

    private fun assignRandomArchetypesToBots() {
        archetypeById.clear()
        memById.clear()
        currentBossId = null

        val nonColluderPool: MutableList<Archetype> = mutableListOf(
            Strobe(), Chaos(), Glutton(), Lurker(),
            Avenger(), Nemesis(), Auditor(), Cabal(),
            Limper(), Scout(), Jester(), Pacifist(), Bully(),
            Cynic(), Echo(), Pitfall()
        )

        val botIds = players.map { it.id }.filter { it != HUMAN_ID }.toMutableList()

        val bossIds = if (difficulty == Difficulty.HARD && botIds.isNotEmpty()) {
            val bossId = botIds.random(rng)
            currentBossId = bossId
            val kind = chooseBossKind()
            archetypeById[bossId] = archetypeForBossKind(kind)
            memById[bossId] = BotMemory()
            setOf(bossId)
        } else {
            emptySet()
        }

        val normalBotIds = botIds.filter { it !in bossIds }.toMutableList()

        val roll = rng.nextInt(100)
        val maxColluders = minOf(2, normalBotIds.size)
        val includeBoth = roll >= 75 && maxColluders >= 2
        val includeSingle = roll in 45..74 && maxColluders >= 1

        val colludersToAdd = when {
            includeBoth -> 2
            includeSingle -> 1
            else -> 0
        }

        val neededFromNonColluders = normalBotIds.size - colludersToAdd
        nonColluderPool.shuffle(rng)
        val selected = nonColluderPool.take(neededFromNonColluders).toMutableList()

        normalBotIds.shuffle(rng)

        val colluderIds = normalBotIds.take(colludersToAdd)
        val remainingIds = normalBotIds.drop(colludersToAdd)

        val NO_PARTNER = -999

        if (colludersToAdd == 1) {
            val id = colluderIds[0]
            val isRomeo = rng.nextBoolean()
            val arch =
                if (isRomeo) Colluder(code = "J", displayName = "Romeo", partnerId = NO_PARTNER)
                else Colluder(code = "R", displayName = "Juliet", partnerId = NO_PARTNER)
            archetypeById[id] = arch
            memById[id] = BotMemory()
        } else if (colludersToAdd == 2) {
            val a = colluderIds[0]
            val b = colluderIds[1]
            archetypeById[a] = Colluder(code = "J", displayName = "Romeo", partnerId = b)
            memById[a] = BotMemory()
            archetypeById[b] = Colluder(code = "R", displayName = "Juliet", partnerId = a)
            memById[b] = BotMemory()
        }

        selected.shuffle(rng)
        for (i in remainingIds.indices) {
            val pid = remainingIds[i]
            archetypeById[pid] = selected[i]
            memById[pid] = BotMemory()
        }
    }

    private fun buildTurnOrderFromStarter(activeIds: List<Int>): List<Int> {
        if (activeIds.isEmpty()) return emptyList()
        val activeSet = activeIds.toSet()
        val order = mutableListOf<Int>()
        for (i in players.indices) {
            val idx = (starterIndex + i) % players.size
            val id = players[idx].id
            if (id in activeSet) order += id
        }
        return order
    }

    private fun indexOfId(pid: Int): Int =
        players.indexOfFirst { it.id == pid }.let { if (it >= 0) it else 0 }

    private fun targetableIdsForHuman(): List<Int> =
        if (actorMustPassBecauseAlreadyScored(HUMAN_ID) ||
            hasReachedStormfrontTargetingLimit() ||
            !actorHasEnoughLegalTargetsToTarget(HUMAN_ID) && weatherHas(WeatherEffectTag.MUST_TARGET_IF_LEGAL)
        ) {
            emptyList()
        } else {
            legalTargetIdsForActor(HUMAN_ID)
        }

    private fun displayNameFor(pid: Int): String {
        val p = players.first { it.id == pid }
        if (pid == HUMAN_ID) return p.baseName

        return when (difficulty) {
            Difficulty.EASY -> archetypeById[pid]?.displayName ?: p.baseName
            Difficulty.NORMAL, Difficulty.HARD -> p.baseName
        }
    }

    private fun snapshot(): RoundResult {
        val currentActor = when (phase) {
            EnginePhase.BOT_TURN, EnginePhase.PLAYER_TURN -> turnOrder.getOrNull(turnCursor)
            else -> null
        }
        val targetables =
            if (phase == EnginePhase.PLAYER_TURN) targetableIdsForHuman() else emptyList()

        return RoundResult(
            phase = phase,
            roundNumber = roundNumber,
            log = log.toList(),
            players = players.map { it.copy(baseName = displayNameFor(it.id)) },
            winnerIds = winnerIds,
            bannerText = bannerText,
            targetableIdsForHuman = targetables,
            currentActorId = currentActor,
            hatHolderId = hatHolderId,
            currentStarterId = players.getOrNull(starterIndex)?.id,
            lastEventKind = lastEventKind,
            currentWeatherName = currentWeatherCard?.displayName,
            currentWeatherEffect = currentWeatherCard?.effectText,
            forcedGuessForHuman = lockedGuessForRound(),
            mustTargetForHuman = phase == EnginePhase.PLAYER_TURN && actorMustTarget(HUMAN_ID),
            requiresSecondTargetForHuman = phase == EnginePhase.PLAYER_TURN && actorNeedsTwoTargetsIfTargeting(),
            activeTargetArrowActorId = activeTargetArrowActorId,
            activeTargetArrowTargetIds = activeTargetArrowTargetIds,
            marbleTransfers = latestMarbleTransfers.toList(),
            botArchetypeNamesByPlayerId = players
                .asSequence()
                .filter { it.id != HUMAN_ID }
                .mapNotNull { player ->
                    archetypeById[player.id]?.displayName?.let { archetypeName ->
                        player.id to archetypeName
                    }
                }
                .toMap(),
            smogRevealedPlayerIds = smogRevealedPlayerIds,
            humanHeldHatThisGame = gameHumanHeldHat,
            humanCorrectGuessesThisGame = gameHumanCorrectGuesses,
            humanSubmittedTargetThisGame = gameHumanSubmittedTarget,
            humanStartedAnyRoundThisGame = gameHumanStartedAnyRound,
            humanPerfectBonusIntact = !gameHumanPerfectBonusBroken,
            humanHadUntargetedOneTrickleThisGame = gameHumanHadUntargetedOneTrickle
        )
    }
}