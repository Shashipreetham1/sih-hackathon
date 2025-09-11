package com.hackathon.attendlytics;

import java.util.UUID;
import android.util.Log;

/**
 * Utility class to generate and manage Service UUIDs for BLE attendance system
 */
public class ServiceUUIDGenerator {
    private static final String TAG = "ServiceUUIDGenerator";
    
    /**
     * Generates a completely random UUID for BLE service
     */
    public static String generateRandomUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString().toUpperCase();
        Log.d(TAG, "Generated random UUID: " + uuidString);
        return uuidString;
    }
    
    /**
     * Generates a custom UUID with your app's identifier embedded
     * Format: XXXX{APP_ID}-0000-1000-8000-00805F9B34FB
     */
    public static String generateCustomUUID(String appIdentifier) {
        // Take first 4 chars of identifier, pad with zeros if needed
        String prefix = (appIdentifier + "0000").substring(0, 4).toUpperCase();
        String customUUID = "0000" + prefix + "-0000-1000-8000-00805F9B34FB";
        Log.d(TAG, "Generated custom UUID with identifier '" + appIdentifier + "': " + customUUID);
        return customUUID;
    }
    
    /**
     * Predefined UUIDs for different purposes
     */
    public static class PredefinedUUIDs {
        // Current UUID (C0DE = "CODE")
        public static final String CURRENT = "0000C0DE-0000-1000-8000-00805F9B34FB";
        
        // Alternative themed UUIDs
        public static final String ATTEND = "0000AADD-0000-1000-8000-00805F9B34FB"; // AADD = "ATTEND"
        public static final String CLASS = "0000C1A5-0000-1000-8000-00805F9B34FB";  // C1A5 = "CLASS" 
        public static final String SCHOOL = "0000501-0000-1000-8000-00805F9B34FB";   // 501 = "SCHOOL"
        public static final String HACKATHON = "0000HACF-0000-1000-8000-00805F9B34FB"; // HACF = "HACK"
        
        // Your project specific
        public static final String SIH_2025 = "0000214-0000-1000-8000-00805F9B34FB";  // 2025 = "SIH 2025"
    }
    
    /**
     * Validates if a UUID string is properly formatted
     */
    public static boolean isValidUUID(String uuidString) {
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid UUID format: " + uuidString);
            return false;
        }
    }
    
    /**
     * Gets a user-friendly name for the UUID (for display purposes)
     */
    public static String getUUIDDisplayName(String uuidString) {
        switch (uuidString) {
            case PredefinedUUIDs.CURRENT:
                return "CODE (Current)";
            case PredefinedUUIDs.ATTEND:
                return "ATTEND (Attendance)";
            case PredefinedUUIDs.CLASS:
                return "CLASS (Classroom)";
            case PredefinedUUIDs.SCHOOL:
                return "SCHOOL (Education)";
            case PredefinedUUIDs.HACKATHON:
                return "HACK (Hackathon)";
            case PredefinedUUIDs.SIH_2025:
                return "SIH 2025 (Project)";
            default:
                return "Custom UUID";
        }
    }
    
    /**
     * Demonstration method - run this to see different UUID options
     */
    public static void demonstrateUUIDs() {
        Log.d(TAG, "=== SERVICE UUID OPTIONS ===");
        
        Log.d(TAG, "1. Current UUID: " + PredefinedUUIDs.CURRENT);
        Log.d(TAG, "   Display: " + getUUIDDisplayName(PredefinedUUIDs.CURRENT));
        
        Log.d(TAG, "2. Random UUID: " + generateRandomUUID());
        
        Log.d(TAG, "3. Custom UUIDs:");
        Log.d(TAG, "   ATTEND: " + PredefinedUUIDs.ATTEND);
        Log.d(TAG, "   CLASS: " + PredefinedUUIDs.CLASS);
        Log.d(TAG, "   HACKATHON: " + PredefinedUUIDs.HACKATHON);
        Log.d(TAG, "   SIH 2025: " + PredefinedUUIDs.SIH_2025);
        
        Log.d(TAG, "4. Custom with identifier: " + generateCustomUUID("MYAPP"));
        
        Log.d(TAG, "=============================");
    }
}