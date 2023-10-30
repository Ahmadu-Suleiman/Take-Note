package com.meta4projects.takenote.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.meta4projects.takenote.R
import com.meta4projects.takenote.activities.CategoryActivity
import com.meta4projects.takenote.activities.MainActivity
import com.meta4projects.takenote.adapters.CategoriesAdapter
import com.meta4projects.takenote.database.NoteDatabase.Companion.getINSTANCE
import com.meta4projects.takenote.database.entities.Category
import com.meta4projects.takenote.others.Utils.categoryNames
import com.meta4projects.takenote.others.Utils.getDialogView
import com.meta4projects.takenote.others.Utils.loadNativeAd
import com.meta4projects.takenote.others.Utils.showToast
import com.meta4projects.takenote.others.Utils.updateAllCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesFragment : Fragment() {
    private val categoryList: MutableList<Category> = ArrayList()
    private lateinit var recyclerViewCategories: RecyclerView
    private var textViewEmptyCategory: TextView? = null
    private var categoryAdapter: CategoriesAdapter? = null
    private var categoryLauncher: ActivityResultLauncher<Intent>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult -> if (result.resultCode == Activity.RESULT_OK) updateCategories() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)
        val mainActivity = requireActivity() as MainActivity
        val hamburger = view.findViewById<ImageView>(R.id.hamburger)
        val search = view.findViewById<ImageView>(R.id.search)
        hamburger.setOnClickListener { mainActivity.hamburgerClick() }
        search.setOnClickListener { mainActivity.searchClick() }
        recyclerViewCategories = view.findViewById(R.id.recyclerViewCategories)
        textViewEmptyCategory = view.findViewById(R.id.text_empty_category)
        val templateView = view.findViewById<View>(R.id.fragment_categories_native_ad_notes).findViewById<TemplateView>(R.id.native_ad)
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_category_fragment_unit_id))
        categoryAdapter = CategoriesAdapter(categoryList, true)
        categoryAdapter!!.setListener { categoryName: String? ->
            val intent = Intent(activity, CategoryActivity::class.java)
            intent.putExtra(CATEGORY_NAME_EXTRA, categoryName)
            categoryLauncher!!.launch(intent)
        }
        recyclerViewCategories.adapter = categoryAdapter
        recyclerViewCategories.layoutManager = GridLayoutManager(activity, 2)
        val buttonAddCategory = view.findViewById<FloatingActionButton>(R.id.button_categories_add_category)
        buttonAddCategory.setOnClickListener { showAddCategoryDialog() }
        setCategories()
        return view
    }

    override fun onResume() {
        super.onResume()
        setCategories()
    }

    private fun setEmptyViewsCategory(categories: List<Category?>?) {
        if (categories!!.isEmpty()) textViewEmptyCategory!!.visibility = View.VISIBLE else textViewEmptyCategory!!.visibility = View.GONE
    }

    private fun setCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = getINSTANCE(requireContext().applicationContext).categoryDao().allCategories
            updateAllCategories(categories, requireContext())
            withContext(Dispatchers.Main) {
                requireActivity().runOnUiThread {
                    categoryList.clear()
                    categoryList.addAll(categories)
                    categoryAdapter!!.notifyDataSetChanged()
                    setEmptyViewsCategory(categories)
                }
            }
        }
    }

    private fun showAddCategoryDialog() {
        val view = LayoutInflater.from(activity).inflate(R.layout.layout_add_category, requireActivity().findViewById(R.id.layout_add_category_dialog), false)
        val dialogAddCategory = getDialogView(requireContext(), view)
        val editTextCategory = view.findViewById<EditText>(R.id.input_add_category)
        view.findViewById<View>(R.id.text_add_category).setOnClickListener {
            val categoryName = editTextCategory.text.toString().trim()
            var nameExists = false
            for (name in categoryNames) {
                if (categoryName.equals(name, ignoreCase = true)) {
                    nameExists = true
                    break
                }
            }
            if (nameExists) showToast("category name already exists!", requireActivity()) else if (categoryName.isEmpty()) showToast("category name cannot be empty!", requireActivity()) else {
                dialogAddCategory.dismiss()
                CoroutineScope(Dispatchers.IO).launch {
                    val category = Category(categoryName)
                    getINSTANCE(requireContext().applicationContext).categoryDao().insertCategory(category)
                    val categories = getINSTANCE(requireContext().applicationContext).categoryDao().allCategories
                    updateAllCategories(categories, requireContext())
                    withContext(Dispatchers.Main) {
                        val lastIndex = categoryList.size
                        categoryList.add(lastIndex, category)
                        categoryAdapter!!.notifyItemInserted(lastIndex)
                        recyclerViewCategories.smoothScrollToPosition(lastIndex)
                        setEmptyViewsCategory(categories)
                        showToast("new category added successfully!", requireActivity())
                    }
                }
            }
        }
        view.findViewById<View>(R.id.text_cancel_category).setOnClickListener { dialogAddCategory.dismiss() }
        dialogAddCategory.show()
    }

    private fun updateCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = getINSTANCE(requireContext().applicationContext).categoryDao().allCategories
            withContext(Dispatchers.Main) {
                categoryList.clear()
                categoryList.addAll(categories)
                categoryAdapter!!.notifyDataSetChanged()
                setEmptyViewsCategory(categories)
            }
        }
    }

    companion object {
        const val CATEGORY_NAME_EXTRA = "com.meta4projects.takenote.adapters.CATEGORY_NAME"
    }
}