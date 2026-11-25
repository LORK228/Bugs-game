package com.example.bugs.levels

import com.example.bugs.BugType
import com.example.bugs.LevelEntity
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class LevelDraft(
    val id: Long = 0L,
    val name: String = "Мой уровень",
    val speed: Int = 5,
    val maxBugs: Int = 15,
    val roundDuration: Int = 60,
    val bonusInterval: Int = 15,
    val fastBugChance: Int = 25,
    val rareBugChance: Int = 15
) {
    val commonBugChance: Int
        get() = (100 - fastBugChance - rareBugChance).coerceAtLeast(0)

    companion object {
        fun fromEntity(entity: LevelEntity) = LevelDraft(
            id = entity.id,
            name = entity.name,
            speed = entity.speed,
            maxBugs = entity.maxBugs,
            roundDuration = entity.roundDuration,
            bonusInterval = entity.bonusInterval,
            fastBugChance = entity.fastBugChance,
            rareBugChance = entity.rareBugChance
        )
    }
}

sealed class LevelValidationResult {
    data class Valid(val entity: LevelEntity) : LevelValidationResult()
    data class Invalid(val errors: List<String>) : LevelValidationResult()
}

data class DifficultySummary(
    val index: Int,          // 1..10
    val label: String,
    val commonChance: Int,
    val fastChance: Int,
    val rareChance: Int
)

data class PreviewBug(
    val id: Int,
    val x: Float,
    val y: Float,
    val dx: Float,
    val dy: Float,
    val type: BugType,
    val sizeDp: Float
)

object LevelValidator {
    val SPEED_RANGE = 1..10
    val MAX_BUGS_RANGE = 1..50
    val DURATION_RANGE = 10..300
    val BONUS_INTERVAL_RANGE = 5..60
    val FAST_CHANCE_RANGE = 0..70
    val RARE_CHANCE_RANGE = 0..40
    const val MAX_NAME_LENGTH = 40

    fun validate(draft: LevelDraft, nowMillis: Long = System.currentTimeMillis()): LevelValidationResult {
        val errors = mutableListOf<String>()
        val trimmedName = draft.name.trim()

        if (trimmedName.isEmpty()) {
            errors += "Введите название уровня"
        } else if (trimmedName.length > MAX_NAME_LENGTH) {
            errors += "Название не должно превышать $MAX_NAME_LENGTH символов"
        }

        if (draft.speed !in SPEED_RANGE) {
            errors += "Скорость должна быть в диапазоне ${SPEED_RANGE.first}..${SPEED_RANGE.last}"
        }
        if (draft.maxBugs !in MAX_BUGS_RANGE) {
            errors += "Количество жуков должно быть в диапазоне ${MAX_BUGS_RANGE.first}..${MAX_BUGS_RANGE.last}"
        }
        if (draft.roundDuration !in DURATION_RANGE) {
            errors += "Длительность раунда должна быть в диапазоне ${DURATION_RANGE.first}..${DURATION_RANGE.last} сек"
        }
        if (draft.bonusInterval !in BONUS_INTERVAL_RANGE) {
            errors += "Интервал бонусов должен быть в диапазоне ${BONUS_INTERVAL_RANGE.first}..${BONUS_INTERVAL_RANGE.last} сек"
        }
        if (draft.fastBugChance !in FAST_CHANCE_RANGE) {
            errors += "Шанс быстрых жуков должен быть в диапазоне ${FAST_CHANCE_RANGE.first}..${FAST_CHANCE_RANGE.last}%"
        }
        if (draft.rareBugChance !in RARE_CHANCE_RANGE) {
            errors += "Шанс редких жуков должен быть в диапазоне ${RARE_CHANCE_RANGE.first}..${RARE_CHANCE_RANGE.last}%"
        }
        if (draft.fastBugChance + draft.rareBugChance > 100) {
            errors += "Сумма шансов быстрых и редких жуков не может превышать 100%"
        }

        return if (errors.isEmpty()) {
            LevelValidationResult.Valid(
                LevelEntity(
                    id = draft.id,
                    name = trimmedName,
                    speed = draft.speed,
                    maxBugs = draft.maxBugs,
                    roundDuration = draft.roundDuration,
                    bonusInterval = draft.bonusInterval,
                    fastBugChance = draft.fastBugChance,
                    rareBugChance = draft.rareBugChance,
                    updatedAt = nowMillis
                )
            )
        } else {
            LevelValidationResult.Invalid(errors)
        }
    }
}

