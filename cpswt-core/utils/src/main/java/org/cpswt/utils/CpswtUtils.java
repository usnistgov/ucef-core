package org.cpswt.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utils for Cpswt
 */
public class CpswtUtils {
    /**
     * Portico has this "feature". This is just a copy.
     * @param millis Milliseconds to sleep.
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch(InterruptedException e) {}
    }

    public static void sleepDefault() {
        CpswtUtils.sleep(50);
    }

    /**
     * Returns the {@link File} object of a parameter config file.
     * @param filePath The path to the config file (relative or absolute)
     * @param filePathDir The path to the root directory that contains the config file if filePath param is relative.
     * @return The config {@link File}.
     */
    public static File loadConfigFile(String filePath, String filePathDir) {
        Path configFilePath = getConfigFilePath(filePath, filePathDir);
        return configFilePath.toFile();
    }

    public static Path getConfigFilePath(String filePath, String filePathDir) {
        Path configFilePath = Paths.get(filePath);
        if(!configFilePath.isAbsolute()) {
            configFilePath = Paths.get(filePathDir, filePath);
        }
        return configFilePath;
    }
}
