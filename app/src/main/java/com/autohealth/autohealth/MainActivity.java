package com.autohealth.autohealth;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import com.autohealth.autohealth.fragments.HistoryFragment;
import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.autohealth.autohealth.database.AppDatabase;
import com.autohealth.autohealth.database.SensorData;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    //Тег для логирования
    private static final String TAG = "MainActivity";

    //Переменные используемые для кейса в Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name"; //Имя подключенного устройства
    public static final String TOAST = "toast"; //Всплывающее окно для покдлючения

    protected final static char[] dtcLetters = {'P', 'C', 'B', 'U'};
    protected final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static final String[] PIDS = {
            "01", "02", "03", "04", "05", "06", "07", "08",
            "09", "0A", "0B", "0C", "0D", "0E", "0F", "10",
            "11", "12", "13", "14", "15", "16", "17", "18",
            "19", "1A", "1B", "1C", "1D", "1E", "1F", "20"};

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    final List<String> commandslist = new ArrayList<String>();
    final List<Double> avgconsumption = new ArrayList<Double>();
    final List<String> troubleCodesArray = new ArrayList<String>();
    BluetoothDevice currentdevice;
    boolean commandmode = false, initialized = false, m_getPids = false, tryconnect = false, defaultStart = false;
    String devicename = null, deviceprotocol = null;

    //GUI переменные
    private Button enableBtButton;
    private Button showDevicesButton;
    private Button open_sensors_but;
    private Button clearDatabaseButton;
    private ListView pairedDevicesList;
    private TextView statustext;

    String[] initializeCommands;
    TroubleCodes troubleCodes;
    String VOLTAGE = "ATRV",
            RESET = "ATZ",
            ENGINE_COOLANT_TEMP = "0105",  //A-40
            ENGINE_RPM = "010C",  //((A*256)+B)/4
            VEHICLE_SPEED = "010D",  //A
            THROTTLE_POSITION = "0111",  //A-40
            MAF_AIR_FLOW = "0110";//MAF air flow rate 0 - 655.35	kg/h ((256*A)+B) / 100  [kg/h]

    private String mConnectedDeviceName = "Ecu";
    private int rpmval = 0, coolantTemp = 0, mMaf = 0,
            FaceColor = 0, throttlepos = 0, vehspeed = 0,
            whichCommand = 0, m_dedectPids = 0, connectcount = 0, trycount = 0;
    private int mEnginedisplacement = 1500;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BtCommService mBtService = null;

    StringBuilder inStream = new StringBuilder();

    // The Handler that gets information back from the BluetoothChatService
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;

    private AppDatabase database;
    private Date lastSaveTime = new Date();
    private static final long SAVE_INTERVAL = 1000; // Сохранять каждую секунду
    private SensorData lastSavedData = new SensorData(new Date(), 0, 0, 0, 0, 0);

    private static final int TEMPERATURE_THRESHOLD = 5; // Изменение температуры на 1 градус
    private static final int RPM_THRESHOLD = 50; // Изменение оборотов на 50
    private static final int SPEED_THRESHOLD = 5; // Изменение скорости на 1 км/ч
    private static final int MAF_THRESHOLD = 5; // Изменение MAF на 1 г/с
    private static final int THROTTLE_THRESHOLD = 3; // Изменение дросселя на 1%

    private static MainActivity instance;

    @SuppressLint({"CheckResult", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

        //Инициализация элементов интерфейса
        enableBtButton = findViewById(R.id.enable_bt_button);
        showDevicesButton = findViewById(R.id.show_devices_button);
        open_sensors_but = findViewById(R.id.open_sensors_button);
        open_sensors_but.setEnabled(false);
        pairedDevicesList = findViewById(R.id.paired_devices_list);
        statustext = findViewById(R.id.status_text);

        initializeCommands = new String[]{"ATL0", "ATE1", "ATH1", "ATAT1", "ATSTFF", "ATI", "ATDP", "ATSP0", "0100"};

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
        else
        {
            if (mBtService != null) {
                if (mBtService.getState() == BtCommService.STATE_NONE) {
                    mBtService.start();
                }
            }
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        enableBtButton.setOnClickListener(v -> enableBluetooth());

        showDevicesButton.setOnClickListener(v -> showPairedDevices());

        mBtService = new BtCommService(this, mBtHandler);

        pairedDevicesList.setOnItemClickListener((parent, view, position, id) -> {
            String item = (String) parent.getItemAtPosition(position);
            String deviceAddress = item.substring(item.length() - 17); // MAC-адрес
            tryconnect = true;
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
            if (mBtService != null) {
                mBtService.connect(device);  // пробуем подключиться
                currentdevice = device;
                Toast.makeText(MainActivity.this, "Подключение к " + device.getName(), Toast.LENGTH_SHORT).show();// обновим кнопку
            }
        });

        open_sensors_but.setOnClickListener(v -> {
            if (mBtService.getState() != mBtService.STATE_CONNECTED) {
                // Если не подключены, показываем диалог
                showBluetoothNotConnectedDialog();
            } else {
                // Если подключены, переходим на экран сенсоров
                Intent intent = new Intent(MainActivity.this, SensorsActivity.class);
                startActivity(intent);
            }
        });


        getPreferences();

        resetgauges();

        database = AppDatabase.getDatabase(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth уже включен", Toast.LENGTH_SHORT).show();
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void showBluetoothNotConnectedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка подключения")
                .setMessage("Сначала подключитесь к Bluetooth")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void openSensorsActivity() {
        Intent intent = new Intent(MainActivity.this, SensorsActivity.class);
        startActivity(intent);
    }

    @SuppressLint("MissingPermission")
    public void showPairedDevices() {
        // Получаем сопряженные устройства
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList<String> deviceList = new ArrayList<>();

        // Добавляем устройства в список
        for (BluetoothDevice device : pairedDevices) {
            deviceList.add(device.getName() + "\n" + device.getAddress());
        }

        // Проверяем, есть ли устройства
        if (deviceList.isEmpty()) {
            Toast.makeText(this, "Нет сопряженных устройств", Toast.LENGTH_SHORT).show();
        } else {
            // Создаем адаптер с кастомным макетом
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item_device, deviceList);
            pairedDevicesList.setAdapter(adapter);
        }
    }

    public void resetvalues() {

        m_getPids = false;
        whichCommand = 0;
        trycount = 0;
        initialized = false;
        defaultStart = false;
        avgconsumption.clear();
        //mConversationArrayAdapter.clear();

        resetgauges();
    }

    public void resetgauges() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        }).start();
    }

    private final Handler mBtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case BtCommService.STATE_CONNECTED:

                            statustext.setText(getString(R.string.connectedto, mConnectedDeviceName));
                            open_sensors_but.setEnabled(true);

                            tryconnect = false;
                            resetvalues();
                            sendEcuMessage(RESET);

                            break;
                        case BtCommService.STATE_CONNECTING:
                            statustext.setText(R.string.connecting);
                            break;
                        case BtCommService.STATE_LISTEN:

                        case BtCommService.STATE_NONE:

                            statustext.setText(R.string.unabletoconnect);
                            if (tryconnect) {
                                mBtService.connect(currentdevice);
                                connectcount++;
                                if (connectcount >= 2) {
                                    tryconnect = false;
                                    open_sensors_but.setEnabled(false);
                                }
                            }
                            resetvalues();

                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    if (commandmode || !initialized) {
                        //mConversationArrayAdapter.add("Command:  " + writeMessage);
                    }

                    break;
                case MESSAGE_READ:

                    String tmpmsg = clearMsg(msg);


                    if (commandmode || !initialized) {
                        //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);
                    }

                    analysMsg(msg);

                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void sendEcuMessage(String message) {

        if (mBtService != null)
        {
            // Check that we're actually connected before trying anything
            if (mBtService.getState() != BtCommService.STATE_CONNECTED) {
                //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                if (message.length() > 0) {

                    message = message + "\r";
                    // Get the message bytes and tell the BluetoothChatService to write
                    byte[] send = message.getBytes();
                    mBtService.write(send);
                }
            } catch (Exception e) {
            }
        }
    }

    private String clearMsg(Message msg) {
        String tmpmsg = msg.obj.toString();

        tmpmsg = tmpmsg.replace("null", "");
        tmpmsg = tmpmsg.replaceAll("\\s", ""); //removes all [ \t\n\x0B\f\r]
        tmpmsg = tmpmsg.replaceAll(">", "");
        tmpmsg = tmpmsg.replaceAll("SEARCHING...", "");
        tmpmsg = tmpmsg.replaceAll("ATZ", "");
        tmpmsg = tmpmsg.replaceAll("ATI", "");
        tmpmsg = tmpmsg.replaceAll("atz", "");
        tmpmsg = tmpmsg.replaceAll("ati", "");
        tmpmsg = tmpmsg.replaceAll("ATDP", "");
        tmpmsg = tmpmsg.replaceAll("atdp", "");
        tmpmsg = tmpmsg.replaceAll("ATRV", "");
        tmpmsg = tmpmsg.replaceAll("atrv", "");

        return tmpmsg;
    }

    private void analysMsg (Message msg) {

        String tmpmsg = clearMsg(msg);

        generateVolt(tmpmsg);

        getElmInfo(tmpmsg);

        if (!initialized) {

            sendInitCommands();

        } else {

            checkPids(tmpmsg);

            if (!m_getPids && m_dedectPids == 1) {
                String sPIDs = "0100";
                sendEcuMessage(sPIDs);
                return;
            }

            if (commandmode) {
                getFaultInfo(tmpmsg);
                return;
            }

            try {
                analysPIDS(tmpmsg);
            } catch (Exception e) {
            }

            sendDefaultCommands();
        }
    }

    private void analysPIDS(String dataRecieved) {

        int A = 0;
        int B = 0;
        int PID = 0;

        if ((dataRecieved != null) && (dataRecieved.matches("^[0-9A-F]+$"))) {

            dataRecieved = dataRecieved.trim();
            Log.d(TAG, "Received data: " + dataRecieved);

            int index = dataRecieved.indexOf("41");

            String tmpmsg = null;

            if (index != -1) {

                tmpmsg = dataRecieved.substring(index, dataRecieved.length());
                Log.d(TAG, "Processed message: " + tmpmsg);

                if (tmpmsg.substring(0, 2).equals("41")) {

                    PID = Integer.parseInt(tmpmsg.substring(2, 4), 16);
                    A = Integer.parseInt(tmpmsg.substring(4, 6), 16);
                    B = Integer.parseInt(tmpmsg.substring(6, 8), 16);

                    Log.d(TAG, "PID: " + PID + ", A: " + A + ", B: " + B);
                    calculateEcuValues(PID, A, B);
                }
            }
        }
    }

    private void calculateEcuValues(int PID, int A, int B) {

        double val = 0;
        int intval = 0;
        int tempC = 0;

        switch (PID) {

            case 5://PID(05): Температура охлаждающей жидкости
                tempC = A - 40;
                coolantTemp = tempC;
                sendDataToActivity("TemperatureSensorActivity", "temperature", coolantTemp);
                break;

            case 12: //PID(0C): Количество оборотов двигателя
                val = ((A * 256) + B) / 4;
                rpmval = (int) val;
                sendDataToActivity("EngineRpmActivity", "rpm", rpmval);
                break;

            case 13://PID(0D): Скорость автомобиля
                val = A;
                intval = (int) A;
                vehspeed = intval;
                sendDataToActivity("VehicleSpeedActivity", "speed", vehspeed);
                break;

            case 16://PID(10): Массовый расход воздуха
                val = ((256 * A) + B) / 100 * 3.6; // Конвертируем г/с в кг/ч
                mMaf = (int) val;
                sendDataToActivity("MassAirflowActivity", "maf", mMaf);
                break;

            case 17://PID(11) - положение дроссельной заслонки
                val = A * 100 / 255;
                throttlepos = (int) val;
                sendDataToActivity("ThrottlePositionActivity", "throttle", throttlepos);
                break;

            default:
        }
        
        saveSensorData();
    }

    private void sendDataToActivity(String activityName, String key, int value) {
        Intent intent = new Intent();
        intent.setAction("com.autohealth.autohealth." + activityName);
        intent.putExtra(key, value);
        sendBroadcast(intent);
    }

    private void sendInitCommands() {
        if (initializeCommands.length != 0) {

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = initializeCommands[whichCommand];
            sendEcuMessage(send);

            if (whichCommand == initializeCommands.length - 1) {
                initialized = true;
                whichCommand = 0;
                sendDefaultCommands();
            } else {
                whichCommand++;
            }
        }
    }

    private void sendDefaultCommands() {

        if (commandslist.size() != 0) {

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = commandslist.get(whichCommand);
            sendEcuMessage(send);

            if (whichCommand >= commandslist.size() - 1) {
                whichCommand = 0;
            } else {
                whichCommand++;
            }
        }
    }

    private void checkPids(String tmpmsg) {
        if (tmpmsg.indexOf("41") != -1) {
            int index = tmpmsg.indexOf("41");

            String pidmsg = tmpmsg.substring(index, tmpmsg.length());

            if (pidmsg.contains("4100")) {

                setPidsSupported(pidmsg);
                return;
            }
        }
    }

    private void setPidsSupported(String buffer) {

        trycount++;

        StringBuilder flags = new StringBuilder();
        String buf = buffer.toString();
        buf = buf.trim();
        buf = buf.replace("\t", "");
        buf = buf.replace(" ", "");
        buf = buf.replace(">", "");

        Log.d(TAG, "Buffer for PIDs: " + buf);

        if (buf.indexOf("4100") == 0 || buf.indexOf("4120") == 0) {

            for (int i = 0; i < 8; i++) {
                String tmp = buf.substring(i + 4, i + 5);
                int data = Integer.valueOf(tmp, 16).intValue();
                if ((data & 0x08) == 0x08) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x04) == 0x04) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x02) == 0x02) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x01) == 0x01) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
            }

            Log.d(TAG, "Supported PIDs flags: " + flags.toString());

            commandslist.clear();
            commandslist.add(0, VOLTAGE);
            int pid = 1;

            StringBuilder supportedPID = new StringBuilder();
            supportedPID.append("Supported PIDS:\n");
            for (int j = 0; j < flags.length(); j++) {
                if (flags.charAt(j) == '1') {
                    supportedPID.append(" " + PIDS[j] + " ");
                    if (!PIDS[j].contains("01") && !PIDS[j].contains("20")) {
                        commandslist.add(pid, "01" + PIDS[j]);
                        pid++;
                    }
                }
            }
            Log.d(TAG, "Commands list: " + commandslist.toString());
            m_getPids = true;
            whichCommand = 0;
            sendEcuMessage("ATRV");

        } else {
            return;
        }
    }


    private void getFaultInfo(String tmpmsg) {

        String substr = "43";

        int index = tmpmsg.indexOf(substr);

        if (index == -1)
        {
            substr = "47";
            index = tmpmsg.indexOf(substr);
        }

        if (index != -1) {

            tmpmsg = tmpmsg.substring(index, tmpmsg.length());

            if (tmpmsg.substring(0, 2).equals(substr)) {

                performCalculations(tmpmsg);

                String faultCode = null;
                String faultDesc = null;

                if (troubleCodesArray.size() > 0) {

                    for (int i = 0; i < troubleCodesArray.size(); i++) {
                        faultCode = troubleCodesArray.get(i);
                        faultDesc = troubleCodes.getFaultCode(faultCode);

                        Log.e(TAG, "Fault Code: " + substr + " : " + faultCode + " desc: " + faultDesc);

                        if (faultCode != null && faultDesc != null) {
                        } else if (faultCode != null && faultDesc == null) {
                        }
                    }
                } else {
                }
            }
        }
    }


    protected void performCalculations(String fault) {

        final String result = fault;
        String workingData = "";
        int startIndex = 0;
        troubleCodesArray.clear();

        try{

            if(result.indexOf("43") != -1)
            {
                workingData = result.replaceAll("^43|[\r\n]43|[\r\n]", "");
            }else if(result.indexOf("47") != -1)
            {
                workingData = result.replaceAll("^47|[\r\n]47|[\r\n]", "");
            }

            for (int begin = startIndex; begin < workingData.length(); begin += 4) {
                String dtc = "";
                byte b1 = hexStringToByteArray(workingData.charAt(begin));
                int ch1 = ((b1 & 0xC0) >> 6);
                int ch2 = ((b1 & 0x30) >> 4);
                dtc += dtcLetters[ch1];
                dtc += hexArray[ch2];
                dtc += workingData.substring(begin + 1, begin + 4);

                if (dtc.equals("P0000")) {
                    continue;
                }

                troubleCodesArray.add(dtc);
            }
        }catch(Exception e)
        {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    private byte hexStringToByteArray(char s) {
        return (byte) ((Character.digit(s, 16) << 4));
    }

    private void getElmInfo(String tmpmsg) {

        if (tmpmsg.contains("ELM") || tmpmsg.contains("elm")) {
            devicename = tmpmsg;
        }

        if (tmpmsg.contains("SAE") || tmpmsg.contains("ISO")
                || tmpmsg.contains("sae") || tmpmsg.contains("iso") || tmpmsg.contains("AUTO")) {
            deviceprotocol = tmpmsg;
        }

        if (deviceprotocol != null && devicename != null) {
            devicename = devicename.replaceAll("STOPPED", "");
            deviceprotocol = deviceprotocol.replaceAll("STOPPED", "");
            statustext.setText(devicename + " " + deviceprotocol);
        }
    }

    private void generateVolt(String msg) {

        String VoltText = null;

        if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})\\s*"))) {

            VoltText = msg + "V";

        } else if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})?V\\s*"))) {

            VoltText = msg;

        }

        if (VoltText != null) {
        }
    }


    private void getPreferences() {

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        FaceColor = Integer.parseInt(preferences.getString("FaceColor", "0"));


        mEnginedisplacement = Integer.parseInt(preferences.getString("Enginedisplacement", "1500"));

        m_dedectPids = Integer.parseInt(preferences.getString("DedectPids", "0"));

        if (m_dedectPids == 0) {

            commandslist.clear();

            int i = 0;

            commandslist.add(i, VOLTAGE);

            if (preferences.getBoolean("checkboxENGINE_RPM", true)) {
                commandslist.add(i, ENGINE_RPM);
                i++;
            }

            if (preferences.getBoolean("checkboxVEHICLE_SPEED", true)) {
                commandslist.add(i, VEHICLE_SPEED);
                i++;
            }

            if (preferences.getBoolean("checkboxENGINE_COOLANT_TEMP", true)) {
                commandslist.add(i, ENGINE_COOLANT_TEMP);
                i++;
            }

            if (preferences.getBoolean("checkboxMAF_AIR_FLOW", true)) {
                commandslist.add(i, MAF_AIR_FLOW);
                i++;
            }

            if (preferences.getBoolean("checkboxTHROTTLE_POSITION", true)) {
                commandslist.add(i, THROTTLE_POSITION);
            }

            whichCommand = 0;
        }
    }

    private void saveSensorData() {
        Date currentTime = new Date();
        
        // Проверяем, изменились ли значения значительно
        boolean hasSignificantChange = 
            Math.abs(coolantTemp - lastSavedData.temperature) >= TEMPERATURE_THRESHOLD ||
            Math.abs(rpmval - lastSavedData.rpm) >= RPM_THRESHOLD ||
            Math.abs(vehspeed - lastSavedData.speed) >= SPEED_THRESHOLD ||
            Math.abs(mMaf - lastSavedData.maf) >= MAF_THRESHOLD ||
            Math.abs(throttlepos - lastSavedData.throttlePosition) >= THROTTLE_THRESHOLD;
        
        if (hasSignificantChange) {
            SensorData data = new SensorData(
                currentTime,
                coolantTemp,
                rpmval,
                vehspeed,
                mMaf,
                throttlepos
            );
            
            database.sensorDataDao().insert(data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        Log.d(TAG, "Data saved successfully");
                        lastSavedData = data;
                    },
                    throwable -> Log.e(TAG, "Error saving data", throwable)
                );
        }
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public int getCoolantTemp() {
        return coolantTemp;
    }

    public int getRpmVal() {
        return rpmval;
    }

    public int getSpeed() {
        return vehspeed;
    }

    public int getMaf() {
        return mMaf;
    }

    public int getThrottlePos() {
        return throttlepos;
    }
}