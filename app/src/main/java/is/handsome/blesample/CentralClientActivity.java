package is.handsome.blesample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CentralClientActivity extends AppCompatActivity {

    private static final String TAG = CentralClientActivity.class.getSimpleName();
    private static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 1;

    private TextView deviceNameTextView;
    private Button connectButton;
    private Button readButton;

    private BluetoothDevice device;
    private BluetoothLeScanner leScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService gattService;

    private Handler handler;

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            device = result.getDevice();
            deviceNameTextView.setText(device.getName());
            connectButton.setEnabled(true);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG, "Scan has been failed, errorCode: " + errorCode);
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        readButton.setEnabled(false);
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gattService = gatt.getService(DeviceTemperatureProfile.TEMPERATURE_SERVICE_UUID);
                if (gattService != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            readButton.setEnabled(true);
                        }
                    });
                } else {
                    Toast.makeText(CentralClientActivity.this, "Gatt service is null!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CentralClientActivity.this, "Gatt status is not success", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final int temperatureValue = characteristic.getValue()[0];
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CentralClientActivity.this, "Read temperature Value = " + temperatureValue, Toast.LENGTH_SHORT).show();

                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central_client);

        handler = new Handler();

        initViews();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        ensureBluetoothEnabled(bluetoothAdapter);
        checkBleSupporting();
        leScanner = bluetoothAdapter.getBluetoothLeScanner();

    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScanning();
        disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ACCESS_COARSE_LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanning();
            } else {
                Toast.makeText(CentralClientActivity.this, "Location Permission has not been granted!", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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

    private void initViews() {
        deviceNameTextView = (TextView) findViewById(R.id.central_client_device_name_text_view);
        connectButton = (Button) findViewById(R.id.central_client_connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean autoConnect = false;
                bluetoothGatt = device.connectGatt(CentralClientActivity.this, autoConnect, gattCallback);
                if (!bluetoothGatt.connect()) {
                    Toast.makeText(CentralClientActivity.this, "Connect has been unsuccessful", Toast.LENGTH_SHORT).show();
                } else {
                    stopScanning();
                }
            }
        });
        readButton = (Button) findViewById(R.id.central_client_read_button);
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothGatt.readCharacteristic(gattService.getCharacteristic(DeviceTemperatureProfile.TEMPERATURE_CHARACTERISTIC_UUID));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_central_client, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.central_client_update:
                if (item.isChecked()) {
                    item.setTitle("Scan");
                    stopScanning();
                } else {
                    startScanning();
                    item.setTitle("Stop");
                }
                item.setChecked(!item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.central_client_update);
        if (item.isChecked()) {
            item.setTitle("Scan");
            item.setChecked(!item.isChecked());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void stopScanning() {
        if (leScanner != null) {
            leScanner.stopScan(scanCallback);
            invalidateOptionsMenu();
        }
    }

    private void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    private void startScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        ACCESS_COARSE_LOCATION_REQUEST_CODE);
            } else {
                scanning();
            }
        } else {
            scanning();
        }
    }

    private void scanning() {
        if (leScanner != null) {
            ScanFilter scanFilter = new ScanFilter.Builder()
//                .setServiceUuid(new ParcelUuid(DeviceTemperatureProfile.TEMPERATURE_SERVICE_UUID))
                    .build();
            ArrayList<ScanFilter> filters = new ArrayList<>();
            filters.add(scanFilter);

            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();

            leScanner.startScan(filters, scanSettings, scanCallback);
        }
    }
}


