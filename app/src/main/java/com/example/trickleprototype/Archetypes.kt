package com.example.trickleprototype

import kotlin.random.Random

enum class DieChoice(val v: Int) { ZERO(0), ONE(1), THREE(3) }

sealed class TurnDecision {
    object Pass : TurnDecision()
    data class Guess(val targetId: Int, val guess: DieChoice) : TurnDecision()
}

data class BotMemory(
    // Spite: first attacker forever
    var grudgeTargetId: Int? = null,
    // Kingmaker: chosen king (protected player)
    var protectedId: Int? = null,
    // For patterns / alternating
    var lastChosen: DieChoice? = null,

    // Keep 2-round memory for archetypes that need it (e.g., Opportunist)
    var lastRoundChoicesSeen: Map<Int, Int> = emptyMap(),
    var twoRoundsAgoChoicesSeen: Map<Int, Int> = emptyMap()
)

data class PublicRoundInfo(
    val roundIndex: Int,
    val startingPlayerId: Int,
    val hatHolderId: Int?,
    val revealedThisRound: Map<Int, Int>,
    val targetedThisRound: Set<Int>,

    val lastRoundChoices: Map<Int, Int>,
    val lastRoundAttacks: Map<Int, Int>,   // attackerId -> targetId (0 pass)
    val passStreaks: Map<Int, Int>,        // playerId -> consecutive passes

    val myId: Int,
    val playersAlive: List<Int>,
    val rng: Random,

    // HARD ONLY: bots can see Player's marbles (null in Easy/Normal)
    val humanMarbles: Int?,
    val lastRoundDogpileCounts: Map<Int, Int> = emptyMap(),
    val lastRoundHatMoved: Boolean = false
)

interface Archetype {
    val code: String
    val displayName: String

    fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice
    fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision
}

// ---------- helpers ----------
internal fun candidateTargets(public: PublicRoundInfo, forbidIds: Set<Int> = emptySet()): List<Int> {
    return public.playersAlive
        .filter { it != public.myId }
        .filter { it !in public.targetedThisRound }
        .filter { it !in forbidIds }
}

internal fun anyRevealed(public: PublicRoundInfo, value: Int): List<Int> {
    return public.revealedThisRound.filterValues { it == value }.keys.toList()
}

private fun wasTargetedLastRound(public: PublicRoundInfo, myId: Int): Boolean {
    return public.lastRoundAttacks.values.any { it == myId }
}

private fun attackersWhoTargetedMeLastRound(public: PublicRoundInfo, myId: Int): List<Int> {
    return public.lastRoundAttacks
        .filterValues { it == myId }
        .keys
        .toList()
}

private fun rememberLastRoundChoices(public: PublicRoundInfo, mem: BotMemory) {
    // Only advance memory once per round (chooseDie is called during round start; takeTurn after that)
    // So: if the "lastRoundChoices" we're seeing is different than what we already stored, shift it.
    if (mem.lastRoundChoicesSeen !== public.lastRoundChoices) {
        if (mem.lastRoundChoicesSeen != public.lastRoundChoices) {
            mem.twoRoundsAgoChoicesSeen = mem.lastRoundChoicesSeen
            mem.lastRoundChoicesSeen = public.lastRoundChoices
        }
    }
}

private fun pickTargetWhoChose(public: PublicRoundInfo, value: Int, forbid: Set<Int> = emptySet()): Int? {
    val targets = candidateTargets(public, forbid)
    if (targets.isEmpty()) return null
    val matching = targets.filter { public.lastRoundChoices[it] == value }
    return (matching.ifEmpty { emptyList() }).firstOrNull()
}

private fun pickAnyTarget(public: PublicRoundInfo, forbid: Set<Int> = emptySet()): Int? {
    val targets = candidateTargets(public, forbid)
    if (targets.isEmpty()) return null
    return targets.random(public.rng)
}

