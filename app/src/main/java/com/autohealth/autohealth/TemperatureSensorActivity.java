package com.autohealth.autohealth;

import android.os.Bundle;
import com.autohealth.autohealth.database.SensorData;

public class TemperatureSensorActivity extends BaseSensorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sensorName = "Температура двигателя";
        valueFormat = "Текущая температура: %d°C";
        idealValue = 100f; // Идеальное значение 100°C
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getValueFromData(SensorData data) {
        return data.temperature;
    }

    @Override
    protected int getCurrentValue() {
        return MainActivity.getInstance().getCoolantTemp();
    }
}
