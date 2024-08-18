package com.tazmin.notes

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        val db = FirebaseFirestore.getInstance()
        // Инициализация UI-элементов
        val editTextName: EditText = findViewById(R.id.edit_maintext)
        val editTextDesc: EditText = findViewById(R.id.edit_desctext)
        val saveTextView: TextView = findViewById(R.id.save)
        val backButton: ImageButton = findViewById(R.id.add_back_button)
        backButton.setOnClickListener {
            finish()
        }

        // Получение noteId из интента
        val noteId = intent.getStringExtra("noteId")

        // Если noteId не null, загружаем данные заметки
        if (noteId != null) {
            db.collection("notes").document(noteId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        editTextName.setText(document.getString("name"))
                        editTextDesc.setText(document.getString("desctext"))
                    } else {
                        Toast.makeText(this, "Заметка не найдена", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "Ошибка: noteId отсутствует", Toast.LENGTH_LONG).show()
            finish()
        }

        saveTextView.setOnClickListener {
            // Сохранение изменений в Firestore
            val updatedNote = mapOf(
                "name" to editTextName.text.toString(),
                "desctext" to editTextDesc.text.toString()
            )
            if (noteId != null) {
                db.collection("notes").document(noteId)
                    .update(updatedNote)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Заметка обновлена", Toast.LENGTH_SHORT).show()
                        finish() // Закрыть activity после сохранения
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка при обновлении: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}