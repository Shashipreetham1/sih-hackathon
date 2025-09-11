package com.hackathon.attendlytics;

import android.util.Log;
import org.json.JSONObject;

/**
 * Helper class to parse QR code data containing Device Address and Session ID
 * Supports both device address format and legacy Service UUID format
 */
public class QRCodeParser {
    private static final String TAG = "QRCodeParser";
    
    // Device address format fields
    private String deviceAddress;
    private String deviceName;
    private String teacherName;
    private String className;
    
    // Legacy Service UUID format fields  
    private String serviceUUID;
    private String sessionId;
    private String type;
    private long timestamp;
    private boolean isValid = false;
    
    /**
     * Parses QR code data and extracts Device Address and Session ID
     * Supports formats:
     * 1. Simple Address: "XX:XX:XX:XX:XX:XX" (just the Bluetooth device address)
     * 2. JSON Device Address: {"deviceAddress":"XX:XX:XX:XX:XX:XX","deviceName":"Name","sessionId":"ID","teacherName":"Teacher","className":"Class"}
     * 3. JSON Service UUID: {"serviceUUID":"UUID","sessionId":"ID","type":"BLE_ATTENDANCE","timestamp":123}
     * 4. Pipe-separated: UUID|SESSION_ID or DEVICE_ADDRESS|SESSION_ID
     * 5. Legacy: Just session ID (will use default Service UUID)
     */
    public QRCodeParser(String qrCodeData) {
        parseQRCode(qrCodeData);
    }
    
    private void parseQRCode(String qrCodeData) {
        if (qrCodeData == null || qrCodeData.trim().isEmpty()) {
            Log.e(TAG, "QR code data is null or empty");
            return;
        }
        
        Log.d(TAG, "Parsing QR code data: " + qrCodeData);
        
        // Try simple device address format first (just the address)
        if (parseSimpleAddressFormat(qrCodeData)) {
            isValid = true;
            return;
        }
        
        // Try JSON format
        if (qrCodeData.trim().startsWith("{")) {
            if (parseJsonFormat(qrCodeData)) {
                isValid = true;
                return;
            }
        }
        
        // Try pipe-separated format
        if (qrCodeData.contains("|")) {
            if (parsePipeFormat(qrCodeData)) {
                isValid = true;
                return;
            }
        }
        
        // Try legacy format (just session ID)
        if (parseLegacyFormat(qrCodeData)) {
            isValid = true;
            return;
        }
        
        Log.e(TAG, "Could not parse QR code data in any supported format");
    }
    
    /**
     * Parses simple device address format - just the Bluetooth MAC address or device identifier
     * Format: "XX:XX:XX:XX:XX:XX" or generated device identifiers
     */
    private boolean parseSimpleAddressFormat(String addressData) {
        try {
            String trimmed = addressData.trim();
            
            // Check if it matches Bluetooth MAC address pattern (real or generated)
            if (trimmed.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                deviceAddress = trimmed.toUpperCase(); // Standardize to uppercase
                deviceName = "Device"; // Simple name
                sessionId = "ADDR-" + System.currentTimeMillis(); // Generate session ID
                teacherName = "Teacher";
                className = "Attendance";
                type = "BLE_ATTENDANCE";
                timestamp = System.currentTimeMillis();
                
                Log.d(TAG, "✅ Successfully parsed MAC Address format");
                Log.d(TAG, "  Device Address: " + deviceAddress);
                Log.d(TAG, "  Generated Session ID: " + sessionId);
                return true;
            }
            
            // Check for generated device identifiers (from Android ID, etc.)
            if (trimmed.startsWith("DEVICE_") || 
                trimmed.matches("^[0-9A-F]+$") || 
                trimmed.length() >= 6) {
                
                deviceAddress = trimmed;
                deviceName = "Generated ID";
                sessionId = "GEN-" + System.currentTimeMillis();
                teacherName = "Teacher";
                className = "Attendance";
                type = "BLE_ATTENDANCE";
                timestamp = System.currentTimeMillis();
                
                Log.d(TAG, "✅ Successfully parsed Generated Device ID format");
                Log.d(TAG, "  Device ID: " + deviceAddress);
                Log.d(TAG, "  Generated Session ID: " + sessionId);
                return true;
            }
            
            // Check for error states (but still allow them to be parsed)
            if (trimmed.equals("ADDRESS_UNAVAILABLE") || 
                trimmed.equals("PERMISSION_REQUIRED") || 
                trimmed.equals("BLUETOOTH_UNAVAILABLE") || 
                trimmed.equals("BLUETOOTH_OFF") ||
                trimmed.equals("DEVICE_ERROR") ||
                trimmed.equals("ADDRESS_ERROR")) {
                
                deviceAddress = trimmed;
                deviceName = "Error State";
                sessionId = "ERR-" + System.currentTimeMillis();
                teacherName = "Teacher";
                className = "Attendance";
                type = "BLE_ATTENDANCE";
                timestamp = System.currentTimeMillis();
                
                Log.w(TAG, "⚠️ Parsed error state but allowing it: " + trimmed);
                return true;
            }
            
            Log.w(TAG, "Simple address format doesn't match any known pattern: " + trimmed);
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error parsing simple address format: " + e.getMessage());
            return false;
        }
    }
    
