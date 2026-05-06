package com.bise.omr.processor;

import com.bise.omr.template.BubbleGroupField;
import com.bise.omr.template.QRCodeField;
import com.bise.omr.template.SectionMarkerField;
import com.bise.omr.template.Template;
import com.bise.omr.template.TemplateField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes a scanned image through a template.
 * Runs preprocessing (deskew, threshold), extracts each field, and returns an OmrResult.
 */
public class OmrProcessor {

    private static final Logger log = LoggerFactory.getLogger(OmrProcessor.class);

    private final Template template;
    private final ImagePreprocessor preprocessor;

    public OmrProcessor(Template template) {
        this.template = template;
        this.preprocessor = new ImagePreprocessor();
    }

    public OmrResult process(BufferedImage img) {
        // Step 1: preprocess
        BufferedImage cleaned = preprocessor.deskewAndNormalize(img);

        // Step 2: scale image to template canvas dimensions so field coordinates are correct.
        // Template coordinates were recorded at the canvas resolution (150 DPI render).
        // The scanned/uploaded image may be at a different resolution.
        BufferedImage scaled = scaleToTemplate(cleaned);

        OmrResult result = new OmrResult();
        result.setTemplateCode(template.getTemplateCode());

        // Step 3: iterate through template fields in defined order
        for (TemplateField field : template.getFields()) {
            Object extracted = field.extract(scaled);

            if (field instanceof QRCodeField) {
                String qrValue = (String) extracted;
                result.setStudentCode(qrValue != null ? qrValue : "UNKNOWN");
                log.info("QR detected: {}", qrValue);
            } else if (field instanceof BubbleGroupField) {
                @SuppressWarnings("unchecked")
                List<String> answers = (List<String>) extracted;
                result.putAnswers(field.getFieldId(), answers);
            } else if (field instanceof SectionMarkerField) {
                result.addSectionRegion(field.getFieldId(), (BufferedImage) extracted);
            }
        }

        if (result.getStudentCode() == null || result.getStudentCode().isEmpty()) {
            result.setStudentCode("UNKNOWN_" + System.currentTimeMillis());
        }

        return result;
    }

    /**
     * Scale the image to match the template's canvas dimensions so that
     * all stored field coordinates (x, y, w, h) are valid pixel offsets.
     */
    private BufferedImage scaleToTemplate(BufferedImage src) {
        int tw = template.getPageWidth();
        int th = template.getPageHeight();
        if (tw <= 0 || th <= 0 || (src.getWidth() == tw && src.getHeight() == th)) {
            return src;
        }
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(tw, th,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, tw, th, null);
        g.dispose();
        log.debug("Scaled image from {}x{} to template {}x{}", src.getWidth(), src.getHeight(), tw, th);
        return out;
    }

    public List<OmrResult> processBatch(List<BufferedImage> images) {
        List<OmrResult> results = new ArrayList<>();
        for (BufferedImage img : images) {
            results.add(process(img));
        }
        return results;
    }
}
