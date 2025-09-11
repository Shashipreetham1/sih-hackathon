package com.hackathon.attendlytics;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BLEScannerManager {
    private static final String TAG = "BLEScannerManager";
    
    // Service UUID from centralized configuration  
    private static final String SERVICE_UUID_STRING = BLEConfig.SERVICE_UUID;
    private static final UUID SERVICE_UUID = UUID.fromString(SERVICE_UUID_STRING);
    
    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    
    // Interface for scan results
    public interface ScanResultListener {
        void onTargetDeviceFound(ScanResult result, String sessionId);
        void onOtherDeviceFound(ScanResult result);
        void onScanError(int errorCode);
    }
    
    private ScanResultListener scanResultListener;
    
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            processScanResult(result);
        }
        
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                processScanResult(result);
            }
        }
        
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "❌ BLE Scan failed with error code: " + getScanErrorMessage(errorCode));
            if (scanResultListener != null) {
                scanResultListener.onScanError(errorCode);
            }
        }
    };
    
    public BLEScannerManager(Context context) {
        this.context = context;
        initializeBluetooth();
    }
    
    private void initializeBluetooth() {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                Log.d(TAG, "Bluetooth scanner initialized successfully");
            } else {
                Log.e(TAG, "Bluetooth is not available or not enabled for scanning");
            }
        } else {
            Log.e(TAG, "BluetoothManager is null - device doesn't support Bluetooth");
        }
    }
    
    public boolean canScan() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Device doesn't support BLE");
            return false;
        }
        
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not available or not enabled");
            return false;
        }
        
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BLE Scanner not available");
            return false;
        }
        
        return true;
    }
    
    public void startScanning(ScanResultListener listener) {
        this.scanResultListener = listener;
        
        Log.d(TAG, "🔍 Starting BLE scan for Service UUID: " + SERVICE_UUID_STRING);
        
        if (!canScan()) {
            Log.e(TAG, "Cannot start scanning - device/Bluetooth not ready");
            return;
        }
        
        if (isScanning) {
            Log.w(TAG, "Already scanning - stopping current scan first");
            stopScanning();
        }
        
        // Create scan settings for high performance
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        
        // Create scan filters - one specifically for our service UUID
        List<ScanFilter> scanFilters = new ArrayList<>();
        
        // Filter for our specific service UUID
        ScanFilter serviceFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();
        scanFilters.add(serviceFilter);
        
        Log.d(TAG, "Starting scan with filters for: " + SERVICE_UUID_STRING);
        
        // Start scanning
        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
        isScanning = true;
        Log.i(TAG, "✅ BLE scanning started");
    }
    
    public void startScanningAll(ScanResultListener listener) {
        this.scanResultListener = listener;
        
        Log.d(TAG, "🔍 Starting BLE scan for ALL devices (no filters)");
        
        if (!canScan()) {
            Log.e(TAG, "Cannot start scanning - device/Bluetooth not ready");
            return;
        }
        
        if (isScanning) {
            Log.w(TAG, "Already scanning - stopping current scan first");
            stopScanning();
        }
        
        // Create scan settings for high performance
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setReportDelay(0) // Report immediately
                .build();
        
        Log.d(TAG, "Starting unfiltered scan to detect ALL BLE devices including own advertising");
        
        // Start scanning without filters to see all devices
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
        isScanning = true;
        Log.i(TAG, "✅ BLE scanning started (all devices)");
    }
    
    public void stopScanning() {
        Log.d(TAG, "🛑 Stopping BLE scan");
        
        if (bluetoothLeScanner != null && isScanning) {
            bluetoothLeScanner.stopScan(scanCallback);
            isScanning = false;
            Log.i(TAG, "BLE scanning stopped");
        } else {
            Log.w(TAG, "Not currently scanning or scanner not available");
        }
    }
    
    private void processScanResult(ScanResult result) {
        ScanRecord scanRecord = result.getScanRecord();
        if (scanRecord == null) {
            Log.d(TAG, "Scan result has no scan record");
            return;
        }
        
        String deviceName = result.getDevice().getName();
        String deviceAddress = result.getDevice().getAddress();
        int rssi = result.getRssi();
        
        Log.d(TAG, "📱 Device found: " + 
            (deviceName != null ? deviceName : "Unknown") + 
            " (" + deviceAddress + ") RSSI: " + rssi + " dBm");
        
        // Check if this device advertises our service UUID
        List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
        boolean isOurDevice = false;
        String sessionId = null;
        
        if (serviceUuids != null && !serviceUuids.isEmpty()) {
            for (ParcelUuid uuid : serviceUuids) {
                Log.d(TAG, "  Service UUID found: " + uuid.toString());
                
                if (uuid.getUuid().equals(SERVICE_UUID)) {
                    Log.i(TAG, "🎯 FOUND TARGET DEVICE! Service UUID matches: " + SERVICE_UUID_STRING);
                    isOurDevice = true;
                    
                    // Try to extract session ID from service data
                    sessionId = extractSessionId(scanRecord, uuid);
                    break;
                }
            }
        } else {
            Log.v(TAG, "  No service UUIDs advertised by this device");
        }
        
        // Check manufacturer data even if not our service UUID (for debugging)
        if (scanRecord.getManufacturerSpecificData() != null && scanRecord.getManufacturerSpecificData().size() > 0) {
            Log.v(TAG, "  Device has manufacturer data entries: " + scanRecord.getManufacturerSpecificData().size());
            
            // Check our custom manufacturer ID
            byte[] ourManufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF);
            if (ourManufacturerData != null) {
                try {
                    String manufacturerSessionId = new String(ourManufacturerData, "UTF-8");
                    Log.d(TAG, "  Found our manufacturer data with session ID: " + manufacturerSessionId);
                    if (!isOurDevice) {
                        // This might be our device advertising via manufacturer data only
                        sessionId = manufacturerSessionId;
                        isOurDevice = true;
                        Log.i(TAG, "🎯 FOUND TARGET DEVICE via MANUFACTURER DATA!");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not decode manufacturer data: " + e.getMessage());
                }
            }
        }
        
        // Log additional scan record information for debugging
        logScanRecordDetails(scanRecord);
        
        // Notify listener
        if (scanResultListener != null) {
            if (isOurDevice) {
                scanResultListener.onTargetDeviceFound(result, sessionId);
            } else {
                scanResultListener.onOtherDeviceFound(result);
            }
        }
    }
    
    private String extractSessionId(ScanRecord scanRecord, ParcelUuid serviceUuid) {
        String sessionId = null;
        
        // Try to get session ID from service data first
        try {
            byte[] serviceData = scanRecord.getServiceData(serviceUuid);
            if (serviceData != null && serviceData.length > 0) {
                sessionId = new String(serviceData, "UTF-8");
                Log.i(TAG, "📋 Extracted session ID from SERVICE DATA: " + sessionId);
                return sessionId;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not extract session ID from service data: " + e.getMessage());
        }
        
        // Try to get session ID from manufacturer data as fallback
        try {
            if (scanRecord.getManufacturerSpecificData() != null) {
                // Check for our custom manufacturer ID (0xFFFF)
                byte[] manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF);
                if (manufacturerData != null && manufacturerData.length > 0) {
                    sessionId = new String(manufacturerData, "UTF-8");
                    Log.i(TAG, "📋 Extracted session ID from MANUFACTURER DATA: " + sessionId);
                    return sessionId;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not extract session ID from manufacturer data: " + e.getMessage());
        }
        
        if (sessionId == null) {
            Log.w(TAG, "No session ID found in advertising data");
        }
        
        return sessionId;
    }
    
    private void logScanRecordDetails(ScanRecord scanRecord) {
        Log.v(TAG, "=== SCAN RECORD DETAILS ===");
        Log.v(TAG, "Device Name: " + scanRecord.getDeviceName());
        Log.v(TAG, "Tx Power Level: " + scanRecord.getTxPowerLevel());
        Log.v(TAG, "Advertise Flags: " + scanRecord.getAdvertiseFlags());
        
        // Log all service UUIDs
        List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
        if (serviceUuids != null && !serviceUuids.isEmpty()) {
            Log.v(TAG, "Service UUIDs (" + serviceUuids.size() + "):");
            for (ParcelUuid uuid : serviceUuids) {
                Log.v(TAG, "  - " + uuid.toString());
                if (uuid.getUuid().equals(SERVICE_UUID)) {
                    Log.v(TAG, "    ✅ MATCHES TARGET UUID!");
                }
            }
        } else {
            Log.v(TAG, "Service UUIDs: NONE");
        }
        
        // Log manufacturer data
        if (scanRecord.getManufacturerSpecificData() != null) {
            Log.v(TAG, "Manufacturer Data Size: " + scanRecord.getManufacturerSpecificData().size());
            SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
            for (int i = 0; i < manufacturerData.size(); i++) {
                int key = manufacturerData.keyAt(i);
                byte[] data = manufacturerData.valueAt(i);
                String hexData = bytesToHex(data);
                String textData = "";
                try {
                    textData = " ('" + new String(data, "UTF-8").trim() + "')";
                } catch (Exception e) {
                    // Ignore if not valid UTF-8
                }
                Log.v(TAG, "  Manufacturer ID 0x" + Integer.toHexString(key) + ": " + hexData + textData);
                if (key == 0xFFFF) {
                    Log.v(TAG, "    ✅ MATCHES OUR MANUFACTURER ID!");
                }
            }
        } else {
            Log.v(TAG, "Manufacturer Data: NONE");
        }
        
        // Log service data
        if (scanRecord.getServiceData() != null) {
            Log.v(TAG, "Service Data entries: " + scanRecord.getServiceData().size());
            for (ParcelUuid uuid : scanRecord.getServiceData().keySet()) {
                byte[] data = scanRecord.getServiceData(uuid);
                String hexData = bytesToHex(data);
                String textData = "";
                try {
                    textData = " ('" + new String(data, "UTF-8").trim() + "')";
                } catch (Exception e) {
                    // Ignore if not valid UTF-8
                }
                Log.v(TAG, "  Service " + uuid + ": " + hexData + textData);
                if (uuid.getUuid().equals(SERVICE_UUID)) {
                    Log.v(TAG, "    ✅ MATCHES TARGET SERVICE!");
                }
            }
        } else {
            Log.v(TAG, "Service Data: NONE");
        }
        Log.v(TAG, "===========================");
    }
    
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    private String getScanErrorMessage(int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "SCAN_FAILED_ALREADY_STARTED";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "SCAN_FAILED_FEATURE_UNSUPPORTED";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "SCAN_FAILED_INTERNAL_ERROR";
            default:
                return "UNKNOWN_ERROR";
        }
    }
    
    public boolean isScanning() {
        return isScanning;
    }
    
    public void cleanup() {
        if (isScanning) {
            stopScanning();
        }
    }
    
    // Utility method to verify if a UUID string matches our target
    public static boolean isTargetUUID(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            return uuid.equals(SERVICE_UUID);
        } catch (Exception e) {
            return false;
        }
    }
    
    // Get the target Service UUID
    public String getTargetServiceUUID() {
        return SERVICE_UUID_STRING;
    }
}