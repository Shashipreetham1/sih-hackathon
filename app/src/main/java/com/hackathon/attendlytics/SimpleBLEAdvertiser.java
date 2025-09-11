package com.hackathon.attendlytics;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Proper BLE Advertiser using Service UUID and Service Data approach
 * This matches the standard BLE practices for device identification
 */
public class SimpleBLEAdvertiser {
    private static final String TAG = "SimpleBLEAdvertiser";
    
    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private boolean isAdvertising = false;
    
    // Service UUID and data for this session
    private UUID serviceUUID = null;
    private String customTag = "TEACHER"; // Default tag
    
    // Callback for advertising events
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            isAdvertising = true;
            Log.i(TAG, "✅ BLE Advertising started successfully!");
            Log.i(TAG, "📡 Service UUID: " + serviceUUID.toString());
            Log.i(TAG, "🏷️ Service Data Tag: " + customTag);
            Log.i(TAG, "🎯 Students should scan for UUID: " + serviceUUID.toString());
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            isAdvertising = false;
            String errorMessage = getAdvertiseErrorMessage(errorCode);
            Log.e(TAG, "❌ BLE Advertising failed: " + errorMessage + " (Code: " + errorCode + ")");
            Log.e(TAG, "💔 Failed to broadcast UUID: " + (serviceUUID != null ? serviceUUID.toString() : "null"));
            logTroubleshootingInfo();
        }
    };
    
    public SimpleBLEAdvertiser(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        this.bluetoothLeAdvertiser = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeAdvertiser() : null;
    }
    
    /**
     * Starts advertising using proper BLE Service UUID and Service Data approach
     * This is the standard and recommended way to advertise custom data
     */
    public void startAdvertisingWithServiceUUID() {
        if (!canAdvertise()) {
            Log.e(TAG, "Cannot start advertising - requirements not met");
            logTroubleshootingInfo();
            return;
        }
        
        if (isAdvertising) {
            Log.w(TAG, "Already advertising - stopping previous session");
            stopAdvertising();
        }
        
        // Use the predefined Service UUID instead of random to save space
        this.serviceUUID = UUID.fromString(BLEConfig.SERVICE_UUID);
        
        Log.d(TAG, "🚀 Starting BLE advertising with Service UUID approach");
        Log.d(TAG, "📡 Service UUID: " + serviceUUID.toString());
        Log.d(TAG, "🏷️ Service Data Tag: " + customTag);
        
        // Advertising settings - high power, fast discovery, no connection needed
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)  // No connection needed for scanning
                .build();
        
        // Create ParcelUuid for the service
        ParcelUuid serviceParcelUuid = new ParcelUuid(serviceUUID);
        
        // Create minimal advertising data - just the service UUID, no extra service data
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder()
                .addServiceUuid(serviceParcelUuid)  // Add the service UUID
                .setIncludeDeviceName(false)        // Disable device name to save space
                .setIncludeTxPowerLevel(false);     // Disable power level to save space
        
        // SKIP adding service data to avoid DATA_TOO_LARGE error
        // The Service UUID alone is enough for identification
        Log.d(TAG, "✅ Using minimal advertising data (Service UUID only)");
        
        AdvertiseData advertiseData = dataBuilder.build();
        
        Log.d(TAG, "📡 Starting BLE advertisement with:");
        Log.d(TAG, "  - Service UUID: " + serviceUUID.toString());
        Log.d(TAG, "  - Data Size: Minimal (Service UUID only)");
        Log.d(TAG, "  - Mode: LOW_LATENCY (fastest discovery)");
        Log.d(TAG, "  - Power: HIGH (maximum range)");
        Log.d(TAG, "  - Connectable: false (scan-only)");
        
        // Start advertising with minimal data to avoid size limits
        bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, advertiseCallback);
    }
    
    /**
     * Gets the Service UUID that's being advertised (for QR code)
     */
    public UUID getServiceUUID() {
        return serviceUUID;
    }
    
    /**
     * Gets the Service UUID as string (for QR code)
     */
    public String getServiceUUIDString() {
        return serviceUUID != null ? serviceUUID.toString() : null;
    }
    
    /**
     * Sets a custom tag for Service Data
     */
    public void setCustomTag(String tag) {
        this.customTag = tag != null ? tag : "TEACHER";
    }
    
    /**
     * Stops BLE advertising
     */
    public void stopAdvertising() {
        if (bluetoothLeAdvertiser != null && isAdvertising) {
            Log.d(TAG, "🛑 Stopping BLE advertising");
            Log.d(TAG, "📡 Was broadcasting UUID: " + (serviceUUID != null ? serviceUUID.toString() : "null"));
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            isAdvertising = false;
        }
    }
    
    /**
     * Checks if advertising is possible on this device
     */
    public boolean canAdvertise() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter not available");
            return false;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            return false;
        }
        
        // CRITICAL FIX: Check BLUETOOTH_ADVERTISE permission for Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "❌ BLUETOOTH_ADVERTISE permission not granted - advertising will fail!");
                return false;
            } else {
                Log.d(TAG, "✅ BLUETOOTH_ADVERTISE permission granted");
            }
        }
        
        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "BLE Advertising not supported on this device");
            return false;
        }
        
        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.e(TAG, "Multiple advertisement not supported");
            return false;
        }
        
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Bluetooth LE not supported");
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the custom device identifier being broadcast (matches QR code)
     */
    public String getBroadcastIdentifier() {
        return serviceUUID != null ? serviceUUID.toString() : null;
    }
    
    /**
     * Logs troubleshooting information
     */
    private void logTroubleshootingInfo() {
        Log.d(TAG, "=== TROUBLESHOOTING INFO ===");
        Log.d(TAG, "Bluetooth Enabled: " + (bluetoothAdapter != null && bluetoothAdapter.isEnabled()));
        Log.d(TAG, "BLE Supported: " + context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
        Log.d(TAG, "Advertiser Available: " + (bluetoothLeAdvertiser != null));
        Log.d(TAG, "Multiple Adv Support: " + (bluetoothAdapter != null && bluetoothAdapter.isMultipleAdvertisementSupported()));
        Log.d(TAG, "============================");
    }
    
    private String getAdvertiseErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "ADVERTISE_FAILED_ALREADY_STARTED";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "ADVERTISE_FAILED_DATA_TOO_LARGE";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "ADVERTISE_FAILED_INTERNAL_ERROR";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
            default:
                return "UNKNOWN_ERROR";
        }
    }
    
    public boolean isAdvertising() {
        return isAdvertising;
    }
    
    public void cleanup() {
        if (isAdvertising) {
            stopAdvertising();
        }
    }
}