package com.example.bugs

import com.example.bugs.levels.LevelDraft
import com.example.bugs.levels.LevelPreviewCalculator
import com.example.bugs.levels.LevelValidationResult
import com.example.bugs.levels.LevelValidator
import com.example.bugs.viewmodel.LevelEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LevelLogicTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validate_validDraft_returnsTrimmedEntity() {
        val draft = LevelDraft(
            name = "  Ночная охота  ",
            speed = 7,
            maxBugs = 25,
            roundDuration = 90,
            bonusInterval = 20,
            fastBugChance = 40,
            rareBugChance = 20
        )

        val result = LevelValidator.validate(draft, nowMillis = 123456L)

        assertTrue(result is LevelValidationResult.Valid)
        val entity = (result as LevelValidationResult.Valid).entity
        assertEquals("Ночная охота", entity.name)
        assertEquals(7, entity.speed)
        assertEquals(25, entity.maxBugs)
        assertEquals(90, entity.roundDuration)
        assertEquals(123456L, entity.updatedAt)
    }

    @Test
    fun validate_invalidDraft_collectsAllConstraintErrors() {
        val invalid = LevelDraft(
            name = "   ",
            speed = 15,
            maxBugs = 0,
            roundDuration = 5,
            bonusInterval = 120,
            fastBugChance = 70,
            rareBugChance = 40 
        )

        val result = LevelValidator.validate(invalid)

        assertTrue(result is LevelValidationResult.Invalid)
        val errors = (result as LevelValidationResult.Invalid).errors
        assertTrue(errors.size >= 5)
        assertTrue(errors.any { it.contains("название", ignoreCase = true) })
        assertTrue(errors.any { it.contains("100%") })
    }

    @Test
    fun calculateDifficulty_harderParamsProduceHigherIndex() {
        val easy = LevelDraft(speed = 1, maxBugs = 3, bonusInterval = 5, fastBugChance = 0, rareBugChance = 10)
        val hard = LevelDraft(speed = 10, maxBugs = 45, bonusInterval = 60, fastBugChance = 65, rareBugChance = 25)

        val easyDiff = LevelPreviewCalculator.calculateDifficulty(easy)
        val hardDiff = LevelPreviewCalculator.calculateDifficulty(hard)

        assertTrue("Сложный уровень должен иметь индекс выше легкого", hardDiff.index > easyDiff.index)
        assertEquals("Легкий", easyDiff.label)
        assertEquals("Экстремальный", hardDiff.label)
        assertEquals(90, easyDiff.commonChance)
        assertEquals(10, hardDiff.commonChance)
    }

    @Test
    fun previewSimulation_keepsBugsInsideBounds() {
        val draft = LevelDraft(speed = 10, maxBugs = 40, fastBugChance = 50, rareBugChance = 20)
        val width = 300f
        val height = 180f

        var bugs = LevelPreviewCalculator.generatePreviewBugs(draft, width, height, seed = 7)
        assertEquals(16, bugs.size) 

        repeat(50) {
            bugs = LevelPreviewCalculator.stepPreviewBugs(bugs, width, height)
            bugs.forEach { bug ->
                assertTrue("X вне левой границы: ${bug.x}", bug.x >= 0f)
                assertTrue("X вне правой границы: ${bug.x}", bug.x <= width - bug.sizeDp)
                assertTrue("Y вне верхней границы: ${bug.y}", bug.y >= 0f)
                assertTrue("Y вне нижней границы: ${bug.y}", bug.y <= height - bug.sizeDp)
            }
        }
    }

    @Test
    fun levelEditorViewModel_createEditDeleteFlow() = runTest {
        val fakeDao = FakeAppDao()
        val vm = LevelEditorViewModel(fakeDao, testDispatcher)

        vm.updateDraft { it.copy(name = "Спринт", speed = 8, maxBugs = 12, roundDuration = 30) }
        vm.saveCurrentDraft()
        advanceUntilIdle()

        assertEquals(1, vm.levels.size)
        val created = vm.levels.first()
        assertEquals("Спринт", created.name)
        assertTrue(vm.isEditingExisting)

        vm.updateDraft { it.copy(name = "Спринт v2", speed = 9) }
        vm.saveCurrentDraft()
        advanceUntilIdle()

        assertEquals(1, vm.levels.size)
        assertEquals(created.id, vm.levels.first().id)
        assertEquals("Спринт v2", vm.levels.first().name)
        assertEquals(9, vm.levels.first().speed)

        vm.deleteLevel(vm.levels.first())
        advanceUntilIdle()

        assertTrue(vm.levels.isEmpty())
    }
}

private class FakeAppDao : AppDao {
    private val storage = mutableListOf<LevelEntity>()
    private var nextId = 1L

    override suspend fun insertPlayer(player: PlayerEntity): Long = 1L
    override suspend fun getAllPlayers(): List<PlayerEntity> = emptyList()
    override suspend fun insertRecord(record: RecordEntity): Long = 1L
    override suspend fun getAllRecords(): List<RecordWithPlayer> = emptyList()

    override suspend fun getAllLevels(): List<LevelEntity> =
        storage.sortedByDescending { it.updatedAt }

    override suspend fun getLevelById(id: Long): LevelEntity? =
        storage.firstOrNull { it.id == id }

    override suspend fun insertLevel(level: LevelEntity): Long {
        val id = nextId++
        storage += level.copy(id = id)
        return id
    }

    override suspend fun updateLevel(level: LevelEntity) {
        val idx = storage.indexOfFirst { it.id == level.id }
        if (idx >= 0) storage[idx] = level
    }

    override suspend fun deleteLevel(level: LevelEntity) {
        storage.removeAll { it.id == level.id }
    }
}