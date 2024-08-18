package com.tazmin.notes

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddNote : AppCompatActivity() {

    // инициализация Firestore, которая будет использоваться для сохранения данных
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включение поддержки Edge-to-Edge интерфейса
        setContentView(R.layout.activity_add_note) // Привязка к макету активности

        applyWindowInsets() // Применение отступов для системных баров

        // инициализация кнопки "Назад" и установка обработчика нажатия
        val backButton: ImageButton = findViewById(R.id.add_back_button)
        backButton.setOnClickListener { // закрытие активности при нажатии
            finish()
        }

        // инициализация полей для ввода текста и кнопки "Сохранить"
        val editText: EditText = findViewById(R.id.edit_maintext)
        val editTextDesc: EditText = findViewById(R.id.edit_desctext)
        val saveButton: TextView = findViewById(R.id.save)

        // настройка поведения для перехода между полями ввода текста
        setupEditorAction(editText, editTextDesc)

        // установка обработчика нажатия на кнопку "Сохранить"
        saveButton.setOnClickListener { saveNote(editText, editTextDesc) }
    }

    // метод для применения отступов системных баров (статус бара и навигационной панели)
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // установка отступов для элемента, чтобы избежать перекрытия системными барами
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // настройка перехода фокуса между полями ввода при нажатии клавиши "Enter"
    private fun setupEditorAction(editText: EditText, editTextDesc: EditText) {
        editText.setOnEditorActionListener { _, actionId, event ->
            // проверка на нажатие клавиши "Enter" или "Next"
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.repeatCount == 0)) {
                editTextDesc.requestFocus() // перевод фокуса на следующее поле ввода
                true
            } else {
                false
            }
        }
    }

    // метод для сохранения заметки в Firestore
    private fun saveNote(editText: EditText, editTextDesc: EditText) {
        // проверка на заполненность всех полей
        if (editText.text.isNotEmpty() && editTextDesc.text.isNotEmpty()) {

            val currentDate = Timestamp.now()
            // создание объекта заметки для сохранения
            val note = hashMapOf(
                "date" to currentDate,
                "desctext" to editTextDesc.text.toString(),
                "name" to editText.text.toString()
            )

            // использование корутин для выполнения асинхронной операции сохранения
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // добавление заметки в коллекцию "notes" в Firestore
                    db.collection("notes").add(note).await()
                    // переключение на главный поток для работы с UI
                    withContext(Dispatchers.Main) {
                        // показ тоста и переход на MainActivity после успешного сохранения
                        Toast.makeText(this@AddNote, "Заметка сохранена!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } catch (e: Exception) {
                    // обработка ошибок при сохранении
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddNote, "Ошибка при сохранении заметки!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            // если не все поля заполнены, показываем сообщение пользователю
            Toast.makeText(this, "Заполнены не все поля!", Toast.LENGTH_LONG).show()
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}