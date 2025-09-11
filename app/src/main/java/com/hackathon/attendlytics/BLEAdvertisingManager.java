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
import java.util.UUID;
import java.io.UnsupportedEncodingException;

public class BLEAdvertisingManager {
    private static final String TAG = "BLEAdvertisingManager";
    
    // Service UUID from centralized configuration
    private static final String SERVICE_UUID_STRING = BLEConfig.SERVICE_UUID;
    private static final UUID SERVICE_UUID = UUID.fromString(SERVICE_UUID_STRING);
    
    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private boolean isAdvertising = false;
    
    // Callback for advertising events
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            isAdvertising = true;
            Log.i(TAG, "✅ BLE Advertising started successfully!");
            Log.i(TAG, "Service UUID being advertised: " + SERVICE_UUID_STRING);
            Log.i(TAG, "Settings: " + settingsInEffect.toString());
            logAdvertisementDetails();
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            isAdvertising = false;
            String errorMessage = getAdvertiseErrorMessage(errorCode);
            Log.e(TAG, "❌ BLE Advertising failed to start. Error: " + errorMessage + " (Code: " + errorCode + ")");
            logTroubleshootingInfo();
        }
    };
    
    public BLEAdvertisingManager(Context context) {
        this.context = context;
        initializeBluetooth();
    }
    
    private void initializeBluetooth() {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                Log.d(TAG, "Bluetooth initialized successfully");
            } else {
                Log.e(TAG, "Bluetooth is not available or not enabled");
            }
        } else {
            Log.e(TAG, "BluetoothManager is null - device doesn't support Bluetooth");
        }
    }
    
    public boolean canAdvertise() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Device doesn't support BLE");
            return false;
        }
        
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not available or not enabled");
            return false;
        }
        
        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "BLE Advertising not supported on this device");
            return false;
        }
        
        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.w(TAG, "Multiple advertisement not supported - this might affect advertising");
        }
        
        return true;
    }
    
    public void startAdvertising(String sessionId) {
        Log.d(TAG, "🚀 Starting BLE advertising for session: " + sessionId);
        
        if (!canAdvertise()) {
            Log.e(TAG, "Cannot start advertising - device/Bluetooth not ready");
            return;
        }
        
        if (isAdvertising) {
            Log.w(TAG, "Already advertising - stopping current session first");
            stopAdvertising();
        }
        
        // Create advertising settings - optimized for proximity detection
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0) // Advertise indefinitely
                .build();
        
        // Create advertising data with session ID
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID));
        
        // Prepare session ID for BLE transmission
        String bleSessionId = prepareSessionIdForBLE(sessionId);
        boolean sessionIdAdded = false;
        
        // Try to add session ID as service data first (preferred method)
        if (bleSessionId != null) {
            try {
                byte[] sessionData = bleSessionId.getBytes("UTF-8");
                if (sessionData.length <= 20) { // BLE service data limit
                    dataBuilder.addServiceData(new ParcelUuid(SERVICE_UUID), sessionData);
                    sessionIdAdded = true;
                    Log.d(TAG, "✅ Session ID added as SERVICE DATA: " + bleSessionId + " (length: " + sessionData.length + ")");
                } else {
                    Log.w(TAG, "Session ID too long for service data: " + sessionData.length + " bytes");
                }
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, "Could not add session ID as service data: " + e.getMessage());
            }
        }
        
        AdvertiseData advertiseData = dataBuilder.build();
        
        // If service data didn't work, try manufacturer data as fallback
        AdvertiseData scanResponseData = null;
        if (!sessionIdAdded && bleSessionId != null) {
            try {
                byte[] sessionData = bleSessionId.getBytes("UTF-8");
                // Use a custom manufacturer ID (0xFFFF is for testing/development)
                AdvertiseData.Builder scanResponseBuilder = new AdvertiseData.Builder()
                        .addManufacturerData(0xFFFF, sessionData);
                scanResponseData = scanResponseBuilder.build();
                sessionIdAdded = true;
                Log.d(TAG, "✅ Session ID added as MANUFACTURER DATA: " + bleSessionId + " (length: " + sessionData.length + ")");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, "Could not add session ID as manufacturer data: " + e.getMessage());
            }
        }
        
        if (!sessionIdAdded) {
            Log.e(TAG, "❌ Could not add session ID to BLE advertising data!");
        }
        
        Log.d(TAG, "Starting advertising with:");
        Log.d(TAG, "  - Service UUID: " + SERVICE_UUID_STRING);
        Log.d(TAG, "  - Original Session ID: " + sessionId);
        Log.d(TAG, "  - BLE Session ID: " + bleSessionId);
        Log.d(TAG, "  - Session ID in advertising: " + sessionIdAdded);
        Log.d(TAG, "  - Mode: LOW_LATENCY");
        Log.d(TAG, "  - Power: HIGH");
        
        // Start advertising
        if (scanResponseData != null) {
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback);
        } else {
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, advertiseCallback);
        }
    }
    
    /**
     * Logs the current advertising status and configuration for debugging
     */
    public void logAdvertisingStatus() {
        Log.d(TAG, "=== BLE ADVERTISING STATUS ===");
        Log.d(TAG, "Is Advertising: " + isAdvertising);
        Log.d(TAG, "Bluetooth LE Advertiser Available: " + (bluetoothLeAdvertiser != null));
        Log.d(TAG, "Target Service UUID: " + SERVICE_UUID_STRING);
        
        // Log Bluetooth adapter details
        if (bluetoothAdapter != null) {
            Log.d(TAG, "Bluetooth Adapter Details:");
            Log.d(TAG, "  - Adapter Enabled: " + bluetoothAdapter.isEnabled());
            Log.d(TAG, "  - Device Name: " + bluetoothAdapter.getName());
            Log.d(TAG, "  - Device Address: " + bluetoothAdapter.getAddress());
            Log.d(TAG, "  - Multiple Advertisement Supported: " + bluetoothAdapter.isMultipleAdvertisementSupported());
            Log.d(TAG, "  - BLE Advertisement Supported: " + (bluetoothLeAdvertiser != null));
        }
        
        if (isAdvertising) {
            Log.d(TAG, "Advertising Configuration:");
            Log.d(TAG, "  - Mode: LOW_LATENCY (fastest discovery)");
            Log.d(TAG, "  - Power: HIGH (maximum range)");
            Log.d(TAG, "  - Connectable: false");
            Log.d(TAG, "  - Discoverable: true");
            Log.d(TAG, "  - Include device name: true");
            Log.d(TAG, "  - Include power level: true");
        } else {
            Log.d(TAG, "❌ Not currently advertising");
        }
        
        Log.d(TAG, "==============================");
    }

    /**
     * Prepares the session ID for BLE transmission by creating a shorter, BLE-optimized version
     * while maintaining uniqueness for student proximity checking
     */
    private String prepareSessionIdForBLE(String originalSessionId) {
        if (originalSessionId == null) return null;
        
        try {
            // Extract timestamp from "ATTEND-SESSION-{timestamp}" format
            if (originalSessionId.startsWith("ATTEND-SESSION-")) {
                String timestamp = originalSessionId.substring("ATTEND-SESSION-".length());
                
                // Use last 8 digits of timestamp for BLE (reduces from ~30 chars to ~8 chars)
                // This provides uniqueness while fitting in BLE advertising data
                if (timestamp.length() >= 8) {
                    String shortId = timestamp.substring(timestamp.length() - 8);
                    Log.d(TAG, "Optimized session ID: " + originalSessionId + " → " + shortId);
                    return shortId;
                }
            }
            
            // Fallback: use first 8 characters if format is different
            String fallback = originalSessionId.length() > 8 ? 
                originalSessionId.substring(0, 8) : originalSessionId;
            Log.d(TAG, "Fallback session ID: " + originalSessionId + " → " + fallback);
            return fallback;
            
        } catch (Exception e) {
            Log.e(TAG, "Error preparing session ID for BLE: " + e.getMessage());
            return null;
        }
    }
    
    public void stopAdvertising() {
        Log.d(TAG, "🛑 Stopping BLE advertising");
        
        if (bluetoothLeAdvertiser != null && isAdvertising) {
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            isAdvertising = false;
            Log.i(TAG, "BLE Advertising stopped");
        } else {
            Log.w(TAG, "Not currently advertising or advertiser not available");
        }
    }
    
    public boolean isAdvertising() {
        return isAdvertising;
    }
    
    private void logAdvertisementDetails() {
        Log.i(TAG, "=== ADVERTISEMENT DETAILS ===");
        Log.i(TAG, "Service UUID: " + SERVICE_UUID_STRING);
        Log.i(TAG, "UUID as bytes: " + java.util.Arrays.toString(SERVICE_UUID.toString().getBytes()));
        
        // Safely get device name and address with permission checks
        try {
            if (bluetoothAdapter != null) {
                String deviceName = bluetoothAdapter.getName();
                String deviceAddress = bluetoothAdapter.getAddress();
                Log.i(TAG, "Device Name: " + (deviceName != null ? deviceName : "Unknown"));
                Log.i(TAG, "Device Address: " + (deviceAddress != null ? deviceAddress : "Unknown"));
                Log.i(TAG, "Multiple Advertisement Support: " + bluetoothAdapter.isMultipleAdvertisementSupported());
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Cannot access device info due to missing permissions: " + e.getMessage());
            Log.i(TAG, "Device info not available (requires BLUETOOTH_CONNECT permission)");
        } catch (Exception e) {
            Log.w(TAG, "Error getting device info: " + e.getMessage());
        }
        
        Log.i(TAG, "==============================");
    }
    
    private void logTroubleshootingInfo() {
        Log.e(TAG, "=== TROUBLESHOOTING INFO ===");
        Log.e(TAG, "Bluetooth Enabled: " + (bluetoothAdapter != null && bluetoothAdapter.isEnabled()));
        Log.e(TAG, "BLE Supported: " + context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
        Log.e(TAG, "Advertiser Available: " + (bluetoothLeAdvertiser != null));
        Log.e(TAG, "Multiple Adv Support: " + (bluetoothAdapter != null && bluetoothAdapter.isMultipleAdvertisementSupported()));
        Log.e(TAG, "============================");
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
    
    public void cleanup() {
        if (isAdvertising) {
            stopAdvertising();
        }
    }
    
    // Method to get the context (needed for SimpleBLEAdvertiser)
    public Context getContext() {
        return context;
    }
    
    // Method to get the Service UUID being used
    public String getServiceUUID() {
        return SERVICE_UUID_STRING;
    }
    
    // Method to manually verify advertising status
    public void verifyAdvertisingStatus() {
        Log.i(TAG, "=== ADVERTISING STATUS CHECK ===");
        Log.i(TAG, "Internal isAdvertising flag: " + isAdvertising);
        Log.i(TAG, "BluetoothAdapter enabled: " + (bluetoothAdapter != null && bluetoothAdapter.isEnabled()));
        Log.i(TAG, "BluetoothLeAdvertiser available: " + (bluetoothLeAdvertiser != null));
        Log.i(TAG, "Expected Service UUID: " + SERVICE_UUID_STRING);
        Log.i(TAG, "================================");
    }
}