package com.hackathon.attendlytics;

/**
 * Centralized configuration for BLE Service UUIDs
 * Change the SERVICE_UUID here to customize your app's BLE identifier
 */
public class BLEConfig {
    
    // ===== MAIN CONFIGURATION =====
    // Change this line to use a different UUID for your app
    public static final String SERVICE_UUID = PREDEFINED_UUIDS.CURRENT;
    
    // ===== PREDEFINED UUID OPTIONS =====
    public static class PREDEFINED_UUIDS {
        // Current UUID (C0DE = "CODE")
        public static final String CURRENT = "0000C0DE-0000-1000-8000-00805F9B34FB";
        
        // Alternative themed UUIDs - choose any of these:
        public static final String ATTEND = "0000AADD-0000-1000-8000-00805F9B34FB";     // AADD = "ATTEND"
        public static final String CLASS = "0000C1A5-0000-1000-8000-00805F9B34FB";      // C1A5 = "CLASS" 
        public static final String SCHOOL = "00005014-0000-1000-8000-00805F9B34FB";     // 5014 = "SCHOOL"
        public static final String HACKATHON = "0000HACF-0000-1000-8000-00805F9B34FB";  // HACF = "HACK"
        public static final String SIH_2025 = "00002025-0000-1000-8000-00805F9B34FB";   // 2025 = "SIH 2025"
        public static final String CUSTOM = "0000CABE-0000-1000-8000-00805F9B34FB";     // CABE = Custom
    }
    
    // ===== GENERATE YOUR OWN UUID =====
    /*
     * To create your own UUID:
     * 1. Go to https://www.uuidgenerator.net/
     * 2. Generate a UUID
     * 3. Replace SERVICE_UUID above with your generated UUID
     * 
     * OR use any of the PREDEFINED_UUIDS above by changing:
     * public static final String SERVICE_UUID = PREDEFINED_UUIDS.HACKATHON;
     */
    
    // ===== HELPER METHODS =====
    
    /**
     * Gets a user-friendly name for the current UUID
     */
    public static String getServiceUUIDisplayName() {
        switch (SERVICE_UUID) {
            case PREDEFINED_UUIDS.CURRENT:
                return "CODE (Current Default)";
            case PREDEFINED_UUIDS.ATTEND:
                return "ATTEND (Attendance System)";
            case PREDEFINED_UUIDS.CLASS:
                return "CLASS (Classroom)";
            case PREDEFINED_UUIDS.SCHOOL:
                return "SCHOOL (Educational)";
            case PREDEFINED_UUIDS.HACKATHON:
                return "HACK (Hackathon Project)";
            case PREDEFINED_UUIDS.SIH_2025:
                return "SIH 2025 (Smart India Hackathon)";
            case PREDEFINED_UUIDS.CUSTOM:
                return "CUSTOM (User Defined)";
            default:
                return "Custom UUID";
        }
    }
    
    /**
     * Validates if the current UUID is properly formatted
     */
    public static boolean isCurrentUUIValid() {
        try {
            java.util.UUID.fromString(SERVICE_UUID);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Gets configuration summary for logging
     */
    public static String getConfigSummary() {
        return String.format(
            "BLE Configuration:\n" +
            "  Service UUID: %s\n" +
            "  Display Name: %s\n" +
            "  Valid Format: %s\n" +
            "  UUID Type: %s",
            SERVICE_UUID,
            getServiceUUIDisplayName(),
            isCurrentUUIValid(),
            SERVICE_UUID.equals(PREDEFINED_UUIDS.CURRENT) ? "Default" : "Custom"
        );
    }
}