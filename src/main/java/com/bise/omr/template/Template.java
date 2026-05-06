package com.bise.omr.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete OMR template created in the designer.
 * Contains the template code (unique identifier), the page dimensions,
 * and a list of fields (QR codes, bubble groups, section markers).
 */
public class Template {

    private String templateCode;
    private String name;
    private int pageWidth;
    private int pageHeight;
    private List<TemplateField> fields = new ArrayList<>();

    public Template() {
        // for Jackson
    }

    @JsonCreator
    public Template(@JsonProperty("templateCode") String templateCode,
                    @JsonProperty("name") String name,
                    @JsonProperty("pageWidth") int pageWidth,
                    @JsonProperty("pageHeight") int pageHeight) {
        this.templateCode = templateCode;
        this.name = name;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
    }

    public void addField(TemplateField field) {
        fields.add(field);
    }

    public boolean validate() {
        if (templateCode == null || templateCode.isEmpty()) return false;
        if (fields.isEmpty()) return false;
        return true;
    }

    // === Getters / Setters for Jackson ===
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPageWidth() { return pageWidth; }
    public void setPageWidth(int pageWidth) { this.pageWidth = pageWidth; }

    public int getPageHeight() { return pageHeight; }
    public void setPageHeight(int pageHeight) { this.pageHeight = pageHeight; }

    public List<TemplateField> getFields() { return fields; }
    public void setFields(List<TemplateField> fields) { this.fields = fields; }
}
