package com.example.bugs.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bugs.AppDao
import com.example.bugs.LevelEntity
import com.example.bugs.levels.LevelDraft
import com.example.bugs.levels.LevelValidationResult
import com.example.bugs.levels.LevelValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LevelEditorViewModel(
    private val dao: AppDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    var levels by mutableStateOf<List<LevelEntity>>(emptyList())
        private set

    var draft by mutableStateOf(LevelDraft())
        private set

    var validationErrors by mutableStateOf<List<String>>(emptyList())
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    val isEditingExisting: Boolean
        get() = draft.id != 0L

    fun loadLevels() {
        viewModelScope.launch {
            levels = withContext(ioDispatcher) { dao.getAllLevels() }
        }
    }

    fun startNewLevel() {
        draft = LevelDraft()
        validationErrors = emptyList()
        statusMessage = "Создание нового уровня"
    }

    fun selectLevelForEdit(level: LevelEntity) {
        draft = LevelDraft.fromEntity(level)
        validationErrors = emptyList()
        statusMessage = "Редактирование: ${level.name}"
    }

    fun updateDraft(transform: (LevelDraft) -> LevelDraft) {
        draft = transform(draft)
        validationErrors = emptyList()
    }

    fun saveCurrentDraft(onSaved: ((LevelEntity) -> Unit)? = null) {
        when (val result = LevelValidator.validate(draft)) {
            is LevelValidationResult.Invalid -> {
                validationErrors = result.errors
                statusMessage = null
            }
            is LevelValidationResult.Valid -> {
                validationErrors = emptyList()
                viewModelScope.launch {
                    val savedEntity = withContext(ioDispatcher) {
                        if (result.entity.id == 0L) {
                            val newId = dao.insertLevel(result.entity)
                            result.entity.copy(id = newId)
                        } else {
                            dao.updateLevel(result.entity)
                            result.entity
                        }
                    }
                    draft = LevelDraft.fromEntity(savedEntity)
                    levels = withContext(ioDispatcher) { dao.getAllLevels() }
                    statusMessage = "Уровень «${savedEntity.name}» сохранен"
                    onSaved?.invoke(savedEntity)
                }
            }
        }
    }

    fun deleteLevel(level: LevelEntity) {
        viewModelScope.launch {
            withContext(ioDispatcher) { dao.deleteLevel(level) }
            if (draft.id == level.id) {
                draft = LevelDraft()
            }
            levels = withContext(ioDispatcher) { dao.getAllLevels() }
            statusMessage = "Уровень «${level.name}» удален"
        }
    }

    fun buildValidLevelOrNull(): LevelEntity? {
        return when (val result = LevelValidator.validate(draft)) {
            is LevelValidationResult.Valid -> {
                validationErrors = emptyList()
                result.entity
            }
            is LevelValidationResult.Invalid -> {
                validationErrors = result.errors
                null
            }
        }
    }
}