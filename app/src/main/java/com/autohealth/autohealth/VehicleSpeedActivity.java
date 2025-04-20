package com.autohealth.autohealth;

import android.os.Bundle;
import com.autohealth.autohealth.database.SensorData;

public class VehicleSpeedActivity extends BaseSensorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sensorName = "Скорость автомобиля";
        valueFormat = "Текущая скорость: %d км/ч";
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getValueFromData(SensorData data) {
        return data.speed;
    }

    @Override
    protected int getCurrentValue() {
        return MainActivity.getInstance().getSpeed();
    }
}
