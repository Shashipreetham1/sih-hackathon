package com.hackathon.attendlytics;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;

/**
 * Comprehensive BLE system diagnostics and debugging tool
 * Use this to identify exactly what's wrong with BLE implementation
 */
public class BLESystemDiagnostics {
    private static final String TAG = "BLESystemDiagnostics";
    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    
    public BLESystemDiagnostics(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }
    
    /**
     * Runs comprehensive BLE system diagnostics
     * Returns a detailed report of the system status
     */
    public String runFullDiagnostics() {
        StringBuilder report = new StringBuilder();
        report.append("🔍 COMPREHENSIVE BLE SYSTEM DIAGNOSTICS\n");
        report.append("=====================================\n\n");
        
        // 1. Hardware Support Check
        report.append("1. 📱 HARDWARE SUPPORT\n");
        report.append(checkHardwareSupport()).append("\n");
        
        // 2. Permissions Check
        report.append("2. 🔐 PERMISSIONS STATUS\n");
        report.append(checkPermissions()).append("\n");
        
        // 3. Bluetooth Adapter Status
        report.append("3. 📡 BLUETOOTH ADAPTER\n");
        report.append(checkBluetoothAdapter()).append("\n");
        
        // 4. BLE Advertising Support
        report.append("4. 📢 BLE ADVERTISING\n");
        report.append(checkAdvertisingSupport()).append("\n");
        
        // 5. Device Information
        report.append("5. 📋 DEVICE INFORMATION\n");
        report.append(checkDeviceInfo()).append("\n");
        
        // 6. Known Issues and Solutions
        report.append("6. 🔧 TROUBLESHOOTING\n");
        report.append(getTroubleshootingAdvice()).append("\n");
        
        // Log the full report
        Log.i(TAG, report.toString());
        
        return report.toString();
    }
    
    private String checkHardwareSupport() {
        StringBuilder result = new StringBuilder();
        
        // Check BLE feature
        boolean bleSupported = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        result.append(bleSupported ? "✅" : "❌").append(" Bluetooth LE Support: ").append(bleSupported).append("\n");
        
        // Check Bluetooth feature
        boolean bluetoothSupported = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        result.append(bluetoothSupported ? "✅" : "❌").append(" Bluetooth Support: ").append(bluetoothSupported).append("\n");
        
        // Android version
        int apiLevel = android.os.Build.VERSION.SDK_INT;
        result.append("📱 Android API Level: ").append(apiLevel);
        if (apiLevel < 21) {
            result.append(" ❌ (BLE advertising requires API 21+)");
        } else {
            result.append(" ✅");
        }
        result.append("\n");
        
        return result.toString();
    }
    
    private String checkPermissions() {
        StringBuilder result = new StringBuilder();
        
        // Android 12+ permissions
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            String[] newPermissions = {
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            };
            
            for (String permission : newPermissions) {
                boolean granted = ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
                String permissionName = permission.substring(permission.lastIndexOf('.') + 1);
                result.append(granted ? "✅" : "❌").append(" ").append(permissionName).append(": ").append(granted ? "GRANTED" : "DENIED").append("\n");
            }
        }
        