private fun guessFromLastChoice(public: PublicRoundInfo, targetId: Int, defaultGuess: DieChoice): DieChoice {
    return when (public.lastRoundChoices[targetId]) {
        3 -> DieChoice.THREE
        1 -> DieChoice.ONE
        else -> defaultGuess
    }
}

// A lightweight "baseline" chooser (for Romeo/Juliet), matching the bravery ramp you specified.
// NOTE: This does NOT implement the full core-bot guardrails (revenge/learning/endgame) â€” that belongs in GameEngine.
// This just gives them a consistent "human-ish" distribution if you keep them as archetypes.
private fun baselineBraveryDie(public: PublicRoundInfo, allowZeroThisRound: Boolean): DieChoice {
    val r = public.rng.nextInt(100)

    val pThree = when (public.roundIndex) {
        1 -> 20
        2 -> 40
        3, 4 -> 50
        else -> 70 // 5+
    }

    // Default: ONE vs THREE by bravery curve
    val base = if (r < pThree) DieChoice.THREE else DieChoice.ONE

    // Zero is only even *allowed* if they were targeted last round; and even then itâ€™s not guaranteed.
    // Use a modest chance to actually take ZERO (defensive), otherwise stick to ONE/THREE.
    if (allowZeroThisRound) {
        val zRoll = public.rng.nextInt(100)
        if (zRoll < 30) return DieChoice.ZERO
    }

    return base
}

// ---------- Archetypes ----------

/**
 * A: Teacher (UPDATED CANON)
 * - Die: rounds 1â€“3 choose 1.
 * - If targeted last round: next round choose 0.
 * - Otherwise follow: 1,1,1,3,3 (repeat 3s after that).
 * - Action: pass rounds 1â€“3. From round 4+ target someone who chose 3 last round, guess 3.
 */
class Teacher : Archetype {
    override val code = "A"
    override val displayName = "Teacher"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val targetedLast = wasTargetedLastRound(public, public.myId)
        val c = when {
            targetedLast -> DieChoice.ZERO
            public.roundIndex <= 3 -> DieChoice.ONE
            else -> DieChoice.THREE
        }
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        if (public.roundIndex <= 3) return TurnDecision.Pass

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val t = targets.firstOrNull { public.lastRoundChoices[it] == 3 } ?: return TurnDecision.Pass
        return TurnDecision.Guess(t, DieChoice.THREE)
    }
}

/**
 * B: Strobe (UPDATED CANON)
 * - Die: alternates 1,3,1,3,... forever.
 * - Action: round 1 pass.
 *          then alternates: even rounds target, odd rounds pass.
 * - On even rounds: target based on last-round choice:
 *      round 2/6/10... target someone who chose 3 last round, guess 3
 *      round 4/8/12... target someone who chose 1 last round, guess 1
 */
class Strobe : Archetype {
    override val code = "B"
    override val displayName = "Strobe"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val next = when (mem.lastChosen) {
            DieChoice.ONE -> DieChoice.THREE
            DieChoice.THREE -> DieChoice.ONE
            else -> DieChoice.ONE // canonical start
        }
        mem.lastChosen = next
        return next
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        if (public.roundIndex == 1) return TurnDecision.Pass
        if (public.roundIndex % 2 == 1) return TurnDecision.Pass // odd rounds pass (3,5,7,...)

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        // Even rounds alternate desired guess: 2->3, 4->1, 6->3...
        val desired = if (((public.roundIndex / 2) % 2) == 1) DieChoice.THREE else DieChoice.ONE
        val t = targets.firstOrNull { public.lastRoundChoices[it] == desired.v } ?: return TurnDecision.Pass
        return TurnDecision.Guess(t, desired)
    }
}

/**
 * C: Chaos Grandma (UPDATED CANON)
 * - Die: 33/33/33 each round (0/1/3).
 * - Action: 50% pass, 50% target (no revenge behavior).
 * - Target: random valid.
 * - Guess: 50/50 between 1 and 3.
 */
