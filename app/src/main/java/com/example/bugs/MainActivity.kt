package com.example.bugs

import android.os.Bundle
import android.webkit.WebView
import android.widget.CalendarView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bugs.ui.theme.BugsTheme
import java.util.Calendar

data class PlayerProfile(
    val fullName: String,
    val gender: String,
    val course: String,
    val difficulty: Int,
    val birthDate: String,
    val zodiacSign: String
)

data class Author(
    val name: String,
    val photoRes: Int
)

private val courses = listOf("1 курс", "2 курс", "3 курс", "4 курс", "Магистратура")

private val authors = listOf(
    Author("Ринчиндоржиев Е. Б.", R.drawable.ic_author),
    Author("Чудаков М.А", R.drawable.ic_author)
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BugsTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Регистрация", "Правила", "Авторы", "Настройки")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTab) {
            0 -> RegistrationTab()
            1 -> RulesTab()
            2 -> AuthorsTab()
            3 -> SettingsTab()
        }
    }
}

@Composable
fun RegistrationTab() {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var isMale by remember { mutableStateOf(true) }
    var course by remember { mutableStateOf(courses.first()) }
    var courseExpanded by remember { mutableStateOf(false) }
    var difficulty by remember { mutableIntStateOf(1) }

    val calendar = Calendar.getInstance()
    var day by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var month by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var year by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    var zodiacRes by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("ФИО игрока:", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Введите ФИО") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Text("Пол:", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isMale, onClick = { isMale = true })
                Text("Мужской")
            }
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !isMale, onClick = { isMale = false })
                Text("Женский")
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Курс:", fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { courseExpanded = true }) {
                Text(course)
            }
            DropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                courses.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            course = item
                            courseExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Уровень сложности: $difficulty", fontWeight = FontWeight.Bold)
        Slider(
            value = difficulty.toFloat(),
            onValueChange = { difficulty = it.toInt() },
            valueRange = 1f..10f,
            steps = 8
        )

        Spacer(Modifier.height(12.dp))
        Text("Дата рождения:", fontWeight = FontWeight.Bold)
        AndroidView(
            factory = { ctx ->
                CalendarView(ctx).apply {
                    val initial = Calendar.getInstance().apply { set(year, month, day) }
                    setDate(initial.timeInMillis)
                    setOnDateChangeListener { _, y, m, d ->
                        year = y
                        month = m
                        day = d
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (fullName.isBlank()) {
                    Toast.makeText(context, "Пожалуйста, введите ФИО", Toast.LENGTH_SHORT).show()
                } else {
                    val gender = if (isMale) "Мужской" else "Женский"
                    val birthDateStr = String.format("%02d.%02d.%d", day, month + 1, year)
                    val (zodiacName, zodiacImageRes) = getZodiacInfo(day, month)
                    zodiacRes = zodiacImageRes

                    val player = PlayerProfile(
                        fullName = fullName,
                        gender = gender,
                        course = course,
                        difficulty = difficulty,
                        birthDate = birthDateStr,
                        zodiacSign = zodiacName
                    )

                    result = """
                        Игрок зарегистрирован:
                        • ФИО: ${player.fullName}
                        • Пол: ${player.gender}
                        • Курс: ${player.course}
                        • Сложность: ${player.difficulty}
                        • Дата рождения: ${player.birthDate}
                        • Знак зодиака: ${player.zodiacSign}
                    """.trimIndent()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Зарегистрировать игрока")
        }

        Spacer(Modifier.height(16.dp))
        if (zodiacRes != 0) {
            Image(
                painter = painterResource(zodiacRes),
                contentDescription = "Знак зодиака",
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(12.dp))
        if (result.isNotEmpty()) {
            Text(result, fontSize = 16.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
fun RulesTab() {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                loadUrl("file:///android_asset/rules.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun AuthorsTab() {
    LazyColumn(Modifier.fillMaxSize()) {
        items(authors) { author ->
            AuthorRow(author)
        }
    }
}

@Composable
fun AuthorRow(author: Author) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(author.photoRes),
            contentDescription = "Фото",
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(author.name, fontSize = 18.sp)
    }
}

@Composable
fun SettingsTab() {
    var speed by remember { mutableIntStateOf(5) }
    var maxCockroaches by remember { mutableIntStateOf(20) }
    var bonusInterval by remember { mutableIntStateOf(10) }
    var roundDuration by remember { mutableIntStateOf(60) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingSlider("Скорость игры", speed, 1..10, "ед.") { speed = it }
        SettingSlider("Максимальное количество тараканов на экране", maxCockroaches, 1..50, "шт.") { maxCockroaches = it }
        SettingSlider("Интервал появления бонусов", bonusInterval, 1..60, "сек") { bonusInterval = it }
        SettingSlider("Длительность раунда", roundDuration, 10..300, "сек") { roundDuration = it }
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Text("$label: $value $unit", fontWeight = FontWeight.Bold)
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat()
    )
    Spacer(Modifier.height(8.dp))
}

private fun getZodiacInfo(day: Int, month: Int): Pair<String, Int> {
    return when (month) {
        0 -> if (day < 20) "Козерог" to R.drawable.capricorn else "Водолей" to R.drawable.aquarius
        1 -> if (day < 19) "Водолей" to R.drawable.aquarius else "Рыбы" to R.drawable.pisces
        2 -> if (day < 21) "Рыбы" to R.drawable.pisces else "Овен" to R.drawable.aries
        3 -> if (day < 20) "Овен" to R.drawable.aries else "Телец" to R.drawable.taurus
        4 -> if (day < 21) "Телец" to R.drawable.taurus else "Близнецы" to R.drawable.gemini
        5 -> if (day < 21) "Близнецы" to R.drawable.gemini else "Рак" to R.drawable.cancer
        6 -> if (day < 23) "Рак" to R.drawable.cancer else "Лев" to R.drawable.leo
        7 -> if (day < 23) "Лев" to R.drawable.leo else "Дева" to R.drawable.virgo
        8 -> if (day < 23) "Дева" to R.drawable.virgo else "Весы" to R.drawable.libra
        9 -> if (day < 23) "Весы" to R.drawable.libra else "Скорпион" to R.drawable.scorpio
        10 -> if (day < 22) "Скорпион" to R.drawable.scorpio else "Стрелец" to R.drawable.sagittarius
        11 -> if (day < 22) "Стрелец" to R.drawable.sagittarius else "Козерог" to R.drawable.capricorn
        else -> "Неизвестно" to 0
    }
}
