package com.hackathon.attendlytics;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified BLE Scanner that looks for specific teacher device addresses
 * Much simpler than Service UUID scanning - just looks for specific Bluetooth addresses
 */
public class SimpleBLEScanner {
    private static final String TAG = "SimpleBLEScanner";
    
    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private ScanResultListener scanResultListener;
    private List<String> targetAddresses = new ArrayList<>();
    
    // Callback for scan results
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
            String errorMessage = getScanErrorMessage(errorCode);
            Log.e(TAG, "❌ BLE Scan failed: " + errorMessage + " (Code: " + errorCode + ")");
            
            if (scanResultListener != null) {
                scanResultListener.onScanError(errorCode);
            }
        }
    };
    
    public interface ScanResultListener {
        void onTargetDeviceFound(ScanResult result, String deviceAddress, String deviceName);
        void onOtherDeviceFound(ScanResult result);
        void onScanError(int errorCode);
    }
    
    public SimpleBLEScanner(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        this.bluetoothLeScanner = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeScanner() : null;
    }
    
    /**
     * Starts scanning for all BLE devices
     */
    public void startScanning(ScanResultListener listener) {
        this.scanResultListener = listener;
        
        if (!canScan()) {
            Log.e(TAG, "Cannot start scanning - requirements not met");
            if (scanResultListener != null) {
                scanResultListener.onScanError(-1);
            }
            return;
        }
        
        if (isScanning) {
            Log.w(TAG, "Already scanning - stopping previous scan");
            stopScanning();
        }
        
        Log.d(TAG, "🔍 Starting simple BLE scanning for all devices...");
        
        // Scan settings - fast, aggressive scanning
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0) // Report immediately
                .build();
        
        bluetoothLeScanner.startScan(null, settings, scanCallback);
        isScanning = true;
        
        Log.d(TAG, "✅ BLE scanning started");
        if (targetAddresses.size() > 0) {
            Log.d(TAG, "🎯 Looking for target addresses: " + targetAddresses);
        }
    }
    
    /**
     * Starts scanning for specific device addresses
     */
    public void startScanningForAddresses(List<String> addresses, ScanResultListener listener) {
        this.targetAddresses = new ArrayList<>(addresses);
        Log.d(TAG, "🎯 Setting target addresses: " + targetAddresses);
        startScanning(listener);
    }
    
    /**
     * Starts scanning for a single device address
     */
    public void startScanningForAddress(String address, ScanResultListener listener) {
        List<String> addresses = new ArrayList<>();
        addresses.add(address);
        startScanningForAddresses(addresses, listener);
    }
    
    /**
     * Stops BLE scanning
     */
    public void stopScanning() {
        if (bluetoothLeScanner != null && isScanning) {
            Log.d(TAG, "🛑 Stopping BLE scanning");
            bluetoothLeScanner.stopScan(scanCallback);
            isScanning = false;
        }
    }
    
    /**
     * Checks if scanning is possible on this device
     */
    public boolean canScan() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter not available");
            return false;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            return false;
        }
        
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not available");
            return false;
        }
        
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Bluetooth LE not supported");
            return false;
        }
        
        return true;
    }
    
    /**
     * Processes individual scan results
     */
    private void processScanResult(ScanResult result) {
        if (result == null || result.getDevice() == null) {
            return;
        }
        
        String deviceAddress = result.getDevice().getAddress();
        String deviceName = result.getDevice().getName();
        int rssi = result.getRssi();
        
        Log.v(TAG, "📱 Device found: " + 
            (deviceName != null ? deviceName : "Unknown") + 
            " (" + deviceAddress + ") RSSI: " + rssi + " dBm");
        
        // Check if this is a target device
        boolean isTargetDevice = targetAddresses.contains(deviceAddress);
        
        // Extract device info from manufacturer data if available
        String extractedDeviceInfo = extractDeviceInfo(result.getScanRecord());
        
        if (isTargetDevice) {
            Log.i(TAG, "🎯 TARGET DEVICE FOUND!");
            Log.i(TAG, "  Address: " + deviceAddress);
            Log.i(TAG, "  Name: " + (deviceName != null ? deviceName : "Unknown"));
            Log.i(TAG, "  RSSI: " + rssi + " dBm");
            if (extractedDeviceInfo != null) {
                Log.i(TAG, "  Manufacturer Data: " + extractedDeviceInfo);
            }
            
            if (scanResultListener != null) {
                scanResultListener.onTargetDeviceFound(result, deviceAddress, deviceName);
            }
        } else {
            Log.v(TAG, "  Other device - not in target list");
            
            if (scanResultListener != null) {
                scanResultListener.onOtherDeviceFound(result);
            }
        }
        
        // Log scan record details for debugging
        logScanRecordDetails(result.getScanRecord(), deviceAddress);
    }
    
    /**
     * Extracts device info from manufacturer data
     */
    private String extractDeviceInfo(ScanRecord scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        
        SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
        if (manufacturerData != null && manufacturerData.size() > 0) {
            // Look for our custom manufacturer ID (0xFFFF)
            byte[] ourData = manufacturerData.get(0xFFFF);
            if (ourData != null) {
                try {
                    return new String(ourData, "UTF-8");
                } catch (Exception e) {
                    Log.w(TAG, "Could not decode manufacturer data: " + e.getMessage());
                }
            }
        }
        
        return null;
    }
    
    /**
     * Logs detailed scan record information
     */
    private void logScanRecordDetails(ScanRecord scanRecord, String deviceAddress) {
        if (scanRecord == null) return;
        
        Log.v(TAG, "=== SCAN RECORD for " + deviceAddress + " ===");
        
        // Device name
        String deviceName = scanRecord.getDeviceName();
        if (deviceName != null) {
            Log.v(TAG, "  Device Name: " + deviceName);
        }
        
        // Manufacturer data
        SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
        if (manufacturerData != null && manufacturerData.size() > 0) {
            Log.v(TAG, "  Manufacturer Data (" + manufacturerData.size() + " entries):");
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
                Log.v(TAG, "    Manufacturer ID 0x" + Integer.toHexString(key) + ": " + hexData + textData);
            }
        }
        
        // TX power level
        Integer txPowerLevel = scanRecord.getTxPowerLevel();
        if (txPowerLevel != null) {
            Log.v(TAG, "  TX Power Level: " + txPowerLevel + " dBm");
        }
        
        Log.v(TAG, "  ────────────────────");
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
    
    public void addTargetAddress(String address) {
        if (!targetAddresses.contains(address)) {
            targetAddresses.add(address);
            Log.d(TAG, "Added target address: " + address);
        }
    }
    
    public void removeTargetAddress(String address) {
        targetAddresses.remove(address);
        Log.d(TAG, "Removed target address: " + address);
    }
    
    public void clearTargetAddresses() {
        targetAddresses.clear();
        Log.d(TAG, "Cleared all target addresses");
    }
    
    public List<String> getTargetAddresses() {
        return new ArrayList<>(targetAddresses);
    }
    
    public void cleanup() {
        if (isScanning) {
            stopScanning();
        }
    }
}