class ChaosGrandma : Archetype {
    override val code = "C"
    override val displayName = "Chaos Grandma"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val roll = public.rng.nextInt(3)
        val c = when (roll) {
            0 -> DieChoice.ZERO
            1 -> DieChoice.ONE
            else -> DieChoice.THREE
        }
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        // 50/50 pass vs target
        if (public.rng.nextBoolean()) return TurnDecision.Pass

        val t = targets.random(public.rng)
        val g = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        return TurnDecision.Guess(t, g)
    }
}

/**
 * D: Three-Pusher (UPDATED CANON)
 * - Die: always 3.
 * - Action: round 1 pass. round 2+ always target.
 * - Target: someone who chose 3 last round; else any valid.
 * - Guess: always 3.
 */
class ThreePusher : Archetype {
    override val code = "D"
    override val displayName = "Three-Pusher"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)
        mem.lastChosen = DieChoice.THREE
        return DieChoice.THREE
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        if (public.roundIndex == 1) return TurnDecision.Pass

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val t = targets.firstOrNull { public.lastRoundChoices[it] == 3 } ?: targets.random(public.rng)
        return TurnDecision.Guess(t, DieChoice.THREE)
    }
}

/**
 * E: Opportunist (UPDATED CANON)
 * - Die: usually 1 (we keep it as ALWAYS 1 here, since this file doesn't know its exact marbles).
 * - Action: pass rounds 1 and 2.
 * - Round 3+: targets players who chose 3 for TWO rounds in a row (needs 2-round memory).
 *   If no such target exists, pass.
 * - Guess: 3.
 */
class Opportunist : Archetype {
    override val code = "E"
    override val displayName = "Opportunist"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)
        mem.lastChosen = DieChoice.ONE
        return DieChoice.ONE
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        if (public.roundIndex <= 2) return TurnDecision.Pass

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val last = mem.lastRoundChoicesSeen
        val twoAgo = mem.twoRoundsAgoChoicesSeen

        val streak3 = targets.filter { (last[it] == 3) && (twoAgo[it] == 3) }
        if (streak3.isEmpty()) return TurnDecision.Pass

        return TurnDecision.Guess(streak3.random(public.rng), DieChoice.THREE)
    }
}

/**
 * F: Avenger (UPDATED CANON)
 * - Die: 50/50 between 1 and 3. (0 will be handled by core guardrail later.)
 * - Action:
 *   - Round 1 pass.
 *   - Round 2+: targets players who targeted last round.
 *     Priority rules:
 *       1) If anyone targeted ME last round, pick among them (prefer last-round 3 choosers).
 *       2) Else pick among last round attackers (prefer last-round 3 choosers).
 *   - Guess: mirror their last-round choice when possible, else 3.
 */
class Avenger : Archetype {
    override val code = "F"
    override val displayName = "Avenger"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val c = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        if (public.roundIndex == 1) return TurnDecision.Pass

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val attackedMe = attackersWhoTargetedMeLastRound(public, public.myId)
            .filter { it in targets }

        val attackers = public.lastRoundAttacks.keys
            .filter { it in targets }

        val pool = attackedMe.ifEmpty { attackers }
        if (pool.isEmpty()) return TurnDecision.Pass

        val prefer3 = pool.filter { public.lastRoundChoices[it] == 3 }
        val t = (prefer3.ifEmpty { pool }).random(public.rng)

        val g = guessFromLastChoice(public, t, defaultGuess = DieChoice.THREE)
        return TurnDecision.Guess(t, g)
    }
}

/**
 * G: Spite Player (UPDATED CANON)
 * - Die: 50/50 between 1 and 3.
 * - Action: passes until someone attacks them.
 * - Grudge: the FIRST player to attack them becomes grudgeTargetId forever.
 * - Then: always targets grudge (if available) and always guesses 3.
 */
