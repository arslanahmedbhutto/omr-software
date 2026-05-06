package com.bise.omr.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Organizes scanned copies into a folder structure by template code:
 *
 *   output/
 *     {templateCode}/
 *       {studentCode}/
 *         page1.jpg
 *         page2.jpg
 *         ...
 */
public class FileOrganizer {

    private static final Logger log = LoggerFactory.getLogger(FileOrganizer.class);

    private final Path outputRoot;

    public FileOrganizer(String outputRoot) {
        this.outputRoot = Paths.get(outputRoot);
        try {
            Files.createDirectories(this.outputRoot);
        } catch (IOException ex) {
            log.error("Failed to create output root", ex);
        }
    }

    public void createFolderStructure(String templateCode) throws IOException {
        Path templateDir = outputRoot.resolve(templateCode);
        Files.createDirectories(templateDir);
    }

    /**
     * Save an image for a student under the template's folder.
     * Automatically increments page number if the student already has images.
     */
    public File saveByTemplate(String templateCode, String studentCode, BufferedImage img) throws IOException {
        Path studentDir = outputRoot.resolve(templateCode).resolve(studentCode);
        Files.createDirectories(studentDir);

        int pageNumber = 1;
        File out;
        do {
            out = studentDir.resolve("page" + pageNumber + ".jpg").toFile();
            pageNumber++;
        } while (out.exists());

        ImageIO.write(img, "jpg", out);
        log.info("Saved image: {}", out.getAbsolutePath());
        return out;
    }

    public Path getOutputRoot() { return outputRoot; }
}