    private boolean parseJsonFormat(String jsonData) {
        try {
            JSONObject json = new JSONObject(jsonData);
            
            // Check if it's device address format
            if (json.has("deviceAddress")) {
                deviceAddress = json.optString("deviceAddress", null);
                deviceName = json.optString("deviceName", null);
                sessionId = json.optString("sessionId", null);
                teacherName = json.optString("teacherName", null);
                className = json.optString("className", null);
                type = json.optString("type", "BLE_ATTENDANCE");
                timestamp = json.optLong("timestamp", System.currentTimeMillis());
                
                if (deviceAddress != null && sessionId != null) {
                    Log.d(TAG, "✅ Successfully parsed Device Address format");
                    Log.d(TAG, "  Device Address: " + deviceAddress);
                    Log.d(TAG, "  Device Name: " + deviceName);
                    Log.d(TAG, "  Session ID: " + sessionId);
                    Log.d(TAG, "  Teacher: " + teacherName);
                    Log.d(TAG, "  Class: " + className);
                    return true;
                }
            }
            // Legacy Service UUID format
            else if (json.has("serviceUUID")) {
                serviceUUID = json.optString("serviceUUID", null);
                sessionId = json.optString("sessionId", null);
                type = json.optString("type", null);
                timestamp = json.optLong("timestamp", 0);
                
                if (serviceUUID != null && sessionId != null) {
                    Log.d(TAG, "✅ Successfully parsed Legacy Service UUID format");
                    Log.d(TAG, "  Service UUID: " + serviceUUID);
                    Log.d(TAG, "  Session ID: " + sessionId);
                    return true;
                }
            }
            
            Log.w(TAG, "JSON format missing required fields");
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error parsing JSON format: " + e.getMessage());
            return false;
        }
    }
    
    private boolean parsePipeFormat(String pipeData) {
        try {
            String[] parts = pipeData.split("\\|");
            if (parts.length >= 2) {
                serviceUUID = parts[0].trim();
                sessionId = parts[1].trim();
                type = "BLE_ATTENDANCE";
                timestamp = System.currentTimeMillis();
                
                Log.d(TAG, "✅ Successfully parsed pipe format");
                Log.d(TAG, "  Service UUID: " + serviceUUID);
                Log.d(TAG, "  Session ID: " + sessionId);
                return true;
            }
            
            Log.w(TAG, "Pipe format has insufficient parts: " + parts.length);
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error parsing pipe format: " + e.getMessage());
            return false;
        }
    }
    
    private boolean parseLegacyFormat(String legacyData) {
        try {
            // Assume it's just a session ID, use default Service UUID from config
            if (legacyData.startsWith("ATTEND-SESSION-")) {
                serviceUUID = BLEConfig.SERVICE_UUID; // Use centralized configuration
                sessionId = legacyData.trim();
                type = "BLE_ATTENDANCE";
                timestamp = System.currentTimeMillis();
                
                Log.d(TAG, "✅ Successfully parsed legacy format");
                Log.d(TAG, "  Service UUID: " + serviceUUID + " (default)");
                Log.d(TAG, "  Session ID: " + sessionId);
                return true;
            }
            
            Log.w(TAG, "Legacy format does not match expected pattern: " + legacyData);
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error parsing legacy format: " + e.getMessage());
            return false;
        }
    }
    
    // Getters for Device Address format
    public String getDeviceAddress() {
        return deviceAddress;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public String getTeacherName() {
        return teacherName;
    }
    
    public String getClassName() {
        return className;
    }
    
    // Getters for legacy Service UUID format
    public String getServiceUUID() {
        return serviceUUID;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getType() {
        return type;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    // Helper methods
    public boolean isValid() {
        return isValid;
    }
    
    public boolean isDeviceAddressFormat() {
        return deviceAddress != null;
    }
    
    public boolean isServiceUUIDFormat() {
        return serviceUUID != null;
    }
    
    /**
     * Gets the BLE-optimized session ID (last 8 digits of timestamp)
     * This matches what the teacher device advertises in BLE
     */
    public String getBLESessionId() {
        if (sessionId != null && sessionId.startsWith("ATTEND-SESSION-")) {
            String timestamp = sessionId.substring("ATTEND-SESSION-".length());
            if (timestamp.length() >= 8) {
                return timestamp.substring(timestamp.length() - 8);
            }
        }
        return sessionId != null && sessionId.length() > 8 ? 
            sessionId.substring(0, 8) : sessionId;
    }
    
    /**
     * Creates a summary of the parsed data for display
     */
    public String getSummary() {
        if (!isValid) {
            return "❌ Invalid QR Code Data";
        }
        
        if (isDeviceAddressFormat()) {
            return String.format(
                "✅ QR Code Parsed Successfully (Device Address Format)\n" +
                "Device Address: %s\n" +
                "Device Name: %s\n" +
                "Session ID: %s\n" +
                "Teacher: %s\n" +
                "Class: %s\n" +
                "Type: %s\n" +
                "Timestamp: %d",
                deviceAddress, deviceName, sessionId, teacherName, className, type, timestamp
            );
        } else if (isServiceUUIDFormat()) {
            return String.format(
                "✅ QR Code Parsed Successfully (Service UUID Format)\n" +
                "Service UUID: %s\n" +
                "Session ID: %s\n" +
                "BLE Session ID: %s\n" +
                "Type: %s\n" +
                "Timestamp: %d",
                serviceUUID, sessionId, getBLESessionId(), type, timestamp
            );
        }
        
        return "✅ QR Code Parsed (Unknown Format)";
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}