package com.meta4projects.takenote.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.entities.Category;

import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryHolder> {

    private final List<Category> categories;
    boolean isBig;
    private Listener listener;

    public CategoriesAdapter(List<Category> categories) {
        this.categories = categories;
    }

    public CategoriesAdapter(List<Category> categories, boolean isBig) {
        this.categories = categories;
        this.isBig = isBig;
    }

    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (isBig)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_category_detailed, parent, false);
        else
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_category, parent, false);
        return new CategoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, final int position) {
        final Category category = categories.get(position);
        holder.setCategory(category);
        holder.layout.setOnClickListener(v -> {
            if (listener != null) listener.onClick(category.getName());
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public interface Listener {
        void onClick(String categoryName);
    }

    static class CategoryHolder extends RecyclerView.ViewHolder {

        final ConstraintLayout layout;
        final TextView textViewCategoryName;
        final TextView textViewCategoryNumber;

        public CategoryHolder(@NonNull View itemView) {
            super(itemView);

            layout = itemView.findViewById(R.id.layout_category);
            textViewCategoryName = itemView.findViewById(R.id.textView_category_name);
            textViewCategoryNumber = itemView.findViewById(R.id.textView_category_num);
        }

        void setCategory(Category category) {
            textViewCategoryName.setText(category.getName());
            textViewCategoryNumber.setText(String.valueOf(category.getNotesInCategory().size()));
        }
    }

}
