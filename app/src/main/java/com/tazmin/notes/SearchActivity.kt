package com.tazmin.notes

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var adapter: CustomAdapter
    private lateinit var dataList: MutableList<CustomModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val backButton: ImageButton = findViewById(R.id.back_button)
        val clearButton: ImageButton = findViewById(R.id.clear_button)
        val searchEditText: EditText = findViewById(R.id.search_input)
        val recyclerView: RecyclerView = findViewById(R.id.search_results)

        // Инициализация списка данных и адаптера
        dataList = mutableListOf()
        adapter = CustomAdapter(this, dataList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Открываем клавиатуру сразу после открытия активности
        searchEditText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)

        // Возврат на предыдущую активность
        backButton.setOnClickListener { finish() }

        // Очистка введенного текста
        clearButton.setOnClickListener { searchEditText.text.clear() }

        // Загрузка данных из Firestore
        loadDataFromFirestore()

        // Слушатель для фильтрации заметок
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadDataFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("notes")
            .get()
            .addOnSuccessListener { result ->
                dataList.clear()
                for (document in result) {
                    val note = CustomModel(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        desctext = document.getString("desctext") ?: "",
                        date = document.getTimestamp("date") ?: Timestamp.now()
                    )
                    dataList.add(note)
                }
                adapter.notifyDataSetChanged() // Обновляем адаптер после загрузки данных
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_LONG).show()
            }
    }

    private fun filterNotes(query: String) {
        val searchQuery = query.toLowerCase(Locale.getDefault())
        val filteredList = if (searchQuery.isEmpty()) {
            dataList
        } else {
            dataList.filter {
                it.name.toLowerCase(Locale.getDefault()).contains(searchQuery) ||
                        it.desctext.toLowerCase(Locale.getDefault()).contains(searchQuery)
            }.toMutableList()
        }
        // Обновляем данные в адаптере
        adapter.updateData(filteredList)
    }
}
