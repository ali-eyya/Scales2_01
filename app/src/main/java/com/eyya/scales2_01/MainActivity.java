package com.eyya.scales2_01;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView textView;
    Button btnTare;
    private final BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    String name = "K2H";
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
        textView = findViewById(R.id.textView);
        btnTare = findViewById(R.id.btnTare);
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


    class ScanAndConnect extends Thread {

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
                    btnTare.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                BleManager.getInstance().stopNotify(bleDevice,"0000fee0-0000-1000-8000-00805f9b34fb", "0000fee1-0000-1000-8000-00805f9b34fb");
                                Thread.sleep(100);
                                String s = "55AA0001100000FF";
                                //[85, -86, 48, 1, -63, 0, 0, -1]
                                byte[] b = hexStringToByteArray(s);
                                new Write(bleDevice, "0000fee0-0000-1000-8000-00805f9b34fb", "0000fee2-0000-1000-8000-00805f9b34fb", b);
                                new NotifyDevice(bleDevice, "0000fee0-0000-1000-8000-00805f9b34fb", "0000fee1-0000-1000-8000-00805f9b34fb");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }


                @Override
                public void onDisConnected(boolean isActiveDisConnected, BleDevice
                        device, BluetoothGatt gatt, int status) {
                }
            });
        }
        public byte[] hexStringToByteArray(String s) {
            int len = s.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
            return data;
        }
    }

    class NotifyDevice extends Thread {

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

                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onCharacteristicChanged(byte[] data) {
                            final DecimalFormat df = new DecimalFormat("0.0");
                            try {
                                int a = Byte.toUnsignedInt(data[5]);
                                int b = Byte.toUnsignedInt(data[6]);
                                int g = (a + (b * 256));
                                String unit = String.format("%02x", data[3]);
                                String sign = String.format("%02x", data[4]);
                                switch (sign) {
                                    case "c1":
                                        switch (unit) {
                                            case "01":
                                                getMsg(g + "g");
                                                break;
                                            case "02":
                                                double kg = g / 1000.0;
                                                getMsg(kg + "kg");
                                                break;
                                            case "04":
                                                double oz1 = (g * 0.03527397);
                                                double oz2 = (oz1 % 16);
                                                int lb = (int) (oz1 - oz2) / 16;
                                                String oz3 = df.format(oz2);
                                                getMsg(lb + "lb : " + oz3 + "oz");
                                                break;
                                            case "08":
                                                String oz = df.format(g * 0.03527397);
                                                getMsg(oz + "oz");
                                                break;
                                            case "10":
                                                getMsg(g + "ml");
                                                break;
                                            default:
                                                getMsg("err");
                                                break;
                                        }
                                        break;
                                    case "e1":
                                        switch (unit) {
                                            case "01":
                                                getMsg("-" + g + "g");
                                                break;
                                            case "02":
                                                double kg = g / 1000.0;
                                                getMsg("-" + kg + "kg");
                                                break;
                                            case "04":
                                                double oz1 = (g * 0.03527397);
                                                double oz2 = (oz1 % 16);
                                                int lb = (int) (oz1 - oz2) / 16;
                                                String oz3 = df.format(oz2);
                                                getMsg("-" + lb + "lb : " + oz3 + "oz");
                                                break;
                                            case "08":
                                                String oz = df.format(g * 0.03527397);
                                                getMsg("-" + oz + "oz");
                                                break;
                                            case "10":
                                                getMsg("-" + g + "ml");
                                                break;
                                            default:
                                                getMsg("err");
                                                break;
                                        }
                                        break;
                                    default:
                                        System.out.println("err");
                                        break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    });
        }

        public void getMsg(String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(msg);
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