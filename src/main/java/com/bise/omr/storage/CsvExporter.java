package com.bise.omr.storage;

import com.bise.omr.processor.OmrResult;
import com.bise.omr.template.BubbleGroupField;
import com.bise.omr.template.Template;
import com.bise.omr.template.TemplateField;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Exports OmrResult list to a CSV file using OpenCSV.
 * Columns: Student Code, Template Code, Processed At, Image Path, then one column per question.
 */
public class CsvExporter {

    private static final Logger log = LoggerFactory.getLogger(CsvExporter.class);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void export(List<OmrResult> results, File out, Template template) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(out))) {

            // --- Build header ---
            List<String> header = new ArrayList<>();
            header.add("Student Code");
            header.add("Template Code");
            header.add("Processed At");
            header.add("Image Path");

            // One column per question, labelled from startLabel
            for (TemplateField field : template.getFields()) {
                if (field instanceof BubbleGroupField bg) {
                    String start = bg.getStartLabel() != null ? bg.getStartLabel() : "Q";
                    // Parse numeric suffix if present (e.g. "Q1" → prefix="Q", start=1)
                    String prefix = start.replaceAll("\\d+$", "");
                    String numStr = start.replaceAll("^\\D+", "");
                    int startNum = numStr.isEmpty() ? 1 : Integer.parseInt(numStr);
                    for (int i = 0; i < bg.getQuestionCount(); i++) {
                        header.add(prefix + (startNum + i));
                    }
                }
            }
            writer.writeNext(header.toArray(new String[0]));

            // --- Write one row per result ---
            for (OmrResult r : results) {
                List<String> row = new ArrayList<>();
                row.add(r.getStudentCode() != null ? r.getStudentCode() : "");
                row.add(r.getTemplateCode() != null ? r.getTemplateCode() : "");
                row.add(DATE_FMT.format(new Date(r.getProcessedAt())));
                row.add(r.getSavedImagePath() != null ? r.getSavedImagePath().getAbsolutePath() : "");

                // Answers in template field order
                for (String answer : r.orderedAnswerValues(template)) {
                    row.add(answer);
                }

                writer.writeNext(row.toArray(new String[0]));
            }

            log.info("Exported {} results ({} columns) to {}",
                    results.size(), header.size(), out.getName());
        }
    }
}
