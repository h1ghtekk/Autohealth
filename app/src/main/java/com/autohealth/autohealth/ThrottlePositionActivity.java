package com.autohealth.autohealth;

import android.os.Bundle;
import com.autohealth.autohealth.database.SensorData;

public class ThrottlePositionActivity extends BaseSensorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sensorName = "Положение дроссельной заслонки";
        valueFormat = "Текущее положение: %d%%";
        idealValue = 16f; // Идеальное значение 16%
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getValueFromData(SensorData data) {
        return data.throttlePosition;
    }

    @Override
    protected int getCurrentValue() {
        return MainActivity.getInstance().getThrottlePos();
    }
}
