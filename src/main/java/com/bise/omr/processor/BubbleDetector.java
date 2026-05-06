package com.bise.omr.processor;

import com.bise.omr.template.BubbleGroupField.LayoutDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects which bubbles are filled in a BubbleGroup region.
 *
 * Algorithm:
 *  1. Divide the region into a grid based on questionCount x optionsPerQuestion
 *  2. For each cell, count dark pixels (after thresholding)
 *  3. The option with the highest fill ratio above threshold is the answer
 *  4. If multiple options filled, mark as "MULTI"
 *  5. If no option filled strongly, mark as "NONE"
 */
public class BubbleDetector {

    private static final Logger log = LoggerFactory.getLogger(BubbleDetector.class);

    private static final double FILL_THRESHOLD = 0.5;
    private static final String[] OPTIONS = {"A", "B", "C", "D", "E"};

    public List<String> detectFilled(BufferedImage region, int questionCount, int optionsPerQuestion,
                                     LayoutDirection direction) {
        List<String> answers = new ArrayList<>();

        int cellWidth, cellHeight;
        if (direction == LayoutDirection.HORIZONTAL) {
            cellWidth = region.getWidth() / optionsPerQuestion;
            cellHeight = region.getHeight() / questionCount;
        } else {
            cellWidth = region.getWidth() / questionCount;
            cellHeight = region.getHeight() / optionsPerQuestion;
        }

        for (int q = 0; q < questionCount; q++) {
            double maxFill = 0;
            int bestOption = -1;
            int filledCount = 0;

            for (int opt = 0; opt < optionsPerQuestion; opt++) {
                int cellX, cellY;
                if (direction == LayoutDirection.HORIZONTAL) {
                    cellX = opt * cellWidth;
                    cellY = q * cellHeight;
                } else {
                    cellX = q * cellWidth;
                    cellY = opt * cellHeight;
                }

                double fill = calculateFillRatio(region, cellX, cellY, cellWidth, cellHeight);
                if (fill > FILL_THRESHOLD) filledCount++;
                if (fill > maxFill) {
                    maxFill = fill;
                    bestOption = opt;
                }
            }

            if (filledCount == 0) answers.add("NONE");
            else if (filledCount > 1) answers.add("MULTI");
            else answers.add(OPTIONS[bestOption]);
        }

        return answers;
    }

    /**
     * Calculate the ratio of dark pixels in a rectangular region.
     * A simple threshold-based approach for now; replace with OpenCV for accuracy.
     */
    private double calculateFillRatio(BufferedImage img, int x, int y, int w, int h) {
        int darkPixels = 0;
        int totalPixels = 0;
        int endX = Math.min(x + w, img.getWidth());
        int endY = Math.min(y + h, img.getHeight());

        for (int py = y; py < endY; py++) {
            for (int px = x; px < endX; px++) {
                int rgb = img.getRGB(px, py);
                int gray = (int) (((rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF)) / 3.0);
                if (gray < 128) darkPixels++;
                totalPixels++;
            }
        }
        return totalPixels > 0 ? (double) darkPixels / totalPixels : 0;
    }
}
