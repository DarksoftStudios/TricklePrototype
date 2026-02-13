package com.example.trickleprototype

import android.content.Context
import kotlin.random.Random
import org.json.JSONObject

// ----------------------------
// Core data
// ----------------------------
data class PlayerState(
    val id: Int,
    val baseName: String,
    var marbles: Int = 0,
    var revealedChoice: Int? = null
)

enum class Difficulty { EASY, NORMAL, HARD }

data class RoundLogEvent(val text: String)

enum class LogEventKind { PASS, OTHER }

enum class EnginePhase {
    SETUP,
    SELECT,
    BOT_TURN,
    PLAYER_TURN,
    ROUND_END,
    GAME_OVER
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
// ----------------------------
// Engine
// ----------------------------
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
        add(PlayerState(HUMAN_ID, "Player", marbles = 0))
        for (i in 0 until 12) add(PlayerState(i + 2, botNames[i], marbles = 0))
    }

    private var roundNumber: Int = 1
    private var starterIndex: Int = 0
    private var phase: EnginePhase = EnginePhase.SETUP

    // per-round
    private val selectionsThisRound = mutableMapOf<Int, Int>()   // pid -> 0/1/3
    private val revealedThisRound = mutableMapOf<Int, Int>()     // pid -> revealed value
    private val targetedThisRound = mutableSetOf<Int>()          // who has been targeted
    private val attacksThisRound = mutableMapOf<Int, Int>()      // actorId -> targetId (0 pass)

    private var turnOrder: List<Int> = emptyList()
    private var turnCursor: Int = 0

    // Hat state
    private var hatHolderId: Int? = null
    private var hatStartOfRoundHolderId: Int? = null

    // stats: per-game trackers
    private var gameWrongGuesses: Int = 0

    // bot memory kept
    private var lastRoundChoices: Map<Int, Int> = emptyMap()
    private var lastRoundAttacks: Map<Int, Int> = emptyMap()
    private val passStreaks = mutableMapOf<Int, Int>()

    private val archetypeById: MutableMap<Int, Archetype> = mutableMapOf()
    private val memById: MutableMap<Int, BotMemory> = mutableMapOf()

    private val log = mutableListOf<RoundLogEvent>()
    private var bannerText: String? = null
    private var winnerIds: List<Int> = emptyList()

    // ONE-event stepping support
    private data class PendingLog(
        val kind: LogEventKind,
        val apply: () -> Unit
    )

    private val pending = ArrayDeque<PendingLog>()
    private var lastEventKind: LogEventKind? = null

    // stats store (set by UI)
    private var statsStore: StatsStore? = null

    init {
        reset()
    }

    fun attachStatsStore(store: StatsStore) {
        statsStore = store
    }

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

        lastRoundChoices = emptyMap()
        lastRoundAttacks = emptyMap()
        passStreaks.clear()

        log.clear()
        bannerText = null
        winnerIds = emptyList()

        pending.clear()
        lastEventKind = null

        gameWrongGuesses = 0

        assignRandomArchetypesToBots()
    }

    // ----------------------------
    // Public stepped API
    // ----------------------------

    fun startRound(humanChoice: Int): RoundResult {
        if (phase == EnginePhase.GAME_OVER) return snapshot()
        if (phase != EnginePhase.SELECT && phase != EnginePhase.ROUND_END) return snapshot()

        // fresh round
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
        turnOrder = buildTurnOrderFromStarter()
        turnCursor = 0

        // selections
        selectionsThisRound[HUMAN_ID] = humanChoice
        for (pid in 2..13) {
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
                playersAlive = players.map { it.id },
                rng = rng,
                humanMarbles = if (difficulty == Difficulty.HARD) players.first { it.id == HUMAN_ID }.marbles else null
            )
            selectionsThisRound[pid] = arch.chooseDie(pub, mem).v
        }

        // round header (ONE log event, not duplicated)
        enqueueOther {
            log += RoundLogEvent("")
            log += RoundLogEvent("=== ROUND $roundNumber | Starter: ${displayNameFor(starterId)} ===")
        }

        phase = EnginePhase.BOT_TURN
        // Emit the header immediately as the first step (so UI sees it without waiting)
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

        // Build this human action as queued log+effects
        if (targetId == null) {
            queuePass(actorId = HUMAN_ID)
        } else {
            val validTargets = targetableIdsForHuman()
            if (targetId !in validTargets) {
                bannerText = "That target is no longer available. Pick someone else."
                return snapshot()
            }
            val g = guess ?: 1
            queueGuess(actorId = HUMAN_ID, targetId = targetId, guess = g)
        }

        turnCursor++
        phase = EnginePhase.BOT_TURN

        // emit first event immediately
        return stepInternalOrSnapshot()
    }

    // ----------------------------
    // Internal: stepping
    // ----------------------------

    private fun stepInternalOrSnapshot(): RoundResult {
        // If we have a pending log/effect, execute ONE and return
        if (pending.isNotEmpty()) {
            val p = pending.removeFirst()
            p.apply.invoke()
            lastEventKind = p.kind
            return snapshot()
        }

        // No pending events: decide what to do next
        if (turnCursor >= turnOrder.size) {
            // End of round -> queue TRICKLE + resolve + maybe game over
            queueTrickleAndEndRound()
            // Execute one immediately
            return stepInternalOrSnapshot()
        }

        val actorId = turnOrder[turnCursor]

        // If human is up, pause for input
        if (actorId == HUMAN_ID) {
            phase = EnginePhase.PLAYER_TURN
            return snapshot()
        }

        // Bot takes its turn, but we only execute ONE queued event per engine.step() call
        queueBotTurn(actorId)
        turnCursor++
        return stepInternalOrSnapshot()
    }

    private fun queueBotTurn(actorId: Int) {
        val starterId = players[starterIndex].id
        val arch = archetypeById[actorId]!!
        val mem = memById.getOrPut(actorId) { BotMemory() }

        // HARD: block human at 10+
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
            playersAlive = players.map { it.id },
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
        // If already targeted, this becomes a pass-style event (fast)
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

        // Mark targeted immediately so later actors can’t pick them (but logging happens in order)
        targetedThisRound += targetId
        attacksThisRound[actorId] = targetId
        passStreaks[actorId] = 0

        // 1) actor line
        enqueueOther {
            log += RoundLogEvent("${displayNameFor(actorId)} targets ${displayNameFor(targetId)} and guesses $guess.")
        }

        // 2) reveal line
        enqueueOther {
            if (!revealedThisRound.containsKey(targetId)) {
                val c = selectionsThisRound[targetId]!!
                revealedThisRound[targetId] = c
            }
            val actual = revealedThisRound[targetId]!!
            log += RoundLogEvent("${displayNameFor(targetId)} is revealed: $actual")
        }

        // 3) resolution line (+ stats/hat)
        enqueueOther {
            val actor = players.first { it.id == actorId }
            val target = players.first { it.id == targetId }
            val actual = revealedThisRound[targetId]!!

            // stats only for HUMAN guesses
            if (actorId == HUMAN_ID) {
                val store = statsStore
                if (store != null) {
                    val s = store.load()
                    s.totalGuesses += 1
                    if (guess == actual) s.correctGuesses += 1
                    if (actual == 0 && guess != 0) s.timesTrickedByZero += 1
                    store.save(s)
                }

                if (guess != actual) gameWrongGuesses += 1
            }

            if (guess == actual) {
                actor.marbles += guess
                log += RoundLogEvent("${displayNameFor(actorId)} was correct and gains $guess.")
                return@enqueueOther
            }

            // wrong guess
            if (actual == 0) {
                if (actor.marbles > 0) actor.marbles -= 1
                hatHolderId = actorId
                log += RoundLogEvent("${displayNameFor(actorId)} was wrong on a 0, loses 1 (HAT → ${displayNameFor(actorId)}).")
            } else {
                target.marbles += actual
                log += RoundLogEvent("${displayNameFor(actorId)} was wrong — ${displayNameFor(targetId)} gains $actual.")
            }
        }
    }

    private fun queueTrickleAndEndRound() {
        // TRICKLE header
        enqueueOther {
            log += RoundLogEvent("")
            log += RoundLogEvent("TRICKLE ----------------------------------------")
        }

        // trickle events (one per player, one per step)
        for (p in players) {
            if (!revealedThisRound.containsKey(p.id)) {
                enqueueOther {
                    val c = selectionsThisRound[p.id]!!
                    revealedThisRound[p.id] = c
                    p.marbles += c
                    log += RoundLogEvent("${displayNameFor(p.id)} was not targeted and trickles $c.")
                }
            }
        }

        // end-of-round finalize (winners + hat override + next starter + cleanup)
        enqueueOther {
            lastRoundChoices = selectionsThisRound.toMap()
            lastRoundAttacks = attacksThisRound.toMap()

            val winners = players.filter { it.marbles >= WIN_SCORE }
            winnerIds = if (winners.isNotEmpty()) {
                val top = winners.maxOf { it.marbles }
                winners.filter { it.marbles == top }.map { it.id }
            } else emptyList()

            if (winnerIds.isNotEmpty()) {
                val winnersText = winnerIds.joinToString(", ") { displayNameFor(it) }
                val top = players.first { it.id == winnerIds.first() }.marbles

                log += RoundLogEvent("")
                log += RoundLogEvent("GAME OVER --------------------------------------")
                log += RoundLogEvent("WINNER: $winnersText with $top")
                log += RoundLogEvent("-----------------------------------------------")
                log += RoundLogEvent("")


                // Persist stats (end of game)
                statsStore?.let { store ->
                    val s = store.load()
                    val humanFinal = players.first { it.id == HUMAN_ID }.marbles
                    val humanWon = winnerIds.contains(HUMAN_ID)

                    s.totalGames += 1
                    if (humanWon) s.totalWins += 1
                    s.totalMarblesAcrossGames += humanFinal.toLong()

                    if (gameWrongGuesses == 0) s.perfectGames += 1

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

                    store.save(s)
                }
            }

            // Hat override with boomerang exception
            val startHolder = hatStartOfRoundHolderId
            val endHolder = hatHolderId
            val overrideEligible = (startHolder != endHolder) && (endHolder != null)

            roundNumber += 1

            starterIndex = if (overrideEligible) {
                indexOfId(endHolder!!)
            } else {
                (starterIndex + 1) % players.size
            }

            // cleanup for next round
            selectionsThisRound.clear()
            revealedThisRound.clear()
            targetedThisRound.clear()
            attacksThisRound.clear()
            turnOrder = emptyList()
            turnCursor = 0
            hatStartOfRoundHolderId = null

            // If game over, phase changes now; else back to SELECT
            phase = if (winnerIds.isNotEmpty()) EnginePhase.GAME_OVER else EnginePhase.SELECT

            // reset per-game wrong guesses if game ended
            if (phase == EnginePhase.GAME_OVER) {
                gameWrongGuesses = 0
            }
        }
    }

    private fun enqueueOther(block: () -> Unit) {
        pending.addLast(PendingLog(LogEventKind.OTHER, block))
    }

    // ----------------------------
    // Utilities
    // ----------------------------

    private fun assignRandomArchetypesToBots() {
        archetypeById.clear()
        memById.clear()

        val pool: MutableList<Archetype> = mutableListOf(
            Teacher(),
            Strobe(),
            ChaosGrandma(),
            ThreePusher(),
            Opportunist(),
            Avenger(),
            SpitePlayer(),
            Accretion(),
            Auditor(),
            Kingmaker(),
            Colluder(code = "J", displayName = "Romeo", partnerId = 13),
            Colluder(code = "R", displayName = "Juliet", partnerId = 12),
            Limper()
        )

        pool.shuffle(rng)
        val selected = pool.take(12)

        val botIds = (2..13).toList()
        for (i in botIds.indices) {
            val pid = botIds[i]
            archetypeById[pid] = selected[i]
            memById[pid] = BotMemory()
        }
    }

    private fun buildTurnOrderFromStarter(): List<Int> {
        val order = mutableListOf<Int>()
        for (i in players.indices) {
            val idx = (starterIndex + i) % players.size
            order += players[idx].id
        }
        return order
    }

    private fun indexOfId(pid: Int): Int =
        players.indexOfFirst { it.id == pid }.let { if (it >= 0) it else 0 }

    private fun targetableIdsForHuman(): List<Int> {
        return players.asSequence()
            .map { it.id }
            .filter { it != HUMAN_ID }
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
