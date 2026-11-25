package com.example.bugs.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bugs.LevelEntity
import com.example.bugs.R
import com.example.bugs.levels.LevelDraft
import com.example.bugs.levels.LevelPreviewCalculator
import com.example.bugs.levels.LevelValidator
import com.example.bugs.levels.PreviewBug
import com.example.bugs.viewmodel.LevelEditorViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun LevelEditorTab(
    onPlayLevel: (LevelEntity) -> Unit,
    editorViewModel: LevelEditorViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        editorViewModel.loadLevels()
    }

    val draft = editorViewModel.draft
    val difficulty = remember(draft) { LevelPreviewCalculator.calculateDifficulty(draft) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (editorViewModel.isEditingExisting) "Редактирование уровня" else "Новый уровень",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = { editorViewModel.startNewLevel() }) {
                Text("Сбросить / Новый")
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.name,
            onValueChange = { newName -> editorViewModel.updateDraft { it.copy(name = newName) } },
            label = { Text("Название уровня") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        LevelParamSlider(
            label = "Скорость жуков",
            value = draft.speed,
            range = LevelValidator.SPEED_RANGE,
            unit = "ед."
        ) { v -> editorViewModel.updateDraft { it.copy(speed = v) } }

        LevelParamSlider(
            label = "Максимум жуков на поле",
            value = draft.maxBugs,
            range = LevelValidator.MAX_BUGS_RANGE,
            unit = "шт."
        ) { v -> editorViewModel.updateDraft { it.copy(maxBugs = v) } }

        LevelParamSlider(
            label = "Длительность раунда",
            value = draft.roundDuration,
            range = LevelValidator.DURATION_RANGE,
            unit = "сек"
        ) { v -> editorViewModel.updateDraft { it.copy(roundDuration = v) } }

        LevelParamSlider(
            label = "Интервал бонусов",
            value = draft.bonusInterval,
            range = LevelValidator.BONUS_INTERVAL_RANGE,
            unit = "сек"
        ) { v -> editorViewModel.updateDraft { it.copy(bonusInterval = v) } }

        LevelParamSlider(
            label = "Шанс быстрых жуков",
            value = draft.fastBugChance,
            range = LevelValidator.FAST_CHANCE_RANGE,
            unit = "%"
        ) { v ->
            editorViewModel.updateDraft { current ->
                val adjustedRare = current.rareBugChance.coerceAtMost(100 - v)
                current.copy(fastBugChance = v, rareBugChance = adjustedRare)
            }
        }

        LevelParamSlider(
            label = "Шанс редких жуков",
            value = draft.rareBugChance,
            range = LevelValidator.RARE_CHANCE_RANGE,
            unit = "%"
        ) { v ->
            editorViewModel.updateDraft { current ->
                val adjustedFast = current.fastBugChance.coerceAtMost(100 - v)
                current.copy(fastBugChance = adjustedFast, rareBugChance = v)
            }
        }

        Text(
            text = "Обычные: ${difficulty.commonChance}% | Быстрые: ${difficulty.fastChance}% | Редкие: ${difficulty.rareChance}%",
            fontSize = 13.sp,
            color = Color.DarkGray
        )

        Spacer(Modifier.height(14.dp))

        // Живой предварительный просмотр
        Text(
            text = "Предварительный просмотр (${difficulty.label}, ${difficulty.index}/10)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        LevelLivePreviewBox(draft = draft)

        if (editorViewModel.validationErrors.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            editorViewModel.validationErrors.forEach { error ->
                Text("• $error", color = Color(0xFFC62828), fontSize = 14.sp)
            }
        }

        editorViewModel.statusMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(msg, color = Color(0xFF2E7D32), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { editorViewModel.saveCurrentDraft() },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (editorViewModel.isEditingExisting) "Обновить" else "Сохранить")
            }
            Button(
                onClick = {
                    editorViewModel.buildValidLevelOrNull()?.let { validLevel ->
                        onPlayLevel(validLevel)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Запустить")
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(14.dp))

        Text("Сохраненные уровни (${editorViewModel.levels.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (editorViewModel.levels.isEmpty()) {
            Text("Пока нет сохраненных уровней. Настройте параметры выше и нажмите «Сохранить».", color = Color.Gray)
        } else {
            editorViewModel.levels.forEach { level ->
                SavedLevelCard(
                    level = level,
                    onPlay = { onPlayLevel(level) },
                    onEdit = { editorViewModel.selectLevelForEdit(level) },
                    onDelete = { editorViewModel.deleteLevel(level) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LevelLivePreviewBox(draft: LevelDraft) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F7FA))
            .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
            .clipToBounds()
    ) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value
        var previewBugs by remember { mutableStateOf<List<PreviewBug>>(emptyList()) }

        LaunchedEffect(
            draft.speed,
            draft.maxBugs,
            draft.fastBugChance,
            draft.rareBugChance,
            widthDp,
            heightDp
        ) {
            previewBugs = LevelPreviewCalculator.generatePreviewBugs(draft, widthDp, heightDp)
            while (true) {
                delay(33L)
                previewBugs = LevelPreviewCalculator.stepPreviewBugs(previewBugs, widthDp, heightDp)
            }
        }

        previewBugs.forEach { bug ->
            Image(
                painter = painterResource(id = R.drawable.bug),
                contentDescription = bug.type.label,
                colorFilter = ColorFilter.tint(bug.type.tint),
                modifier = Modifier
                    .absoluteOffset(x = bug.x.dp, y = bug.y.dp)
                    .size(bug.sizeDp.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Раунд: ${draft.roundDuration}с • Бонус: каждые ${draft.bonusInterval}с",
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SavedLevelCard(
    level: LevelEntity,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val diff = remember(level) {
        LevelPreviewCalculator.calculateDifficulty(LevelDraft.fromEntity(level))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(level.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${diff.label} (${diff.index}/10)", fontSize = 13.sp, color = Color(0xFF1565C0))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Скорость: ${level.speed} | Жуков: ${level.maxBugs} | Время: ${level.roundDuration}с | Бонус: ${level.bonusInterval}с",
                fontSize = 13.sp,
                color = Color.DarkGray
            )
            Text(
                text = "Быстрые: ${level.fastBugChance}% | Редкие: ${level.rareBugChance}%",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlay) { Text("Играть") }
                OutlinedButton(onClick = onEdit) { Text("Изменить") }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDelete) {
                    Text("Удалить", color = Color(0xFFC62828))
                }
            }
        }
    }
}

@Composable
private fun LevelParamSlider(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Text("$label: $value $unit", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat()
    )
}