package com.meta4projects.takenote.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.entities.Note;
import com.meta4projects.takenote.others.Image;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteHolder> {

    private final List<Note> notes;
    private final Activity activity;
    private Listener listener;

    public NotesAdapter(List<Note> notes, Activity activity) {
        this.notes = notes;
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
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(position, note);
                }
            }
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

        void setNote(Note note) {
            textViewTitle.setText(note.getTitle());
            textViewBody.setText(note.getFirstEdittextInfo());
            textViewDate.setText(new SimpleDateFormat("dd, MMMM, yyyy", Locale.getDefault()).format(new Date(note.getDateTime())));

            if (note.getColor() > 0) {
                layout.setBackgroundResource(note.getColor());
            }

            Bitmap bitmap = Image.getScaledBitmap(note.getImagePath(), activity);

            if (bitmap != null) {
                imageViewNoteImage.setImageBitmap(bitmap);
                imageViewNoteImage.setVisibility(View.VISIBLE);
            } else {
                imageViewNoteImage.setVisibility(View.GONE);
            }
        }
    }
}
