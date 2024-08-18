package com.tazmin.notes

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    // создаем CoroutineScope с главным (Main) диспетчером для обновления UI
    //CoroutineScope: Интерфейс, который определяет область для запуска корутин.
    //Dispatchers.Main: Обозначает, что корутины будут запущены в главном потоке, который используется
    //для обновления пользовательского интерфейса. Кроме Dispatchers.Main, есть другие диспетчеры, такие как Dispatchers.IO для ввода/вывода и Dispatchers.Default для CPU-интенсивных задач.
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_note_main)

        val addButton: ImageButton = findViewById(R.id.add_button)
        addButton.setOnClickListener {
            val intent = Intent(this, AddNote::class.java)
            startActivity(intent)
        }
        // инициализация RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CustomAdapter(this, mutableListOf())

        val searchButton: ImageButton = findViewById(R.id.search_button)
        searchButton.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        // запускаем Coroutine для настройки слушателя базы данных и обновления данных
        scope.launch {
            setupDBListener(recyclerView)
            updateDB(recyclerView)
        }
    }
    // настраиваем слушатель изменений в базе данных Firebase
    private suspend fun setupDBListener(recyclerView: RecyclerView) = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        db.collection("notes").addSnapshotListener { snapshots, e ->
            if (e != null) {
                // Обработка ошибки
                return@addSnapshotListener
            }
            if (snapshots != null) {
                // Обновление данных
                scope.launch { updateDB(recyclerView) }
            }
        }
    }

    // обновляем данные из базы данных и настраиваем RecyclerView
    private suspend fun updateDB(recyclerView: RecyclerView) = withContext(Dispatchers.IO) {
        //  getInstance() - Статический метод, который возвращает экземпляр базы данных Firestore,
        //  связанный с текущим проектом Firebase.
        //  этот экземпляр используется для выполнения операций чтения и записи данных.
        val db = FirebaseFirestore.getInstance()
        try {
            // получаем данные из коллекции "reports"
            val snapshot = db.collection("notes").get().await()
            val noteList = mutableListOf<CustomModel>()

            // преобразуем документы в объекты CustomModel
            for (doc in snapshot) {
                val note = CustomModel(
                    doc.id,
                    doc.getString("name").toString(),
                    doc.getString("desctext").toString(),
                    doc.getTimestamp("date")!!
                )
                noteList.add(note)
            }

            // сортируем список по дате в порядке убывания
            noteList.sortByDescending { it.date }

            // обновляем UI в главном потоке
            withContext(Dispatchers.Main) {
                val noteAdapter = CustomAdapter(this@MainActivity, noteList)
                recyclerView.adapter = noteAdapter
            }
        } catch (e: Exception) {
            // обрабатываем ошибку и показываем сообщение пользователю
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Не работает БД, увы", Toast.LENGTH_LONG).show()
            }
        }
    }
}