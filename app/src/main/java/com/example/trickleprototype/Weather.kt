package com.example.trickleprototype

import kotlin.random.Random

enum class WeatherCategory {
    BASELINE,
    SCORING,
    REVEAL,
    TARGETING,
    ECONOMY,
    RISK,
    TRANSFORM,
    RESOLUTION,
    INFORMATION
}

enum class WeatherComplexity {
    EASY,
    MEDIUM,
    HARD
}

enum class WeatherEffectTag {
    NO_OP,

    REVEAL_ONES,
    REVEAL_THREES,

    SCORE_ONES_AS_TWO,
    SCORE_THREES_AS_FOUR,
    DOUBLE_ALL_MARBLE_GAINS,
    DOUBLE_ALL_MARBLE_LOSSES,

    LOCK_GUESSES_SAME_AS_FIRST,
    LOCK_GUESSES_OPPOSITE_OF_FIRST,
    FIRST_GUESSER_NO_REWARD_IF_CORRECT,
    FIRST_WRONG_ON_ZERO_NO_LOSS_BUT_HAT_MOVES,
    MUST_TARGET_TWO_WITH_SAME_GUESS_IF_TARGETING,
    MUST_TARGET_IF_LEGAL,
    LIMIT_TARGETING_ACTIONS_TO_HALF_ROUNDED_DOWN,

    USE_CUP_TO_CUP_TRANSFERS,
    NO_MARBLES_MOVE,
    UNTARGETED_ZERO_TRICKLES_FOR_ONE,

    WRONG_ZERO_COSTS_THREE,
    FIRST_WRONG_ZERO_GIVES_THREE,
    WRONG_ZERO_GIVES_ONE,
    WRONG_ZERO_COSTS_TWO,

    ROTATE_SELECTED_VALUES_0_TO_1_TO_3_TO_0,

    ONE_POSITIVE_SCORING_EVENT_PER_PLAYER,
    REDISTRIBUTE_ALL_POSITIVE_NET_GAINS_TO_ALL_PLAYERS,
    ONLY_TOP_ROUND_SCORERS_KEEP_POINTS,
    ONLY_LOWEST_POSITIVE_ROUND_SCORERS_KEEP_POINTS,

    HIDE_UNTARGETED_TRICKLE_REVEALS,
    REDUCE_TARGETED_GAINS_BY_ONE,
    INCREASE_TRICKLE_GAINS_BY_TWO,
    REVEAL_HIGHEST_SCORE_CHOICES_AND_PROTECT
}

data class WeatherCard(
    val id: String,
    val displayName: String,
    val effectText: String,
    val ruleText: String,
    val category: WeatherCategory,
    val complexity: WeatherComplexity,
    val copies: Int,
    val enabled: Boolean,
    val digitalOnly: Boolean = false,
    val includedInDeck: Boolean = true,
    val effectTags: Set<WeatherEffectTag> = emptySet()
)

object Weather {

    const val FIRST_ROUND_WEATHER_ID = "drizzle"

    val allCards: List<WeatherCard> = listOf(
        WeatherCard(
            id = "drizzle",
            displayName = "Drizzle",
            effectText = "Normal gameplay",
            ruleText = "Use base Trickle rules with no modifications.",
            category = WeatherCategory.BASELINE,
            complexity = WeatherComplexity.EASY,
            copies = 3,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.NO_OP)
        ),

