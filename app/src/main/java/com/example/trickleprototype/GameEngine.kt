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

enum class LogEventKind { PASS, OTHER }

enum class EnginePhase {
    SETUP, SELECT, BOT_TURN, PLAYER_TURN, ROUND_END, GAME_OVER
}

data class RoundResult(
    val phase: EnginePhase,
    val log: List<RoundLogEvent>,
    val players: List<PlayerState>,
    val winnerIds: List<Int>,
    val bannerText: String?,
    val targetableIdsForHuman: List<Int>,
    val currentActorId: Int?,
    val lastEventKind: LogEventKind?
)

class GameEngine(
    private val rng: Random = Random.Default
) {
    companion object {
        const val HUMAN_ID = 1
        const val WIN_SCORE = 13
    }

    private var difficulty: Difficulty = Difficulty.NORMAL

    private val botNames = listOf(
        "Alessandro", "Barbara", "Clark", "David", "Erika", "Fred",
        "Graham", "Harry", "Ian", "Josh", "Kelly", "Lois"
    )

    private val players: MutableList<PlayerState> = mutableListOf<PlayerState>().apply {
        add(PlayerState(HUMAN_ID, "You", marbles = 0))
        for (i in 0 until 12) add(PlayerState(i + 2, botNames[i], marbles = 0))
    }



    fun setHumanName(name: String) {
        val cleaned = name.trim().ifBlank { "You" }
        players.firstOrNull { it.id == HUMAN_ID }?.baseName = cleaned
    }

    private var roundNumber: Int = 1
    private var starterIndex: Int = 0
    private var phase: EnginePhase = EnginePhase.SETUP

    private val selectionsThisRound = mutableMapOf<Int, Int>()
    private val revealedThisRound = mutableMapOf<Int, Int>()
    private val targetedThisRound = mutableSetOf<Int>()
    private val attacksThisRound = mutableMapOf<Int, Int>()

    private var turnOrder: List<Int> = emptyList()
    private var turnCursor: Int = 0

    private var hatHolderId: Int? = null
    private var hatStartOfRoundHolderId: Int? = null

    private var tiebreakerParticipantIds: Set<Int>? = null
    private var tiebreakerStageIndex: Int = -1
    private var tiebreakerRoundsRemaining: Int = 0
    private var gameEndedInBotTie: Boolean = false

    // stats: per-game trackers
    private var gameWrongGuesses: Int = 0

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

    private var humanCorrectGuessStreak: Int = 0
    private var unlockedOnARollThisGame: Boolean = false
    private var unlockedDumbLuckThisGame: Boolean = false

    private var startedRoundBecauseHat: Boolean = false // computed at startRound
    private var strobeCorrect3Count: Int = 0
    private var threePusherCorrect3Count: Int = 0

    private var lastRoundChoices: Map<Int, Int> = emptyMap()
    private var lastRoundAttacks: Map<Int, Int> = emptyMap()
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

    private var statsStore: StatsStore? = null

    init { reset() }

    fun attachStatsStore(store: StatsStore) { statsStore = store }

    fun setDifficulty(d: Difficulty) {
        difficulty = d
        if (phase == EnginePhase.SETUP) phase = EnginePhase.SELECT
    }

    fun getDifficulty(): Difficulty = difficulty
    fun getPhase(): EnginePhase = phase

    fun getPlayersSnapshot(): List<PlayerState> =
        players.map { p -> p.copy(baseName = displayNameFor(p.id)) }

    fun reset() {
        players.forEach { it.marbles = 0; it.revealedChoice = null }
        roundNumber = 1
        starterIndex = rng.nextInt(players.size)
        phase = EnginePhase.SETUP

        selectionsThisRound.clear()
        revealedThisRound.clear()
        targetedThisRound.clear()
        attacksThisRound.clear()

        turnOrder = emptyList()
        turnCursor = 0

        hatHolderId = null
        hatStartOfRoundHolderId = null
        tiebreakerParticipantIds = null
        tiebreakerStageIndex = -1
        tiebreakerRoundsRemaining = 0
        gameEndedInBotTie = false

        lastRoundChoices = emptyMap()
        lastRoundAttacks = emptyMap()
        passStreaks.clear()

        log.clear()
        bannerText = null
        winnerIds = emptyList()

        pending.clear()
        lastEventKind = null

        gameWrongGuesses = 0

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
        humanCorrectGuessStreak = 0
        unlockedOnARollThisGame = false
        unlockedDumbLuckThisGame = false
        startedRoundBecauseHat = false
        strobeCorrect3Count = 0
        threePusherCorrect3Count = 0

        assignRandomArchetypesToBots()
    }

    fun startRound(humanChoice: Int): RoundResult {
        if (phase == EnginePhase.GAME_OVER) return snapshot()
        if (phase != EnginePhase.SELECT && phase != EnginePhase.ROUND_END) return snapshot()

        gameRoundReached = maxOf(gameRoundReached, roundNumber)

        selectionsThisRound.clear()
        revealedThisRound.clear()
        targetedThisRound.clear()
        attacksThisRound.clear()
        bannerText = null
        winnerIds = emptyList()
        pending.clear()
        lastEventKind = null

        hatStartOfRoundHolderId = hatHolderId

        val starterId = players[starterIndex].id
        // Start because you have the Hat means starter == hatHolder AND hatHolder exists
        startedRoundBecauseHat = (hatHolderId != null && starterId == hatHolderId)

        val activeIds = activePlayerIds()
        turnOrder = buildTurnOrderFromStarter(activeIds)
        turnCursor = 0

        if (HUMAN_ID in activeIds) {
            selectionsThisRound[HUMAN_ID] = humanChoice
            gameHumanChoicesUsed += humanChoice
            if (humanChoice == 3) gameHumanEverChose3 = true
        }

        for (pid in 2..13) {
            if (pid !in activeIds) continue

            val arch = archetypeById[pid]!!
            val mem = memById.getOrPut(pid) { BotMemory() }

            val pub = PublicRoundInfo(
                roundIndex = roundNumber,
                startingPlayerId = starterId,
                hatHolderId = hatHolderId,
                revealedThisRound = emptyMap(),
                targetedThisRound = emptySet(),
                lastRoundChoices = lastRoundChoices,
                lastRoundAttacks = lastRoundAttacks,
                passStreaks = passStreaks.toMap(),
                myId = pid,
                playersAlive = activeIds,
                rng = rng,
                humanMarbles = if (difficulty == Difficulty.HARD) players.first { it.id == HUMAN_ID }.marbles else null
            )
            selectionsThisRound[pid] = arch.chooseDie(pub, mem).v
        }

        enqueueOther {
            log += RoundLogEvent("")
            log += RoundLogEvent("=== ROUND $roundNumber | Starter: ${displayNameFor(starterId)} ===")
        }

        phase = EnginePhase.BOT_TURN
        return stepInternalOrSnapshot()
    }

    fun step(): RoundResult {
        if (phase != EnginePhase.BOT_TURN) return snapshot()
        return stepInternalOrSnapshot()
    }

    fun submitHumanTurn(targetId: Int?, guess: Int?): RoundResult {
        if (phase != EnginePhase.PLAYER_TURN) return snapshot()
        val actorId = turnOrder.getOrNull(turnCursor) ?: return snapshot()
        if (actorId != HUMAN_ID) return snapshot()

        if (targetId == null) {
            gameHumanPassedAtLeastOnce = true
            queuePass(actorId = HUMAN_ID)
        } else {
            val validTargets = targetableIdsForHuman()
            if (targetId !in validTargets) {
                bannerText = "That target is no longer available. Pick someone else."
                return snapshot()
            }
            val g = guess ?: 1

            gameHumanMadeGuess = true
            gameHumanGuessesUsed += g

            queueGuess(actorId = HUMAN_ID, targetId = targetId, guess = g)
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

        if (actorId == HUMAN_ID) {
            phase = EnginePhase.PLAYER_TURN
            return snapshot()
        }

        queueBotTurn(actorId)
        turnCursor++
        return stepInternalOrSnapshot()
    }

    private fun queueBotTurn(actorId: Int) {
        val starterId = players[starterIndex].id
        val arch = archetypeById[actorId]!!
        val mem = memById.getOrPut(actorId) { BotMemory() }

        val playerMarbles = if (difficulty == Difficulty.HARD) players.first { it.id == HUMAN_ID }.marbles else null
        val humanTargetable = (HUMAN_ID !in targetedThisRound) && (HUMAN_ID != actorId)

        if (difficulty == Difficulty.HARD && playerMarbles != null && playerMarbles >= 10 && humanTargetable) {
            queueGuess(actorId = actorId, targetId = HUMAN_ID, guess = 3)
            passStreaks[actorId] = 0
            return
        }

        val pub = PublicRoundInfo(
            roundIndex = roundNumber,
            startingPlayerId = starterId,
            hatHolderId = hatHolderId,
            revealedThisRound = revealedThisRound.toMap(),
            targetedThisRound = targetedThisRound.toSet(),
            lastRoundChoices = lastRoundChoices,
            lastRoundAttacks = lastRoundAttacks,
            passStreaks = passStreaks.toMap(),
            myId = actorId,
            playersAlive = activePlayerIds(),
            rng = rng,
            humanMarbles = playerMarbles
        )

        when (val decision = arch.takeTurn(pub, mem)) {
            is TurnDecision.Pass -> queuePass(actorId)
            is TurnDecision.Guess -> queueGuess(actorId, decision.targetId, decision.guess.v)
        }
    }

    private fun queuePass(actorId: Int) {
        pending.addLast(
            PendingLog(kind = LogEventKind.PASS) {
                log += RoundLogEvent("${displayNameFor(actorId)} passes.")
                attacksThisRound[actorId] = 0
                passStreaks[actorId] = (passStreaks[actorId] ?: 0) + 1
            }
        )
    }

    private fun queueGuess(actorId: Int, targetId: Int, guess: Int) {
        if (targetId in targetedThisRound) {
            pending.addLast(
                PendingLog(kind = LogEventKind.PASS) {
                    log += RoundLogEvent("${displayNameFor(actorId)} tried to target ${displayNameFor(targetId)}, but they were already targeted. (pass)")
                    attacksThisRound[actorId] = 0
                    passStreaks[actorId] = (passStreaks[actorId] ?: 0) + 1
                }
            )
            return
        }

        targetedThisRound += targetId
        attacksThisRound[actorId] = targetId
        passStreaks[actorId] = 0

        // Track ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œghost cupÃƒÂ¢Ã¢â€šÂ¬Ã‚Â condition (human must never be targeted)
        if (targetId == HUMAN_ID) gameHumanWasTargeted = true

        enqueueOther {
            log += RoundLogEvent("${displayNameFor(actorId)} targets ${displayNameFor(targetId)} and guesses $guess.")
        }

        enqueueOther {
            if (!revealedThisRound.containsKey(targetId)) {
                val c = selectionsThisRound[targetId]!!
                revealedThisRound[targetId] = c
            }
            val actual = revealedThisRound[targetId]!!
            log += RoundLogEvent("${displayNameFor(targetId)} is revealed: $actual")
        }

        enqueueOther {
            val actor = players.first { it.id == actorId }
            val target = players.first { it.id == targetId }
            val actual = revealedThisRound[targetId]!!

            // Per-guess achievement stats (persistent counters)
            if (actorId == HUMAN_ID) {
                statsStore?.let { store ->
                    val s = store.load()
                    s.totalGuesses += 1
                    if (guess == actual) s.correctGuesses += 1
                    if (actual == 0 && guess != 0) s.timesTrickedByZero += 1
                    store.save(s)
                }
            }

            // Track â€œyou tricked a bot with your 0"
            // (bot guessed you, you revealed 0, they guessed non-zero)
            if (targetId == HUMAN_ID && actual == 0 && guess != 0) {
                gameHumanTrickedBotWithZero = true
            }

            // Resolve guess outcome
            if (guess == actual) {

                //  Special Zero Hero reward: correct 0 guess takes the Hat and avoids the usual â€œguess cost"
                // reward concrete + adds Hat control as you described.)
                if (guess == 0) {
                    hatHolderId = actorId
                    log += RoundLogEvent("${displayNameFor(actorId)} nailed a 0 and takes the Hat.")
                } else {
                    actor.marbles += guess
                    log += RoundLogEvent("${displayNameFor(actorId)} was correct and gains $guess.")
                }

                // Human-specific correct streak tracking + special achievements
                if (actorId == HUMAN_ID) {
                    humanCorrectGuessStreak += 1

                    // Dumb Luck: correctly guess a 3 in round 1
                    if (!unlockedDumbLuckThisGame && roundNumber == 1 && guess == 3) {
                        unlockedDumbLuckThisGame = true
                    }

                    // On a Roll: 3 correct guesses in a row
                    if (!unlockedOnARollThisGame && humanCorrectGuessStreak >= 3) {
                        unlockedOnARollThisGame = true
                    }

                    // Shakespeare tracking: correct guess on Romeo/Juliet
                    val arch = archetypeById[targetId]
                    if (arch is Colluder) {
                        when (arch.displayName) {
                            "Romeo" -> gameHumanCorrectRomeo = true
                            "Juliet" -> gameHumanCorrectJuliet = true
                        }
                    }

                    // Caught the Strobe / Pushover tracking (correctly guess their 3)
                    val targArch = archetypeById[targetId]
                    if (guess == 3 && actual == 3) {
                        if (targArch is Strobe) strobeCorrect3Count += 1
                        if (targArch is ThreePusher) threePusherCorrect3Count += 1
                    }
                }

                return@enqueueOther
            }

            // Wrong guess resets human streak
            if (actorId == HUMAN_ID) {
                humanCorrectGuessStreak = 0
            }

            if (actual == 0) {
                if (actor.marbles > 0) actor.marbles -= 1
                hatHolderId = actorId
                log += RoundLogEvent("${displayNameFor(actorId)} was wrong on a 0, loses 1 (HAT moves to ${displayNameFor(actorId)}).")

                if (actorId == HUMAN_ID && guess != 0) {
                    gameWrongGuesses += 1
                    gameHumanWasTrickedByZero = true
                }
            } else {
                target.marbles += actual
                log += RoundLogEvent("${displayNameFor(actorId)} was wrong, ${displayNameFor(targetId)} gains $actual.")
                if (actorId == HUMAN_ID) gameWrongGuesses += 1
            }
        }
    }

    private fun queueTrickleAndEndRound() {
        enqueueOther {
            log += RoundLogEvent("")
            log += RoundLogEvent("TRICKLE ----------------------------------------")
        }

        val activeIds = activePlayerIds().toSet()
        for (p in players) {
            if (p.id !in activeIds) continue
            if (!revealedThisRound.containsKey(p.id)) {
                enqueueOther {
                    val c = selectionsThisRound[p.id]!!
                    revealedThisRound[p.id] = c
                    p.marbles += c
                    log += RoundLogEvent("${displayNameFor(p.id)} wasn't targeted, trickles $c.")
                }
            }
        }

        enqueueOther {
            lastRoundChoices = selectionsThisRound.toMap()
            lastRoundAttacks = attacksThisRound.toMap()

            winnerIds = resolveWinnersAfterRound()

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

                statsStore?.let { store ->
                    val s = store.load()
                    val humanFinal = players.first { it.id == HUMAN_ID }.marbles
                    val humanWon = winnerIds.contains(HUMAN_ID)

                    // -------------------------------------------------
                    // ALWAYS TRACK DIFFICULTY PARTICIPATION
                    // (needed for Tourist achievement)
                    // -------------------------------------------------

                    when (difficulty) {
                        Difficulty.EASY -> s.easyGames += 1
                        Difficulty.NORMAL -> s.normalGames += 1
                        Difficulty.HARD -> s.hardGames += 1
                    }

                    if (!s.playedAllDifficulties && s.easyGames > 0 && s.normalGames > 0 && s.hardGames > 0) {
                        s.playedAllDifficulties = true
                        log += RoundLogEvent("*** Achievement Unlocked: Tourist - Played on Easy, Normal, and Hard! ***")
                    }

                    // -------------------------------------------------
                    // TOTAL PROGRESSION (NORMAL OR HARD ONLY)
                    // -------------------------------------------------

                    if (difficulty != Difficulty.EASY) {

                        s.totalGames += 1
                        s.totalMarblesAcrossGames += humanFinal.toLong()

                        if (humanWon) {
                            s.totalWins += 1
                        }

                        if (!s.played13Games && s.totalGames >= 13) {
                            s.played13Games = true
                            log += RoundLogEvent("*** Achievement Unlocked: Amateur - Played 13 games! ***")
                        }

                        if (!s.played113Games && s.totalGames >= 113) {
                            s.played113Games = true
                            log += RoundLogEvent("*** Achievement Unlocked: Professional - Played 113 games! ***")
                        }

                        if (!s.played1113Games && s.totalGames >= 1113) {
                            s.played1113Games = true
                            log += RoundLogEvent("*** Achievement Unlocked: Expert - Played 1,113 games! ***")
                        }

                        if (!s.has113MarblesTotal && s.totalMarblesAcrossGames >= 113L) {
                            s.has113MarblesTotal = true
                            log += RoundLogEvent("*** Achievement Unlocked: Bucket Filler - Gained 113 marbles across games! ***")
                        }

                        if (!s.has1113MarblesTotal && s.totalMarblesAcrossGames >= 1113L) {
                            s.has1113MarblesTotal = true
                            log += RoundLogEvent("*** Achievement Unlocked: Tub Filler - Gained 1,113 marbles across games! ***")
                        }

                        if (!s.has11113MarblesTotal && s.totalMarblesAcrossGames >= 11113L) {
                            s.has11113MarblesTotal = true
                            log += RoundLogEvent("*** Achievement Unlocked: Pool Filler - Gained 11,113 marbles across games! ***")
                        }
                    }

                    // -------------------------------------------------
                    // GAME COMPLETION ACHIEVEMENTS
                    // -------------------------------------------------

                    if (!s.firstGameCompleted) {
                        s.firstGameCompleted = true
                        log += RoundLogEvent("*** Achievement Unlocked: First Drip - Completed a game! ***")
                    }

                    if (!s.pacifistGame && !gameHumanMadeGuess) {
                        s.pacifistGame = true
                        log += RoundLogEvent("*** Achievement Unlocked: Pacifist (Game) - Finished a game without guessing! ***")
                    }

                    if (!s.reachedRound6 && gameRoundReached >= 6) {
                        s.reachedRound6 = true
                        log += RoundLogEvent("*** Achievement Unlocked: Idle Hands - Reached round 6! ***")
                    }

                    // -------------------------------------------------
                    // WIN ACHIEVEMENTS
                    // -------------------------------------------------

                    if (humanWon) {

                        when (difficulty) {
                            Difficulty.EASY -> {
                                if (!s.wonEasy) {
                                    s.wonEasy = true
                                    log += RoundLogEvent("*** Achievement Unlocked: Comp Stomp - Won on Easy! ***")
                                }
                                s.easyWins += 1
                            }

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
                                    log += RoundLogEvent("*** Achievement Unlocked: TR1CKL3! - Won on Hard! ***")
                                }
                                s.hardWins += 1
                            }
                        }

                        // Win milestones (Normal+ only)
                        if (difficulty != Difficulty.EASY) {

                            if (!s.won13thGame && s.totalWins >= 13) {
                                s.won13thGame = true
                                log += RoundLogEvent("*** Achievement Unlocked: Trickle Pro - Won 13 games! ***")
                            }

                            if (!s.won113thGame && s.totalWins >= 113) {
                                s.won113thGame = true
                                log += RoundLogEvent("*** Achievement Unlocked: Trickle Champion - Won 113 games! ***")

                            }
                            if (!s.won113thGame && s.totalWins >= 1113) {
                                s.won113thGame = true
                                log += RoundLogEvent("*** Achievement Unlocked: Trickle God - Won 1113 games! ***")
                            }
                        }
                    }

                    store.save(s)
                }
            }

            val startHolder = hatStartOfRoundHolderId
            val endHolder = hatHolderId
            val overrideEligible = (startHolder != endHolder) && (endHolder != null)

            if (phase != EnginePhase.GAME_OVER) {
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
            turnOrder = emptyList()
            turnCursor = 0
            hatStartOfRoundHolderId = null

            if (phase != EnginePhase.GAME_OVER) {
                phase = EnginePhase.SELECT
            }

            if (phase == EnginePhase.GAME_OVER) {
                gameWrongGuesses = 0
            }
        }
    }


    private fun activePlayerIds(): List<Int> {
        val restricted = tiebreakerParticipantIds
        return if (restricted == null) players.map { it.id } else players.map { it.id }.filter { it in restricted }
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
            phase = EnginePhase.GAME_OVER
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
            log += RoundLogEvent("${displayNameFor(firstId)} rolls $firstRoll. ${displayNameFor(secondId)} rolls $secondRoll.")
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

        val chooserPick = if (rng.nextBoolean()) 1 else 3
        val guesserGuess = if (rng.nextBoolean()) 1 else 3

        log += RoundLogEvent("${displayNameFor(chooserId)} had the lower roll and secretly chooses between 1 and 3.")
        log += RoundLogEvent("${displayNameFor(guesserId)} guesses $guesserGuess.")
        log += RoundLogEvent("${displayNameFor(chooserId)} chose $chooserPick.")

        return if (guesserGuess == chooserPick) {
            log += RoundLogEvent("${displayNameFor(guesserId)} is correct and wins the tiebreaker.")
            listOf(guesserId)
        } else {
            log += RoundLogEvent("${displayNameFor(guesserId)} is wrong, so ${displayNameFor(chooserId)} wins the tiebreaker.")
            listOf(chooserId)
        }
    }

    private fun clearTiebreakerState() {
        tiebreakerParticipantIds = null
        tiebreakerStageIndex = -1
        tiebreakerRoundsRemaining = 0
    }

    private fun enqueueOther(block: () -> Unit) {
        pending.addLast(PendingLog(LogEventKind.OTHER, block))
    }

    // Archetype assignment (unchanged)
    private fun assignRandomArchetypesToBots() {
        archetypeById.clear()
        memById.clear()

        val nonColluderPool: MutableList<Archetype> = mutableListOf(
            Teacher(), Strobe(), ChaosGrandma(), ThreePusher(), Opportunist(),
            Avenger(), SpitePlayer(), Accretion(), Auditor(), Kingmaker(),
            Limper(), Scout(), HatFarmer(), PacifistCollector()
        )

        val roll = rng.nextInt(100)
        val includeBoth = roll >= 75
        val includeSingle = roll in 45..74

        val colludersToAdd = when {
            includeBoth -> 2
            includeSingle -> 1
            else -> 0
        }

        val neededFromNonColluders = 12 - colludersToAdd
        nonColluderPool.shuffle(rng)
        val selected = nonColluderPool.take(neededFromNonColluders).toMutableList()

        val botIds = (2..13).toMutableList()
        botIds.shuffle(rng)

        val colluderIds = botIds.take(colludersToAdd)
        val remainingIds = botIds.drop(colludersToAdd)

        val NO_PARTNER = -999

        if (colludersToAdd == 1) {
            val id = colluderIds[0]
            val isRomeo = rng.nextBoolean()
            val arch = if (isRomeo) Colluder(code = "J", displayName = "Romeo", partnerId = NO_PARTNER)
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

    private fun targetableIdsForHuman(): List<Int> {
        val activeSet = activePlayerIds().toSet()
        return players.asSequence()
            .map { it.id }
            .filter { it != HUMAN_ID }
            .filter { it in activeSet }
            .filter { it !in targetedThisRound }
            .toList()
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
        val targetables = if (phase == EnginePhase.PLAYER_TURN) targetableIdsForHuman() else emptyList()

        return RoundResult(
            phase = phase,
            log = log.toList(),
            players = players.map { it.copy(baseName = displayNameFor(it.id)) },
            winnerIds = winnerIds,
            bannerText = bannerText,
            targetableIdsForHuman = targetables,
            currentActorId = currentActor,
            lastEventKind = lastEventKind
        )
    }
}