package com.hackathon.attendlytics;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TeacherFragment extends Fragment {

    private static final String TAG = "TeacherFragment";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001;
    private static final ParcelUuid SERVICE_UUID = ParcelUuid.fromString("12345678-1234-1234-1234-123456789abc");

    // UI Components
    private TextView textViewSessionId;
    private TextView textViewBleStatus;
    private TextView textViewStatus;
    private TextView textViewAttendanceLabel;
    private TextView textViewAttendanceCount;
    private ImageView imageViewQrCode;
    private Button buttonStartSession;
    private Button buttonStopSession;
    private ProgressBar progressBarSession;
    private RecyclerView recyclerViewAttendance;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference currentSessionRef;
    private ListenerRegistration attendanceListener;

    // BLE Components
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseCallback advertiseCallback;

    // Session Management
    private String currentSessionId;
    private boolean isSessionActive = false;

    // Attendance Management
    private AttendeeAdapter attendeeAdapter;
    private int attendeeCount = 0;

    public TeacherFragment() {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher, container, false);
        
        initializeViews(view);
        setupClickListeners();
        checkBluetoothSupport();
        
        return view;
    }

    private void initializeViews(View view) {
        textViewSessionId = view.findViewById(R.id.textViewSessionId);
        textViewBleStatus = view.findViewById(R.id.textViewBleStatus);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        textViewAttendanceLabel = view.findViewById(R.id.textViewAttendanceLabel);
        textViewAttendanceCount = view.findViewById(R.id.textViewAttendanceCount);
        imageViewQrCode = view.findViewById(R.id.imageViewQrCode);
        buttonStartSession = view.findViewById(R.id.buttonStartSession);
        buttonStopSession = view.findViewById(R.id.buttonStopSession);
        progressBarSession = view.findViewById(R.id.progressBarSession);
        recyclerViewAttendance = view.findViewById(R.id.recyclerViewAttendance);

        // Setup RecyclerView
        attendeeAdapter = new AttendeeAdapter();
        recyclerViewAttendance.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAttendance.setAdapter(attendeeAdapter);
    }

    private void setupClickListeners() {
        buttonStartSession.setOnClickListener(v -> startSession());
        buttonStopSession.setOnClickListener(v -> stopSession());
    }

    private void checkBluetoothSupport() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
            updateStatus("Bluetooth not supported on this device");
            updateBleStatus("BLE Status: Not Supported", Color.RED);
            buttonStartSession.setEnabled(false);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is disabled");
            updateStatus("Please enable Bluetooth to use BLE features");
            updateBleStatus("BLE Status: Bluetooth Disabled", Color.RED);
        } else {
            Log.d(TAG, "Bluetooth is enabled");
            updateBleStatus("BLE Status: Ready", Color.GRAY);
            
            // Check if BLE advertising is supported
            if (bluetoothAdapter.getBluetoothLeAdvertiser() == null) {
                Log.w(TAG, "BLE advertising not supported");
                updateStatus("BLE advertising not supported. QR code only.");
                updateBleStatus("BLE Status: Not Supported", Color.YELLOW);
            } else {
                Log.d(TAG, "BLE advertising supported");
                updateStatus("Ready to start attendance session");
            }
        }
    }

    private void startSession() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        showProgress(true);
        updateStatus("Starting session...");

        // Generate Session ID
        currentSessionId = generateSessionId();
        textViewSessionId.setText(currentSessionId);

        // Create session in Firestore
        createFirestoreSession();
    }

    private String generateSessionId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String teacherId = currentUser != null ? currentUser.getUid() : "unknown";
        
        // Create a shorter, more BLE-friendly Session ID
        long timestamp = System.currentTimeMillis();
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String shortTeacherId = teacherId.substring(0, Math.min(4, teacherId.length()));
        
        // Format: shortTeacherId_timestamp_shortUUID (more compact)
        String sessionId = shortTeacherId + "_" + timestamp + "_" + shortUuid;
        
        Log.d(TAG, "Generated Session ID: " + sessionId + " (length: " + sessionId.length() + ")");
        return sessionId;
    }

    private void createFirestoreSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            updateStatus("Authentication error");
            showProgress(false);
            return;
        }

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", currentSessionId);
        sessionData.put("teacherId", currentUser.getUid());
        sessionData.put("teacherEmail", currentUser.getEmail());
        sessionData.put("startTime", new Date());
        sessionData.put("isActive", true);
        sessionData.put("attendees", new HashMap<>());

        currentSessionRef = db.collection("sessions").document(currentSessionId);
        currentSessionRef.set(sessionData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session created successfully");
                    startAttendanceListener();
                    generateQRCode();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating session", e);
                    updateStatus("Failed to create session: " + e.getMessage());
                    showProgress(false);
                });
    }

    private void generateQRCode() {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(currentSessionId, BarcodeFormat.QR_CODE, 200, 200);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            imageViewQrCode.setImageBitmap(bitmap);
            startBleAdvertising();
            
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
            updateStatus("Failed to generate QR code");
            showProgress(false);
        }
    }

    private void startBleAdvertising() {
        Log.d(TAG, "Starting BLE advertising...");
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is null");
            updateStatus("Bluetooth not supported on this device");
            showProgress(false);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            updateStatus("Please enable Bluetooth and try again");
            showProgress(false);
            return;
        }

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "BLE advertiser is null - device doesn't support BLE advertising");
            updateStatus("BLE advertising not supported on this device");
            showProgress(false);
            return;
        }

        // Check permissions
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions");
            updateStatus("Missing Bluetooth permissions");
            showProgress(false);
            return;
        }

        // Create more compatible advertise settings
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0)
                .build();

        // Create shorter session ID for BLE (max 20 bytes for service data)
        String shortSessionId = currentSessionId.substring(Math.max(0, currentSessionId.length() - 16));
        byte[] sessionIdBytes = shortSessionId.getBytes(StandardCharsets.UTF_8);
        
        Log.d(TAG, "Original Session ID: " + currentSessionId);
        Log.d(TAG, "Short Session ID for BLE: " + shortSessionId);
        Log.d(TAG, "Session ID bytes length: " + sessionIdBytes.length);

        // Create advertise data with shorter session ID
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(SERVICE_UUID)
                .addServiceData(SERVICE_UUID, sessionIdBytes)
                .build();

        // Create advertise callback with better error handling
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "✅ BLE advertising started successfully");
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isSessionActive = true;
                        updateBleStatus("BLE Status: Broadcasting", Color.GREEN);
                        updateStatus("✅ Session Active! Students can scan QR or detect BLE beacon");
                        updateButtonStates();
                        showAttendanceSection(true);
                        showProgress(false);
                    });
                }
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                String errorMessage = getBleErrorMessage(errorCode);
                Log.e(TAG, "❌ BLE advertising failed: " + errorCode + " - " + errorMessage);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatus("BLE Error: " + errorMessage);
                        updateBleStatus("BLE Status: Failed (" + errorCode + ")", Color.RED);
                        showProgress(false);
                        
                        // Try fallback approach
                        if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
                            Toast.makeText(getContext(), "Trying simpler BLE setup...", Toast.LENGTH_SHORT).show();
                            startSimpleBleAdvertising();
                        }
                    });
                }
            }
        };

        // Start advertising
        try {
            Log.d(TAG, "Attempting to start BLE advertising...");
            bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting BLE advertising", e);
            updateStatus("Permission denied for BLE advertising");
            showProgress(false);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting BLE advertising", e);
            updateStatus("Error starting BLE: " + e.getMessage());
            showProgress(false);
        }
    }

    private boolean checkBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private String getBleErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "Already advertising";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "Data too large";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "Feature not supported";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "Internal error";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "Too many advertisers";
            default:
                return "Unknown error (" + errorCode + ")";
        }
    }

    private void startSimpleBleAdvertising() {
        Log.d(TAG, "Starting simple BLE advertising without service data...");
        
        // Very simple advertise settings
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(false)
                .setTimeout(0)
                .build();

        // Simple advertise data - just the service UUID, no data
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(SERVICE_UUID)
                .build();

        // Create new callback for simple advertising
        AdvertiseCallback simpleCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "✅ Simple BLE advertising started successfully");
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isSessionActive = true;
                        updateBleStatus("BLE Status: Broadcasting (Simple)", Color.YELLOW);
                        updateStatus("✅ Session Active! QR code contains full Session ID");
                        updateButtonStates();
                        showAttendanceSection(true);
                        showProgress(false);
                    });
                }
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(TAG, "❌ Simple BLE advertising also failed: " + errorCode);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatus("BLE unavailable. QR code method only.");
                        updateBleStatus("BLE Status: Unavailable", Color.RED);
                        showProgress(false);
                        
                        // Session is still active, just without BLE
                        isSessionActive = true;
                        updateButtonStates();
                        showAttendanceSection(true);
                    });
                }
            }
        };

        try {
            bluetoothLeAdvertiser.startAdvertising(settings, data, simpleCallback);
            advertiseCallback = simpleCallback; // Update reference for stopping
        } catch (Exception e) {
            Log.e(TAG, "Error starting simple BLE advertising", e);
            updateStatus("BLE unavailable. QR code method only.");
            updateBleStatus("BLE Status: Unavailable", Color.RED);
            showProgress(false);
            
            // Session is still active, just without BLE
            isSessionActive = true;
            updateButtonStates();
            showAttendanceSection(true);
        }
    }

    private void stopSession() {
        showProgress(true);
        updateStatus("Stopping session...");

        // Stop BLE advertising
        if (bluetoothLeAdvertiser != null && advertiseCallback != null) {
            try {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception stopping BLE advertising", e);
            }
        }

        // Update Firestore session
        if (currentSessionRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isActive", false);
            updates.put("endTime", new Date());

            currentSessionRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Session ended successfully");
                        resetSessionState();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error ending session", e);
                        resetSessionState();
                    });
        } else {
            resetSessionState();
        }
    }

    private void resetSessionState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                isSessionActive = false;
                currentSessionId = null;
                currentSessionRef = null;
                
                textViewSessionId.setText("No active session");
                imageViewQrCode.setImageBitmap(null);
                updateBleStatus("BLE Status: Stopped", Color.RED);
                updateStatus("Session ended");
                updateButtonStates();
                showAttendanceSection(false);
                showProgress(false);
            });
        }
    }

    private void updateButtonStates() {
        buttonStartSession.setEnabled(!isSessionActive);
        buttonStopSession.setEnabled(isSessionActive);
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
        if (progressBarSession != null) {
            progressBarSession.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private boolean checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ requires specific Bluetooth permissions
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(requireContext(), permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Missing permission: " + permission);
                    return false;
                }
            }
            return true;
        } else {
            // Android 11 and below
            String[] permissions = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };

            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(requireContext(), permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Missing permission: " + permission);
                    return false;
                }
            }
            return true;
        }
    }

    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(getContext(), "Permissions granted. You can now start a session.", 
                              Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permissions required for BLE functionality", 
                              Toast.LENGTH_LONG).show();
                buttonStartSession.setEnabled(false);
            }
        }
    }

    private void startAttendanceListener() {
        if (currentSessionRef == null) return;
        
        Log.d(TAG, "Starting real-time attendance listener");
        
        attendanceListener = currentSessionRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                updateAttendanceList(documentSnapshot);
            }
        });
    }

    private void updateAttendanceList(DocumentSnapshot sessionDoc) {
        Map<String, Object> attendees = (Map<String, Object>) sessionDoc.get("attendees");
        
        if (attendees != null) {
            List<AttendeeAdapter.AttendeeInfo> attendeeList = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : attendees.entrySet()) {
                Map<String, Object> attendeeData = (Map<String, Object>) entry.getValue();
                
                String studentId = (String) attendeeData.get("studentId");
                String studentEmail = (String) attendeeData.get("studentEmail");
                Date joinTime = attendeeData.get("joinTime") instanceof Date ? 
                    (Date) attendeeData.get("joinTime") : new Date();
                String method = (String) attendeeData.get("method");
                
                attendeeList.add(new AttendeeAdapter.AttendeeInfo(studentId, studentEmail, joinTime, method));
            }
            
            // Sort by join time (most recent first)
            attendeeList.sort((a, b) -> b.joinTime.compareTo(a.joinTime));
            
            // Update UI
            attendeeCount = attendeeList.size();
            updateAttendanceCount();
            attendeeAdapter.updateAttendeeList(attendeeList);
            
            Log.d(TAG, "Updated attendance list: " + attendeeCount + " students");
        } else {
            attendeeCount = 0;
            updateAttendanceCount();
            attendeeAdapter.updateAttendeeList(new ArrayList<>());
        }
    }

    private void updateAttendanceCount() {
        String countText = attendeeCount == 1 ? "1 student joined" : attendeeCount + " students joined";
        textViewAttendanceCount.setText(countText);
    }

    private void showAttendanceSection(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        textViewAttendanceLabel.setVisibility(visibility);
        textViewAttendanceCount.setVisibility(visibility);
        recyclerViewAttendance.setVisibility(visibility);
    }

    private void stopAttendanceListener() {
        if (attendanceListener != null) {
            attendanceListener.remove();
            attendanceListener = null;
            Log.d(TAG, "Stopped attendance listener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAttendanceListener();
        if (isSessionActive) {
            stopSession();
        }
    }
}