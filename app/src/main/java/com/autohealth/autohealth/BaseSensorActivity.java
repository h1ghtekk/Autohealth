package com.autohealth.autohealth;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.autohealth.autohealth.database.AppDatabase;
import com.autohealth.autohealth.database.SensorData;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.Observable;
import java.util.concurrent.TimeUnit;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

public abstract class BaseSensorActivity extends AppCompatActivity {
    protected LineChart chart;
    protected TextView currentValue;
    protected AppDatabase database;
    protected CompositeDisposable disposables = new CompositeDisposable();
    protected String sensorName;
    protected String valueFormat;
    protected float idealValue;
    private static final long UPDATE_INTERVAL = 1000; // 1 секунда
    private List<Entry> lastEntries = new ArrayList<>();
    private int lastSavedValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_base);

        chart = findViewById(R.id.chart);
        currentValue = findViewById(R.id.current_value);
        database = AppDatabase.getDatabase(this);

        setupChart();
        startDataUpdates();
    }

    private void setupChart() {
        chart = findViewById(R.id.chart);
        
        // Настройка внешнего вида графика
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setExtraBottomOffset(10f);
        
        // Настройка легенды
        Legend legend = chart.getLegend();
        legend.setEnabled(false);
        
        // Настройка осей
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAxisLineColor(Color.WHITE);
        xAxis.setAxisLineWidth(4f);
        xAxis.setTextSize(15f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"1", "2", "3", "4", "5"}));
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisLineColor(Color.WHITE);
        leftAxis.setAxisLineWidth(4f);
        leftAxis.setTextSize(15f);
        leftAxis.setAxisMinimum(0f);
        
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Настройка данных
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(16f);
        
        LineDataSet set = new LineDataSet(null, "Значения");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        set.setDrawFilled(true);
        set.setDrawCircles(true);
        set.setLineWidth(3f);
        set.setCircleRadius(6f);
        set.setCircleColor(Color.WHITE);
        set.setColor(Color.WHITE);
        set.setFillColor(Color.WHITE);
        set.setFillAlpha(50);
        set.setDrawHorizontalHighlightIndicator(false);
        set.setDrawVerticalHighlightIndicator(false);
        
        data.addDataSet(set);
        chart.setData(data);
    }

    protected void startDataUpdates() {
        // Загружаем начальные данные
        loadData();

        // Запускаем периодическое обновление
        disposables.add(
            Observable.interval(UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    tick -> updateCurrentValue(),
                    throwable -> Toast.makeText(this, "Ошибка обновления: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show()
                )
        );
    }

    protected void updateCurrentValue() {
        int currentValue = getCurrentValue();
        this.currentValue.setText(String.format(valueFormat, currentValue));
        
        // Проверяем, изменилось ли значение
        if (currentValue != lastSavedValue) {
            // Сдвигаем все значения вправо
            for (int i = lastEntries.size() - 1; i > 0; i--) {
                lastEntries.get(i).setY(lastEntries.get(i - 1).getY());
            }
            
            // Добавляем новое значение в начало
            if (lastEntries.size() > 0) {
                lastEntries.get(0).setY(currentValue);
            }
            
            // Обновляем график
            if (chart.getData() != null) {
                chart.notifyDataSetChanged();
                chart.invalidate();
            }
            
            lastSavedValue = currentValue;
        }
    }

    protected void loadData() {
        disposables.add(database.sensorDataDao()
                .getLatestData(5)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    this::updateChart,
                    throwable -> {
                        Toast.makeText(this, "Ошибка загрузки данных: " + throwable.getMessage(), 
                            Toast.LENGTH_LONG).show();
                        throwable.printStackTrace();
                    }
                ));
    }

    protected void updateChart(List<SensorData> data) {
        lastEntries.clear();
        
        // Получаем текущее значение
        int currentValue = getCurrentValue();
        lastSavedValue = currentValue;
        
        // Создаем 5 точек (даже если данных меньше)
        for (int i = 0; i < 5; i++) {
            float value;
            if (i < data.size()) {
                // Берем значения в обратном порядке, чтобы новое было справа
                value = (float) getValueFromData(data.get(data.size() - 1 - i));
            } else {
                value = currentValue;
            }
            lastEntries.add(new Entry(i, value));
        }

        // Создаем набор данных для текущих значений
        LineDataSet dataSet = new LineDataSet(lastEntries, sensorName);
        dataSet.setColor(getResources().getColor(R.color.green));
        //dataSet.setValueTextColor(getResources().getColor(R.color.white));
        //dataSet.setValueTextSize(14f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(getResources().getColor(R.color.Background));
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(4f);
        dataSet.setDrawValues(true);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        // Создаем набор данных для идеального значения
        List<Entry> idealEntries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            idealEntries.add(new Entry(i, idealValue));
        }
        LineDataSet idealDataSet = new LineDataSet(idealEntries, "Идеальное значение");
        idealDataSet.setColor(getResources().getColor(R.color.red));
        idealDataSet.setDrawCircles(false);
        idealDataSet.setLineWidth(4f);
        idealDataSet.setDrawValues(false);
        idealDataSet.setMode(LineDataSet.Mode.LINEAR);
        idealDataSet.enableDashedLine(10f, 10f, 0f);

        LineData lineData = new LineData(dataSet, idealDataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    protected abstract int getValueFromData(SensorData data);

    protected abstract int getCurrentValue();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
} 