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
    // Kingmaker: chosen protected player
    var protectedId: Int? = null,
    // For patterns / alternating
    var lastChosen: DieChoice? = null
)

data class PublicRoundInfo(
    val roundIndex: Int,
    val startingPlayerId: Int,
    val hatHolderId: Int?,
    val revealedThisRound: Map<Int, Int>,
    val targetedThisRound: Set<Int>,

    val lastRoundChoices: Map<Int, Int>,
    val lastRoundAttacks: Map<Int, Int>,   // attackerId -> targetId
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

// ---------- Archetypes ----------

/** A: Teacher (you said this one was correct; keeping it “teacherish”) */
class Teacher : Archetype {
    override val code = "A"
    override val displayName = "Teacher"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val roll = public.rng.nextInt(100)
        val c = when {
            roll < 60 -> DieChoice.ONE
            roll < 85 -> DieChoice.THREE
            else -> DieChoice.ZERO
        }
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        val targets = candidateTargets(public, mem.protectedId?.let { setOf(it) } ?: emptySet())
        if (targets.isEmpty()) return TurnDecision.Pass

        val r3 = anyRevealed(public, 3).firstOrNull { it in targets }
        if (r3 != null) return TurnDecision.Guess(r3, DieChoice.THREE)

        val r1 = anyRevealed(public, 1).firstOrNull { it in targets }
        if (r1 != null) return TurnDecision.Guess(r1, DieChoice.ONE)

        if (public.rng.nextInt(100) < 70) return TurnDecision.Pass
        return TurnDecision.Guess(targets.random(public.rng), DieChoice.ONE)
    }
}

/** B: Strobe (kept simple: toggles 1 <-> 3) */
class Strobe : Archetype {
    override val code = "B"
    override val displayName = "Strobe"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val next = when (mem.lastChosen) {
            DieChoice.ONE -> DieChoice.THREE
            DieChoice.THREE -> DieChoice.ONE
            else -> if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        }
        mem.lastChosen = next
        return next
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val r3 = anyRevealed(public, 3).firstOrNull { it in targets }
        if (r3 != null) return TurnDecision.Guess(r3, DieChoice.THREE)

        if (public.rng.nextInt(100) < 60) return TurnDecision.Pass
        return TurnDecision.Guess(targets.random(public.rng), DieChoice.ONE)
    }
}

/** C: Chaos Grandma — 33/33/33 at start (your correction) */
class ChaosGrandma : Archetype {
    override val code = "C"
    override val displayName = "Chaos Grandma"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
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
        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass
        if (public.rng.nextInt(100) < 20) return TurnDecision.Pass

        val r3 = anyRevealed(public, 3).firstOrNull { it in targets }
        if (r3 != null) return TurnDecision.Guess(r3, DieChoice.THREE)

        val g = if (public.rng.nextBoolean()) DieChoice.ONE else DieChoice.THREE
        return TurnDecision.Guess(targets.random(public.rng), g)
    }
}

/** D: Three-Pusher (kept as your “correct”) */
class ThreePusher : Archetype {
    override val code = "D"
    override val displayName = "Three-Pusher"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val c = if (public.rng.nextInt(100) < 80) DieChoice.THREE else DieChoice.ONE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val r3 = anyRevealed(public, 3).firstOrNull { it in targets }
        if (r3 != null) return TurnDecision.Guess(r3, DieChoice.THREE)

        if (public.rng.nextInt(100) < 15) return TurnDecision.Pass
        return TurnDecision.Guess(targets.random(public.rng), DieChoice.THREE)
    }
}

/** E: Opportunist (your correction) */
class Opportunist : Archetype {
    override val code = "E"
    override val displayName = "Opportunist"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        // “choose 1 early”
        val c = if (public.roundIndex == 1) DieChoice.ONE else DieChoice.ONE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        // Pass round 1
        if (public.roundIndex == 1) return TurnDecision.Pass

        // Only attack if someone chose 3 last round
        val threesLastRound = public.lastRoundChoices
            .filterValues { it == 3 }
            .keys
            .filter { it != public.myId }
            .filter { it in public.playersAlive }
            .filter { it !in public.targetedThisRound }

        if (threesLastRound.isEmpty()) return TurnDecision.Pass
        val t = threesLastRound.random(public.rng)
        return TurnDecision.Guess(t, DieChoice.THREE)
    }
}

/** F: Avenger (your newest correction) */
class Avenger : Archetype {
    override val code = "F"
    override val displayName = "Avenger"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        // “chooses 1 or 3 like a normal human” (no 0)
        val c = if (public.rng.nextInt(100) < 70) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        // passes unless someone attacked last round
        val attackers = public.lastRoundAttacks.keys
            .filter { it != public.myId }
            .filter { it in public.playersAlive }
            .filter { it !in public.targetedThisRound }

        if (attackers.isEmpty()) return TurnDecision.Pass

        // never attacks someone who passed last round (so attackers list already satisfies that)
        // prefer attackers who chose 3 last round
        val preferred = attackers.filter { public.lastRoundChoices[it] == 3 }
        val target = (preferred.ifEmpty { attackers }).random(public.rng)

