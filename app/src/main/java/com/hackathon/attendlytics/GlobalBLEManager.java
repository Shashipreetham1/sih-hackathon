package com.hackathon.attendlytics;

import android.content.Context;
import android.util.Log;

/**
 * Singleton class to manage BLE advertising across the entire application
 * This ensures advertising continues even when fragments are destroyed/recreated
 */
public class GlobalBLEManager {
    private static final String TAG = "GlobalBLEManager";
    private static GlobalBLEManager instance;
    private BLEAdvertisingManager bleAdvertisingManager;
    private boolean isSessionActive = false;
    private String currentSessionId = null;
    
    // Private constructor for singleton
    private GlobalBLEManager() {
    }
    
    public static synchronized GlobalBLEManager getInstance() {
        if (instance == null) {
            instance = new GlobalBLEManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        if (bleAdvertisingManager == null) {
            bleAdvertisingManager = new BLEAdvertisingManager(context.getApplicationContext());
            Log.d(TAG, "Global BLE Manager initialized");
        }
    }
    
    public boolean startSession(String sessionId) {
        if (bleAdvertisingManager == null) {
            Log.e(TAG, "BLE Manager not initialized");
            return false;
        }
        
        if (isSessionActive) {
            Log.w(TAG, "Session already active - stopping previous session");
            stopSession();
        }
        
        Log.d(TAG, "🚀 Starting global BLE session: " + sessionId);
        bleAdvertisingManager.startAdvertising(sessionId);
        isSessionActive = true;
        currentSessionId = sessionId;
        return true;
    }
    
    /**
     * Starts a BLE session with a custom device identifier (DEPRECATED - use SimpleBLEAdvertiser directly)
     * This identifier will be broadcast via BLE and should match what's in the QR code
     */
    @Deprecated
    public boolean startSessionWithIdentifier(String sessionId, String deviceIdentifier) {
        Log.w(TAG, "⚠️ startSessionWithIdentifier is deprecated - use SimpleBLEAdvertiser directly in fragments");
        Log.d(TAG, "🚀 Attempting to start session with identifier (deprecated approach)");
        Log.d(TAG, "📱 Session ID: " + sessionId);
        Log.d(TAG, "🎯 Device Identifier: " + deviceIdentifier);
        
        // This method is deprecated - fragments should use SimpleBLEAdvertiser directly
        isSessionActive = true;
        currentSessionId = sessionId;
        
        Log.d(TAG, "✅ Session marked as active (fragments handle BLE advertising directly)");
        return true;
    }
    
    public void stopSession() {
        if (bleAdvertisingManager != null && isSessionActive) {
            Log.d(TAG, "🛑 Stopping global BLE session");
            bleAdvertisingManager.stopAdvertising();
            isSessionActive = false;
            currentSessionId = null;
        }
    }
    
    public boolean isSessionActive() {
        return isSessionActive;
    }
    
    public String getCurrentSessionId() {
        return currentSessionId;
    }
    
    public boolean canAdvertise() {
        return bleAdvertisingManager != null && bleAdvertisingManager.canAdvertise();
    }
    
    public boolean isAdvertising() {
        return bleAdvertisingManager != null && bleAdvertisingManager.isAdvertising();
    }
    
    public BLEAdvertisingManager getAdvertisingManager() {
        return bleAdvertisingManager;
    }
    
    public String getServiceUUID() {
        return bleAdvertisingManager != null ? bleAdvertisingManager.getServiceUUID() : null;
    }
    
    public void verifyAdvertisingStatus() {
        if (bleAdvertisingManager != null) {
            bleAdvertisingManager.verifyAdvertisingStatus();
        }
    }
    
    public void cleanup() {
        if (bleAdvertisingManager != null) {
            bleAdvertisingManager.cleanup();
            isSessionActive = false;
            currentSessionId = null;
        }
    }
}