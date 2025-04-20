package com.autohealth.autohealth.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.autohealth.autohealth.R;
import com.autohealth.autohealth.adapters.HistoryAdapter;
import com.autohealth.autohealth.database.AppDatabase;
import com.autohealth.autohealth.database.SensorData;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HistoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private AppDatabase database;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);
        
        database = AppDatabase.getDatabase(requireContext());
        
        loadData();
        
        return view;
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
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
} 