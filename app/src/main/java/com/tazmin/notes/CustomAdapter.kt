package com.tazmin.notes

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore


// Класс адаптера, который принимает контекст и список данных (MutableList<CustomModel>)
class CustomAdapter(private val context: Context,
                    private var dataList: MutableList<CustomModel>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    // этот метод создается для создания нового ViewHolder, который используется для отображения элементов списка
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // инициализация макета элемента списка (list_item_layout) и его присоединение к ViewHolder
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_layout, parent, false)
        return ViewHolder(view)
    }

    // этот метод связывает данные из списка с элементами интерфейса (ViewHolder)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // получаем текущий элемент данных из списка
        val currentItem = dataList[position]
        // устанавливаем данные в соответствующие TextView внутри ViewHolder
        holder.textViewName.text = currentItem.name

        // форматирование даты
        val timestamp = currentItem.date as Timestamp
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(timestamp.toDate()) // Конвертируем Timestamp в Date
        holder.textViewDate.text = formattedDate

        val maxLength = 100 // максимальное количество символов в главном меню
        val description = if (currentItem.desctext.length > maxLength) {
            currentItem.desctext.substring(0, maxLength) + "..."
        } else {
            currentItem.desctext
        }
        holder.textViewDescription.text = description

        // обработчик клика на элемент списка
        holder.itemView.setOnClickListener {
            val intent = Intent(context, EditNoteActivity::class.java)
            intent.putExtra("noteId", currentItem.id)
            context.startActivity(intent)
        }
        holder.deleteButton.setOnClickListener {
            // удаление заметки из Firestore и обновление RecyclerView
            showDeleteConfirmationDialog(currentItem.id, position)
        }
    }

    // этот метод возвращает количество элементов в списке
    override fun getItemCount(): Int = dataList.size

    // класс ViewHolder, который хранит ссылки на элементы интерфейса для каждого элемента списка
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)
        val textViewName: TextView = itemView.findViewById(R.id.name)
        val textViewDescription: TextView = itemView.findViewById(R.id.desk)
        val textViewDate: TextView = itemView.findViewById(R.id.date)
    }

    private fun removeItem(noteId: String, position: Int) {
        val db = FirebaseFirestore.getInstance()
        db.collection("notes").document(noteId)
            .delete()
            .addOnSuccessListener {
                // удаляем элемент из списка и уведомляем адаптер
                dataList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, dataList.size)
                Toast.makeText(context, "Заметка удалена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка удаления: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteConfirmationDialog(noteId: String, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Удалить заметку")
        builder.setMessage("Вы уверены, что хотите удалить эту заметку?")

        // кнопка "Да"
        builder.setPositiveButton("Да") { dialog, _ ->
            removeItem(noteId, position)
            dialog.dismiss()
        }

        // кнопка "Нет"
        builder.setNegativeButton("Нет") { dialog, _ ->
            dialog.dismiss()
        }

        // показ диалога
        builder.create().show()
    }

    fun updateData(newList: MutableList<CustomModel>) {
        dataList = newList
        notifyDataSetChanged()
    }
}