class SpitePlayer : Archetype {
    override val code = "G"
    override val displayName = "Spite Player"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val c = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        // If we don't have a grudge yet, and someone attacked me last round, lock the first attacker
        if (mem.grudgeTargetId == null) {
            val attackers = attackersWhoTargetedMeLastRound(public, public.myId)
                .sorted() // deterministic "first"
            if (attackers.isNotEmpty()) {
                mem.grudgeTargetId = attackers.first()
            }
        }

        val grudge = mem.grudgeTargetId
        if (grudge == null || grudge !in targets) {
            return TurnDecision.Pass
        }

        return TurnDecision.Guess(grudge, DieChoice.THREE)
    }
}

/**
 * H: Accretion (UPDATED CANON)
 * - Die: rounds 1â€“2 choose 1; rounds 3+ choose 3.
 * - Action: pass rounds 1â€“4.
 * - Round 5+: target someone who chose 3 last round and guess 3 (else pass).
 */
class Accretion : Archetype {
    override val code = "H"
    override val displayName = "Accretion"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val c = if (public.roundIndex <= 2) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        if (public.roundIndex <= 4) return TurnDecision.Pass

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val t = targets.firstOrNull { public.lastRoundChoices[it] == 3 } ?: return TurnDecision.Pass
        return TurnDecision.Guess(t, DieChoice.THREE)
    }
}

/**
 * I: Auditor (UPDATED CANON)
 * - Die: 50/50 1 or 3.
 * - Action: pass rounds 1â€“2.
 * - Round 3+: target players who have passed 2+ rounds in a row; guess 3.
 *   If none exist, pass.
 */
class Auditor : Archetype {
    override val code = "I"
    override val displayName = "Auditor"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val c = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        if (public.roundIndex <= 2) return TurnDecision.Pass

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val suspicious = targets.filter { (public.passStreaks[it] ?: 0) >= 2 }
        if (suspicious.isEmpty()) return TurnDecision.Pass

        return TurnDecision.Guess(suspicious.random(public.rng), DieChoice.THREE)
    }
}

/**
 * J/R: Colluder (Romeo/Juliet) (UPDATED CANON)
 * - Baseline "core" die/turn behavior (approx here), but NEVER targets partner.
 * - IMPORTANT: your real "core bot logic" (revenge/learning/endgame/zero-guardrail)
 *   should live in GameEngine, not here. This is just an archetype flavor layer.
 */
class Colluder(
    override val code: String,
    override val displayName: String,
    private val partnerId: Int
) : Archetype {

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val attackedLast = wasTargetedLastRound(public, public.myId)
        val c = baselineBraveryDie(public, allowZeroThisRound = attackedLast)
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        val targets = candidateTargets(public, forbidIds = setOf(partnerId))
        if (targets.isEmpty()) return TurnDecision.Pass

        // Round 1: 75% target, 25% pass (simple approximation here)
        if (public.roundIndex == 1) {
            if (public.rng.nextInt(100) < 25) return TurnDecision.Pass
            val t = targets.random(public.rng)
            val g = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
            return TurnDecision.Guess(t, g)
        }

        // Prefer someone who chose 3 last round (greedy), else pass sometimes
        val t3 = targets.firstOrNull { public.lastRoundChoices[it] == 3 }
        if (t3 != null) return TurnDecision.Guess(t3, DieChoice.THREE)

        if (public.rng.nextInt(100) < 60) return TurnDecision.Pass
        val t = targets.random(public.rng)
        return TurnDecision.Guess(t, DieChoice.ONE)
    }
}

/**
 * K: Kingmaker (UPDATED CANON)
 * - Die: 50% 1, 50% 3.
 * - Round 1: pick a random KING (protectedId) (not self).
 * - Action: usually pass.
 * - Only targets someone who targeted their KING last round (never targets king).
 */
