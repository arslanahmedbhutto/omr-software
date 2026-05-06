package com.bise.omr.scanner;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Converts PDF files to BufferedImage for display in the template designer.
 * Uses Apache PDFBox.
 */
public class PdfImporter {

    private static final Logger log = LoggerFactory.getLogger(PdfImporter.class);

    /**
     * Convert the first page of a PDF to an image at the given DPI.
     */
    public BufferedImage convertToImage(File pdf, int dpi) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage img = renderer.renderImageWithDPI(0, dpi, ImageType.RGB);
            log.info("Converted PDF {} to image: {}x{}", pdf.getName(), img.getWidth(), img.getHeight());
            return img;
        }
    }

    /**
     * Convert a specific page of a PDF to an image at the given DPI.
     */
    public BufferedImage convertToImage(File pdf, int pageIndex, int dpi) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            return renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
        }
    }

    public int getPageCount(File pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        }
    }
}
