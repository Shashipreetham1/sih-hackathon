# Device Address QR Code System

## 🎯 **Simplified Approach**

Your app now generates QR codes with **actual Bluetooth device addresses** instead of complex Service UUIDs.

---

## 📱 **What the QR Code Contains Now**

### **JSON Format**:
```json
{
  "deviceAddress": "XX:XX:XX:XX:XX:XX",
  "deviceName": "Samsung Galaxy S21",
  "sessionId": "ATTEND-SESSION-1757620808921",
  "teacherName": "Teacher",
  "className": "Attendance Session",
  "type": "BLE_ATTENDANCE",
  "timestamp": 1757620808921
}
```

### **Example QR Code Data**:
```json
{
  "deviceAddress": "B4:B5:2F:12:34:56",
  "deviceName": "My Phone",
  "sessionId": "ATTEND-SESSION-1757620808921",
  "teacherName": "Teacher",
  "className": "Attendance Session",
  "type": "BLE_ATTENDANCE", 
  "timestamp": 1757620808921
}
```

---

## 🔧 **How It Works**

### **Teacher Device (When Starting Attendance)**:

1. **Gets actual Bluetooth device address** (e.g., `B4:B5:2F:12:34:56`)
2. **Gets device name** (e.g., `"Samsung Galaxy S21"`)
3. **Creates QR code** with this information + session ID
4. **Students can scan QR code** and get the teacher's Bluetooth address

### **What Students Get**:
- **Device Address**: `B4:B5:2F:12:34:56` (teacher's Bluetooth MAC)
- **Device Name**: `Samsung Galaxy S21` (teacher's device name)
- **Session ID**: `ATTEND-SESSION-1757620808921`
- **Teacher Name**: `Teacher` (can be customized)
- **Class Name**: `Attendance Session` (can be customized)

---

## 📊 **Key Information**

### **Device Address**:
- **Real Bluetooth MAC address** of teacher's device
- **Format**: `XX:XX:XX:XX:XX:XX` (e.g., `B4:B5:2F:12:34:56`)
- **Unique identifier** for each device

### **Device Name**:
- **Bluetooth device name** (e.g., `"John's iPhone"`, `"Samsung Galaxy"`)
- **User-friendly identifier**

### **Session ID**:
- **Same format** as before: `ATTEND-SESSION-{timestamp}`
- **Links QR code to attendance session**

---

## 🎯 **Benefits**

✅ **Simple**: No complex Service UUIDs  
✅ **Direct**: Uses actual device Bluetooth address  
✅ **Clear**: Students know exactly which device to look for  
✅ **Practical**: Real Bluetooth identifiers that devices can scan for  

---

## 🔍 **Testing the QR Code**

### **To See What's Generated**:

1. **Start attendance** in your app
2. **Check Android Studio Logcat** for:
   ```
   QR Code Data with Device Address: {"deviceAddress":"XX:XX:XX:XX:XX:XX",...}
   ```
3. **Scan QR code** with any QR scanner app to see the JSON data

### **Example Log Output**:
```
TeacherFragment: Device Address: B4:B5:2F:12:34:56
TeacherFragment: Device Name: My Android Phone
TeacherFragment: QR Code Data with Device Address: {"deviceAddress":"B4:B5:2F:12:34:56","deviceName":"My Android Phone","sessionId":"ATTEND-SESSION-1757620808921","teacherName":"Teacher","className":"Attendance Session","type":"BLE_ATTENDANCE","timestamp":1757620808921}
```

---

## 🚀 **What This Enables**

Now students can:
1. **Scan QR code** → Get teacher's Bluetooth address
2. **Look for that specific Bluetooth device** in their area
3. **Verify proximity** by detecting that exact device address
4. **Confirm attendance** when they find the matching device

**No more complex Service UUIDs - just simple, direct Bluetooth device addresses!** 🎉

---

## 📝 **Customization Options**

You can customize the teacher and class names in `TeacherFragment.java`:

```java
qrData.put("teacherName", "Prof. John Smith"); // Your name
qrData.put("className", "Computer Science 101"); // Your class
```

This will show in the QR code data for students to see which teacher/class they're marking attendance for.