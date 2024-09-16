package io.github.stcksmsh.kdp.common;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static volatile Logger instance;
    private PrintWriter writer;
    private static final Object lock = new Object();

    public enum LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    private Logger(String filename) {
        try {
            writer = new PrintWriter(new FileWriter(filename, false), true);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Logger getInstance(String filename) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Logger(filename);
                }
            }
        }
        return instance;
    }

    /// Return calling class name
    public static String getTAG() {
        return Thread.currentThread().getStackTrace()[2].getClassName();
    }

    public void D (String message) {
        log(message, "unknown", LogLevel.DEBUG);
    }

    public void D (String TAG, String message ) {
        log(message, TAG, LogLevel.DEBUG);
    }

    public void I (String message) {
        log(message, "unknown", LogLevel.INFO);
    }

    public void I (String TAG, String message ) {
        log(message, TAG, LogLevel.INFO);
    }

    public void W (String message) {
        log(message, "unknown", LogLevel.WARNING);
    }

    public void W (String TAG, String message ) {
        log(message, TAG, LogLevel.WARNING);
    }

    public void E (String message) {
        log(message, "unknown", LogLevel.ERROR);
    }

    public void E (String TAG, String message ) {
        log(message, TAG, LogLevel.ERROR);
    }

    public void E (Exception e) {
        logException(e, "unknown");
    }

    public void E (String TAG, Exception e) {
        logException(e, TAG);
    }

    private void log(String message, String TAG, LogLevel level) {
        synchronized (lock) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.println(timestamp + " [" + level + "] [" + TAG + "] " + message);
            writer.flush();
        }
    }

    private void logException(Exception e, String TAG) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage()).append("\n");
        for (StackTraceElement ste : e.getStackTrace()) {
            sb.append(ste.toString()).append("\n");
        }
        log(sb.toString(), TAG, LogLevel.ERROR);
    }

    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}