object LevelPreviewCalculator {

    fun calculateDifficulty(draft: LevelDraft): DifficultySummary {
        val speedNorm = (draft.speed - 1) / 9f
        val bugsNorm = (draft.maxBugs - 1) / 49f
        val fastNorm = draft.fastBugChance / 70f
        val bonusNorm = (draft.bonusInterval - 5) / 55f // реже бонусы -> сложнее

        val raw = (speedNorm * 0.40f + bugsNorm * 0.25f + fastNorm * 0.20f + bonusNorm * 0.15f)
            .coerceIn(0f, 1f)
        val index = (1 + raw * 9f).roundToInt().coerceIn(1, 10)

        val label = when (index) {
            in 1..3 -> "Легкий"
            in 4..6 -> "Средний"
            in 7..8 -> "Сложный"
            else -> "Экстремальный"
        }

        return DifficultySummary(
            index = index,
            label = label,
            commonChance = draft.commonBugChance,
            fastChance = draft.fastBugChance,
            rareChance = draft.rareBugChance
        )
    }

    fun pickBugType(fastChance: Int, rareChance: Int, roll: Int): BugType {
        val safeRoll = roll.coerceIn(0, 99)
        return when {
            safeRoll < fastChance -> BugType.FAST
            safeRoll < fastChance + rareChance -> BugType.RARE
            else -> BugType.COMMON
        }
    }

    fun generatePreviewBugs(
        draft: LevelDraft,
        areaWidthDp: Float,
        areaHeightDp: Float,
        seed: Int = 42
    ): List<PreviewBug> {
        val random = Random(seed)
        val previewCount = draft.maxBugs.coerceIn(1, 16)
        val safeW = areaWidthDp.coerceAtLeast(80f)
        val safeH = areaHeightDp.coerceAtLeast(80f)

        return List(previewCount) { idx ->
            val type = pickBugType(draft.fastBugChance, draft.rareBugChance, random.nextInt(100))
            val scaledSize = (type.sizeDp * 0.5f).coerceIn(18f, 34f)
            val angle = random.nextFloat() * (2f * Math.PI.toFloat())
            val velocity = (type.speed * (0.4f + draft.speed * 0.14f))

            PreviewBug(
                id = idx,
                x = random.nextFloat() * (safeW - scaledSize).coerceAtLeast(1f),
                y = random.nextFloat() * (safeH - scaledSize).coerceAtLeast(1f),
                dx = cos(angle) * velocity,
                dy = sin(angle) * velocity,
                type = type,
                sizeDp = scaledSize
            )
        }
    }

    fun stepPreviewBugs(
        bugs: List<PreviewBug>,
        areaWidthDp: Float,
        areaHeightDp: Float
    ): List<PreviewBug> {
        return bugs.map { bug ->
            val maxX = (areaWidthDp - bug.sizeDp).coerceAtLeast(0f)
            val maxY = (areaHeightDp - bug.sizeDp).coerceAtLeast(0f)
            var nextX = bug.x + bug.dx
            var nextY = bug.y + bug.dy
            var nextDx = bug.dx
            var nextDy = bug.dy

            if (nextX <= 0f || nextX >= maxX) {
                nextDx = -nextDx
                nextX = nextX.coerceIn(0f, maxX)
            }
            if (nextY <= 0f || nextY >= maxY) {
                nextDy = -nextDy
                nextY = nextY.coerceIn(0f, maxY)
            }

            bug.copy(x = nextX, y = nextY, dx = nextDx, dy = nextDy)
        }
    }
}
