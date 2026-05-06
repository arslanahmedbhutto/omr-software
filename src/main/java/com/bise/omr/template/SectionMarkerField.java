package com.bise.omr.template;

import java.awt.image.BufferedImage;

/**
 * TemplateField marking a section of the sheet that contains handwritten answers
 * (Short Questions, Long Questions, ATP). These regions are not auto-graded — they
 * are saved as cropped image regions for teacher review.
 */
public class SectionMarkerField extends TemplateField {

    private String sectionName;
    private String sectionType;  // "SHORT", "LONG", "ATP"

    public SectionMarkerField() {
        super();
    }

    public SectionMarkerField(String fieldId, int x, int y, int width, int height, int pageNumber,
                              String sectionName, String sectionType) {
        super(fieldId, x, y, width, height, pageNumber);
        this.sectionName = sectionName;
        this.sectionType = sectionType;
    }

    @Override
    public Object extract(BufferedImage img) {
        // Sections are not auto-graded; return the cropped region for teacher review
        return img.getSubimage(x, y, width, height);
    }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getSectionType() { return sectionType; }
    public void setSectionType(String sectionType) { this.sectionType = sectionType; }
}
