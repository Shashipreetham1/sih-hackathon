package com.hackathon.attendlytics;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.hackathon.attendlytics.databinding.FragmentStudentBinding;
import com.hackathon.attendlytics.R; // Added this import

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public class StudentFragment extends Fragment {

    private static final String TAG = "StudentFragment";
    private FragmentStudentBinding binding;
    private ActivityResultLauncher<Intent> qrScannerLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private String scannedSessionId = null;
    private ParcelUuid scannedServiceUUID = null;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private boolean isGeneralScanning = false;
    private Handler scanHandler;
    private final List<Integer> rssiValues = new ArrayList<>();
    private final List<String> discoveredDevices = new ArrayList<>();
    private ArrayAdapter<String> devicesAdapter;
    
    // Teacher device detection variables
    private boolean teacherDeviceFound = false;
    private boolean proximityVerified = false;
    private String teacherDeviceAddress = null;
    private int currentRSSI = 0;
    private boolean teacherDeviceCallbackTriggered = false;

    // Define a default UUID for fallback. This will be overridden by QR code data.
    private static final ParcelUuid DEFAULT_SERVICE_UUID = new ParcelUuid(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

    private static final long SCAN_PERIOD = 5000; // 5 seconds for RSSI collection
    private static final long SCAN_TIMEOUT = 15000; // 15 seconds overall to find device

    // Step 4: Proximity Verification thresholds
    private static final int PROXIMITY_RSSI_THRESHOLD = -60; // Close range threshold (stronger signal = closer)
    private static final int RSSI_VARIANCE_THRESHOLD = 5;
    private static final String TEACHER_TAG = "TEACHER"; // Service Data must contain this tag

    private static final String[] BLE_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final String[] ANDROID_11_BLE_PERMISSIONS = { Manifest.permission.ACCESS_FINE_LOCATION };

    public StudentFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BluetoothManager bluetoothManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        scanHandler = new Handler(Looper.getMainLooper());

        setupActivityResultLaunchers();
    }

    private void setupActivityResultLaunchers() {
        qrScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());
                    if (scanResult != null && scanResult.getContents() != null) {
                        // Step 1: QR Code Scanner Integration - Extract Service UUID
                        String qrContent = scanResult.getContents();
                        Log.d(TAG, "Step 1: QR Code scanned: " + qrContent);
                        
                        // Parse JSON from QR code to extract Service UUID
                        try {
                            JSONObject qrData = new JSONObject(qrContent);
                            scannedSessionId = qrData.getString("sessionId");
                            String serviceUUIDString = qrData.getString("serviceUUID");
                            scannedServiceUUID = new ParcelUuid(UUID.fromString(serviceUUIDString));
                            
                            // Display extracted information
                            binding.textviewSessionId.setText("Session: " + scannedSessionId + "\nService UUID: " + serviceUUIDString);
                            binding.buttonVerifyProximity.setEnabled(true);
                            binding.textviewStatusMessage.setText("Status: Service UUID extracted. Ready for proximity verification.");
                            Toast.makeText(getContext(), "Service UUID extracted: " + serviceUUIDString, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Step 1 Complete - SessionID: " + scannedSessionId + ", ServiceUUID: " + serviceUUIDString);
                            
                        } catch (JSONException e) {
                            Log.e(TAG, "Step 1 Failed - QR code JSON parsing error: " + e.getMessage());
                            // Fallback: try to extract UUID from plain text
                            if (qrContent.contains("-")) {
                                // Assume it's a plain UUID string
                                try {
                                    scannedServiceUUID = new ParcelUuid(UUID.fromString(qrContent));
                                    scannedSessionId = "DEFAULT_SESSION";
                                    binding.textviewSessionId.setText("Service UUID: " + qrContent + "\n(Plain UUID format)");
                                    binding.buttonVerifyProximity.setEnabled(true);
                                    binding.textviewStatusMessage.setText("Status: Plain UUID extracted. Ready for proximity verification.");
                                    Toast.makeText(getContext(), "UUID extracted: " + qrContent, Toast.LENGTH_LONG).show();
                                    Log.d(TAG, "Step 1 Complete (fallback) - Plain UUID: " + qrContent);
                                } catch (IllegalArgumentException uuidError) {
                                    Log.e(TAG, "Step 1 Failed - Invalid UUID format: " + uuidError.getMessage());
                                    binding.textviewStatusMessage.setText("Status: Invalid QR code format.");
                                    Toast.makeText(getContext(), "Invalid QR code format", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            } else {
                                binding.textviewStatusMessage.setText("Status: Invalid QR code format.");
                                Toast.makeText(getContext(), "Invalid QR code format", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Scan Cancelled", Toast.LENGTH_LONG).show();
                        binding.textviewStatusMessage.setText("Status: QR scan cancelled.");
                    }
                } else {
                    Toast.makeText(getContext(), "Scan Failed or Cancelled", Toast.LENGTH_LONG).show();
                    binding.textviewStatusMessage.setText("Status: QR scan failed or cancelled.");
                }
            }
        );

        requestPermissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissionsResult -> {
                boolean allGranted = true;
                for (Boolean granted : permissionsResult.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Log.d(TAG, "Step 2 Complete - Bluetooth and Location permissions granted");
                    binding.textviewStatusMessage.setText("Status: Step 2 Complete - Permissions granted. Initializing BLE Scanner...");
                    initializeAndStartTeacherDeviceDetection();
                    // Also start general scan if not already running for debugging
                    if (!isGeneralScanning) {
                        startGeneralBleScan();
                    }
                } else {
                    binding.textviewStatusMessage.setText("Status: BLE permissions denied. Cannot verify proximity.");
                    Toast.makeText(getContext(), "BLE Permissions are required.", Toast.LENGTH_LONG).show();
                    binding.buttonVerifyProximity.setEnabled(true);
                    binding.progressbarScanning.setVisibility(View.GONE);
                }
            }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentBinding.inflate(inflater, container, false);
        
        // Initialize devices list view for showing discovered BLE devices
        ListView devicesListView = binding.listviewDiscoveredDevices;
        discoveredDevices.clear();
        devicesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, discoveredDevices);
        devicesListView.setAdapter(devicesAdapter);
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textviewStatusMessage.setText("Status: Waiting to scan QR.");
        binding.buttonScanQr.setOnClickListener(v -> {
            binding.textviewStatusMessage.setText("Status: Initiating QR scan...");
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(StudentFragment.this);
            integrator.setPrompt("Scan QR Code for Session ID");
            integrator.setOrientationLocked(false);
            integrator.setBeepEnabled(true);
            qrScannerLauncher.launch(integrator.createScanIntent());
        });

        binding.buttonVerifyProximity.setEnabled(false);
        binding.buttonVerifyProximity.setOnClickListener(v -> {
            if (scannedSessionId == null || scannedSessionId.isEmpty() || scannedServiceUUID == null) {
                Toast.makeText(getContext(), "Please scan a QR code first.", Toast.LENGTH_SHORT).show();
                binding.textviewStatusMessage.setText("Status: Please scan QR first.");
                return;
            }
            
            // Step 2: BLE Scanner Implementation
            Log.d(TAG, "Step 2: Starting BLE Scanner Implementation");
            binding.buttonVerifyProximity.setEnabled(false);
            binding.progressbarScanning.setVisibility(View.VISIBLE);
            binding.textviewStatusMessage.setText("Status: Step 2 - Requesting Bluetooth and Location permissions...");
            
            // Request Bluetooth and Location permissions
            requestBlePermissions();
        });

        binding.buttonRefreshDevices.setOnClickListener(v -> {
            Log.d(TAG, "Refresh devices button clicked");
            
            // Check if Bluetooth is enabled
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                binding.textviewStatusMessage.setText("Status: Bluetooth not enabled. Please enable Bluetooth.");
                Toast.makeText(getContext(), "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Initialize scanner if not already done
            if (bluetoothLeScanner == null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                Log.d(TAG, "BluetoothLeScanner re-initialized: " + (bluetoothLeScanner != null));
            }
            
            // Check permissions
            if (!hasRequiredPermissions()) {
                Log.d(TAG, "Requesting permissions for device refresh");
                binding.textviewStatusMessage.setText("Status: Requesting BLE permissions...");
                requestBlePermissionsForRefresh();
                return;
            }
            
            if (!isGeneralScanning) {
                binding.textviewStatusMessage.setText("Status: Refreshing device list...");
                startGeneralBleScan();
            } else {
                Toast.makeText(getContext(), "Already scanning for devices", Toast.LENGTH_SHORT).show();
            }
        });

        // Start general device discovery immediately
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            Log.d(TAG, "BluetoothLeScanner initialized: " + (bluetoothLeScanner != null));
            // Check permissions first
            if (hasRequiredPermissions()) {
                Log.d(TAG, "Permissions available, starting general scan immediately");
                startGeneralBleScan();
            } else {
                Log.d(TAG, "Permissions not available, requesting them");
                binding.textviewStatusMessage.setText("Status: Need BLE permissions. Click 'Refresh Devices' to grant.");
            }
        } else {
            Log.w(TAG, "Bluetooth adapter not available or not enabled");
            binding.textviewStatusMessage.setText("Status: Bluetooth not enabled. Please enable Bluetooth.");
        }
    }

    private boolean hasRequiredPermissions() {
        String[] platformPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            platformPermissions = BLE_PERMISSIONS;
        } else {
            platformPermissions = ANDROID_11_BLE_PERMISSIONS;
        }

        for (String permission : platformPermissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestBlePermissionsForRefresh() {
        List<String> permissionsToRequest = new ArrayList<>();
        String[] platformPermissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            platformPermissions = BLE_PERMISSIONS;
        } else {
            platformPermissions = ANDROID_11_BLE_PERMISSIONS;
        }

        for (String permission : platformPermissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            // Create a permission launcher specifically for refresh
            ActivityResultLauncher<String[]> refreshPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissionsResult -> {
                    boolean allGranted = true;
                    for (Boolean granted : permissionsResult.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        binding.textviewStatusMessage.setText("Status: Permissions granted. Starting device scan...");
                        startGeneralBleScan();
                    } else {
                        binding.textviewStatusMessage.setText("Status: BLE permissions denied. Cannot scan for devices.");
                        Toast.makeText(getContext(), "BLE Permissions are required to scan for devices.", Toast.LENGTH_LONG).show();
                    }
                }
            );
            refreshPermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            startGeneralBleScan();
        }
    }

    private void requestBlePermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        String[] platformPermissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            platformPermissions = BLE_PERMISSIONS;
        } else {
            platformPermissions = ANDROID_11_BLE_PERMISSIONS; // ACCESS_FINE_LOCATION only
        }

        for (String permission : platformPermissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            binding.textviewStatusMessage.setText("Status: Permissions granted. Ready for QR scan...");
            // Permissions are ready, user can now scan QR code to start the workflow
        }
    }

    // Step 2 & 3: BLE Scanner Implementation and Device Detection Logic
    private void initializeAndStartTeacherDeviceDetection() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Step 2 Failed - Bluetooth not enabled");
            Toast.makeText(getContext(), "Bluetooth is not enabled. Please enable it.", Toast.LENGTH_LONG).show();
            binding.textviewStatusMessage.setText("Status: Step 2 Failed - Bluetooth not enabled.");
            binding.buttonVerifyProximity.setEnabled(true);
            binding.progressbarScanning.setVisibility(View.GONE);
            return;
        }

        // Initialize BluetoothLeScanner
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "Step 2 Failed - Failed to get BluetoothLeScanner");
            binding.textviewStatusMessage.setText("Status: Step 2 Failed - BLE scanner unavailable.");
            binding.buttonVerifyProximity.setEnabled(true);
            binding.progressbarScanning.setVisibility(View.GONE);
            return;
        }
        
        Log.d(TAG, "Step 2 Complete - BluetoothLeScanner initialized successfully");
        
        // Step 3: Start teacher device detection with specific UUID
        startTeacherDeviceDetection();
        
        // Also start general scan for debugging purposes
        startGeneralBleScan();
    }

    // Step 3: Device Detection Logic - Create ScanFilter with UUID and start scanning
    private void startTeacherDeviceDetection() {
        if (isScanning) {
            Log.d(TAG, "Teacher device scan already in progress.");
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e(TAG, "Step 3 Failed - BLUETOOTH_SCAN permission missing");
            binding.textviewStatusMessage.setText("Status: Step 3 Failed - Scan permission missing.");
            binding.buttonVerifyProximity.setEnabled(true);
            binding.progressbarScanning.setVisibility(View.GONE);
            return;
        }

        // Reset detection variables
        rssiValues.clear();
        teacherDeviceFound = false;
        proximityVerified = false;
        teacherDeviceAddress = null;
        currentRSSI = 0;
        teacherDeviceCallbackTriggered = false;
        
        Log.d(TAG, "Step 3: Starting teacher device detection with Service UUID: " + scannedServiceUUID.getUuid().toString());
        Log.i(TAG, "=== TEACHER DEVICE DETECTION STARTED ===");
        Log.i(TAG, "Target Service UUID: " + scannedServiceUUID.getUuid().toString());
        Log.i(TAG, "Looking for service data containing: " + TEACHER_TAG);
        Log.i(TAG, "========================================");
        
        // Create ScanFilter with the UUID from QR code
        ScanFilter teacherFilter = new ScanFilter.Builder()
                .setServiceUuid(scannedServiceUUID)  // Filter for exact Service UUID match
                .build();
        
        // Set scan settings to LOW_LATENCY mode for fast detection
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        isScanning = true;
        binding.progressbarScanning.setVisibility(View.VISIBLE);
        binding.textviewStatusMessage.setText("Status: Step 3 - Scanning for teacher device with UUID: " + scannedServiceUUID.getUuid().toString());
        
        try {
            // Start scanning with teacher device filter
            bluetoothLeScanner.startScan(Collections.singletonList(teacherFilter), scanSettings, teacherDeviceCallback);
            Log.d(TAG, "Step 3 - Teacher device scan started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Step 3 Failed - Failed to start teacher device scan: " + e.getMessage());
            isScanning = false;
            binding.textviewStatusMessage.setText("Status: Step 3 Failed - Cannot start scan");
            binding.buttonVerifyProximity.setEnabled(true);
            binding.progressbarScanning.setVisibility(View.GONE);
            return;
        }
        
        // Stop scan after timeout if teacher device not found
        scanHandler.postDelayed(() -> {
            if (isScanning && !teacherDeviceFound) {
                Log.w(TAG, "Step 3 Timeout - Teacher device not found within " + SCAN_TIMEOUT + "ms");
                Log.w(TAG, "=== DEBUGGING INFO ===");
                Log.w(TAG, "Target UUID: " + scannedServiceUUID.getUuid().toString());
                Log.w(TAG, "General scan found " + discoveredDevices.size() + " devices total");
                Log.w(TAG, "Teacher device callback was called: " + (teacherDeviceCallbackTriggered ? "YES" : "NO"));
                Log.w(TAG, "=====================");
                stopTeacherDeviceDetection();
                binding.textviewStatusMessage.setText("Status: Step 3 Failed - Teacher device not found (check device list below)");
                binding.progressbarScanning.setVisibility(View.GONE);
                binding.buttonVerifyProximity.setEnabled(true);
            }
        }, SCAN_TIMEOUT);
    }

    // Step 3 & 4: Teacher Device Detection and Proximity Verification Callback
    private final ScanCallback teacherDeviceCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            
            teacherDeviceCallbackTriggered = true;
            Log.d(TAG, "teacherDeviceCallback.onScanResult() called - Device detected!");
            
            if (result.getDevice() == null || result.getScanRecord() == null) {
                Log.w(TAG, "Teacher scan: Device or ScanRecord is null");
                return;
            }
            
            String deviceAddress = result.getDevice().getAddress();
            currentRSSI = result.getRssi();
            
            Log.d(TAG, "Step 3: Found device with matching Service UUID: " + deviceAddress + ", RSSI: " + currentRSSI);
            
            // Check Service Data contains "TEACHER" tag
            byte[] serviceData = result.getScanRecord().getServiceData(scannedServiceUUID);
            if (serviceData != null) {
                String serviceDataString = new String(serviceData, StandardCharsets.UTF_8);
                Log.d(TAG, "Step 3: Service data found: " + serviceDataString);
                
                // Verify Service Data contains "TEACHER" tag
                if (serviceDataString.contains(TEACHER_TAG)) {
                    Log.d(TAG, "Step 3 Complete - Teacher device found! Address: " + deviceAddress);
                    teacherDeviceFound = true;
                    teacherDeviceAddress = deviceAddress;
                    
                    // Step 4: Proximity Verification - Measure RSSI value
                    rssiValues.add(currentRSSI);
                    
                    requireActivity().runOnUiThread(() -> {
                        binding.textviewStatusMessage.setText("Status: Step 3 Complete - Teacher found! Step 4 - Verifying proximity... (" + rssiValues.size() + " readings)");
                    });
                    
                    // Collect multiple RSSI readings for accuracy
                    if (rssiValues.size() >= 3) {
                        // Stop scanning and evaluate proximity
                        stopTeacherDeviceDetection();
                        evaluateProximityAndSubmitAttendance();
                    }
                } else {
                    Log.d(TAG, "Step 3: Device found but service data doesn't contain TEACHER tag: " + serviceDataString);
                }
            } else {
                Log.d(TAG, "Step 3: Device found but no service data available");
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Step 3 Failed - Teacher device scan failed: " + errorCode);
            String errorMsg = getBleScanErrorMessage(errorCode);
            
            requireActivity().runOnUiThread(() -> {
                binding.textviewStatusMessage.setText("Status: Step 3 Failed - " + errorMsg);
                binding.progressbarScanning.setVisibility(View.GONE);
                binding.buttonVerifyProximity.setEnabled(true);
            });
            
            isScanning = false;
        }
    };

    // Step 4 & 5: Proximity Verification and Attendance Submission
    private void evaluateProximityAndSubmitAttendance() {
        if (rssiValues.isEmpty()) {
            Log.w(TAG, "Step 4 Failed - No RSSI values collected");
            requireActivity().runOnUiThread(() -> {
                binding.textviewStatusMessage.setText("Status: Step 4 Failed - No signal strength readings");
                binding.progressbarScanning.setVisibility(View.GONE);
                binding.buttonVerifyProximity.setEnabled(true);
            });
            return;
        }

        // Calculate average RSSI for proximity verification
        double sum = 0;
        for (int rssi : rssiValues) {
            sum += rssi;
        }
        double averageRSSI = sum / rssiValues.size();
        
        Log.d(TAG, "Step 4: Proximity verification - Average RSSI: " + averageRSSI + ", Threshold: " + PROXIMITY_RSSI_THRESHOLD);
        
        // Step 4: Confirm student is within acceptable distance
        if (averageRSSI > PROXIMITY_RSSI_THRESHOLD) {
            Log.d(TAG, "Step 4 Complete - Proximity verified! Student is close enough to teacher");
            proximityVerified = true;
            
            // Step 5: Attendance Submission
            submitAttendance(averageRSSI);
        } else {
            Log.w(TAG, "Step 4 Failed - Student too far from teacher. RSSI: " + averageRSSI + " (Required: > " + PROXIMITY_RSSI_THRESHOLD + ")");
            
            requireActivity().runOnUiThread(() -> {
                binding.textviewStatusMessage.setText(String.format("Status: Step 4 Failed - Too far from teacher (Signal: %.1f, Required: > %d)", averageRSSI, PROXIMITY_RSSI_THRESHOLD));
                Toast.makeText(getContext(), "Move closer to teacher and try again", Toast.LENGTH_LONG).show();
                binding.progressbarScanning.setVisibility(View.GONE);
                binding.buttonVerifyProximity.setEnabled(true);
            });
        }
    }

    // Step 5: Attendance Submission
    private void submitAttendance(double verifiedRSSI) {
        Log.d(TAG, "Step 5: Submitting attendance - Teacher found + Proximity confirmed");
        
        // Create attendance data
        String studentId = "STUDENT_" + System.currentTimeMillis(); // Replace with actual student ID
        String attendanceData = createAttendanceData(studentId, verifiedRSSI);
        
        Log.d(TAG, "Step 5: Attendance data: " + attendanceData);
        
        requireActivity().runOnUiThread(() -> {
            // Show success feedback
            binding.textviewStatusMessage.setText(String.format("Status: Step 5 Complete - Attendance submitted! (Signal: %.1f)", verifiedRSSI));
            Toast.makeText(getContext(), "Attendance submitted successfully!", Toast.LENGTH_LONG).show();
            binding.progressbarScanning.setVisibility(View.GONE);
            
            // Navigate to next screen (CaptchaFragment or success screen)
            try {
                NavHostFragment.findNavController(StudentFragment.this)
                        .navigate(R.id.studentToCaptcha);
            } catch (Exception e) {
                Log.w(TAG, "Navigation failed: " + e.getMessage());
                binding.buttonVerifyProximity.setEnabled(true);
            }
        });
    }

    private String createAttendanceData(String studentId, double rssi) {
        return "{" +
                "\"studentId\":\"" + studentId + "\"," +
                "\"sessionId\":\"" + scannedSessionId + "\"," +
                "\"serviceUUID\":\"" + scannedServiceUUID.getUuid().toString() + "\"," +
                "\"teacherDevice\":\"" + teacherDeviceAddress + "\"," +
                "\"rssi\":" + rssi + "," +
                "\"timestamp\":" + System.currentTimeMillis() + "," +
                "\"proximityVerified\":" + proximityVerified +
                "}";
    }

    private void stopTeacherDeviceDetection() {
        if (isScanning && bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.w(TAG, "BLUETOOTH_SCAN permission missing when trying to stop teacher device scan.");
            }
            Log.d(TAG, "Stopping teacher device detection scan");
            bluetoothLeScanner.stopScan(teacherDeviceCallback);
            isScanning = false;
            scanHandler.removeCallbacksAndMessages(null);
        }
    }

    private String getBleScanErrorMessage(int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "Scan already started";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "App registration failed";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "BLE feature unsupported";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "Internal error";
            default:
                return "Unknown error (" + errorCode + ")";
        }
    }

    private final ScanCallback generalScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "General scan callback triggered - Device found: " + (result.getDevice() != null ? result.getDevice().getAddress() : "null"));
            
            if (result.getDevice() != null) {
                String deviceInfo = formatDeviceInfo(result);
                
                // Avoid duplicates by checking if device already exists
                boolean deviceExists = false;
                for (String existingDevice : discoveredDevices) {
                    if (existingDevice.contains(result.getDevice().getAddress())) {
                        deviceExists = true;
                        break;
                    }
                }
                
                if (!deviceExists) {
                    discoveredDevices.add(deviceInfo);
                    Log.d(TAG, "Adding new device to list: " + result.getDevice().getAddress());
                    requireActivity().runOnUiThread(() -> {
                        devicesAdapter.notifyDataSetChanged();
                        binding.textviewStatusMessage.setText("Status: Found " + discoveredDevices.size() + " BLE devices");
                    });
                    Log.d(TAG, "General scan - New device discovered: " + deviceInfo);
                } else {
                    Log.d(TAG, "Device already exists in list: " + result.getDevice().getAddress());
                }
            } else {
                Log.w(TAG, "Scan result with null device");
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "General scan batch results: " + results.size() + " devices");
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "General BLE Scan Failed: " + errorCode);
            String errorMsg = "Unknown error";
            switch (errorCode) {
                case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                    errorMsg = "Scan already started";
                    break;
                case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    errorMsg = "Application registration failed";
                    break;
                case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    errorMsg = "Feature unsupported";
                    break;
                case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                    errorMsg = "Internal error";
                    break;
            }
            binding.textviewStatusMessage.setText("Status: BLE Scan failed - " + errorMsg);
            isGeneralScanning = false;
        }
    };

    // Old callback removed - using teacherDeviceCallback for the new workflow

    // Old methods removed - using new teacher device detection workflow

    private void startGeneralBleScan() {
        if (isGeneralScanning) {
            Log.d(TAG, "General scan already in progress.");
            return;
        }
        
        // Check if bluetoothLeScanner is available
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null, cannot start general scan");
            binding.textviewStatusMessage.setText("Status: BLE Scanner not available");
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.w(TAG, "BLUETOOTH_SCAN permission missing for general scan.");
            binding.textviewStatusMessage.setText("Status: BLE scan permission missing");
            return;
        }

        // Clear previous discoveries
        discoveredDevices.clear();
        requireActivity().runOnUiThread(() -> {
            devicesAdapter.notifyDataSetChanged();
        });
        
        // General scan settings for discovering all BLE devices
        ScanSettings generalScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        isGeneralScanning = true;
        Log.d(TAG, "Starting general BLE scan to discover all devices");
        binding.textviewStatusMessage.setText("Status: Scanning for BLE devices...");
        
        try {
            // Start scan without filters to discover all BLE devices
            bluetoothLeScanner.startScan(new ArrayList<>(), generalScanSettings, generalScanCallback);
            Log.d(TAG, "General BLE scan started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start general BLE scan: " + e.getMessage());
            isGeneralScanning = false;
            binding.textviewStatusMessage.setText("Status: Failed to start BLE scan");
            return;
        }
        
        // Stop general scan after the main scan period
        scanHandler.postDelayed(() -> {
            Log.d(TAG, "General scan timeout reached, stopping scan");
            stopGeneralBleScan();
        }, SCAN_TIMEOUT);
    }

    private void stopGeneralBleScan() {
        if (isGeneralScanning && bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.w(TAG, "BLUETOOTH_SCAN permission missing when trying to stop general scan.");
            }
            Log.d(TAG, "Stopping general BLE scan. Total devices found: " + discoveredDevices.size());
            bluetoothLeScanner.stopScan(generalScanCallback);
            isGeneralScanning = false;
            
            requireActivity().runOnUiThread(() -> {
                if (discoveredDevices.isEmpty()) {
                    binding.textviewStatusMessage.setText("Status: No BLE devices found. Try refreshing or check Bluetooth.");
                } else {
                    binding.textviewStatusMessage.setText("Status: Found " + discoveredDevices.size() + " BLE devices");
                }
            });
        }
    }

    private void stopBleScan() {
        stopTeacherDeviceDetection(); // Stop teacher device scan
        stopGeneralBleScan(); // Stop general scan
    }

    // Old evaluateRssi method removed - using new workflow in evaluateProximityAndSubmitAttendance

    private String getSessionLogInfo() {
        return "{\"serviceUUID\":\"" + (scannedServiceUUID != null ? scannedServiceUUID.getUuid().toString() : "null") + 
               "\",\"sessionId\":\"" + (scannedSessionId != null ? scannedSessionId : "null") + 
               "\",\"type\":\"BLE_ATTENDANCE\",\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String formatDeviceInfo(ScanResult result) {
        StringBuilder info = new StringBuilder();
        String deviceName = result.getDevice().getName();
        String deviceAddress = result.getDevice().getAddress();
        int rssi = result.getRssi();
        
        Log.d(TAG, "Formatting device info for: " + deviceAddress + ", Name: " + deviceName + ", RSSI: " + rssi);
        
        info.append(deviceName != null ? deviceName : "Unknown Device");
        info.append("\n").append(deviceAddress);
        info.append(" (RSSI: ").append(rssi).append(")");
        
        // Add service UUIDs if available
        if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
            info.append("\nServices: ");
            boolean hasTargetUUID = false;
            for (ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
                info.append("\n  ").append(uuid.getUuid().toString());
                
                // Check if this device has our target Service UUID
                if (scannedServiceUUID != null && uuid.equals(scannedServiceUUID)) {
                    hasTargetUUID = true;
                    info.append(" ★ TARGET UUID");
                    
                    // Check service data for TEACHER tag
                    byte[] serviceData = result.getScanRecord().getServiceData(scannedServiceUUID);
                    if (serviceData != null) {
                        String serviceDataString = new String(serviceData, StandardCharsets.UTF_8);
                        info.append("\n    Service Data: ").append(serviceDataString);
                        if (serviceDataString.contains(TEACHER_TAG)) {
                            info.append(" ★ TEACHER FOUND");
                        }
                    } else {
                        info.append("\n    Service Data: None");
                    }
                }
            }
            
            if (hasTargetUUID) {
                Log.i(TAG, "FOUND DEVICE WITH TARGET UUID: " + deviceAddress + " - " + scannedServiceUUID.getUuid().toString());
            }
        } else {
            Log.d(TAG, "Device " + deviceAddress + " has no advertised service UUIDs");
        }
        
        // Add service data info if available
        if (result.getScanRecord() != null && result.getScanRecord().getServiceData() != null && !result.getScanRecord().getServiceData().isEmpty()) {
            info.append("\nService Data:");
            for (ParcelUuid uuid : result.getScanRecord().getServiceData().keySet()) {
                byte[] data = result.getScanRecord().getServiceData(uuid);
                String dataStr = data != null ? new String(data, StandardCharsets.UTF_8) : "null";
                info.append("\n  ").append(uuid.getUuid().toString()).append(": ").append(dataStr);
            }
        } else {
            Log.d(TAG, "Device " + deviceAddress + " has no service data");
        }
        
        String formattedInfo = info.toString();
        Log.d(TAG, "Formatted device info: " + formattedInfo);
        return formattedInfo;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopBleScan(); // Stop scan when fragment is not visible
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopBleScan(); // Ensure scan is stopped
        scanHandler.removeCallbacksAndMessages(null); // Clean up handler
        binding = null;
    }
}
