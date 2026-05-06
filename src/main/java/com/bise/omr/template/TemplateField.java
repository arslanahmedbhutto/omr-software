package com.bise.omr.template;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.awt.image.BufferedImage;

/**
 * Abstract base class for all template fields drawn on the designer canvas.
 * Subclasses define how their specific region is extracted from a scanned image.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = QRCodeField.class, name = "qr"),
        @JsonSubTypes.Type(value = BubbleGroupField.class, name = "bubble"),
        @JsonSubTypes.Type(value = SectionMarkerField.class, name = "section")
})
public abstract class TemplateField {

    protected String fieldId;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected int pageNumber;

    public TemplateField() {
        // for Jackson
    }

    public TemplateField(String fieldId, int x, int y, int width, int height, int pageNumber) {
        this.fieldId = fieldId;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pageNumber = pageNumber;
    }

    /**
     * Extract data from the region of the scanned image defined by this field.
     * Return type is Object because different subclasses return different types
     * (String for QR, List<String> for bubbles, Boolean for section markers).
     */
    public abstract Object extract(BufferedImage img);

    // === Getters / Setters ===
    public String getFieldId() { return fieldId; }
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
}
