package com.autohealth.autohealth.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.Date;
import java.util.List;

@Dao
public interface SensorDataDao {
    @Insert
    Completable insert(SensorData sensorData);

    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT :limit")
    Flowable<List<SensorData>> getLatestData(int limit);

    @Query("SELECT * FROM sensor_data WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    Flowable<List<SensorData>> getDataByTimeRange(long startTime, long endTime);

    @Query("DELETE FROM sensor_data WHERE timestamp < :timestamp")
    Completable deleteOlderThan(long timestamp);

    @Query("SELECT * FROM sensor_data WHERE timestamp >= :since ORDER BY timestamp DESC")
    Flowable<List<SensorData>> getDataSince(long since);

    @Query("DELETE FROM sensor_data")
    Single<Integer> deleteAll();

    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC")
    Single<List<SensorData>> getAll();

    @Query("SELECT * FROM sensor_data WHERE timestamp >= :since ORDER BY timestamp DESC")
    Single<List<SensorData>> getDataSince(Date since);
} 