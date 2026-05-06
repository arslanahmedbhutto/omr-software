package com.bise.omr.processor;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Reads QR codes and 1D barcodes from image regions using ZXing.
 */
public class QRCodeReader {

    private static final Logger log = LoggerFactory.getLogger(QRCodeReader.class);

    public String read(BufferedImage img) {
        try {
            int w = img.getWidth();
            int h = img.getHeight();
            int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);

            RGBLuminanceSource source = new RGBLuminanceSource(w, h, pixels);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (Exception ex) {
            log.debug("QR read failed: {}", ex.getMessage());
            return null;
        }
    }
}
