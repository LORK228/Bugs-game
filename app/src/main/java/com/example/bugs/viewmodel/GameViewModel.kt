package com.example.bugs.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bugs.AppDatabase
import com.example.bugs.Bug
import com.example.bugs.BugType
import com.example.bugs.PlayerEntity
import com.example.bugs.RecordEntity
import com.example.bugs.network.CbrNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

data class BonusItem(val x: Float, val y: Float)

class GameViewModel(
    private val database: AppDatabase
) : ViewModel() {

    var isGameRunning by mutableStateOf(false)
    var currentPlayer by mutableStateOf<PlayerEntity?>(null)

    fun startSession(player: PlayerEntity) {
        currentPlayer = player
        isGameRunning = true
    }

    fun stopSession() {
        isGameRunning = false
    }

    var score by mutableIntStateOf(0)
        private set
    var hits by mutableIntStateOf(0)
        private set
    var misses by mutableIntStateOf(0)
        private set
    var timeLeft by mutableIntStateOf(60)
        private set
    var isGameOver by mutableStateOf(false)
        private set
    var isGravityBonusActive by mutableStateOf(false)
        private set
    var currentBonus by mutableStateOf<BonusItem?>(null)
        private set

    val activeBugs = mutableStateListOf<Bug>()

    private var gravityBonusTimeLeft = 0
    private var timeUntilNextBonus = 15
    private var goldPrice: Float = 8500f
    private var isInitialized = false

    private var timerJob: Job? = null
    private var bugSpawnerJob: Job? = null
    private var goldSpawnerJob: Job? = null
    private var bugPhysicsJob: Job? = null

    var screenWidth = 0f
    var screenHeight = 0f
    var gravityX = 0f
    var gravityY = 0f

    fun updateScreenDimensions(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        screenWidth = width
        screenHeight = height

        // Подтягиваем жуков внутрь новых границ экрана после поворота
        for (i in activeBugs.indices) {
            val bug = activeBugs[i]
            val clampedX = bug.x.coerceIn(0f, (width - bug.size).coerceAtLeast(0f))
            val clampedY = bug.y.coerceIn(60f, (height - bug.size).coerceAtLeast(60f))
            activeBugs[i] = bug.copy(x = clampedX, y = clampedY)
        }
    }
    fun startGame(
        player: PlayerEntity,
        speedSetting: Int,
        maxCockroaches: Int,
        bonusInterval: Int,
        roundDuration: Int,
        width: Float,
        height: Float
    ) {
        updateScreenDimensions(width, height)

        if (isInitialized) return
        isInitialized = true

        timeLeft = roundDuration
        timeUntilNextBonus = bonusInterval

        viewModelScope.launch(Dispatchers.IO) {
            goldPrice = CbrNetworkClient.fetchGoldPrice()
        }

        startTimer(bonusInterval, player)
        startBugSpawner(speedSetting, maxCockroaches)
        startGoldBugSpawner(speedSetting)
        startPhysicsLoop()
    }

    private fun startTimer(bonusInterval: Int, player: PlayerEntity) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timeLeft > 0 && !isGameOver) {
                delay(1000)
                timeLeft--

                if (isGravityBonusActive) {
                    gravityBonusTimeLeft--
                    if (gravityBonusTimeLeft <= 0) {
                        isGravityBonusActive = false
                    }
                }

                timeUntilNextBonus--
                if (timeUntilNextBonus <= 0 && currentBonus == null && !isGravityBonusActive) {
                    val bx = (50..screenWidth.toInt().coerceAtLeast(100) - 50).random().toFloat()
                    val by = (100..screenHeight.toInt().coerceAtLeast(200) - 50).random().toFloat()
                    currentBonus = BonusItem(bx, by)
                    timeUntilNextBonus = bonusInterval
                }
            }
            onGameOver(player)
        }
    }

    private fun startBugSpawner(speedSetting: Int, maxCockroaches: Int) {
        bugSpawnerJob?.cancel()
        bugSpawnerJob = viewModelScope.launch {
            while (!isGameOver) {
                if (activeBugs.size < maxCockroaches && screenWidth > 0f) {
                    val roll = (1..100).random()
                    val type = when {
                        roll <= 60 -> BugType.COMMON
                        roll <= 85 -> BugType.FAST
                        else -> BugType.RARE
                    }
                    val speed = type.speed * (0.6f + speedSetting * 0.1f)
                    val dirX = if (Random().nextBoolean()) 1f else -1f
                    val dirY = if (Random().nextBoolean()) 1f else -1f

                    val maxX = (screenWidth - type.sizeDp).toInt().coerceAtLeast(1)
                    val minY = 100
                    val maxY = (screenHeight - type.sizeDp - 20).toInt().coerceAtLeast(minY)

                    activeBugs.add(
                        Bug(
                            x = (0..maxX).random().toFloat(),
                            y = (minY..maxY).random().toFloat(),
                            dx = speed * dirX,
                            dy = speed * dirY,
                            size = type.sizeDp,
                            type = type,
                            cost = type.cost
                        )
                    )
                }
                delay((1000 - (speedSetting * 50)).toLong().coerceAtLeast(300))
            }
        }
    }

    private fun startGoldBugSpawner(speedSetting: Int) {
        goldSpawnerJob?.cancel()
        goldSpawnerJob = viewModelScope.launch {
            while (!isGameOver) {
                delay(20_000L)
                if (!isGameOver && screenWidth > 0f) {
                    val goldType = BugType.GOLD
                    val dynamicCost = (goldPrice / 100f).toInt().coerceAtLeast(50)
                    val speed = goldType.speed * (0.6f + speedSetting * 0.1f)
                    val dirX = if (Random().nextBoolean()) 1f else -1f
                    val dirY = if (Random().nextBoolean()) 1f else -1f

                    val maxX = (screenWidth - goldType.sizeDp).toInt().coerceAtLeast(1)
                    val minY = 100
                    val maxY = (screenHeight - goldType.sizeDp - 20).toInt().coerceAtLeast(minY)

                    activeBugs.add(
                        Bug(
                            x = (0..maxX).random().toFloat(),
                            y = (minY..maxY).random().toFloat(),
                            dx = speed * dirX,
                            dy = speed * dirY,
                            size = goldType.sizeDp,
                            type = goldType,
                            cost = dynamicCost
                        )
                    )
                }
            }
        }
    }

    private fun startPhysicsLoop() {
        bugPhysicsJob?.cancel()
        bugPhysicsJob = viewModelScope.launch {
            while (isActive && !isGameOver) {
                for (i in activeBugs.indices.reversed()) {
                    if (i !in activeBugs.indices) continue
                    val bug = activeBugs[i]
                    val sizeF = bug.size.toFloat()
                    var newDx = bug.dx
                    var newDy = bug.dy

                    if (isGravityBonusActive) {
                        newDx += gravityX * 0.5f
                        newDy += gravityY * 0.5f
                        newDx *= 0.95f
                        newDy *= 0.95f
                    } else {
                        if ((1..100).random() > 98) newDx = -newDx
                        if ((1..100).random() > 98) newDy = -newDy
                    }

                    var newX = bug.x + newDx
                    var newY = bug.y + newDy

                    if (newX <= 0f || newX >= screenWidth - sizeF) {
                        newDx = -newDx
                        newX = newX.coerceIn(0f, (screenWidth - sizeF).coerceAtLeast(0f))
                    }
                    if (newY <= 60f || newY >= screenHeight - sizeF) {
                        newDy = -newDy
                        newY = newY.coerceIn(60f, (screenHeight - sizeF).coerceAtLeast(60f))
                    }

                    activeBugs[i] = bug.copy(x = newX, y = newY, dx = newDx, dy = newDy)
                }
                delay(16)
            }
        }
    }

    fun onScreenClick() {
        if (!isGameOver) {
            score -= 5
            misses++
        }
    }

    fun onBugClick(bug: Bug) {
        if (!isGameOver) {
            score += bug.cost
            hits++
            activeBugs.remove(bug)
        }
    }

    fun onBonusClick(): Boolean {
        if (!isGameOver && currentBonus != null) {
            currentBonus = null
            isGravityBonusActive = true
            gravityBonusTimeLeft = 5
            return true
        }
        return false
    }

    private fun onGameOver(player: PlayerEntity) {
        isGameOver = true
        val accuracy = if (hits + misses > 0) (hits * 100) / (hits + misses) else 0
        val record = RecordEntity(
            playerId = player.id,
            score = score,
            difficulty = player.difficulty,
            hits = hits,
            misses = misses,
            accuracy = accuracy
        )
        viewModelScope.launch(Dispatchers.IO) {
            database.appDao().insertRecord(record)
        }
    }

    fun restartGame(
        player: PlayerEntity,
        speedSetting: Int,
        maxCockroaches: Int,
        bonusInterval: Int,
        roundDuration: Int
    ) {
        activeBugs.clear()
        score = 0
        hits = 0
        misses = 0
        timeLeft = roundDuration
        isGameOver = false
        isGravityBonusActive = false
        currentBonus = null
        timeUntilNextBonus = bonusInterval
        isInitialized = false
        startGame(player, speedSetting, maxCockroaches, bonusInterval, roundDuration, screenWidth, screenHeight)
    }
}