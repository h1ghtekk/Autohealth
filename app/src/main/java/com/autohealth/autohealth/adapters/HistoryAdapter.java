package com.autohealth.autohealth.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.autohealth.autohealth.R;
import com.autohealth.autohealth.database.SensorData;
import io.reactivex.schedulers.Schedulers;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private List<SensorData> dataList = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SensorData data = dataList.get(position);
        holder.tvTime.setText(dateFormat.format(data.timestamp));
        holder.tvTemperature.setText(String.format(Locale.getDefault(), "Температура: %d°C", data.temperature));
        holder.tvRpm.setText(String.format(Locale.getDefault(), "Обороты: %d", data.rpm));
        holder.tvSpeed.setText(String.format(Locale.getDefault(), "Скорость: %d км/ч", data.speed));
        holder.tvMaf.setText(String.format(Locale.getDefault(), "MAF: %d кг/с", data.maf));
        holder.tvThrottle.setText(String.format(Locale.getDefault(), "Дроссель: %d%%", data.throttlePosition));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void setData(List<SensorData> newData) {
        this.dataList = newData;
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvTemperature;
        TextView tvRpm;
        TextView tvSpeed;
        TextView tvMaf;
        TextView tvThrottle;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvTemperature = itemView.findViewById(R.id.tv_temperature);
            tvRpm = itemView.findViewById(R.id.tv_rpm);
            tvSpeed = itemView.findViewById(R.id.tv_speed);
            tvMaf = itemView.findViewById(R.id.tv_maf);
            tvThrottle = itemView.findViewById(R.id.tv_throttle);
        }
    }
} 