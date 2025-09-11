# BLE Debugging Guide

## Issue Description
Your BLE scanner cannot detect your own device's advertisement, even though advertising appears to be working correctly.

## Enhanced Debugging Features Added
1. **Detailed scan result logging** - Shows all advertisement data in hex and text format
2. **Enhanced advertising status logging** - Displays current advertising configuration
3. **Improved BLE diagnostics interface** - Better system status checking
4. **Comprehensive manufacturer data support** - Handles both service data and manufacturer data

## Debugging Steps

### Step 1: Check System Status
1. Go to **Teacher Fragment** → **Start Attendance**
2. Navigate to **BLE Diagnostics**
3. Tap **"Check System Status"** button
4. Verify all items show ✅:
   - Session active: true
   - Is advertising: true  
   - Can advertise: true
   - All permissions: true

### Step 2: Enable Detailed Logging
Open **Android Studio → Logcat** and filter by tags:
- `BLEAdvertisingManager` - Shows advertising status
- `BLEScannerManager` - Shows scan results
- `GlobalBLEManager` - Shows session management

### Step 3: Test Scanning Sequence
1. In BLE Diagnostics, tap **"Scan All Devices"** first
   - This tests basic BLE scanning functionality
   - Should show nearby BLE devices
2. Then tap **"Scan for Target"** 
   - This looks specifically for your Service UUID: `0000C0DE-0000-1000-8000-00805F9B34FB`

### Step 4: Analyze Logcat Output
Look for these key messages:

**Advertising Status:**
```
=== BLE ADVERTISING STATUS ===
Is Advertising: true
Target Service UUID: 0000C0DE-0000-1000-8000-00805F9B34FB
```

**Scanner Finding Devices:**
```
📱 Device found: [Device Name] (XX:XX:XX:XX:XX:XX) RSSI: -XX dBm
=== SCAN RECORD DETAILS ===
Service UUIDs (X):
  - 0000C0DE-0000-1000-8000-00805F9B34FB
    ✅ MATCHES TARGET UUID!
```

## Common Issues and Solutions

### Issue 1: Android Limitation - Can't Scan Own Advertisement
**Problem**: Some Android devices cannot scan their own BLE advertisements due to hardware/software limitations.

**Solutions**:
1. **Test with another device**: Use a second phone/tablet to scan for your advertisement
2. **Use external BLE scanner app**: Download "BLE Scanner" from Google Play Store
3. **Check with different Android versions**: Behavior varies across Android versions

### Issue 2: Advertisement Data Not Included
**Problem**: Device advertises Service UUID but session ID is missing.

**Check Logcat for**:
```
✅ Session ID added as SERVICE DATA: [ID] (length: X)
```
or
```
✅ Session ID added as MANUFACTURER DATA: [ID] (length: X)
```

**If missing**: Session ID might be too long for BLE data limits (20 bytes max).

### Issue 3: Permission Issues
**Problem**: Scanning fails due to missing permissions.

**Solution**: 
1. Go to Android Settings → Apps → Your App → Permissions
2. Grant all Bluetooth and Location permissions
3. Restart the app

### Issue 4: Bluetooth Stack Issues
**Problem**: Android BLE stack becomes unresponsive.

**Solution**:
1. Turn Bluetooth OFF and ON
2. Restart the app
3. Clear app data if necessary

## Testing with External Tools

### Method 1: BLE Scanner App
1. Install "BLE Scanner" app from Google Play
2. Start attendance in your app
3. Open BLE Scanner app
4. Look for device with Service UUID: `0000C0DE-0000-1000-8000-00805F9B34FB`

### Method 2: Another Phone
1. Install your app on a second device
2. Start attendance on Device A
3. Go to BLE Diagnostics on Device B
4. Scan for target - should find Device A

## Advanced Debugging

### Enable Verbose Logging
In Logcat, set filter level to **Verbose** to see detailed advertisement data:
- Service Data in hex format
- Manufacturer Data content  
- All advertised Service UUIDs
- Device capabilities

### Check Advertisement Timing
- Advertisements are sent periodically (every 100ms-1000ms)
- Scanning may take 10-30 seconds to find devices
- RSSI values indicate signal strength (-30 to -90 dBm typical)

## Expected Behavior

When working correctly, you should see:

1. **In Logcat (Advertising)**:
```
✅ BLE Advertising started successfully!
Service UUID being advertised: 0000C0DE-0000-1000-8000-00805F9B34FB
✅ Session ID added as SERVICE DATA: [session_id]
```

2. **In Logcat (Scanning)**:
```
🎯 FOUND TARGET DEVICE! Service UUID matches: 0000C0DE-0000-1000-8000-00805F9B34FB
```

3. **In BLE Diagnostics UI**:
```
✅ MATCH FOUND!
Device: [Your Device Name]
Session ID: [Extracted Session ID]
Service UUID: 0000C0DE-0000-1000-8000-00805F9B34FB
```

## If Still Not Working

The most likely cause is **Android platform limitation** where devices cannot scan their own advertisements. This is normal behavior on many Android devices.

**Recommended approach**:
1. Test the proximity detection feature with two separate devices
2. One device starts attendance (becomes advertiser)
3. Other device scans for proximity (becomes scanner)
4. Verify session ID matching works between devices

This architecture is actually how the real attendance system would work - teacher devices advertise, student devices scan for proximity.