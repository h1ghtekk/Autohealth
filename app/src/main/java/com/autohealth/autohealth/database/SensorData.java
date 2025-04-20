package com.autohealth.autohealth.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

@Entity(tableName = "sensor_data")
@TypeConverters({DateConverter.class})
public class SensorData {
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public Date timestamp;
    public int temperature;
    public int rpm;
    public int speed;
    public int maf;
    public int throttlePosition;
    
    public SensorData(Date timestamp, int temperature, int rpm, int speed, int maf, int throttlePosition) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.rpm = rpm;
        this.speed = speed;
        this.maf = maf;
        this.throttlePosition = throttlePosition;
    }
} 