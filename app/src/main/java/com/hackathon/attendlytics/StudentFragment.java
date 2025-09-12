package com.hackathon.attendlytics;

import android.Manifest;
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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentFragment extends Fragment {

    private static final String TAG = "StudentFragment";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 2001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2002;
    private static final ParcelUuid SERVICE_UUID = ParcelUuid.fromString("12345678-1234-1234-1234-123456789abc");
    private static final long BLE_SCAN_DURATION = 30000; // 30 seconds
    
    // Proximity verification constants
    private static final int PROXIMITY_REQUIRED_RSSI = -70; // Strong signal required (closer than ~5 meters)
    private static final int PROXIMITY_VERIFICATION_TIMEOUT = 10000; // 10 seconds to verify proximity

    // UI Components
    private TextView textViewBleStatus;
    private TextView textViewStatus;
    private TextView textViewSessionId;
    private TextView textViewTeacherInfo;
    private TextView textViewProximityStatus;
    private Button buttonScanQr;
    private Button buttonScanBle;
    private Button buttonJoinAttendance;
    private Button buttonBackToDashboard;
    private ProgressBar progressBarScanning;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // BLE Components
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private Handler scanHandler;

    // Session Management
    private String detectedSessionId;
    private String detectionMethod;
    private String teacherName;
    private String teacherEmail;
    private boolean isScanning = false;
    
    // Proximity Verification
    private boolean proximityVerified = false;
    private int lastRssiValue = -999;
    private Handler proximityHandler;

    public StudentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Initialize Bluetooth
        bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        
        // Initialize handler
        scanHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student, container, false);
        
        initializeViews(view);
        setupClickListeners();
        checkBluetoothSupport();
        
        return view;
    }

    private void initializeViews(View view) {
        textViewBleStatus = view.findViewById(R.id.textViewBleStatus);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        textViewSessionId = view.findViewById(R.id.textViewSessionId);
        textViewTeacherInfo = view.findViewById(R.id.textViewTeacherInfo);
        textViewProximityStatus = view.findViewById(R.id.textViewProximityStatus);
        buttonScanQr = view.findViewById(R.id.buttonScanQr);
        buttonScanBle = view.findViewById(R.id.buttonScanBle);
        buttonJoinAttendance = view.findViewById(R.id.buttonJoinAttendance);
        buttonBackToDashboard = view.findViewById(R.id.buttonBackToDashboard);
        progressBarScanning = view.findViewById(R.id.progressBarScanning);
    }

    private void setupClickListeners() {
        buttonScanQr.setOnClickListener(v -> startQrScanning());
        buttonScanBle.setOnClickListener(v -> startBleScanning());
        buttonJoinAttendance.setOnClickListener(v -> joinAttendanceSession());
        buttonBackToDashboard.setOnClickListener(v -> navigateBackToDashboard());
    }

    private void checkBluetoothSupport() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
            updateBleStatus("BLE Status: Not Supported", Color.RED);
            buttonScanBle.setEnabled(false);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is disabled");
            updateBleStatus("BLE Status: Bluetooth Disabled", Color.RED);
            updateStatus("Please enable Bluetooth to use BLE scanning");
        } else {
            Log.d(TAG, "Bluetooth is enabled");
            updateBleStatus("BLE Status: Ready to scan", Color.GRAY);
            
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {
                Log.w(TAG, "BLE scanning not supported");
                updateBleStatus("BLE Status: Scanning Not Supported", Color.YELLOW);
                buttonScanBle.setEnabled(false);
            } else {
                Log.d(TAG, "BLE scanning supported");
                
                // Check permissions at startup
                if (!checkBluetoothPermissions()) {
                    Log.d(TAG, "Bluetooth permissions missing - requesting at startup");
                    updateStatus("Please grant Bluetooth permissions for BLE scanning");
                    requestBluetoothPermissions();
                } else {
                    updateStatus("Ready to scan for attendance sessions");
                }
            }
        }
    }

    private void startQrScanning() {
        Log.d(TAG, "Starting QR code scanning");
        
        if (!checkCameraPermission()) {
            requestCameraPermission();
            return;
        }

        updateStatus("Opening QR scanner...");
        
        // Use ZXing library for QR scanning
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan Teacher's QR Code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    private void startBleScanning() {
        Log.d(TAG, "Starting BLE scanning");
        Log.d(TAG, "SERVICE_UUID: " + SERVICE_UUID.toString());
        
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Bluetooth permissions not granted");
            requestBluetoothPermissions();
            return;
        }

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null");
            updateStatus("BLE scanning not available");
            return;
        }

        if (isScanning) {
            Log.d(TAG, "Already scanning, stopping first");
            stopBleScanning();
            return;
        }

        showProgress(true);
        updateStatus("Scanning for teacher beacons...");
        updateBleStatus("BLE Status: Scanning", Color.BLUE);
        buttonScanBle.setText("🛑 Stop Scanning");
        isScanning = true;

        // Create scan filter for our service UUID
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(SERVICE_UUID)
                .build();
        filters.add(filter);
        
        Log.d(TAG, "Created scan filter with SERVICE_UUID: " + SERVICE_UUID.toString());

        // Create scan settings
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        
        Log.d(TAG, "Created scan settings with LOW_LATENCY mode");

        // Create scan callback
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(TAG, "BLE device found: " + result.getDevice().getAddress() + " RSSI: " + result.getRssi());
                Log.d(TAG, "Device name: " + result.getDevice().getName());
                
                // Log scan record details
                if (result.getScanRecord() != null) {
                    Log.d(TAG, "Service UUIDs: " + result.getScanRecord().getServiceUuids());
                    Log.d(TAG, "Service data map: " + result.getScanRecord().getServiceData());
                    Log.d(TAG, "Device name from record: " + result.getScanRecord().getDeviceName());
                }
                
                handleBleDetection(result);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(TAG, "Batch scan results: " + results.size() + " devices");
                for (ScanResult result : results) {
                    handleBleDetection(result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "BLE scan failed: " + errorCode);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatus("BLE scan failed: " + getBleErrorMessage(errorCode));
                        stopBleScanning();
                        
                        // Try scanning without filters as fallback
                        if (errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                            Log.d(TAG, "Trying fallback scan without filters");
                            startFallbackBleScanning();
                        }
                    });
                }
            }
        };

        // Start scanning
        try {
            Log.d(TAG, "Starting filtered BLE scan...");
            bluetoothLeScanner.startScan(filters, settings, scanCallback);
            
            // Stop scanning after duration
            scanHandler.postDelayed(() -> {
                if (isScanning && detectedSessionId == null) {
                    Log.d(TAG, "Filtered scan found no devices, trying unfiltered scan");
                    stopBleScanning();
                    startFallbackBleScanning();
                }
            }, BLE_SCAN_DURATION);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting BLE scan", e);
            updateStatus("Permission denied for BLE scanning");
            stopBleScanning();
        } catch (Exception e) {
            Log.e(TAG, "Exception starting BLE scan", e);
            updateStatus("Error starting BLE scan: " + e.getMessage());
            stopBleScanning();
        }
    }
    
    private void startFallbackBleScanning() {
        Log.d(TAG, "Starting fallback BLE scanning without filters");
        
        if (bluetoothLeScanner == null || !checkBluetoothPermissions()) {
            return;
        }
        
        showProgress(true);
        updateStatus("Scanning for ANY BLE devices (fallback mode)...");
        updateBleStatus("BLE Status: Fallback scanning", 0xFFFF8800); // Orange color
        isScanning = true;

        // Create scan settings for unfiltered scan
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        // Create fallback scan callback
        ScanCallback fallbackCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(TAG, "Fallback scan - BLE device found: " + result.getDevice().getAddress() + " RSSI: " + result.getRssi());
                
                // Check if this device has our service UUID
                if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                    for (ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
                        Log.d(TAG, "Service UUID found: " + uuid.toString());
                        if (SERVICE_UUID.equals(uuid)) {
                            Log.d(TAG, "Found matching SERVICE_UUID in fallback scan!");
                            handleBleDetection(result);
                            return;
                        }
                    }
                }
                
                // Check service data for our UUID
                if (result.getScanRecord() != null && result.getScanRecord().getServiceData() != null) {
                    if (result.getScanRecord().getServiceData().containsKey(SERVICE_UUID)) {
                        Log.d(TAG, "Found SERVICE_UUID in service data!");
                        handleBleDetection(result);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "Fallback BLE scan failed: " + errorCode);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatus("Fallback BLE scan failed: " + getBleErrorMessage(errorCode));
                        stopBleScanning();
                    });
                }
            }
        };

        // Start unfiltered scanning
        try {
            Log.d(TAG, "Starting unfiltered BLE scan...");
            bluetoothLeScanner.startScan(new ArrayList<>(), settings, fallbackCallback);
            
            // Store the callback for stopping
            scanCallback = fallbackCallback;
            
            // Stop scanning after duration
            scanHandler.postDelayed(() -> {
                if (isScanning && detectedSessionId == null) {
                    updateStatus("No teacher beacons found in any scan mode. Try QR scanning.");
                    stopBleScanning();
                }
            }, BLE_SCAN_DURATION);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting fallback BLE scan", e);
            updateStatus("Permission denied for fallback BLE scanning");
            stopBleScanning();
        } catch (Exception e) {
            Log.e(TAG, "Exception starting fallback BLE scan", e);
            updateStatus("Error starting fallback BLE scan: " + e.getMessage());
            stopBleScanning();
        }
    }

    private void handleBleDetection(ScanResult result) {
        int rssi = result.getRssi();
        lastRssiValue = rssi; // Store RSSI for proximity verification
        
        Log.d(TAG, "=== BLE DEVICE ANALYSIS ===");
        Log.d(TAG, "Device: " + result.getDevice().getAddress() + " RSSI: " + rssi);
        Log.d(TAG, "Device Name: " + result.getDevice().getName());
        
        if (result.getScanRecord() == null) {
            Log.w(TAG, "Scan record is null");
            return;
        }
        
        Log.d(TAG, "Device Name from record: " + result.getScanRecord().getDeviceName());
        Log.d(TAG, "Service UUIDs: " + result.getScanRecord().getServiceUuids());
        Log.d(TAG, "Service Data keys: " + (result.getScanRecord().getServiceData() != null ? 
                result.getScanRecord().getServiceData().keySet() : "null"));
        
        // Check if this device advertises our service UUID
        boolean hasServiceUuid = false;
        if (result.getScanRecord().getServiceUuids() != null) {
            for (ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
                Log.d(TAG, "Checking service UUID: " + uuid.toString());
                if (SERVICE_UUID.equals(uuid)) {
                    hasServiceUuid = true;
                    Log.d(TAG, "✅ Found matching SERVICE_UUID!");
                    break;
                }
            }
        }
        
        // Extract session ID from service data
        byte[] serviceData = result.getScanRecord().getServiceData(SERVICE_UUID);
        if (serviceData != null) {
            String sessionId = new String(serviceData, StandardCharsets.UTF_8);
            Log.d(TAG, "✅ Session ID detected via BLE: " + sessionId + " (Signal strength: " + rssi + " dBm)");
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // For BLE detection, proximity is automatically verified by signal presence
                    proximityVerified = true;
                    updateProximityStatus("✅ Proximity verified via BLE (Signal: " + rssi + " dBm)", Color.GREEN);
                    handleSessionDetected(sessionId, "BLE Beacon");
                });
            }
        } else if (hasServiceUuid) {
            // Device has our service UUID but no service data
            Log.d(TAG, "⚠️ Device has SERVICE_UUID but no service data");
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (rssi >= PROXIMITY_REQUIRED_RSSI) {
                        updateStatus("Strong teacher beacon detected! Please scan QR code for Session ID.");
                        updateBleStatus("Teacher beacon found - scan QR code", Color.GREEN);
                    } else {
                        updateStatus("Teacher beacon detected but weak signal. Move closer and scan QR code.");
                        updateBleStatus("Weak teacher beacon - move closer", Color.YELLOW);
                    }
                    stopBleScanning();
                });
            }
        } else {
            // Device doesn't have our service UUID
            Log.d(TAG, "❌ Device does not have our SERVICE_UUID");
        }
        
        Log.d(TAG, "=== END BLE ANALYSIS ===");
    }

    private void stopBleScanning() {
        if (bluetoothLeScanner != null && scanCallback != null && isScanning) {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception stopping BLE scan", e);
            }
        }
        
        isScanning = false;
        buttonScanBle.setText("📡 Scan for BLE Beacons");
        updateBleStatus("BLE Status: Ready to scan", Color.GRAY);
        showProgress(false);
        
        // Clear handler callbacks
        scanHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle QR scan result
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Log.d(TAG, "QR scan cancelled");
                updateStatus("QR scan cancelled");
            } else {
                String scannedSessionId = result.getContents();
                Log.d(TAG, "QR code scanned: " + scannedSessionId);
                handleSessionDetected(scannedSessionId, "QR Code");
            }
        }
    }

    private void handleSessionDetected(String sessionId, String method) {
        Log.d(TAG, "Session detected via " + method + ": " + sessionId);
        
        detectedSessionId = sessionId;
        detectionMethod = method;
        textViewSessionId.setText("Session ID: " + sessionId);
        updateStatus("✅ Session detected via " + method + "! Verifying...");
        
        // Stop any ongoing BLE scanning
        if (isScanning) {
            stopBleScanning();
        }
        
        // Verify session in Firestore
        verifySession(sessionId);
    }

    private void verifySession(String sessionId) {
        showProgress(true);
        updateStatus("Verifying session with teacher...");
        
        db.collection("sessions").document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getBoolean("isActive") == Boolean.TRUE) {
                        // Session is valid and active
                        teacherName = documentSnapshot.getString("teacherEmail");
                        teacherEmail = documentSnapshot.getString("teacherEmail");
                        
                        textViewTeacherInfo.setText("Teacher: " + (teacherName != null ? teacherName : "Unknown"));
                        updateStatus("✅ Valid session found! Ready to join attendance.");
                        
                        buttonJoinAttendance.setEnabled(true);
                        buttonJoinAttendance.setVisibility(View.VISIBLE);
                        showProgress(false);
                        
                    } else {
                        // Session not found or inactive
                        updateStatus("❌ Session not found or inactive. Please try again.");
                        resetSessionState();
                        showProgress(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error verifying session", e);
                    updateStatus("❌ Error verifying session: " + e.getMessage());
                    resetSessionState();
                    showProgress(false);
                });
    }

    private void joinAttendanceSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            updateStatus("Please sign in first");
            return;
        }

        if (detectedSessionId == null) {
            updateStatus("No session detected");
            return;
        }

        // Check if proximity verification is required (for QR scans)
        if ("QR Code".equals(detectionMethod)) {
            if (!proximityVerified) {
                updateStatus("🔍 Verifying proximity to teacher...");
                startProximityVerification();
                return;
            }
        }

        // Proceed with attendance marking
        markAttendanceWithProximity();
    }

    private void startProximityVerification() {
        if (!checkBluetoothPermissions()) {
            updateStatus("❌ Bluetooth permissions required for proximity verification");
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            updateStatus("❌ Please enable Bluetooth for proximity verification");
            return;
        }

        showProgress(true);
        proximityVerified = false;
        lastRssiValue = -999;
        updateProximityStatus("🔍 Checking proximity...", Color.BLUE);

        // Start BLE scanning specifically for proximity verification
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            updateStatus("❌ BLE scanner not available");
            showProgress(false);
            return;
        }

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(SERVICE_UUID)
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        ScanCallback proximityCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                
                int rssi = result.getRssi();
                lastRssiValue = rssi;
                
                Log.d(TAG, "Proximity check - RSSI: " + rssi + " (Required: " + PROXIMITY_REQUIRED_RSSI + ")");
                
                if (rssi >= PROXIMITY_REQUIRED_RSSI) {
                    Log.d(TAG, "✅ Proximity verified! Strong signal detected");
                    proximityVerified = true;
                    stopProximityVerification();
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateProximityStatus("✅ Proximity verified (Signal: " + rssi + " dBm)", Color.GREEN);
                            updateStatus("✅ Proximity verified! Marking attendance...");
                            markAttendanceWithProximity();
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateProximityStatus("📶 Move closer (Signal: " + rssi + " dBm)", Color.YELLOW);
                            updateStatus("📶 Move closer to teacher (Signal: " + rssi + " dBm)");
                        });
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "Proximity verification scan failed: " + errorCode);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatus("❌ Proximity verification failed. Please try again.");
                        showProgress(false);
                    });
                }
            }
        };

        try {
            bluetoothLeScanner.startScan(List.of(filter), settings, proximityCallback);
            isScanning = true;
            scanCallback = proximityCallback;
            
            // Set timeout for proximity verification
            if (proximityHandler == null) {
                proximityHandler = new Handler();
            }
            
            proximityHandler.postDelayed(() -> {
                if (isScanning && !proximityVerified) {
                    stopProximityVerification();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String message = lastRssiValue != -999 ? 
                                "❌ Too far from teacher. Signal: " + lastRssiValue + " dBm (Need: " + PROXIMITY_REQUIRED_RSSI + " dBm or stronger)" :
                                "❌ Teacher's beacon not detected. Please move closer.";
                            updateStatus(message);
                            showProgress(false);
                        });
                    }
                }
            }, PROXIMITY_VERIFICATION_TIMEOUT);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception during proximity verification", e);
            updateStatus("❌ Permission denied for proximity verification");
            showProgress(false);
        }
    }

    private void stopProximityVerification() {
        if (bluetoothLeScanner != null && scanCallback != null && isScanning) {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
                isScanning = false;
                scanCallback = null;
                Log.d(TAG, "Proximity verification stopped");
            } catch (SecurityException e) {
                Log.e(TAG, "Error stopping proximity verification", e);
            }
        }
        
        if (proximityHandler != null) {
            proximityHandler.removeCallbacksAndMessages(null);
        }
    }

    private void markAttendanceWithProximity() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            updateStatus("Authentication error");
            showProgress(false);
            return;
        }

        showProgress(true);
        updateStatus("Marking attendance with proximity verification...");

        // Create attendance record with proximity info
        Map<String, Object> attendeeData = new HashMap<>();
        attendeeData.put("studentId", currentUser.getUid());
        attendeeData.put("studentEmail", currentUser.getEmail());
        attendeeData.put("joinTime", new Date());
        attendeeData.put("method", detectionMethod != null ? detectionMethod : "Unknown");
        
        // Add proximity verification details
        if ("QR Code".equals(detectionMethod)) {
            attendeeData.put("proximityVerified", proximityVerified);
            attendeeData.put("signalStrength", lastRssiValue);
        } else {
            // For BLE detection, proximity is inherently verified
            attendeeData.put("proximityVerified", true);
            attendeeData.put("signalStrength", lastRssiValue);
        }

        // Add to session's attendees
        db.collection("sessions").document(detectedSessionId)
                .update("attendees." + currentUser.getUid(), attendeeData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Attendance marked successfully with proximity verification");
                    updateStatus("✅ Attendance marked successfully!");
                    
                    // Navigate to result fragment
                    Bundle bundle = new Bundle();
                    bundle.putString("sessionId", detectedSessionId);
                    bundle.putString("method", detectionMethod != null ? detectionMethod : "Unknown");
                    bundle.putBoolean("success", true);
                    
                    try {
                        NavHostFragment.findNavController(StudentFragment.this)
                                .navigate(R.id.action_studentFragment_to_attendanceResultFragment, bundle);
                    } catch (Exception e) {
                        Log.e(TAG, "Navigation error", e);
                        // Show success message if navigation fails
                        updateStatus("✅ Attendance marked successfully! Session: " + detectedSessionId);
                        showProgress(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking attendance", e);
                    updateStatus("❌ Failed to mark attendance: " + e.getMessage());
                    showProgress(false);
                    
                    // Navigate to result fragment with error
                    Bundle bundle = new Bundle();
                    bundle.putString("sessionId", detectedSessionId);
                    bundle.putString("method", detectionMethod != null ? detectionMethod : "Unknown");
                    bundle.putBoolean("success", false);
                    
                    try {
                        NavHostFragment.findNavController(StudentFragment.this)
                                .navigate(R.id.action_studentFragment_to_attendanceResultFragment, bundle);
                    } catch (Exception navError) {
                        Log.e(TAG, "Navigation error", navError);
                    }
                });
    }

    private void resetSessionState() {
        detectedSessionId = null;
        detectionMethod = null;
        teacherName = null;
        teacherEmail = null;
        proximityVerified = false;
        lastRssiValue = -999;
        textViewSessionId.setText("Session ID: Not connected");
        textViewTeacherInfo.setText("Teacher: Unknown");
        hideProximityStatus();
        buttonJoinAttendance.setEnabled(false);
        buttonJoinAttendance.setVisibility(View.GONE);
    }

    private boolean checkCameraPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    private boolean checkBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            requestPermissions(permissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
        } else {
            String[] permissions = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            requestPermissions(permissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
        }
    }

    private String getBleErrorMessage(int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "Scan already started";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "App registration failed";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "Feature not supported";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "Internal error";
            default:
                return "Unknown error (" + errorCode + ")";
        }
    }

    private void updateStatus(String message) {
        if (textViewStatus != null) {
            textViewStatus.setText(message);
        }
    }

    private void updateBleStatus(String message, int color) {
        if (textViewBleStatus != null) {
            textViewBleStatus.setText(message);
            textViewBleStatus.setTextColor(color);
        }
    }

    private void showProgress(boolean show) {
        if (progressBarScanning != null) {
            progressBarScanning.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Camera permission granted. You can now scan QR codes.", 
                              Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Camera permission required for QR scanning", 
                              Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(getContext(), "Bluetooth permissions granted. You can now scan for beacons.", 
                              Toast.LENGTH_SHORT).show();
                checkBluetoothSupport();
            } else {
                Toast.makeText(getContext(), "Bluetooth permissions required for BLE scanning", 
                              Toast.LENGTH_LONG).show();
                buttonScanBle.setEnabled(false);
            }
        }
    }

    private void updateProximityStatus(String status, int color) {
        if (textViewProximityStatus != null) {
            textViewProximityStatus.setText(status);
            textViewProximityStatus.setTextColor(color);
            textViewProximityStatus.setVisibility(View.VISIBLE);
        }
    }

    private void hideProximityStatus() {
        if (textViewProximityStatus != null) {
            textViewProximityStatus.setVisibility(View.GONE);
        }
    }

    private void navigateBackToDashboard() {
        try {
            NavHostFragment.findNavController(StudentFragment.this)
                    .navigate(R.id.action_studentFragment_to_studentDashboardFragment);
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            // Fallback: pop back stack
            NavHostFragment.findNavController(StudentFragment.this).popBackStack();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isScanning) {
            stopBleScanning();
        }
        stopProximityVerification();
        if (scanHandler != null) {
            scanHandler.removeCallbacksAndMessages(null);
        }
        if (proximityHandler != null) {
            proximityHandler.removeCallbacksAndMessages(null);
        }
    }
}