package com.pdfprocessor.util;

/**
 * Utility class for file operations
 */
public class FileUtils {
    
    /**
     * Get file size in human readable format
     */
    public static String getFileSizeInHumanReadable(Long fileSize) {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = fileSize.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }
}
