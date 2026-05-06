package com.bise.omr.processor;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc.*;
import org.bytedeco.opencv.opencv_imgproc.Vec4iVector;
import org.bytedeco.opencv.opencv_core.Scalar4i;
import org.bytedeco.javacv.Java2DFrameUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.BORDER_REPLICATE;

/**
 * Image preprocessing pipeline before OMR extraction.
 * Functions: deskew, adaptive threshold, corner mark detection for alignment.
 */
public class ImagePreprocessor {

    private static final Logger log = LoggerFactory.getLogger(ImagePreprocessor.class);

    /**
     * Apply full preprocessing pipeline to a scanned image.
     */
    public BufferedImage deskewAndNormalize(BufferedImage img) {
        img = deskew(img);
        img = threshold(img);
        return img;
    }

    /**
     * Detect rotation angle using the Probabilistic Hough Line Transform and
     * rotate the image to straighten it.
     *
     * Algorithm:
     * 1. Convert to grayscale and run Canny edge detection.
     * 2. Run HoughLinesP to collect line segments.
     * 3. Compute each segment's angle; accumulate a weighted median angle.
     * 4. Rotate the full image by -angle so the dominant lines become horizontal.
     */
    public BufferedImage deskew(BufferedImage img) {
        // --- 1. Convert BufferedImage → OpenCV Mat (BGR or gray) ---
        Mat src = Java2DFrameUtils.toMat(img);

        Mat gray = new Mat();
        if (src.channels() == 1) {
            gray = src.clone();
        } else {
            cvtColor(src, gray, COLOR_BGR2GRAY);
        }

        // --- 2. Canny edge detection ---
        Mat edges = new Mat();
        Canny(gray, edges, 50, 150, 3, false);

        // --- 3. Probabilistic Hough Line Transform ---
        // Vec4iVector: each entry is (x1, y1, x2, y2) as ints
        Vec4iVector lines = new Vec4iVector();
        HoughLinesP(edges,
                lines,
                1, // rho resolution (pixels)
                Math.PI / 180, // theta resolution (radians)
                80, // accumulator threshold
                img.getWidth() / 4.0, // minLineLength
                10); // maxLineGap

        // --- 4. Compute dominant skew angle ---
        long nLines = lines.size();
        if (nLines == 0) {
            log.debug("deskew: no lines detected, returning original image");
            src.close();
            gray.close();
            edges.close();
            lines.close();
            return img;
        }

        double angleSum = 0;
        double weightSum = 0;

        for (long i = 0; i < nLines; i++) {
            Scalar4i seg = lines.get(i);
            int x1 = seg.get(0);
            int y1 = seg.get(1);
            int x2 = seg.get(2);
            int y2 = seg.get(3);

            double dx = x2 - x1;
            double dy = y2 - y1;
            double length = Math.sqrt(dx * dx + dy * dy);

            // Angle in degrees in [-90, 90)
            double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
            // Normalise to [-45, 45] — we only want small skew corrections
            if (angleDeg < -45)
                angleDeg += 90;
            if (angleDeg > 45)
                angleDeg -= 90;

            // Weight longer segments more heavily
            angleSum += angleDeg * length;
            weightSum += length;
        }

        double skewAngle = (weightSum > 0) ? (angleSum / weightSum) : 0;
        log.debug("deskew: detected skew angle = {}°, from {} lines", String.format("%.3f", skewAngle), nLines);

        // --- 5. Rotate by -skewAngle to straighten ---
        int cx = src.cols() / 2;
        int cy = src.rows() / 2;
        Mat rotMat = getRotationMatrix2D(new Point2f(cx, cy), -skewAngle, 1.0);

        Mat rotated = new Mat();
        warpAffine(src, rotated, rotMat,
                new Size(src.cols(), src.rows()),
                INTER_LINEAR,
                BORDER_REPLICATE,
                new Scalar(255, 255, 255, 0));

        // --- 6. Convert back to BufferedImage ---
        BufferedImage result = Java2DFrameUtils.toBufferedImage(rotated);

        // Release native resources
        src.close();
        gray.close();
        edges.close();
        lines.close();
        rotMat.close();
        rotated.close();

        return result;
    }

    /**
     * Apply adaptive thresholding for bubble detection.
     */
    public BufferedImage threshold(BufferedImage img) {
        // TODO: Implement with OpenCV cv::adaptiveThreshold
        return img;
    }

    /**
     * Detect the 4 registration / corner marks on an exam sheet
     * to normalize scale and perspective.
     */
    public int[][] findCornerMarks(BufferedImage img) {
        // TODO: return [[x1,y1],[x2,y2],[x3,y3],[x4,y4]] of the 4 corners
        return new int[4][2];
    }
}
