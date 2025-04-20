package com.autohealth.autohealth;

import android.os.Bundle;
import com.autohealth.autohealth.database.SensorData;

public class MassAirflowActivity extends BaseSensorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sensorName = "Массовый расход воздуха";
        valueFormat = "Текущий расход: %d кг/ч";
        idealValue = 9f; // Идеальное значение 9 кг/ч
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getValueFromData(SensorData data) {
        return data.maf;
    }

    @Override
    protected int getCurrentValue() {
        return MainActivity.getInstance().getMaf();
    }
}
