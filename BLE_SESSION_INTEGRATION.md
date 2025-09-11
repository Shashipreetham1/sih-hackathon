# BLE Session ID Integration Guide

## Overview
The teacher's BLE advertising now includes the session ID that matches the QR code, enabling students to perform proximity verification.

## Session ID Flow

### Teacher Side (Implemented)
1. **QR Code Generation**: Creates session ID like `ATTEND-SESSION-1757617409426`
2. **BLE Advertising**: Optimizes session ID to `09426` (last 8 digits) for BLE transmission
3. **Dual Advertisement**: Sends session ID via both:
   - Service Data (preferred)
   - Manufacturer Data (fallback)

### Student Side (For Your Implementation)
1. **Scan QR Code**: Extract full session ID `ATTEND-SESSION-1757617409426`
2. **Extract BLE Session ID**: Take last 8 digits → `09426`
3. **BLE Proximity Scan**: Look for BLE device with:
   - Service UUID: `0000C0DE-0000-1000-8000-00805F9B34FB`
   - Session ID: `09426` (in service data or manufacturer data)
4. **Verify Match**: Confirm the BLE session ID matches the QR session ID

## Technical Details

### BLE Advertisement Structure
```
Service UUID: 0000C0DE-0000-1000-8000-00805F9B34FB
Service Data: "09426" (optimized session ID)
OR
Manufacturer Data (0xFFFF): "09426" (if service data fails)
```

### Session ID Conversion
```java
// QR Code: "ATTEND-SESSION-1757617409426"
// BLE ID:   "09426" (last 8 digits of timestamp)

public String convertToBLESessionId(String qrSessionId) {
    if (qrSessionId.startsWith("ATTEND-SESSION-")) {
        String timestamp = qrSessionId.substring("ATTEND-SESSION-".length());
        if (timestamp.length() >= 8) {
            return timestamp.substring(timestamp.length() - 8);
        }
    }
    return qrSessionId.length() > 8 ? qrSessionId.substring(0, 8) : qrSessionId;
}
```

### Student BLE Scanner Implementation
```java
// 1. Extract session ID from QR code
String qrSessionId = "ATTEND-SESSION-1757617409426"; // from QR scan
String expectedBLEId = convertToBLESessionId(qrSessionId); // "09426"

// 2. Scan for BLE device with matching Service UUID
ScanFilter filter = new ScanFilter.Builder()
    .setServiceUuid(ParcelUuid.fromString("0000C0DE-0000-1000-8000-00805F9B34FB"))
    .build();

// 3. In scan callback, check session ID
@Override
public void onScanResult(int callbackType, ScanResult result) {
    ScanRecord scanRecord = result.getScanRecord();
    
    // Check service data
    byte[] serviceData = scanRecord.getServiceData(serviceUuid);
    if (serviceData != null) {
        String bleSessionId = new String(serviceData, "UTF-8");
        if (expectedBLEId.equals(bleSessionId)) {
            // MATCH FOUND - this is the correct teacher's device
            // Check RSSI for proximity verification
            int rssi = result.getRssi();
            // Proceed with attendance marking
        }
    }
    
    // Also check manufacturer data as fallback
    byte[] manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF);
    if (manufacturerData != null) {
        String bleSessionId = new String(manufacturerData, "UTF-8");
        if (expectedBLEId.equals(bleSessionId)) {
            // MATCH FOUND via manufacturer data
        }
    }
}
```

## Proximity Verification
- Use RSSI (signal strength) to determine proximity
- Typical values:
  - Very close (< 1m): -30 to -50 dBm
  - Close (1-3m): -50 to -70 dBm
  - Far (3-10m): -70 to -90 dBm

## Troubleshooting
- If session ID not found in service data, check manufacturer data
- Ensure proper permissions (BLUETOOTH_SCAN, ACCESS_FINE_LOCATION)
- Session IDs are case-sensitive
- BLE advertising continues even when navigating between screens

## Testing
Use the BLE Diagnostics screen to verify:
1. Session ID is properly embedded in advertisements
2. Both service data and manufacturer data methods work
3. Session ID matches between QR and BLE