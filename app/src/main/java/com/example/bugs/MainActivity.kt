package com.example.bugs // Ваше имя пакета

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.Spinner
import android.widget.CalendarView
import java.util.Calendar


// Структура данных для хранения информации об игроке
data class PlayerProfile(
    val fullName: String,
    val gender: String,
    val course: String,
    val difficulty: Int,
    val birthDate: String,
    val zodiacSign: String
)

class MainActivity : androidx.activity.ComponentActivity() {

    private var selectedDay = 1
    private var selectedMonth = 0 // В CalendarView месяцы 0..11
    private var selectedYear = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов UI
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val rbMale = findViewById<RadioButton>(R.id.rbMale)
        val spinnerCourse = findViewById<Spinner>(R.id.spinnerCourse)
        val seekBarDifficulty = findViewById<SeekBar>(R.id.seekBarDifficulty)
        val tvDifficultyLabel = findViewById<TextView>(R.id.tvDifficultyLabel)
        val calendarBirthDate = findViewById<CalendarView>(R.id.calendarBirthDate)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val ivZodiac = findViewById<ImageView>(R.id.ivZodiac)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        // Устанавливаем текущую дату календаря по умолчанию
        val calendar = Calendar.getInstance()
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectedMonth = calendar.get(Calendar.MONTH)
        selectedYear = calendar.get(Calendar.YEAR)

        // Слушатель изменения SeekBar (Сложность)
        seekBarDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDifficultyLabel.text = "Уровень сложности: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Слушатель выбора даты в CalendarView
        calendarBirthDate.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
        }

        // Обработка клика по кнопке "Зарегистрировать"
        btnSave.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            if (fullName.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите ФИО", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = if (rbMale.isChecked) "Мужской" else "Женский"
            val course = spinnerCourse.selectedItem.toString()
            val difficulty = seekBarDifficulty.progress
            val birthDateStr = String.format("%02d.%02d.%d", selectedDay, selectedMonth + 1, selectedYear)

            // Расчет знака зодиака
            val (zodiacName, zodiacImageRes) = getZodiacInfo(selectedDay, selectedMonth)

            // Сохранение в структуру данных
            val player = PlayerProfile(
                fullName = fullName,
                gender = gender,
                course = course,
                difficulty = difficulty,
                birthDate = birthDateStr,
                zodiacSign = zodiacName
            )

            // Вывод знака зодиака (картинки) и результат в TextView
            if (zodiacImageRes != 0) {
                ivZodiac.setImageResource(zodiacImageRes)
            }

            tvResult.text = """
                Игрок зарегистрирован:
                • ФИО: ${player.fullName}
                • Пол: ${player.gender}
                • Курс: ${player.course}
                • Сложность: ${player.difficulty}
                • Дата рождения: ${player.birthDate}
                • Знак зодиака: ${player.zodiacSign}
            """.trimIndent()
        }
    }

    // Вспомогательная функция определения знака зодиака и соответствующего ресурса
    private fun getZodiacInfo(day: Int, month: Int): Pair<String, Int> {
        // month: 0 = Январь, 1 = Февраль, ..., 11 = Декабрь
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
}