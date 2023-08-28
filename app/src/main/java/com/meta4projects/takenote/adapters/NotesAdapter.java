package com.meta4projects.takenote.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.entities.Note;
import com.meta4projects.takenote.models.Subsection;
import com.meta4projects.takenote.others.Image;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteHolder> implements Filterable {

    private final List<Note> notes;
    private final List<Note> allNotes;
    final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Note> filteredList = new ArrayList<>();
            String searchText = constraint.toString().toLowerCase().trim();

            if (searchText.isEmpty()) {
                filteredList.addAll(allNotes);
            } else {
                for (Note note : allNotes) {
                    if (note.getTitle().toLowerCase().contains(searchText)) {
                        filteredList.add(note);
                    } else if (note.getFirstEdittextInfo().toLowerCase().contains(searchText)) {
                        filteredList.add(note);
                    } else {
                        for (String edittextInfo : note.getEditTextInfo()) {
                            if (edittextInfo.toLowerCase().contains(searchText)) {
                                filteredList.add(note);
                                break;
                            }
                        }

                        for (Subsection subsection : note.getSubsections()) {
                            if (subsection.getTitle().toLowerCase().contains(searchText)) {
                                filteredList.add(note);
                                break;
                            } else if (subsection.getBody().toLowerCase().contains(searchText)) {
                                filteredList.add(note);
                                break;
                            }
                        }
                    }
                }
            }

            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredList;

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notes.clear();
            notes.addAll((List<Note>) results.values);
            notifyDataSetChanged();
        }
    };
    private final Activity activity;
    private Listener listener;

    public NotesAdapter(List<Note> notes, Activity activity) {
        this.notes = notes;
        this.allNotes = new ArrayList<>(notes);
        this.activity = activity;
    }

    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_note, parent, false);
        return new NoteHolder(view, activity);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteHolder holder, final int position) {
        final Note note = notes.get(position);

        holder.setNote(note);
        holder.layout.setOnClickListener(v -> {
            if (listener != null) listener.onClick(position, note);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    public void refresh() {
        allNotes.clear();
        allNotes.addAll(notes);
    }

    public interface Listener {
        void onClick(int position, Note note);
    }

    static class NoteHolder extends RecyclerView.ViewHolder {

        final Activity activity;

        final ConstraintLayout layout;
        final TextView textViewTitle;
        final TextView textViewBody;
        final TextView textViewDate;
        final ImageView imageViewNoteImage;

        public NoteHolder(@NonNull View itemView, Activity activity) {
            super(itemView);

            this.activity = activity;

            layout = itemView.findViewById(R.id.layout_note);
            textViewTitle = itemView.findViewById(R.id.textView_title);
            textViewBody = itemView.findViewById(R.id.textView_body);
            textViewDate = itemView.findViewById(R.id.textView_date);
            imageViewNoteImage = itemView.findViewById(R.id.imageViewNoteImage);
        }

        private void setNote(Note note) {
            textViewTitle.setText(note.getTitle());
            textViewBody.setText(note.getFirstEdittextInfo());
            textViewDate.setText(new SimpleDateFormat("dd, MMMM, yyyy", Locale.getDefault()).format(new Date(note.getDateTime())));

            if (note.getColor() > 0) layout.setBackgroundResource(note.getColor());

            Bitmap bitmap = Image.getScaledBitmap(note.getImagePath(), activity);
            if (bitmap != null) {
                Glide.with(activity).asBitmap().load(bitmap).into(imageViewNoteImage);
                imageViewNoteImage.setVisibility(View.VISIBLE);
            } else imageViewNoteImage.setVisibility(View.GONE);
        }
    }
}
