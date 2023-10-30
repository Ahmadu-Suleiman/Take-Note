package com.meta4projects.takenote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.meta4projects.takenote.R
import com.meta4projects.takenote.adapters.CategoriesAdapter.CategoryHolder
import com.meta4projects.takenote.database.entities.Category

class CategoriesAdapter : RecyclerView.Adapter<CategoryHolder> {
    private val categories: List<Category>
    private var isBig = false
    private var listener: Listener? = null

    constructor(categories: List<Category>) {
        this.categories = categories
    }

    constructor(categories: MutableList<Category>, isBig: Boolean) {
        this.categories = categories
        this.isBig = isBig
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryHolder {
        val view: View = if (isBig) LayoutInflater.from(parent.context).inflate(R.layout.layout_category_detailed, parent, false) else LayoutInflater.from(parent.context).inflate(R.layout.layout_category, parent, false)
        return CategoryHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
        val category = categories[position]
        holder.setCategory(category)
        holder.layout.setOnClickListener { if (listener != null) listener!!.onClick(category.name) }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun interface Listener {
        fun onClick(categoryName: String?)
    }

    class CategoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layout: ConstraintLayout
        private val textViewCategoryName: TextView
        private val textViewCategoryNumber: TextView

        init {
            layout = itemView.findViewById(R.id.layout_category)
            textViewCategoryName = itemView.findViewById(R.id.textView_category_name)
            textViewCategoryNumber = itemView.findViewById(R.id.textView_category_num)
        }

        fun setCategory(category: Category) {
            textViewCategoryName.text = category.name
            textViewCategoryNumber.text = category.notesInCategory.size.toString()
        }
    }
}