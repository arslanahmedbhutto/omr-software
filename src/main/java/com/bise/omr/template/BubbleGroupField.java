package com.bise.omr.template;

import com.bise.omr.processor.BubbleDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * TemplateField for a grid of MCQ bubbles.
 * Defines how many questions, how many options per question, and the layout direction.
 */
public class BubbleGroupField extends TemplateField {

    private static final Logger log = LoggerFactory.getLogger(BubbleGroupField.class);

    public enum LayoutDirection { HORIZONTAL, VERTICAL }

    private int questionCount = 20;
    private int optionsPerQuestion = 4;
    private LayoutDirection direction = LayoutDirection.HORIZONTAL;
    private String startLabel = "Q1";

    public BubbleGroupField() {
        super();
    }

    public BubbleGroupField(String fieldId, int x, int y, int width, int height, int pageNumber,
                            int questionCount, int optionsPerQuestion) {
        super(fieldId, x, y, width, height, pageNumber);
        this.questionCount = questionCount;
        this.optionsPerQuestion = optionsPerQuestion;
    }

    @Override
    public Object extract(BufferedImage img) {
        try {
            BufferedImage region = img.getSubimage(x, y, width, height);
            List<String> answers = new BubbleDetector().detectFilled(region, questionCount, optionsPerQuestion, direction);
            return answers;
        } catch (Exception ex) {
            log.warn("Bubble extract failed for field {}: {}", fieldId, ex.getMessage());
            return null;
        }
    }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public int getOptionsPerQuestion() { return optionsPerQuestion; }
    public void setOptionsPerQuestion(int optionsPerQuestion) { this.optionsPerQuestion = optionsPerQuestion; }

    public LayoutDirection getDirection() { return direction; }
    public void setDirection(LayoutDirection direction) { this.direction = direction; }

    public String getStartLabel() { return startLabel; }
    public void setStartLabel(String startLabel) { this.startLabel = startLabel; }
}
