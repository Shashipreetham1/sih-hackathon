package com.hackathon.attendlytics;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.navigation.Navigation;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class TeacherFragment extends Fragment {

    private static final String TAG = "TeacherFragment";
    private ImageView imageViewQrCode;
    private Button buttonStartAttendance;
    private Button buttonBLEDiagnostics;
    private boolean isAttendanceStarted = false;
    private String currentSessionId = null;

    public TeacherFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_teacher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageViewQrCode = view.findViewById(R.id.imageViewQrCode);
        buttonStartAttendance = view.findViewById(R.id.buttonStartAttendance);
        buttonBLEDiagnostics = view.findViewById(R.id.buttonBLEDiagnostics);
        
        // Check if there's an active session from global manager
        GlobalBLEManager globalManager = GlobalBLEManager.getInstance();
        if (globalManager.isSessionActive()) {
            // Restore UI state
            isAttendanceStarted = true;
            currentSessionId = globalManager.getCurrentSessionId();
            buttonStartAttendance.setText("Stop Attendance");
            
            // Regenerate QR code if we have session ID
            if (currentSessionId != null) {
                try {
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    Bitmap bitmap = barcodeEncoder.encodeBitmap(currentSessionId, BarcodeFormat.QR_CODE, 250, 250);
                    imageViewQrCode.setImageBitmap(bitmap);
                    Log.d(TAG, "Restored QR code for existing session: " + currentSessionId);
                } catch (Exception e) {
                    Log.e(TAG, "Error restoring QR code", e);
                }
            }
        }

        buttonStartAttendance.setOnClickListener(v -> {
            if (!isAttendanceStarted) {
                startAttendanceSession();
            } else {
                stopAttendanceSession();
            }
        });
        
        // Add BLE Diagnostics button listener
        buttonBLEDiagnostics.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_teacherFragment_to_bleDiagnosticsFragment);
        });
    }

    private void startAttendanceSession() {
        Log.d(TAG, "🚀 Starting attendance session");
        
        GlobalBLEManager globalManager = GlobalBLEManager.getInstance();
        
        // Check if BLE advertising is available
        if (!globalManager.canAdvertise()) {
            Toast.makeText(getContext(), "BLE advertising not available on this device", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Cannot start attendance session - BLE advertising not available");
            return;
        }
        
        // Task 2: Generate a random session ID
        currentSessionId = "ATTEND-SESSION-" + System.currentTimeMillis();
        Log.d(TAG, "Generated Session ID: " + currentSessionId);

        // Task 3: Start BLE advertising and get Service UUID for QR code
        try {
            // Start BLE advertising with Service UUID approach
            SimpleBLEAdvertiser advertiser = new SimpleBLEAdvertiser(getContext());
            advertiser.setCustomTag("TEACHER"); // Set service data tag
            advertiser.startAdvertisingWithServiceUUID();
            
            // Get the Service UUID that's being broadcast
            String serviceUUIDString = advertiser.getServiceUUIDString();
            if (serviceUUIDString == null) {
                throw new Exception("Failed to generate Service UUID for advertising");
            }
            
            Log.d(TAG, "🎯 Service UUID for QR & BLE: " + serviceUUIDString);
            
            // Create QR code with the Service UUID
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(serviceUUIDString, BarcodeFormat.QR_CODE, 300, 300);
            imageViewQrCode.setImageBitmap(bitmap);
            Log.d(TAG, "✅ QR Code generated with Service UUID: " + serviceUUIDString);

            // Update UI
            buttonStartAttendance.setText("Stop Attendance");
            isAttendanceStarted = true;
            
            Toast.makeText(getContext(), 
                String.format("✅ Attendance session started!\n📡 Service UUID: %s...\n🎯 Broadcasting via BLE\n📱 QR code ready to scan", 
                    serviceUUIDString.substring(0, 8)), 
                Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error starting attendance session", e);
            Toast.makeText(getContext(), "Error starting attendance session: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
            // Clear any partial state
            imageViewQrCode.setImageBitmap(null);
            currentSessionId = null;
        }
    }

    private void stopAttendanceSession() {
        Log.d(TAG, "🛑 Stopping attendance session");
        
        // Task 5: Stop BLE advertising via global manager
        GlobalBLEManager.getInstance().stopSession();
        Log.d(TAG, "BLE advertising stopped");
        
        // Clear QR Code
        imageViewQrCode.setImageBitmap(null);
        Log.d(TAG, "QR code cleared");

        // Update UI
        buttonStartAttendance.setText("Start Attendance");
        isAttendanceStarted = false;
        currentSessionId = null;
        
        Toast.makeText(getContext(), "Attendance session stopped", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Attendance session stopped successfully");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // DON'T stop the session when fragment is destroyed
        // The global manager will handle session persistence
        Log.d(TAG, "Fragment destroyed - session continues in background");
    }
    
    // Debug method to verify advertising status
    public void verifyAdvertisingStatus() {
        GlobalBLEManager.getInstance().verifyAdvertisingStatus();
    }
    
    /**
     * Gets the BLE-optimized session ID that matches what's being advertised
     * Students can use this to match QR code session ID with BLE advertisement
     */
    public String getBLESessionId() {
        if (currentSessionId != null && currentSessionId.startsWith("ATTEND-SESSION-")) {
            String timestamp = currentSessionId.substring("ATTEND-SESSION-".length());
            if (timestamp.length() >= 8) {
                return timestamp.substring(timestamp.length() - 8);
            }
        }
        return currentSessionId != null && currentSessionId.length() > 8 ? 
            currentSessionId.substring(0, 8) : currentSessionId;
    }
    
    /**
     * Gets the full session ID from QR code
     */
    public String getFullSessionId() {
        return currentSessionId;
    }
    
    /**
     * Checks if attendance session is currently active
     */
    public boolean isAttendanceActive() {
        return isAttendanceStarted && GlobalBLEManager.getInstance().isSessionActive();
    }
    
    /**
     * Creates QR code data containing a usable device identifier for BLE advertising
     * Since Android 6.0+, real MAC addresses are not accessible for privacy.
     * We use alternative approaches to get a device identifier.
     */
    private String createDeviceAddressQRCodeData(String sessionId) {
        try {
            // Get Bluetooth adapter to retrieve device info
            BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            
            if (bluetoothAdapter != null) {
                try {
                    // Try to get the actual address first
                    String deviceAddress = bluetoothAdapter.getAddress();
                    Log.d(TAG, "Raw device address from adapter: " + deviceAddress);
                    
                    // Check if we got a real address (not the privacy fallback)
                    if (deviceAddress != null && 
                        !deviceAddress.equals("02:00:00:00:00:00") && 
                        !deviceAddress.equals("00:00:00:00:00:00")) {
                        Log.d(TAG, "✅ Got real device address: " + deviceAddress);
                        return deviceAddress; // Return the real address
                    } else {
                        Log.w(TAG, "⚠️ Got privacy fallback address, using alternative approach");
                        
                        // For modern Android, use a generated identifier based on device info
                        String deviceIdentifier = generateDeviceIdentifier();
                        Log.d(TAG, "📱 Using generated device identifier: " + deviceIdentifier);
                        return deviceIdentifier;
                    }
                    
                } catch (SecurityException e) {
                    Log.w(TAG, "❌ Cannot access device address due to permissions: " + e.getMessage());
                    
                    // Still try to generate an identifier
                    String deviceIdentifier = generateDeviceIdentifier();
                    Log.d(TAG, "🔄 Generated identifier despite permission issue: " + deviceIdentifier);
                    return deviceIdentifier;
                }
            } else {
                Log.w(TAG, "❌ Bluetooth adapter not available");
                return "BLUETOOTH_OFF";
            }
            
        } catch (Exception e) {
            Log.e(TAG, "💥 Error getting device identifier: " + e.getMessage());
            return "DEVICE_ERROR";
        }
    }
    
    /**
     * Generates a stable device identifier that can be used for BLE advertising
     * when the real MAC address is not available due to privacy restrictions
     */
    private String generateDeviceIdentifier() {
        try {
            // Use Android ID (stable across app installs on same device)
            String androidId = android.provider.Settings.Secure.getString(
                getActivity().getContentResolver(), 
                android.provider.Settings.Secure.ANDROID_ID
            );
            
            if (androidId != null && !androidId.equals("9774d56d682e549c")) { // Not the emulator default
                // Format as MAC-like address for consistency
                String formatted = formatAsMAC(androidId);
                Log.d(TAG, "📱 Generated MAC-style identifier from Android ID: " + formatted);
                return formatted;
            } else {
                // Fallback: use a combination of device info
                String deviceInfo = android.os.Build.MODEL + "_" + android.os.Build.SERIAL;
                String formatted = formatAsMAC(deviceInfo);
                Log.d(TAG, "🔧 Generated MAC-style identifier from device info: " + formatted);
                return formatted;
            }
            
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Error generating device identifier: " + e.getMessage());
            // Final fallback
            return "DEVICE_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Formats a string as a MAC address-like identifier
     */
    private String formatAsMAC(String input) {
        try {
            // Create hash of the input
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            
            // Take first 6 bytes and format as MAC
            StringBuilder mac = new StringBuilder();
            for (int i = 0; i < 6 && i < hash.length; i++) {
                if (i > 0) mac.append(":");
                mac.append(String.format("%02X", hash[i] & 0xFF));
            }
            
            return mac.toString();
            
        } catch (Exception e) {
            // Simple fallback formatting
            String hex = Integer.toHexString(Math.abs(input.hashCode())).toUpperCase();
            if (hex.length() >= 12) hex = hex.substring(0, 12);
            else hex = (hex + "000000000000").substring(0, 12);
            
            return hex.replaceAll("(.{2})(?=.)", "$1:");
        }
    }

    /**
     * Creates structured QR code data containing both Service UUID and Session ID
     * Format: {"serviceUUID":"0000C0DE-0000-1000-8000-00805F9B34FB","sessionId":"ATTEND-SESSION-1234567890"}
     */
    private String createQRCodeData(String sessionId, String serviceUUID) {
        try {
            // Create a JSON object with Service UUID and Session ID
            org.json.JSONObject qrData = new org.json.JSONObject();
            qrData.put("serviceUUID", serviceUUID);
            qrData.put("sessionId", sessionId);
            qrData.put("type", "BLE_ATTENDANCE");
            qrData.put("timestamp", System.currentTimeMillis());
            
            return qrData.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating QR code data: " + e.getMessage());
            // Fallback to simple format if JSON creation fails
            return serviceUUID + "|" + sessionId;
        }
    }
}
