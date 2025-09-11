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

public class StudentFragment extends Fragment {

    private static final String TAG = "StudentFragment";
    private FragmentStudentBinding binding;
    private ActivityResultLauncher<Intent> qrScannerLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private String scannedSessionId = null;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private Handler scanHandler;
    private final List<Integer> rssiValues = new ArrayList<>();

    // Define a UUID for your service. This MUST match the Teacher app's advertising UUID.
    // Example UUID - REPLACE WITH YOUR ACTUAL UUID
    private static final ParcelUuid SERVICE_UUID = new ParcelUuid(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // Example, replace

    private static final long SCAN_PERIOD = 5000; // 5 seconds for RSSI collection
    private static final long SCAN_TIMEOUT = 15000; // 15 seconds overall to find device

    private static final int RSSI_THRESHOLD = -65; // dBm
    private static final int RSSI_VARIANCE_THRESHOLD = 5;

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
                        scannedSessionId = scanResult.getContents();
                        binding.textviewSessionId.setText(scannedSessionId);
                        binding.buttonVerifyProximity.setEnabled(true);
                        binding.textviewStatusMessage.setText("Status: QR Scanned. Ready to verify proximity.");
                        Toast.makeText(getContext(), "Scanned: " + scannedSessionId, Toast.LENGTH_LONG).show();
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
                    binding.textviewStatusMessage.setText("Status: Permissions granted. Initializing BLE Scan...");
                    initializeAndStartBleScan();
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
            if (scannedSessionId == null || scannedSessionId.isEmpty()) {
                Toast.makeText(getContext(), "Please scan a QR code first.", Toast.LENGTH_SHORT).show();
                binding.textviewStatusMessage.setText("Status: Please scan QR first.");
                return;
            }
            binding.buttonVerifyProximity.setEnabled(false);
            binding.progressbarScanning.setVisibility(View.VISIBLE);
            binding.textviewStatusMessage.setText("Status: Checking permissions...");
            requestBlePermissions();
        });
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
            binding.textviewStatusMessage.setText("Status: Permissions granted. Initializing BLE Scan...");
            initializeAndStartBleScan();
        }
    }

    private void initializeAndStartBleScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Bluetooth is not enabled. Please enable it.", Toast.LENGTH_LONG).show();
            binding.textviewStatusMessage.setText("Status: Bluetooth not enabled.");
            binding.buttonVerifyProximity.setEnabled(true);
            binding.progressbarScanning.setVisibility(View.GONE);
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "Failed to get BluetoothLeScanner");
            binding.textviewStatusMessage.setText("Status: Failed to init BLE scanner.");
            binding.buttonVerifyProximity.setEnabled(true);
            binding.progressbarScanning.setVisibility(View.GONE);
            return;
        }
        startBleScanWithRssiCollection();
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice() != null && result.getScanRecord() != null) {
                byte[] serviceData = result.getScanRecord().getServiceData(SERVICE_UUID);
                if (serviceData != null) {
                    String advertisedSessionId = new String(serviceData, StandardCharsets.UTF_8);
                    Log.d(TAG, "Found device: " + result.getDevice().getAddress() + ", RSSI: " + result.getRssi() + ", Advertised ID: " + advertisedSessionId);
                    if (scannedSessionId.equals(advertisedSessionId)) {
                        // Correct device found, collect RSSI
                        rssiValues.add(result.getRssi());
                        binding.textviewStatusMessage.setText("Status: Found Teacher. Collecting RSSI... (" + rssiValues.size() + " readings)");
                        // No need to stop scan here, let the SCAN_PERIOD timer handle it
                        // to collect multiple readings from the same device.
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result); // Process each result
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "BLE Scan Failed: " + errorCode);
            binding.textviewStatusMessage.setText("Status: BLE Scan failed. Error code: " + errorCode);
            isScanning = false;
            binding.progressbarScanning.setVisibility(View.GONE);
            binding.buttonVerifyProximity.setEnabled(true);
            scanHandler.removeCallbacksAndMessages(null); // Remove any pending timeout callbacks
        }
    };

    private void startBleScanWithRssiCollection() {
        if (isScanning) {
            Log.d(TAG, "Scan already in progress.");
            return;
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
             binding.textviewStatusMessage.setText("Status: BLUETOOTH_SCAN permission missing.");
             binding.buttonVerifyProximity.setEnabled(true);
             binding.progressbarScanning.setVisibility(View.GONE);
             return;
        }

        rssiValues.clear();
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceData(SERVICE_UUID, scannedSessionId.getBytes(StandardCharsets.UTF_8))
                .build();
        
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        isScanning = true;
        binding.progressbarScanning.setVisibility(View.VISIBLE);
        binding.textviewStatusMessage.setText("Status: Scanning for Teacher device with Session ID: " + scannedSessionId);
        Log.d(TAG, "Starting BLE scan for Session ID: " + scannedSessionId);
        bluetoothLeScanner.startScan(Collections.singletonList(scanFilter), scanSettings, leScanCallback);

        // Stop scan after SCAN_PERIOD for RSSI collection analysis
        scanHandler.postDelayed(() -> {
            if (isScanning) {
                stopBleScan();
                evaluateRssi();
            }
        }, SCAN_PERIOD);

        // Overall timeout for finding the device at all
        scanHandler.postDelayed(() -> {
            if (isScanning && rssiValues.isEmpty()) { // If still scanning and no matching device found yet
                Log.w(TAG, "BLE Scan Timeout: No matching device found within " + SCAN_TIMEOUT + "ms");
                stopBleScan();
                binding.textviewStatusMessage.setText("Status: Proximity check failed. Teacher device not found.");
                binding.progressbarScanning.setVisibility(View.GONE);
                binding.buttonVerifyProximity.setEnabled(true);
            }
        }, SCAN_TIMEOUT);
    }

    private void stopBleScan() {
        if (isScanning && bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                 binding.textviewStatusMessage.setText("Status: BLUETOOTH_SCAN permission missing for stopping scan.");
                 // Fall through, attempt to stop anyway, but log it
                 Log.w(TAG, "BLUETOOTH_SCAN permission missing when trying to stop scan.");
            }
            Log.d(TAG, "Stopping BLE scan.");
            bluetoothLeScanner.stopScan(leScanCallback);
            isScanning = false;
            scanHandler.removeCallbacksAndMessages(null); // Clear all pending runnables for scan
        }
    }

    private void evaluateRssi() {
        binding.progressbarScanning.setVisibility(View.GONE);
        if (rssiValues.isEmpty()) {
            binding.textviewStatusMessage.setText("Status: Proximity check failed. No RSSI readings.");
            binding.buttonVerifyProximity.setEnabled(true);
            Log.w(TAG, "No RSSI values collected for session: " + scannedSessionId);
            // TODO: Optional Firestore Logging - FAILURE (No readings)
            return;
        }

        double sum = 0;
        for (int rssi : rssiValues) {
            sum += rssi;
        }
        double averageRssi = sum / rssiValues.size();

        double varianceSum = 0;
        for (int rssi : rssiValues) {
            varianceSum += Math.pow(rssi - averageRssi, 2);
        }
        double variance = rssiValues.size() > 1 ? varianceSum / (rssiValues.size() - 1) : 0;

        Log.d(TAG, "RSSI Evaluation: Count=" + rssiValues.size() + ", Avg=" + String.format("%.2f", averageRssi) + ", Var=" + String.format("%.2f", variance));

        if (averageRssi > RSSI_THRESHOLD && variance < RSSI_VARIANCE_THRESHOLD) {
            binding.textviewStatusMessage.setText(String.format("Status: Proximity PASS (Avg: %.1f, Var: %.1f)", averageRssi, variance));
            Toast.makeText(getContext(), "Proximity Verified!", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Proximity VERIFIED for session: " + scannedSessionId);
            // TODO: Optional Firestore Logging - SUCCESS
            // Navigate to CaptchaFragment (stub)
            NavHostFragment.findNavController(StudentFragment.this)
                    .navigate(R.id.studentToCaptcha); // Ensure this action exists in your nav graph
        } else {
            binding.textviewStatusMessage.setText(String.format("Status: Proximity FAIL (Avg: %.1f, Var: %.1f)", averageRssi, variance));
            Toast.makeText(getContext(), "Proximity check failed.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Proximity FAILED for session: " + scannedSessionId + " Avg RSSI: " + averageRssi + ", Variance: " + variance);
            // TODO: Optional Firestore Logging - FAILURE (Thresholds not met)
        }
        binding.buttonVerifyProximity.setEnabled(true); // Allow retry
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
