package com.bise.omr;

import com.bise.omr.template.BubbleGroupField;
import com.bise.omr.template.QRCodeField;
import com.bise.omr.template.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateTest {

    @Test
    public void testTemplateCreation() {
        Template t = new Template("OMR-EXAM-2026", "Science Group 2026", 2480, 3508);
        t.addField(new QRCodeField("qr1", 100, 100, 150, 150, 1));
        t.addField(new BubbleGroupField("mcq1", 300, 1400, 500, 800, 1, 20, 4));

        assertEquals("OMR-EXAM-2026", t.getTemplateCode());
        assertEquals(2, t.getFields().size());
        assertTrue(t.validate());
    }

    @Test
    public void testEmptyTemplateFailsValidation() {
        Template t = new Template();
        assertFalse(t.validate());
    }
}
