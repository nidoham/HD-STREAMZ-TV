package com.nidoham.hdstreamztv.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nidoham.hdstreamztv.R;
import com.nidoham.hdstreamztv.model.VideoQuality;
import java.util.List;

public class QualityDialog {
    private final Dialog dialog;
    private final QualityAdapter adapter;
    private OnQualitySelectedListener listener;

    public interface OnQualitySelectedListener {
        void onQualitySelected(VideoQuality quality);
    }

    public QualityDialog(Context context, List<VideoQuality> qualities, VideoQuality currentQuality) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_quality);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        RecyclerView recyclerView = dialog.findViewById(R.id.qualityRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        
        adapter = new QualityAdapter(qualities, currentQuality, quality -> {
            if (listener != null) {
                listener.onQualitySelected(quality);
            }
            dialog.dismiss();
        });
        
        recyclerView.setAdapter(adapter);

        // Close dialog button
        dialog.findViewById(R.id.closeButton).setOnClickListener(v -> dialog.dismiss());
    }

    public void setOnQualitySelectedListener(OnQualitySelectedListener listener) {
        this.listener = listener;
    }

    public void show() {
        dialog.show();
    }

    private static class QualityAdapter extends RecyclerView.Adapter<QualityAdapter.QualityViewHolder> {
        private final List<VideoQuality> qualities;
        private final VideoQuality currentQuality;
        private final OnQualityClickListener listener;

        interface OnQualityClickListener {
            void onQualityClick(VideoQuality quality);
        }

        QualityAdapter(List<VideoQuality> qualities, VideoQuality currentQuality, OnQualityClickListener listener) {
            this.qualities = qualities;
            this.currentQuality = currentQuality;
            this.listener = listener;
        }

        @Override
        public QualityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quality, parent, false);
            return new QualityViewHolder(view);
        }

        @Override
        public void onBindViewHolder(QualityViewHolder holder, int position) {
            VideoQuality quality = qualities.get(position);
            holder.bind(quality, quality.getQuality().equals(currentQuality.getQuality()), listener);
        }

        @Override
        public int getItemCount() {
            return qualities.size();
        }

        static class QualityViewHolder extends RecyclerView.ViewHolder {
            private final TextView qualityText;
            private final View selectedIndicator;

            QualityViewHolder(View itemView) {
                super(itemView);
                qualityText = itemView.findViewById(R.id.qualityText);
                selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
            }

            void bind(VideoQuality quality, boolean isSelected, OnQualityClickListener listener) {
                qualityText.setText(quality.getQuality());
                selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
                itemView.setOnClickListener(v -> listener.onQualityClick(quality));
            }
        }
    }
}