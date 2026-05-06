package com.bise.omr.processor;

import com.bise.omr.template.BubbleGroupField;
import com.bise.omr.template.Template;
import com.bise.omr.template.TemplateField;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Contains the complete result of processing a single OMR sheet.
 */
public class OmrResult {

    private String studentCode;
    private String templateCode;
    // LinkedHashMap preserves insertion order (template field order)
    private final Map<String, List<String>> answers = new LinkedHashMap<>();
    private final Map<String, BufferedImage> sectionRegions = new HashMap<>();
    private File savedImagePath;
    private long processedAt = System.currentTimeMillis();

    public void putAnswers(String fieldId, List<String> answerList) {
        answers.put(fieldId, answerList);
    }

    public void addSectionRegion(String fieldId, BufferedImage region) {
        sectionRegions.put(fieldId, region);
    }

    /**
     * Return answer values in the order the template defines its bubble fields.
     * Each entry is the detected answer (A/B/C/D/NONE/MULTI) for one question.
     */
    public List<String> orderedAnswerValues(Template template) {
        List<String> row = new ArrayList<>();
        for (TemplateField field : template.getFields()) {
            if (field instanceof BubbleGroupField) {
                List<String> fieldAnswers = answers.get(field.getFieldId());
                if (fieldAnswers != null) row.addAll(fieldAnswers);
            }
        }
        return row;
    }

    // === Getters/Setters ===
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public Map<String, List<String>> getAnswers() { return answers; }
    public Map<String, BufferedImage> getSectionRegions() { return sectionRegions; }

    public File getSavedImagePath() { return savedImagePath; }
    public void setSavedImagePath(File savedImagePath) { this.savedImagePath = savedImagePath; }

    public long getProcessedAt() { return processedAt; }
}