class Kingmaker : Archetype {
    override val code = "K"
    override val displayName = "Kingmaker"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val c = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        // Pick king on round 1 (or if unset)
        if (mem.protectedId == null) {
            val opts = public.playersAlive.filter { it != public.myId }
            if (opts.isNotEmpty()) mem.protectedId = opts.random(public.rng)
        }

        val kingId = mem.protectedId ?: return TurnDecision.Pass

        val forbid = setOf(kingId)
        val targets = candidateTargets(public, forbidIds = forbid)
        if (targets.isEmpty()) return TurnDecision.Pass

        // Only target attackers of the king
        val attackersOfKing = public.lastRoundAttacks
            .filterValues { it == kingId }
            .keys
            .filter { it in targets }

        if (attackersOfKing.isEmpty()) return TurnDecision.Pass

        val t = attackersOfKing.random(public.rng)
        val g = guessFromLastChoice(public, t, defaultGuess = DieChoice.THREE)
        return TurnDecision.Guess(t, g)
    }
}

/**
 * L: Limper (UPDATED CANON)
 * - Die: always 1.
 * - If attacked last round: may choose 0 defensively (allowed), but still "limpy".
 * - Action: always pass. Never targets.
 */
class Limper : Archetype {
    override val code = "L"
    override val displayName = "Limper"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)

        val attackedLast = wasTargetedLastRound(public, public.myId)
        val c = if (attackedLast && public.rng.nextInt(100) < 60) DieChoice.ZERO else DieChoice.ONE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)
        return TurnDecision.Pass
    }
}

/**
 * M: Scout (NEW)
 * - Die: "normal" 50/50 1/3.
 * - Action: ALWAYS targets (no passing).
 *   - Round 1: random target, random guess 1/3.
 *   - Round 2+: target someone who chose 3 last round; else someone who chose 1 last round.
 *     Guess matches that (3 or 1).
 */
class Scout : Archetype {
    override val code = "M"
    override val displayName = "Scout"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)
        val c = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        if (public.roundIndex == 1) {
            val t = targets.random(public.rng)
            val g = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
            return TurnDecision.Guess(t, g)
        }

        val t3 = targets.firstOrNull { public.lastRoundChoices[it] == 3 }
        if (t3 != null) return TurnDecision.Guess(t3, DieChoice.THREE)

        val t1 = targets.firstOrNull { public.lastRoundChoices[it] == 1 }
        if (t1 != null) return TurnDecision.Guess(t1, DieChoice.ONE)

        // Fallback: still guesses
        val t = targets.random(public.rng)
        val g = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        return TurnDecision.Guess(t, g)
    }
}

/**
 * N: Hat Farmer (NEW)
 * - Die: "normal" 50/50 1/3.
 * - Action:
 *   - Round 1: pass.
 *   - Round 2+: targets someone who was targeted/guessed LAST round (i.e., appeared as a target in lastRoundAttacks values).
 *     If multiple, pick random among valid targets.
 *   - Guess: defaults to 3 (hat-chasing / chaos), but you can tweak later.
 */
class HatFarmer : Archetype {
    override val code = "N"
    override val displayName = "Hat Farmer"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)
        val c = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)

        if (public.roundIndex == 1) return TurnDecision.Pass

        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val previouslyTargeted = public.lastRoundAttacks.values
            .filter { it != 0 }
            .distinct()
            .filter { it in targets }

        val t = if (previouslyTargeted.isNotEmpty()) {
            previouslyTargeted.random(public.rng)
        } else {
            return TurnDecision.Pass
        }

        return TurnDecision.Guess(t, DieChoice.THREE)
    }
}

/**
 * O: Pacifist Collector (NEW)
 * - Die: always 3.
 * - Action: always pass.
 */
class PacifistCollector : Archetype {
    override val code = "O"
    override val displayName = "Pacifist Collector"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        rememberLastRoundChoices(public, mem)
        mem.lastChosen = DieChoice.THREE
        return DieChoice.THREE
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        rememberLastRoundChoices(public, mem)
        return TurnDecision.Pass
    }
}