        // Location permissions (required for BLE on older Android)
        String[] locationPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };
        
        for (String permission : locationPermissions) {
            boolean granted = ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            String permissionName = permission.substring(permission.lastIndexOf('.') + 1);
            result.append(granted ? "✅" : "❌").append(" ").append(permissionName).append(": ").append(granted ? "GRANTED" : "DENIED").append("\n");
        }
        
        return result.toString();
    }
    
    private String checkBluetoothAdapter() {
        StringBuilder result = new StringBuilder();
        
        if (bluetoothManager == null) {
            result.append("❌ BluetoothManager: null (Bluetooth not supported)\n");
            return result.toString();
        }
        
        result.append("✅ BluetoothManager: Available\n");
        
        if (bluetoothAdapter == null) {
            result.append("❌ BluetoothAdapter: null (Bluetooth not supported)\n");
            return result.toString();
        }
        
        result.append("✅ BluetoothAdapter: Available\n");
        result.append("📊 Bluetooth Enabled: ").append(bluetoothAdapter.isEnabled() ? "✅ YES" : "❌ NO").append("\n");
        
        // Device name and address (with permission handling)
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                String deviceName = bluetoothAdapter.getName();
                result.append("📱 Device Name: ").append(deviceName != null ? deviceName : "Unknown").append("\n");
                
                String deviceAddress = bluetoothAdapter.getAddress();
                result.append("🔗 Device Address: ").append(deviceAddress != null ? deviceAddress : "Unknown").append("\n");
            } else {
                result.append("⚠️ Device Name/Address: Not available (missing BLUETOOTH_CONNECT permission)\n");
            }
        } catch (SecurityException e) {
            result.append("⚠️ Device Name/Address: Security exception - ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
    }
    
    private String checkAdvertisingSupport() {
        StringBuilder result = new StringBuilder();
        
        if (bluetoothAdapter == null) {
            result.append("❌ Cannot check advertising - BluetoothAdapter is null\n");
            return result.toString();
        }
        
        // Multiple advertisement support
        boolean multipleAdvSupported = bluetoothAdapter.isMultipleAdvertisementSupported();
        result.append("📢 Multiple Advertisement Support: ").append(multipleAdvSupported ? "✅ YES" : "❌ NO").append("\n");
        
        // BLE Advertiser availability
        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        result.append("🔊 BluetoothLeAdvertiser: ").append(advertiser != null ? "✅ Available" : "❌ Not Available").append("\n");
        
        // LE Extended Advertising support (Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            try {
                boolean extendedAdvertisingSupported = bluetoothAdapter.isLeExtendedAdvertisingSupported();
                result.append("📡 LE Extended Advertising: ").append(extendedAdvertisingSupported ? "✅ YES" : "❌ NO").append("\n");
            } catch (Exception e) {
                result.append("📡 LE Extended Advertising: ⚠️ Cannot determine\n");
            }
        }
        
        return result.toString();
    }
    
    private String checkDeviceInfo() {
        StringBuilder result = new StringBuilder();
        
        result.append("🏭 Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n");
        result.append("📱 Model: ").append(android.os.Build.MODEL).append("\n");
        result.append("🔢 Android Version: ").append(android.os.Build.VERSION.RELEASE).append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
        result.append("🏗️ Build: ").append(android.os.Build.ID).append("\n");
        
        return result.toString();
    }
    
    private String getTroubleshootingAdvice() {
        StringBuilder advice = new StringBuilder();
        
        advice.append("💡 COMMON ISSUES & SOLUTIONS:\n\n");
        
        // Issue 1: Same device scanning
        advice.append("1. 🔍 Same-Device BLE Scanning:\n");
        advice.append("   ❌ Problem: Many Android devices cannot scan their own BLE advertisements\n");
        advice.append("   ✅ Solution: Test with TWO separate devices\n");
        advice.append("   📱 Device A: Start attendance (advertises)\n");
        advice.append("   📱 Device B: Scan for proximity (scans)\n\n");
        
        // Issue 2: Privacy and MAC address
        advice.append("2. 🔒 Bluetooth Address Privacy:\n");
        advice.append("   ❌ Problem: Modern Android uses random MAC addresses for privacy\n");
        advice.append("   ✅ Solution: Don't rely on MAC addresses - use Service UUIDs\n");
        advice.append("   🎯 Focus: Service UUID 0000C0DE-0000-1000-8000-00805F9B34FB\n\n");
        
        // Issue 3: Permissions
        advice.append("3. 🔐 Permission Issues:\n");
        advice.append("   ❌ Problem: Missing or denied Bluetooth permissions\n");
        advice.append("   ✅ Solution: Grant ALL Bluetooth and Location permissions\n");
        advice.append("   📋 Required: BLUETOOTH_ADVERTISE, BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION\n\n");
        
        // Issue 4: Device compatibility
        advice.append("4. 📱 Device Compatibility:\n");
        advice.append("   ❌ Problem: Not all devices support BLE advertising\n");
        advice.append("   ✅ Solution: Test on multiple devices, especially newer Android phones\n");
        advice.append("   🏆 Best: Samsung Galaxy, Google Pixel, OnePlus devices\n\n");
        
        // Issue 5: Testing tools
        advice.append("5. 🔧 External Testing Tools:\n");
        advice.append("   📱 Install 'BLE Scanner' app from Google Play Store\n");
        advice.append("   🔍 Look for Service UUID: 0000C0DE-0000-1000-8000-00805F9B34FB\n");
        advice.append("   ✅ This confirms if your advertising is actually working\n\n");
        
        return advice.toString();
    }
    
    /**
     * Quick check if BLE advertising should work on this device
     */
    public boolean isBLEAdvertisingLikelyToWork() {
        // Basic requirements
        boolean bleSupported = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        boolean apiSupported = android.os.Build.VERSION.SDK_INT >= 21;
        boolean adapterAvailable = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        boolean advertiserAvailable = bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeAdvertiser() != null;
        boolean multipleAdvSupported = bluetoothAdapter != null && bluetoothAdapter.isMultipleAdvertisementSupported();
        
        boolean permissionsOk = true;
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            permissionsOk = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
        }
        
        return bleSupported && apiSupported && adapterAvailable && advertiserAvailable && multipleAdvSupported && permissionsOk;
    }
    
    /**
     * Gets a simple status summary
     */
    public String getQuickStatus() {
        if (isBLEAdvertisingLikelyToWork()) {
            return "✅ BLE Advertising should work on this device";
        } else {
            return "❌ BLE Advertising may not work - run full diagnostics";
        }
    }
}