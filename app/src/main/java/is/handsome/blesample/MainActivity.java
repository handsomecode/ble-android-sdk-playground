package is.handsome.blesample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView statusTextView;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer gattServer;
    private Handler handler;
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "advertiseCallback:onSuccess: " + settingsInEffect.toString());

        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.i(TAG, "advertiseCallback:onFailure: " + errorCode);
        }
    };

    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                postStatus(getString(R.string.status_connected));
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                postStatus(getString(R.string.status_disconnected));
            }

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device,
                                                int requestId,
                                                int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if (DeviceTemperatureProfile.TEMPERATURE_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                gattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        new byte[]{42});
            } else {
                gattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED,
                        0,
                        null);
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }
    };

    private void postStatus(final String status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                statusTextView.setText(status);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        initView();

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        ensureBluetoothEnabled(bluetoothAdapter);
        checkBleSupporting();
    }

    private void initGattServer() {
        gattServer = bluetoothManager.openGattServer(this, gattServerCallback);

        BluetoothGattService temperatureService = new BluetoothGattService(
                DeviceTemperatureProfile.TEMPERATURE_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic temperatureCharacteristic = new BluetoothGattCharacteristic(
                DeviceTemperatureProfile.TEMPERATURE_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        temperatureService.addCharacteristic(temperatureCharacteristic);

        gattServer.addService(temperatureService);
    }

    private void checkBleSupporting() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void ensureBluetoothEnabled(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBluetoothIntent);
        }
    }

    private void initView() {
        Switch startServerSwitchView = (Switch) findViewById(R.id.main_start_server_switch);
        assert startServerSwitchView != null;
        startServerSwitchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (bluetoothAdapter.isMultipleAdvertisementSupported()) {
                        initGattServer();
                        startAdvertising();
                        statusTextView.setText(R.string.status_advertising);
                    } else {
                        Toast.makeText(MainActivity.this, "No Advertising Support.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                    shutdownGattServer();
                    statusTextView.setText(R.string.status_not_started);
                    Log.i(TAG, "Stop advertising");
                }
            }
        });

        statusTextView = (TextView) findViewById(R.id.main_status_value_text_view);
    }

    private void startAdvertising() {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);

        Log.i(TAG, "Start advertising");
    }

    private void shutdownGattServer() {
        gattServer.close();
    }
}
