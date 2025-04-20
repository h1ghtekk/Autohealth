package com.autohealth.autohealth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.autohealth.autohealth.adapters.HistoryAdapter;
import com.autohealth.autohealth.database.AppDatabase;
import com.autohealth.autohealth.database.SensorData;
import java.util.Calendar;
import java.util.List;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private AppDatabase database;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);
        
        database = AppDatabase.getDatabase(this);
        
        loadData();
    }

    private void loadData() {
        // Получаем данные за последний час
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -1);
        long oneHourAgo = calendar.getTimeInMillis();

        disposables.add(database.sensorDataDao()
                .getDataSince(oneHourAgo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateData));
    }

    private void updateData(List<SensorData> data) {
        adapter.setData(data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
} 