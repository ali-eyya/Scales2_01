package com.eyya.scales2_01;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    //TextView textView = (TextView) findViewById(R.id.textView);
    private final BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    String name = "K2H";
    BleDevice bleDevice;
    BleScanCallback callback;
    String uuid_characteristic_notify;
    String uuid_service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bleCheck();
        bleIsOn();
        initLocationPermission();
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setSplitWriteNum(20)
                .setConnectOverTime(10000)
                .setOperateTimeout(5000);

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setDeviceName(true, name)
                .setAutoConnect(true)
                .setScanTimeOut(10000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);


        try {
            new ScanAndConnect();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private void bleIsOn() {
        /*
          Check if Bluetooth is on
          Request to turn on
         */
        assert bAdapter != null;
        if (!bAdapter.isEnabled()) {
            Intent bTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            launchBTRequestActivity.launch(bTIntent);
        }
    }

    private void bleCheck() {
        /*
         * Check if device supports BLE
         */
        if (bAdapter == null) {
            Toast.makeText(this, "Device Does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }


    /*
     * Bluetooth request activity launch
     */
    ActivityResultLauncher<Intent> launchBTRequestActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    // your operation....
                }
            });

    private void initLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }


    static class ScanAndConnect extends Thread {

        public ScanAndConnect() throws InterruptedException {
            BleManager.getInstance().scanAndConnect(new BleScanAndConnectCallback() {
                @Override
                public void onScanStarted(boolean success) {
                }

                @Override
                public void onScanning(BleDevice bleDevice) {

                }

                @Override
                public void onScanFinished(BleDevice scanResult) {
                }

                @Override
                public void onStartConnect() {
                }

                @Override
                public void onConnectFail(BleDevice bleDevice, BleException exception) {
                }

                @Override
                public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                    System.out.println("Connected to: " + bleDevice.getName());
                    gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);
                    List<BluetoothGattService> serviceList = gatt.getServices();
                    for (BluetoothGattService service : serviceList) {
                        UUID uuid_service = service.getUuid();
                        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristicList) {
                            UUID uuid_chara = characteristic.getUuid();
                            System.out.println("service: " + uuid_service + " ::cahr: " + uuid_chara);
                        }
                    }
                    new NotifyDevice(bleDevice, "0000fee0-0000-1000-8000-00805f9b34fb", "0000fee1-0000-1000-8000-00805f9b34fb");
                    //byte[] b = {5, 5, 'A', 'A'};
                    //new Write(bleDevice, uuid_service.toString(), "96d9ccca-6a8e-4cfa-b0cf-fa9a99bb8c76", b);
                }

                @Override
                public void onDisConnected(boolean isActiveDisConnected, BleDevice
                        device, BluetoothGatt gatt, int status) {
                }
            });
        }
    }

    static class NotifyDevice extends Thread {
        public NotifyDevice(BleDevice bleDevice, String uuid_service, String uuid_characteristic_notify) {
            BleManager.getInstance().notify(
                    bleDevice,
                    uuid_service,
                    uuid_characteristic_notify,
                    new BleNotifyCallback() {
                        @Override
                        public void onNotifySuccess() {

                        }

                        @Override
                        public void onNotifyFailure(BleException exception) {
                            System.out.println(exception);
                        }

                        @Override
                        public void onCharacteristicChanged(byte[] data) {
                            System.out.println(Arrays.toString(data));
                            //textView.set(data.toString());
                        }
                    });
        }
    }

    static class Write {
        public Write(BleDevice bleDevice, String uuid_service, String uuid_characteristic_write, byte[] data) {
            BleManager.getInstance().write(
                    bleDevice,
                    uuid_service,
                    uuid_characteristic_write,
                    data,
                    new BleWriteCallback() {
                        @Override
                        public void onWriteSuccess(int current, int total, byte[] justWrite) {
                            System.out.println(current + " " + total + " " + Arrays.toString(justWrite));
                        }

                        @Override
                        public void onWriteFailure(BleException exception) {
                            System.out.println("Write Fail: " + exception);
                        }
                    });
        }
    }
}