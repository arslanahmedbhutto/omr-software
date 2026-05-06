package com.bise.omr.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls a TWAIN-compatible scanner to capture pages.
 *
 * IMPORTANT: This class currently contains a TEST MODE implementation that reads
 * images from a folder instead of an actual scanner. To integrate a real scanner,
 * uncomment the Morena library integration and place morena7.jar in the lib/ folder.
 *
 * See: http://www.gnome.sk/Twain/jtp.html (Morena scanner library)
 */
public class ScannerController {

    private static final Logger log = LoggerFactory.getLogger(ScannerController.class);

    private String connectedDevice;
    private File testImageFolder = new File("test-images");
    private int testIndex = 0;
    private File[] testImages;

    public List<String> listDevices() {
        // TODO: Replace with Morena or JTwain device enumeration
        // Example: return Arrays.asList(Morena.listSources());
        log.info("Scanner device enumeration not yet implemented — using test mode");
        List<String> devices = new ArrayList<>();
        if (testImageFolder.exists()) {
            devices.add("Test Mode (" + testImageFolder.getName() + ")");
        }
        return devices;
    }

    public void connect(String device) {
        this.connectedDevice = device;
        log.info("Connected to: {}", device);
        // TODO: actual TWAIN connection
        if (testImageFolder.exists()) {
            testImages = testImageFolder.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jpg") ||
                    name.toLowerCase().endsWith(".png") ||
                    name.toLowerCase().endsWith(".jpeg"));
            testIndex = 0;
        }
    }

    /**
     * Scan a single page. In test mode, returns the next image from the test folder.
     * Returns null when no more pages are available.
     */
    public BufferedImage scanSinglePage() {
        // TODO: Replace with actual scanner call, e.g.:
        // return Morena.scan(connectedDevice);

        if (testImages == null || testIndex >= testImages.length) {
            return null;
        }
        try {
            BufferedImage img = javax.imageio.ImageIO.read(testImages[testIndex]);
            log.info("Test scan: loaded {}", testImages[testIndex].getName());
            testIndex++;
            return img;
        } catch (Exception ex) {
            log.error("Failed to load test image", ex);
            return null;
        }
    }

    public List<BufferedImage> scanBatch(int count) {
        List<BufferedImage> pages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BufferedImage page = scanSinglePage();
            if (page == null) break;
            pages.add(page);
        }
        return pages;
    }

    public void disconnect() {
        this.connectedDevice = null;
        log.info("Scanner disconnected");
    }

    public String getConnectedDevice() { return connectedDevice; }
}
