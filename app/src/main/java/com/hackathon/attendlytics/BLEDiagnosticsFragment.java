package com.hackathon.attendlytics;

import android.Manifest;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class BLEDiagnosticsFragment extends Fragment implements BLEScannerManager.ScanResultListener {
    private static final String TAG = "BLEDiagnostics";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;
    
    private BLEScannerManager bleScannerManager;
    private Button buttonStartScan, buttonStopScan, buttonScanAll;
    private TextView textViewResults, textViewStatus;
    private StringBuilder scanResults;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ble_diagnostics, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components
        buttonStartScan = view.findViewById(R.id.buttonStartScan);
        buttonStopScan = view.findViewById(R.id.buttonStopScan);
        buttonScanAll = view.findViewById(R.id.buttonScanAll);
        Button buttonCheckStatus = view.findViewById(R.id.buttonCheckStatus);
        Button buttonRequestPermissions = view.findViewById(R.id.buttonRequestPermissions);
        textViewResults = view.findViewById(R.id.textViewResults);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        
        // Initialize scanner
        bleScannerManager = new BLEScannerManager(getContext());
        scanResults = new StringBuilder();
        
        // Set up button listeners
        buttonStartScan.setOnClickListener(v -> startTargetedScan());
        buttonStopScan.setOnClickListener(v -> stopScan());
        buttonScanAll.setOnClickListener(v -> startAllDevicesScan());
        buttonCheckStatus.setOnClickListener(v -> checkSystemStatus());
        buttonRequestPermissions.setOnClickListener(v -> requestBluetoothPermissions());
        
        updateUI();
        displayDiagnosticInfo();
    }
    
    private void startTargetedScan() {
        Log.d(TAG, "Starting targeted scan for Service UUID: " + bleScannerManager.getTargetServiceUUID());
        
        if (!checkBluetoothPermissions()) {
            Log.w(TAG, "Missing Bluetooth permissions, requesting...");
            requestBluetoothPermissions();
            return;
        }
        
        if (!bleScannerManager.canScan()) {
            String errorMsg = "Cannot scan - BLE not ready";
            Log.e(TAG, errorMsg);
            textViewStatus.setText("Error: BLE not available");
            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            return;
        }
        
        scanResults.setLength(0);
        scanResults.append("🎯 TARGETED SCAN RESULTS (Service UUID: ").append(bleScannerManager.getTargetServiceUUID()).append(")\n");
        scanResults.append("=====================================\n");
        
        // Show current global session status
        GlobalBLEManager globalManager = GlobalBLEManager.getInstance();
        scanResults.append("Global Session Active: ").append(globalManager.isSessionActive()).append("\n");
        if (globalManager.isSessionActive()) {
            scanResults.append("Current Session ID: ").append(globalManager.getCurrentSessionId()).append("\n");
        }
        scanResults.append("Scan started at: ").append(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");
        
        try {
            bleScannerManager.startScanning(this);
            updateUI();
            textViewStatus.setText("Scanning for target UUID...");
            textViewResults.setText(scanResults.toString());
            
            Toast.makeText(getContext(), "Started targeted scan for your app's UUID", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Scan started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting scan: " + e.getMessage(), e);
            textViewStatus.setText("Scan error: " + e.getMessage());
            Toast.makeText(getContext(), "Error starting scan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void startAllDevicesScan() {
        Log.d(TAG, "Starting scan for all BLE devices");
        
        if (!checkBluetoothPermissions()) {
            Log.w(TAG, "Missing Bluetooth permissions, requesting...");
            requestBluetoothPermissions();
            return;
        }
        
        if (!bleScannerManager.canScan()) {
            String errorMsg = "Cannot scan - BLE not ready";
            Log.e(TAG, errorMsg);
            textViewStatus.setText("Error: BLE not available");
            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            return;
        }
        
        scanResults.setLength(0);
        scanResults.append("📱 ALL DEVICES SCAN RESULTS\n");
        scanResults.append("===========================\n");
        scanResults.append("Scan started at: ").append(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");
        
        try {
            bleScannerManager.startScanningAll(this);
            updateUI();
            textViewStatus.setText("Scanning all BLE devices...");
            textViewResults.setText(scanResults.toString());
            
            Toast.makeText(getContext(), "Started scanning all BLE devices", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "All devices scan started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting all devices scan: " + e.getMessage(), e);
            textViewStatus.setText("Scan error: " + e.getMessage());
            Toast.makeText(getContext(), "Error starting scan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopScan() {
        Log.d(TAG, "Stopping BLE scan");
        bleScannerManager.stopScanning();
        updateUI();
        textViewStatus.setText("Scan stopped");
        Toast.makeText(getContext(), "Scan stopped", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onTargetDeviceFound(ScanResult result, String sessionId) {
        Log.i(TAG, "🎯 TARGET DEVICE FOUND!");
        
        String deviceInfo = String.format(
            "✅ MATCH FOUND!\n" +
            "Device: %s\n" +
            "Address: %s\n" +
            "RSSI: %d dBm\n" +
            "Session ID: %s\n" +
            "Time: %s\n" +
            "Service UUID: %s\n\n",
            result.getDevice().getName() != null ? result.getDevice().getName() : "Unknown",
            result.getDevice().getAddress(),
            result.getRssi(),
            sessionId != null ? sessionId : "Not available",
            new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date()),
            bleScannerManager.getTargetServiceUUID()
        );
        
        scanResults.append(deviceInfo);
        
        getActivity().runOnUiThread(() -> {
            textViewResults.setText(scanResults.toString());
            Toast.makeText(getContext(), "🎯 Your app's advertisement found!", Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onOtherDeviceFound(ScanResult result) {
        String deviceName = result.getDevice().getName() != null ? result.getDevice().getName() : "Unknown";
        String deviceAddress = result.getDevice().getAddress();
        int rssi = result.getRssi();
        
        // Extract service UUIDs if available
        StringBuilder serviceUuids = new StringBuilder();
        if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
            for (android.os.ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
                if (serviceUuids.length() > 0) serviceUuids.append(", ");
                serviceUuids.append(uuid.toString());
            }
        }
        
        String deviceInfo = String.format(
            "📱 Device: %s\n" +
            "   Address: %s\n" +
            "   RSSI: %d dBm\n" +
            "   Services: %s\n" +
            "   Time: %s\n\n",
            deviceName,
            deviceAddress,
            rssi,
            serviceUuids.length() > 0 ? serviceUuids.toString() : "None advertised",
            new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date())
        );
        
        scanResults.append(deviceInfo);
        
        getActivity().runOnUiThread(() -> textViewResults.setText(scanResults.toString()));
    }
    
    @Override
    public void onScanError(int errorCode) {
        String errorMsg = "Scan failed with error code: " + errorCode;
        Log.e(TAG, errorMsg);
        
        getActivity().runOnUiThread(() -> {
            textViewStatus.setText("Scan error: " + errorCode);
            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
        });
    }
    
    private void updateUI() {
        boolean isScanning = bleScannerManager.isScanning();
        buttonStartScan.setEnabled(!isScanning);
        buttonScanAll.setEnabled(!isScanning);
        buttonStopScan.setEnabled(isScanning);
        
        if (!isScanning) {
            textViewStatus.setText("Ready to scan");
        }
    }
    
    private void displayDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== BLE DIAGNOSTIC INFORMATION ===\n");
        
        // Show current Service UUID configuration
        info.append("📡 SERVICE UUID CONFIGURATION:\n");
        info.append("Current UUID: ").append(BLEConfig.SERVICE_UUID).append("\n");
        info.append("Display Name: ").append(BLEConfig.getServiceUUIDisplayName()).append("\n");
        info.append("Valid Format: ").append(BLEConfig.isCurrentUUIValid() ? "✅ YES" : "❌ NO").append("\n\n");
        
        info.append("Target Service UUID: ").append(bleScannerManager.getTargetServiceUUID()).append("\n");
        info.append("Scanner Ready: ").append(bleScannerManager.canScan()).append("\n");
        info.append("Bluetooth Permissions: ").append(checkBluetoothPermissions()).append("\n");
        info.append("Android Version: ").append(android.os.Build.VERSION.SDK_INT).append("\n\n");
        
        // Show global session status
        GlobalBLEManager globalManager = GlobalBLEManager.getInstance();
        info.append("Global Session Active: ").append(globalManager.isSessionActive()).append("\n");
        if (globalManager.isSessionActive()) {
            info.append("Current Session ID: ").append(globalManager.getCurrentSessionId()).append("\n");
        }
        
        info.append("Current Time: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");
        
        info.append("🎛️ UUID CUSTOMIZATION:\n");
        info.append("To change your Service UUID:\n");
        info.append("1. Open BLEConfig.java\n");
        info.append("2. Change SERVICE_UUID to any PREDEFINED_UUIDS option\n");
        info.append("3. Or use your own custom UUID\n");
        info.append("4. Rebuild the app\n\n");
        
        info.append("📱 AVAILABLE UUID OPTIONS:\n");
        info.append("CURRENT: ").append(BLEConfig.PREDEFINED_UUIDS.CURRENT).append(" (Default)\n");
        info.append("ATTEND: ").append(BLEConfig.PREDEFINED_UUIDS.ATTEND).append("\n");
        info.append("CLASS: ").append(BLEConfig.PREDEFINED_UUIDS.CLASS).append("\n");
        info.append("HACKATHON: ").append(BLEConfig.PREDEFINED_UUIDS.HACKATHON).append("\n");
        info.append("SIH_2025: ").append(BLEConfig.PREDEFINED_UUIDS.SIH_2025).append("\n\n");
        
        info.append("TESTING STEPS:\n");
        info.append("1. Grant all permissions when prompted\n");
        info.append("2. Go back to Teacher -> Start Attendance\n");
        info.append("3. Return here and tap 'Scan for Target'\n");
        info.append("4. You should see your own device advertising\n");
        info.append("5. Try 'Scan All Devices' to see other BLE devices\n\n");
        
        info.append("TROUBLESHOOTING:\n");
        info.append("• If permissions denied, check app settings\n");
        info.append("• Make sure Bluetooth is enabled\n");
        info.append("• Try 'Scan All Devices' first to test basic scanning\n");
        info.append("• Look for devices with your Service UUID\n\n");
        
        info.append("Expected Service UUID: ").append(bleScannerManager.getTargetServiceUUID()).append("\n\n");
        
        textViewResults.setText(info.toString());
    }
    
    private boolean checkBluetoothPermissions() {
        boolean hasPermissions = false;
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+
            boolean scanPermission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean connectPermission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            hasPermissions = scanPermission && connectPermission;
            
            Log.d(TAG, "Android 12+ permissions - SCAN: " + scanPermission + ", CONNECT: " + connectPermission);
        } else {
            // Android 11 and below
            boolean locationPermission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            hasPermissions = locationPermission;
            
            Log.d(TAG, "Android 11- permissions - LOCATION: " + locationPermission);
        }
        
        Log.d(TAG, "Overall permissions check result: " + hasPermissions);
        return hasPermissions;
    }
    
    private void requestBluetoothPermissions() {
        Log.d(TAG, "Requesting Bluetooth permissions");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            }, BLUETOOTH_PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, BLUETOOTH_PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Bluetooth permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Bluetooth permissions required for scanning", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bleScannerManager != null) {
            bleScannerManager.cleanup();
        }
    }
    
    private void checkSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== SYSTEM STATUS CHECK ===\n");
        status.append("Time: ").append(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");
        
        // Run comprehensive BLE diagnostics
        BLESystemDiagnostics diagnostics = new BLESystemDiagnostics(getContext());
        status.append(diagnostics.runFullDiagnostics());
        
        textViewResults.setText(status.toString());
        textViewStatus.setText("Comprehensive system diagnostics completed");
        
        Log.d(TAG, "System status check completed");
        Toast.makeText(getContext(), "System diagnostics updated - check console logs for details", Toast.LENGTH_LONG).show();
    }
}