        WeatherCard(
            id = "downpour",
            displayName = "Downpour",
            effectText = "All 1's score as 2's",
            ruleText = "During Trickling, if a player was not targeted and their selection is 1, award 2 instead of 1. Before trickling, if a player who chose 1 is guessed incorrectly, they are awarded 2 instead of 1. If a player guesses another player's 1 correctly, the guesser is awarded 2 instead of 1.",
            category = WeatherCategory.SCORING,
            complexity = WeatherComplexity.EASY,
            copies = 4,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.SCORE_ONES_AS_TWO)
        ),

        WeatherCard(
            id = "fog",
            displayName = "Fog",
            effectText = "1's reveal early and can't be targeted",
            ruleText = "After all players secretly choose and the weather is revealed, immediately reveal every player who selected 1 in the log. Any player revealed this way is untargetable for Step 2 targeting this round.",
            category = WeatherCategory.REVEAL,
            complexity = WeatherComplexity.EASY,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.REVEAL_ONES)
        ),

        WeatherCard(
            id = "sunny_day",
            displayName = "Sunny Day",
            effectText = "3's reveal early and can't be targeted",
            ruleText = "After all players secretly choose and the weather is revealed, immediately reveal every player who selected 3 in the log. Any player revealed this way is untargetable for Step 2 targeting this round.",
            category = WeatherCategory.REVEAL,
            complexity = WeatherComplexity.EASY,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.REVEAL_THREES)
        ),

        WeatherCard(
            id = "low_pressure",
            displayName = "Low Pressure",
            effectText = "Every guess must copy the first",
            ruleText = "Once the first targeting action with a guess occurs, store that guessed value. For the rest of the round, any later player who targets must use that same guess value. They cannot choose the other guess.",
            category = WeatherCategory.TARGETING,
            complexity = WeatherComplexity.MEDIUM,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.LOCK_GUESSES_SAME_AS_FIRST)
        ),

        WeatherCard(
            id = "windshear",
            displayName = "Windshear",
            effectText = "No one can copy the first guess",
            ruleText = "Once the first targeting action with a guess occurs, store that guessed value. For the rest of the round, any later player who targets must guess the other value: if the first guess was 1, later guessers must guess 3; if the first guess was 3, later guessers must guess 1.",
            category = WeatherCategory.TARGETING,
            complexity = WeatherComplexity.MEDIUM,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.LOCK_GUESSES_OPPOSITE_OF_FIRST)
        ),

        WeatherCard(
            id = "static_charge",
            displayName = "Static Charge",
            effectText = " No marbles move for the first guesser",
            ruleText = "Identify the first player this round who makes a target-and-guess action. If their guess is correct, award them 0 instead of the normal reward. If their guess is wrong and the target actually chose 0, do not subtract 1 from the guesser, but still move the Jester Hat to that guesser. All other targeting resolutions use normal rules.",
            category = WeatherCategory.TARGETING,
            complexity = WeatherComplexity.MEDIUM,
            copies = 3,
            enabled = true,
            effectTags = setOf(
                WeatherEffectTag.FIRST_GUESSER_NO_REWARD_IF_CORRECT,
                WeatherEffectTag.FIRST_WRONG_ON_ZERO_NO_LOSS_BUT_HAT_MOVES
            )
        ),

        WeatherCard(
            id = "crosswinds",
            displayName = "Crosswinds",
            effectText = "If you target, you must target 2 players with the same guess",
            ruleText = "On a players Step 2 turn, they may still pass. If they target, they must choose exactly 2 untargeted targets, and they must apply the same guess value to both. Resolve both targetings as part of that one action. A player cannot choose only 1 target under this weather, if they only have 1 target they must pass.",
            category = WeatherCategory.TARGETING,
            complexity = WeatherComplexity.HARD,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.MUST_TARGET_TWO_WITH_SAME_GUESS_IF_TARGETING)
        ),

        WeatherCard(
            id = "sleet",
            displayName = "Sleet",
            effectText = "Scoring flow changes from bowl->cup to cup->cup",
            ruleText = "If a players choice is untargeted, they get points from the bowl like usual. If a player guesses another player wrong, the target takes their choice from the guesser's total instead of the bowl. If a player has their choice guessed correctly, they give that many marbles from their total to the guesser. If a player guesses wrong and the right answer was a 0, the guesser gives 1 marble to the target instead of back to the bowl. If the amount of marbles that need to transfer is greater than those that can be removed from a player, take as many as will leave that player at 0 and then stop. Players can not have negative marbles.",
            category = WeatherCategory.ECONOMY,
            complexity = WeatherComplexity.HARD,
            copies = 1,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.USE_CUP_TO_CUP_TRANSFERS)
        ),

        WeatherCard(
            id = "thunderstorm",
            displayName = "Thunderstorm",
            effectText = "Wrong guesses on a 0 cost 3 marbles instead of 1",
            ruleText = "If a guesser targets a player, guesses wrong, and that target actually chose 0, subtract 3 from the guesser instead of 1, to a minimum of 0 if needed. Then move the Jester Hat to the guesser.",
            category = WeatherCategory.RISK,
            complexity = WeatherComplexity.EASY,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.WRONG_ZERO_COSTS_THREE)
        ),

        WeatherCard(
            id = "drought",
            displayName = "Drought",
            effectText = "Guesses move no marbles, but trickling and the Hat still work",
            ruleText = "Correct guesses award 0. Wrong guesses on 1 or 3 award 0 to the target. Untargeted players still gain their normal trickle. If a guess is wrong on a 0, the usual Hat movement still happens, but no marble loss occurs because guesses do not move marbles this round.",
            category = WeatherCategory.ECONOMY,
            complexity = WeatherComplexity.MEDIUM,
            copies = 1,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.NO_MARBLES_MOVE)
        ),

        WeatherCard(
            id = "lightning_storm",
            displayName = "Lightning Storm",
            effectText = "The first player who guesses wrong on a 0 gets +3",
            ruleText = "The first time in the round that a player guesses wrong and the target actually chose 0, that guesser gains 3 marbles instead of losing 1. The Hat still moves to that guesser. Any later wrong-on-0 events in the same round resolve normally unless otherwise specified.",
            category = WeatherCategory.RISK,
            complexity = WeatherComplexity.MEDIUM,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.FIRST_WRONG_ZERO_GIVES_THREE)
        ),

        WeatherCard(
            id = "tornado",
            displayName = "Tornado",
            effectText = "If you can target, you must target",
            ruleText = "On a players Step 2 turn, if there is at least one legal untargeted player they are allowed to target, they may not pass. They must make a targeting action. Only players with no legal target may pass.",
            category = WeatherCategory.TARGETING,
            complexity = WeatherComplexity.MEDIUM,
            copies = 1,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.MUST_TARGET_IF_LEGAL)
        ),

        WeatherCard(
            id = "hail",
            displayName = "Hail",
            effectText = "All 3's score as 4's",
            ruleText = "During Trickling, if a player was not targeted and their selection is 3, award 4 instead of 3. Before trickling, if a player who chose 3 is guessed incorrectly, they are awarded 4 instead of 3. If a player guesses another player's 3 correctly, the guesser is awarded 4 instead of 3.",
            category = WeatherCategory.SCORING,
            complexity = WeatherComplexity.EASY,
            copies = 3,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.SCORE_THREES_AS_FOUR)
        ),

        WeatherCard(
            id = "hurricane",
            displayName = "Hurricane",
            effectText = "Untargeted 0's gain 1 during Trickling",
            ruleText = "During Trickling, if a player chose 0 and was not targeted and guessed as a 0, award them 1 marble.",
            category = WeatherCategory.ECONOMY,
            complexity = WeatherComplexity.EASY,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.UNTARGETED_ZERO_TRICKLES_FOR_ONE)
        ),

        WeatherCard(
            id = "rainbow",
            displayName = "Rainbow",
            effectText = "Wrong guesses on a 0 give the guesser +1",
            ruleText = "If a guesser guesses wrong and the target actually chose 0, add 1 marble to the guesser instead of subtracting 1. Then move the Jester Hat to the guesser.",
            category = WeatherCategory.RISK,
            complexity = WeatherComplexity.EASY,
            copies = 2,
            enabled = true,
            effectTags = setOf(WeatherEffectTag.WRONG_ZERO_GIVES_ONE)
        ),

        WeatherCard(
            id = "perfect_storm",
            displayName = "Perfect Storm",
            effectText = "All scoring is doubled",
            ruleText = "Double all marble gains this round. Double all marble losses this round. Specifically, wrong-on-0 penalties become -2 instead of -1, and the Hat still moves. Correct guesses of 1 become +2, correct guesses of 3 become +6, wrong guesses into actual 1 become target +2, wrong guesses into actual 3 become target +6, untargeted trickle of 1 becomes +2, untargeted trickle of 3 becomes +6.",
            category = WeatherCategory.SCORING,
            complexity = WeatherComplexity.HARD,
            copies = 1,
            enabled = true,
            effectTags = setOf(
                WeatherEffectTag.DOUBLE_ALL_MARBLE_GAINS,
                WeatherEffectTag.DOUBLE_ALL_MARBLE_LOSSES,
                WeatherEffectTag.WRONG_ZERO_COSTS_TWO
            )
        ),

        WeatherCard(
            id = "heat_mirage",
            displayName = "Heat Mirage",
            effectText = "Choices rotate before targeting: 0->1, 1->3, 3->0",
            ruleText = "After all players have selected and weather is revealed, transform each players effective selection for this round as follows: 0 becomes 1, 1 becomes 3, 3 becomes 0. All targeting and Trickling then resolve using the transformed value, not the originally chosen value.",
            category = WeatherCategory.TRANSFORM,
            complexity = WeatherComplexity.MEDIUM,
            copies = 2,
            enabled = true,
            digitalOnly = true,
            effectTags = setOf(WeatherEffectTag.ROTATE_SELECTED_VALUES_0_TO_1_TO_3_TO_0)
        ),

        WeatherCard(
            id = "high_pressure",
            displayName = "High Pressure",
            effectText = "You may score from only one source this round",
            ruleText = "For each player, only the first scoring event that would give them marbles this round actually gives marbles. Any later positive marble gains to that same player during the same round become 0. Negative effects and Hat movement still resolve normally. i.e. if a player gains points from guessing correctly, they do not trickle later on. if a player gains points because they were guessed incorrectly, they must pass instead of targeting.",
            category = WeatherCategory.RESOLUTION,
            complexity = WeatherComplexity.HARD,
            copies = 2,
            enabled = true,
            digitalOnly = true,
            effectTags = setOf(WeatherEffectTag.ONE_POSITIVE_SCORING_EVENT_PER_PLAYER)
        ),

        WeatherCard(
            id = "stormfront",
            displayName = "Stormfront",
            effectText = "Only the first half of players can target",
            ruleText = "Let N be the number of players in the game. This round, only the first floor(N/2) actual target-and-guess actions may occur. Once that many targeting actions have happened, every remaining player must pass on their Step 2 turn. Trickling still happens normally.",
            category = WeatherCategory.TARGETING,
            complexity = WeatherComplexity.HARD,
            copies = 2,
            enabled = true,
            digitalOnly = true,
            effectTags = setOf(WeatherEffectTag.LIMIT_TARGETING_ACTIONS_TO_HALF_ROUNDED_DOWN)
        ),

        WeatherCard(
            id = "cold_rain",
            displayName = "Cold Rain",
            effectText = "Marbles are divided evenly at the end of the round",
            ruleText = "Resolve the round normally. Compute each players net marble gain for the round, counting only positive net gains into the pool. Sum those gains into one pool. Evenly distribute that pool among all players. Any remainder stays in the bowl.",
            category = WeatherCategory.RESOLUTION,
            complexity = WeatherComplexity.HARD,
            copies = 1,
            enabled = true,
            digitalOnly = true,
            effectTags = setOf(WeatherEffectTag.REDISTRIBUTE_ALL_POSITIVE_NET_GAINS_TO_ALL_PLAYERS)
        ),

        WeatherCard(
            id = "thunderhead",
            displayName = "Thunderhead",
            effectText = "Only the top round-scorers keep their points",
            ruleText = "After the round resolves, compare how many marbles each player gained this round. Only the player or players tied for the highest round gain keep those gains. All other players round gains are reduced to 0. Hat movement and non-scoring state changes still stand.",
            category = WeatherCategory.RESOLUTION,
            complexity = WeatherComplexity.HARD,
            copies = 1,
            enabled = true,
            digitalOnly = true,
            effectTags = setOf(WeatherEffectTag.ONLY_TOP_ROUND_SCORERS_KEEP_POINTS)
        ),

        WeatherCard(
            id = "cool_breeze",
            displayName = "Cool Breeze",
            effectText = "Only the lowest round-scorers keep their points",
            ruleText = "After the round resolves, compare how many marbles each player gained this round. Only the player or players tied for the lowest positive kept qualifying round gain keep those gains, according to your established Cool Breeze interpretation. All other players round gains are reduced to 0. Players who gained 0 points do not count as the lowest, you must gain at least 1 point to be considered a scorer.",
            category = WeatherCategory.RESOLUTION,
            complexity = WeatherComplexity.HARD,
            copies = 1,
            enabled = true,
            digitalOnly = true,
            includedInDeck = true,
            effectTags = setOf(WeatherEffectTag.ONLY_LOWEST_POSITIVE_ROUND_SCORERS_KEEP_POINTS)
        ),

        WeatherCard(
            id = "snow",
            displayName = "Snow",
            effectText = "Targeted gains are reduced by 1, and Trickling gains are increased by 2",
            ruleText = "Any positive marble gain caused by targeting is reduced by 1, to a minimum of 0. During Trickling, each untargeted player's gain is increased by 2.",
            category = WeatherCategory.SCORING,
            complexity = WeatherComplexity.MEDIUM,
            copies = 2,
            enabled = true,
            digitalOnly = true,
            effectTags = setOf(
                WeatherEffectTag.REDUCE_TARGETED_GAINS_BY_ONE,
                WeatherEffectTag.INCREASE_TRICKLE_GAINS_BY_TWO
            )
        ),

        WeatherCard(
            id = "whiteout",
            displayName = "Whiteout",
            effectText = "Untargeted players choices are hidden during Trickling",
            ruleText = "Scoring still resolves normally for untargeted players, but their chosen values are not shown to players or logs as public reveal information. The system still knows the values internally; visibility only is suppressed.",
            category = WeatherCategory.INFORMATION,
            complexity = WeatherComplexity.MEDIUM,
            copies = 333,
            enabled = true,
            digitalOnly = true,
            effectTags = setOf(WeatherEffectTag.HIDE_UNTARGETED_TRICKLE_REVEALS)
        ),

        WeatherCard(
            id = "smog",
            displayName = "Smog",
            effectText = "Highest scorers reveal score and 0/1/3, gain now, and cannot be targeted",
            ruleText = "When weather is revealed, every player tied for the highest score reveals both their current score and their selected 0, 1, or 3. They immediately gain that amount, then do not gain again during Trickling because they have already been revealed. Those players cannot be targeted for the rest of the round.",
            category = WeatherCategory.INFORMATION,
            complexity = WeatherComplexity.HARD,
            copies = 2,
            enabled = true,
            digitalOnly = true,
            effectTags = setOf(WeatherEffectTag.REVEAL_HIGHEST_SCORE_CHOICES_AND_PROTECT)
        )
    )

    val enabledCards: List<WeatherCard>
        get() = allCards.filter { it.enabled }

    val enabledPhysicalCards: List<WeatherCard>
        get() = enabledCards.filter { !it.digitalOnly && it.includedInDeck }

    val enabledDigitalCards: List<WeatherCard>
        get() = enabledCards.filter { it.includedInDeck }

    fun cardById(id: String): WeatherCard? =
        allCards.firstOrNull { it.id == id }

    fun firstRoundWeather(): WeatherCard =
        cardById(FIRST_ROUND_WEATHER_ID)
            ?: error("Missing required first-round weather card: $FIRST_ROUND_WEATHER_ID")

    fun buildDeck(includeDigitalCards: Boolean): List<WeatherCard> {
        val source = if (includeDigitalCards) enabledDigitalCards else enabledPhysicalCards
        return source.flatMap { card -> List(card.copies) { card } }
    }

    fun drawRandomWeather(
        rng: Random,
        includeDigitalCards: Boolean,
        excludeIds: Set<String> = emptySet()
    ): WeatherCard {
        val deck = buildDeck(includeDigitalCards).filter { it.id !in excludeIds }
        require(deck.isNotEmpty()) { "No enabled weather cards available to draw." }
        return deck.random(rng)
    }

    fun easyImplementationDeck(includeDigitalCards: Boolean = false): List<WeatherCard> {
        val easyIds = setOf(
            "drizzle",
            "downpour",
            "fog",
            "sunny_day",
            "low_pressure",
            "windshear",
            "hail"
        )

        val source = if (includeDigitalCards) allCards else allCards.filter { !it.digitalOnly }
        return source
            .filter { it.id in easyIds && it.includedInDeck }
            .flatMap { card -> List(card.copies) { card } }
    }

    fun isImplemented(card: WeatherCard): Boolean = card.enabled

    fun affectsReveal(card: WeatherCard): Boolean =
        WeatherEffectTag.REVEAL_ONES in card.effectTags ||
                WeatherEffectTag.REVEAL_THREES in card.effectTags

    fun affectsScoring(card: WeatherCard): Boolean =
        WeatherEffectTag.SCORE_ONES_AS_TWO in card.effectTags ||
                WeatherEffectTag.SCORE_THREES_AS_FOUR in card.effectTags ||
                WeatherEffectTag.DOUBLE_ALL_MARBLE_GAINS in card.effectTags ||
                WeatherEffectTag.DOUBLE_ALL_MARBLE_LOSSES in card.effectTags ||
                WeatherEffectTag.UNTARGETED_ZERO_TRICKLES_FOR_ONE in card.effectTags ||
                WeatherEffectTag.REDUCE_TARGETED_GAINS_BY_ONE in card.effectTags ||
                WeatherEffectTag.INCREASE_TRICKLE_GAINS_BY_TWO in card.effectTags

    fun affectsTargeting(card: WeatherCard): Boolean =
        WeatherEffectTag.LOCK_GUESSES_SAME_AS_FIRST in card.effectTags ||
                WeatherEffectTag.LOCK_GUESSES_OPPOSITE_OF_FIRST in card.effectTags ||
                WeatherEffectTag.FIRST_GUESSER_NO_REWARD_IF_CORRECT in card.effectTags ||
                WeatherEffectTag.FIRST_WRONG_ON_ZERO_NO_LOSS_BUT_HAT_MOVES in card.effectTags ||
                WeatherEffectTag.MUST_TARGET_TWO_WITH_SAME_GUESS_IF_TARGETING in card.effectTags ||
                WeatherEffectTag.MUST_TARGET_IF_LEGAL in card.effectTags ||
                WeatherEffectTag.LIMIT_TARGETING_ACTIONS_TO_HALF_ROUNDED_DOWN in card.effectTags ||
                WeatherEffectTag.REVEAL_HIGHEST_SCORE_CHOICES_AND_PROTECT in card.effectTags

    fun affectsResolution(card: WeatherCard): Boolean =
        WeatherEffectTag.ONE_POSITIVE_SCORING_EVENT_PER_PLAYER in card.effectTags ||
                WeatherEffectTag.REDISTRIBUTE_ALL_POSITIVE_NET_GAINS_TO_ALL_PLAYERS in card.effectTags ||
                WeatherEffectTag.ONLY_TOP_ROUND_SCORERS_KEEP_POINTS in card.effectTags ||
                WeatherEffectTag.ONLY_LOWEST_POSITIVE_ROUND_SCORERS_KEEP_POINTS in card.effectTags

    fun affectsInformation(card: WeatherCard): Boolean =
        WeatherEffectTag.HIDE_UNTARGETED_TRICKLE_REVEALS in card.effectTags ||
                WeatherEffectTag.REVEAL_HIGHEST_SCORE_CHOICES_AND_PROTECT in card.effectTags

    fun transformSelectedChoice(choice: DieChoice, card: WeatherCard): DieChoice {
        if (WeatherEffectTag.ROTATE_SELECTED_VALUES_0_TO_1_TO_3_TO_0 !in card.effectTags) {
            return choice
        }

        return when (choice) {
            DieChoice.ZERO -> DieChoice.ONE
            DieChoice.ONE -> DieChoice.THREE
            DieChoice.THREE -> DieChoice.ZERO
        }
    }

    fun scoreValueForChosenNumber(chosenValue: Int, card: WeatherCard?): Int {
        if (card == null) return chosenValue

        var result = when {
            chosenValue == 1 && WeatherEffectTag.SCORE_ONES_AS_TWO in card.effectTags -> 2
            chosenValue == 3 && WeatherEffectTag.SCORE_THREES_AS_FOUR in card.effectTags -> 4
            chosenValue == 0 && WeatherEffectTag.UNTARGETED_ZERO_TRICKLES_FOR_ONE in card.effectTags -> 1
            else -> chosenValue
        }

        if (WeatherEffectTag.INCREASE_TRICKLE_GAINS_BY_TWO in card.effectTags) {
            result += 2
        }

        if (WeatherEffectTag.DOUBLE_ALL_MARBLE_GAINS in card.effectTags) {
            result *= 2
        }

        return result
    }

    fun wrongZeroPenalty(card: WeatherCard?): Int {
        if (card == null) return -1

        return when {
            WeatherEffectTag.WRONG_ZERO_COSTS_THREE in card.effectTags -> -3
            WeatherEffectTag.WRONG_ZERO_COSTS_TWO in card.effectTags -> -2
            WeatherEffectTag.WRONG_ZERO_GIVES_ONE in card.effectTags -> +1
            else -> -1
        }
    }

    fun hasFirstWrongZeroBonus(card: WeatherCard?): Boolean =
        card != null && WeatherEffectTag.FIRST_WRONG_ZERO_GIVES_THREE in card.effectTags

    fun noMarblesMove(card: WeatherCard?): Boolean =
        card != null && WeatherEffectTag.NO_MARBLES_MOVE in card.effectTags

    fun useCupToCupTransfers(card: WeatherCard?): Boolean =
        card != null && WeatherEffectTag.USE_CUP_TO_CUP_TRANSFERS in card.effectTags

    fun revealOnes(card: WeatherCard?): Boolean =
        card != null && WeatherEffectTag.REVEAL_ONES in card.effectTags

    fun revealThrees(card: WeatherCard?): Boolean =
        card != null && WeatherEffectTag.REVEAL_THREES in card.effectTags
}