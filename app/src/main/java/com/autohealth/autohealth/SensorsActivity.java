package com.autohealth.autohealth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.autohealth.autohealth.database.AppDatabase;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SensorsActivity extends AppCompatActivity {

    private Button historyButton;
    private Button clearDatabaseButton;
    private View fragmentContainer;
    private View mainContent;
    private AppDatabase database;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors);

        // Инициализация базы данных
        database = AppDatabase.getDatabase(this);

        historyButton = findViewById(R.id.history_button);
        clearDatabaseButton = findViewById(R.id.clear_database_button);
        fragmentContainer = findViewById(R.id.fragment_container);
        mainContent = findViewById(R.id.buttons_layout);

        // Кнопка для перехода к датчику скорости автомобиля
        Button vehiclespeedButton = findViewById(R.id.vehicle_speed);
        vehiclespeedButton.setOnClickListener(v -> {
            Intent intent = new Intent(SensorsActivity.this, VehicleSpeedActivity.class);
            startActivity(intent);
        });

        // Кнопка для перехода к датчику оборотов двигателя
        Button enginerpmButton = findViewById(R.id.engine_rpm);
        enginerpmButton.setOnClickListener(v -> {
            Intent intent = new Intent(SensorsActivity.this, EngineRpmActivity.class);
            startActivity(intent);
        });

        // Кнопка для перехода к датчику температуры охлаждающей жидкости
        Button temperatureSensorButton = findViewById(R.id.engine_temperature);
        temperatureSensorButton.setOnClickListener(v -> {
            Intent intent = new Intent(SensorsActivity.this, TemperatureSensorActivity.class);
            startActivity(intent);
        });

        // Кнопка для перехода к датчику массового расхода воздуха
        Button massairflowButton = findViewById(R.id.mass_airflow);
        massairflowButton.setOnClickListener(v -> {
            Intent intent = new Intent(SensorsActivity.this, MassAirflowActivity.class);
            startActivity(intent);
        });

        // Кнопка для перехода к датчику положения дроссельной заслонки
        Button throttleButton = findViewById(R.id.throttle_position);
        throttleButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ThrottlePositionActivity.class);
            startActivity(intent);
        });

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });

        clearDatabaseButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Очистка базы данных")
                .setMessage("Вы уверены, что хотите очистить всю историю измерений?")
                .setPositiveButton("Да", (dialog, which) -> {
                    database.sensorDataDao().deleteAll()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            count -> Toast.makeText(this, "База данных очищена", Toast.LENGTH_SHORT).show(),
                            throwable -> Toast.makeText(this, "Ошибка при очистке базы данных", Toast.LENGTH_SHORT).show()
                        );
                })
                .setNegativeButton("Нет", null)
                .show();
        });
    }
}
