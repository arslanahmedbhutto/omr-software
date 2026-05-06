package com.bise.omr.template;

import com.bise.omr.processor.QRCodeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * TemplateField for reading a QR code or barcode from a defined region.
 * Used to auto-detect student seat numbers or sheet codes.
 */
public class QRCodeField extends TemplateField {

    private static final Logger log = LoggerFactory.getLogger(QRCodeField.class);

    private String codeFormat = "QR";  // or "BARCODE_128" etc.

    public QRCodeField() {
        super();
    }

    public QRCodeField(String fieldId, int x, int y, int width, int height, int pageNumber) {
        super(fieldId, x, y, width, height, pageNumber);
    }

    @Override
    public Object extract(BufferedImage img) {
        try {
            BufferedImage region = img.getSubimage(x, y, width, height);
            return new QRCodeReader().read(region);
        } catch (Exception ex) {
            log.warn("QR extract failed for field {}: {}", fieldId, ex.getMessage());
            return null;
        }
    }

    public String getCodeFormat() { return codeFormat; }
    public void setCodeFormat(String codeFormat) { this.codeFormat = codeFormat; }
}
