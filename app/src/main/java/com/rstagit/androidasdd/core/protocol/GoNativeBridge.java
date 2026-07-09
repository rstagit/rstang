package com.rstagit.androidasdd.core.protocol;


public final class GoNativeBridge {

    private static volatile boolean libraryLoaded = false;
    private static Throwable loadError = null;

    static {
        try {
            System.loadLibrary("androidasdd_jni");
            libraryLoaded = true;
        } catch (Throwable t) {
            
            
            loadError = t;
            libraryLoaded = false;
        }
    }

    private GoNativeBridge() {
    }

    
    public static boolean isAvailable() {
        return libraryLoaded;
    }

    public static Throwable getLoadError() {
        return loadError;
    }

    
    public static native long spfStart(int listenPort, String remoteEndpoint, String fakeSni, String method);

    
    public static native void spfStop(long sessionId);

    
    public static native String spfPollLog();

    
    public static native String spfParseSni(byte[] data, int length);

    
    public static native String spfVersion();
}