        // guess based on their last round choice if known, otherwise 1
        val g = if (public.lastRoundChoices[target] == 3) DieChoice.THREE else DieChoice.ONE
        return TurnDecision.Guess(target, g)
    }
}

/** G: Spite Player — first attacker forever */
class SpitePlayer : Archetype {
    override val code = "G"
    override val displayName = "Spite Player"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val roll = public.rng.nextInt(100)
        val c = when {
            roll < 70 -> DieChoice.ONE
            roll < 90 -> DieChoice.THREE
            else -> DieChoice.ZERO
        }
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val grudge = mem.grudgeTargetId
        if (grudge != null && grudge in targets) {
            return TurnDecision.Guess(grudge, DieChoice.ONE)
        }

        if (public.rng.nextInt(100) < 60) return TurnDecision.Pass
        return TurnDecision.Guess(targets.random(public.rng), DieChoice.ONE)
    }
}

/** H: Accretion (left as before-ish; you said it was correct) */
class Accretion : Archetype {
    override val code = "H"
    override val displayName = "Accretion"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val roll = public.rng.nextInt(100)
        val c = when {
            roll < 55 -> DieChoice.ZERO
            roll < 90 -> DieChoice.ONE
            else -> DieChoice.THREE
        }
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass
        if (public.rng.nextInt(100) < 55) return TurnDecision.Pass

        val r1 = anyRevealed(public, 1).firstOrNull { it in targets }
        if (r1 != null) return TurnDecision.Guess(r1, DieChoice.ONE)

        return TurnDecision.Guess(targets.random(public.rng), DieChoice.ONE)
    }
}

/** I: Auditor — targets 3-pass streak */
class Auditor : Archetype {
    override val code = "I"
    override val displayName = "Auditor"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val c = if (public.rng.nextInt(100) < 80) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass

        val suspicious = targets.filter { (public.passStreaks[it] ?: 0) >= 3 }
        if (suspicious.isNotEmpty()) {
            val t = suspicious.random(public.rng)
            return TurnDecision.Guess(t, DieChoice.ONE)
        }

        if (public.rng.nextInt(100) < 65) return TurnDecision.Pass
        return TurnDecision.Guess(targets.random(public.rng), DieChoice.ONE)
    }
}

/** J/R: Colluder — teacherish, but never targets partner */
class Colluder(
    override val code: String,
    override val displayName: String,
    private val partnerId: Int
) : Archetype {
    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val roll = public.rng.nextInt(100)
        val c = when {
            roll < 60 -> DieChoice.ONE
            roll < 85 -> DieChoice.THREE
            else -> DieChoice.ZERO
        }
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        val targets = candidateTargets(public, forbidIds = setOf(partnerId))
        if (targets.isEmpty()) return TurnDecision.Pass

        val r3 = anyRevealed(public, 3).firstOrNull { it in targets }
        if (r3 != null) return TurnDecision.Guess(r3, DieChoice.THREE)

        if (public.rng.nextInt(100) < 70) return TurnDecision.Pass
        return TurnDecision.Guess(targets.random(public.rng), DieChoice.ONE)
    }
}

/** K: Kingmaker — picks a protected player first time, never attacks them, attacks anyone who attacked them */
class Kingmaker : Archetype {
    override val code = "K"
    override val displayName = "Kingmaker"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val c = if (public.rng.nextInt(100) < 75) DieChoice.ONE else DieChoice.THREE
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        if (mem.protectedId == null) {
            val opts = public.playersAlive.filter { it != public.myId }
            if (opts.isNotEmpty()) mem.protectedId = opts.random(public.rng)
        }

        val protected = mem.protectedId
        val forbid = protected?.let { setOf(it) } ?: emptySet()
        val targets = candidateTargets(public, forbidIds = forbid)
        if (targets.isEmpty()) return TurnDecision.Pass

        // Attack anyone who attacked the protected player last round
        if (protected != null) {
            val attackersOfProtected = public.lastRoundAttacks
                .filterValues { it == protected }
                .keys
                .filter { it in targets }

            if (attackersOfProtected.isNotEmpty()) {
                val t = attackersOfProtected.random(public.rng)
                return TurnDecision.Guess(t, DieChoice.ONE)
            }
        }

        // Otherwise usually pass
        if (public.rng.nextInt(100) < 75) return TurnDecision.Pass
        return TurnDecision.Guess(targets.random(public.rng), DieChoice.ONE)
    }
}

/** L: Limper (debug archetype; keep simple) */
class Limper : Archetype {
    override val code = "L"
    override val displayName = "Limper"

    override fun chooseDie(public: PublicRoundInfo, mem: BotMemory): DieChoice {
        val c = if (public.rng.nextInt(100) < 85) DieChoice.ONE else DieChoice.ZERO
        mem.lastChosen = c
        return c
    }

    override fun takeTurn(public: PublicRoundInfo, mem: BotMemory): TurnDecision {
        val targets = candidateTargets(public)
        if (targets.isEmpty()) return TurnDecision.Pass
        if (public.rng.nextInt(100) < 85) return TurnDecision.Pass
        return TurnDecision.Guess(targets.random(public.rng), DieChoice.ONE)
    }
}
