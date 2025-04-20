package com.autohealth.autohealth;

import android.os.Bundle;
import com.autohealth.autohealth.database.SensorData;

public class EngineRpmActivity extends BaseSensorActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sensorName = "Обороты двигателя";
        valueFormat = "Текущие обороты: %d об/мин";
        idealValue = 900f; // Идеальное значение 900 об/мин
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getValueFromData(SensorData data) {
        return data.rpm;
    }

    @Override
    protected int getCurrentValue() {
        // Получаем текущее значение оборотов из MainActivity
        return MainActivity.getInstance().getRpmVal();
    }
}
