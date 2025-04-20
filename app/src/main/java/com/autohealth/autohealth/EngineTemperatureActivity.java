package com.autohealth.autohealth;

import android.os.Bundle;
import com.autohealth.autohealth.database.SensorData;

public class EngineTemperatureActivity extends BaseSensorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sensorName = "Температура двигателя";
        valueFormat = "Текущая температура: %d°C";
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getValueFromData(SensorData data) {
        return data.temperature;
    }

    @Override
    protected int getCurrentValue() {
        // Получаем текущее значение температуры из MainActivity
        return MainActivity.getInstance().getCoolantTemp();
    }
} 