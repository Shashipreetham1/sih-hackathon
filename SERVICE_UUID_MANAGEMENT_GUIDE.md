# Service UUID Management Guide

## 🎯 **Your Current Service UUID**

Your app is currently using: **`0000C0DE-0000-1000-8000-00805F9B34FB`**

- **UUID Meaning**: "C0DE" = "CODE" (perfect for coding projects!)
- **Format**: Standard UUID v4 format (128-bit)
- **Purpose**: Identifies your BLE attendance system uniquely

---

## 🔍 **How to Check Your Current UUID**

### **Method 1: In BLE Diagnostics (Recommended)**
1. Open your app → **BLE Diagnostics**
2. Look for **"SERVICE UUID CONFIGURATION"** section
3. Shows current UUID and display name

### **Method 2: In Code**
Current UUID is defined in: `app/src/main/java/com/hackathon/attendlytics/BLEConfig.java`

```java
public static final String SERVICE_UUID = PREDEFINED_UUIDS.CURRENT;
```

### **Method 3: In Android Studio Logcat**
Filter by tag: `BLEAdvertisingManager` or `BLEScannerManager`
Look for: `Service UUID being advertised: 0000C0DE-0000-1000-8000-00805F9B34FB`

---

## 🎛️ **How to Change Your Service UUID**

### **Option 1: Use Predefined UUIDs (Easy)**

Open `BLEConfig.java` and change line 9:

```java
// Current (default)
public static final String SERVICE_UUID = PREDEFINED_UUIDS.CURRENT;

// Change to any of these:
public static final String SERVICE_UUID = PREDEFINED_UUIDS.ATTEND;      // AADD = "ATTEND"
public static final String SERVICE_UUID = PREDEFINED_UUIDS.CLASS;       // C1A5 = "CLASS"
public static final String SERVICE_UUID = PREDEFINED_UUIDS.HACKATHON;   // HACF = "HACK"  
public static final String SERVICE_UUID = PREDEFINED_UUIDS.SIH_2025;    // 2025 = "SIH 2025"
```

### **Option 2: Generate Completely New UUID**

1. **Go to**: https://www.uuidgenerator.net/
2. **Generate** a new UUID
3. **Replace** in `BLEConfig.java`:
```java
public static final String SERVICE_UUID = "YOUR-NEW-UUID-HERE";
```

### **Option 3: Create Custom Themed UUID**

Make your own meaningful UUID:
```java
// Example custom UUIDs:
public static final String SERVICE_UUID = "0000ABCD-0000-1000-8000-00805F9B34FB"; // ABCD
public static final String SERVICE_UUID = "00001234-0000-1000-8000-00805F9B34FB"; // 1234
public static final String SERVICE_UUID = "0000CAFE-0000-1000-8000-00805F9B34FB"; // CAFE
```

---

## 📱 **Available UUID Options**

All available in `BLEConfig.PREDEFINED_UUIDS`:

| Option | UUID | Meaning | Use Case |
|--------|------|---------|----------|
| **CURRENT** | `0000C0DE-0000-1000-8000-00805F9B34FB` | "CODE" | Default (coding projects) |
| **ATTEND** | `0000AADD-0000-1000-8000-00805F9B34FB` | "ATTEND" | Attendance systems |
| **CLASS** | `0000C1A5-0000-1000-8000-00805F9B34FB` | "CLASS" | Classroom applications |
| **HACKATHON** | `0000HACF-0000-1000-8000-00805F9B34FB` | "HACK" | Hackathon projects |
| **SIH_2025** | `00002025-0000-1000-8000-00805F9B34FB` | "2025" | Smart India Hackathon |
| **SCHOOL** | `00005014-0000-1000-8000-00805F9B34FB` | "SCHOOL" | Educational systems |

---

## 🔧 **Steps to Change UUID**

### **1. Choose Your UUID**
- Pick from predefined options above, OR
- Generate a new one at https://www.uuidgenerator.net/

### **2. Update BLEConfig.java**
```java
// Change this line:
public static final String SERVICE_UUID = PREDEFINED_UUIDS.YOUR_CHOICE;
```

### **3. Rebuild App**
```bash
./gradlew assembleDebug
```

### **4. Verify Change**
- Open app → **BLE Diagnostics**
- Check **"Current UUID"** shows your new UUID
- Start attendance and verify it uses new UUID

---

## ⚠️ **Important Notes**

### **UUID Consistency**
- **All devices** must use the **same UUID** to communicate
- If you change UUID, **all app instances** need the same change
- Students and teachers must have **identical UUIDs**

### **Testing After Change**
1. **Start attendance** with new UUID
2. **Use BLE Scanner app** to verify new UUID is advertised
3. **Test proximity detection** between devices

### **UUID Conflicts**
- Different UUIDs = devices won't find each other
- Same UUID = devices can communicate
- Use unique UUIDs to avoid conflicts with other BLE devices

---

## 🎯 **Why Change UUIDs?**

### **Reasons to Keep Current UUID**:
- ✅ Already working and tested
- ✅ Meaningful ("CODE" for coding project)
- ✅ No compatibility issues

### **Reasons to Change UUID**:
- 🎯 More meaningful for your project (e.g., "SIH_2025")
- 🔒 Avoid conflicts with other BLE devices
- 🎨 Personal branding/customization
- 🧪 Testing different configurations

---

## 📊 **UUID Status Check**

Run this in your app to see current configuration:

1. **BLE Diagnostics** → **Check System Status**
2. Look for **"SERVICE UUID CONFIGURATION"** section
3. Shows:
   - Current UUID
   - Display name
   - Valid format status
   - Configuration type (Default/Custom)

---

## 🚀 **Quick UUID Change Example**

To change from default to hackathon theme:

```java
// In BLEConfig.java, change line 9 from:
public static final String SERVICE_UUID = PREDEFINED_UUIDS.CURRENT;

// To:
public static final String SERVICE_UUID = PREDEFINED_UUIDS.HACKATHON;
```

**Result**: UUID changes from `0000C0DE-...` to `0000HACF-...`

Your app will now advertise and scan for the HACKATHON UUID! 🎉

---

## 💡 **Pro Tips**

1. **Document your UUID choice** so team members know which one you're using
2. **Test UUID changes** with BLE Scanner app before deploying
3. **Keep UUID consistent** across all devices in your system  
4. **Use meaningful prefixes** (like "HACF" for hackathon) for easy identification