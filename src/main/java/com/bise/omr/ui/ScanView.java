package com.bise.omr.ui;

import com.bise.omr.processor.OmrProcessor;
import com.bise.omr.processor.OmrResult;
import com.bise.omr.scanner.PdfImporter;
import com.bise.omr.scanner.ScannerController;
import com.bise.omr.storage.CsvExporter;
import com.bise.omr.storage.FileOrganizer;
import com.bise.omr.template.Template;
import com.bise.omr.template.TemplateRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2 — Scan & Process copies.
 *
 * Workflow:
 *  1. User loads a previously saved template (.json)
 *  2. User connects to scanner
 *  3. User starts batch scan — each scanned page is processed through the template
 *  4. Results (QR code + bubble answers) are extracted and stored
 *  5. Images are organized into output folders by template code
 *  6. Final CSV of results is exported
 */
public class ScanView {

    private static final Logger log = LoggerFactory.getLogger(ScanView.class);

    private Stage stage;
    private Template loadedTemplate;

    private Label templateLabel;
    private ListView<String> scannedList;
    private ProgressBar progressBar;
    private Label statusLabel;

    private final List<OmrResult> results = new ArrayList<>();

    public void show(Stage stage) {
        this.stage = stage;

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(buildScanList());
        root.setBottom(buildStatusArea());

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("Scan & Process");
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildToolbar() {
        Button btnLoadTemplate = new Button("Load Template");
        btnLoadTemplate.setOnAction(e -> onLoadTemplate());

        Button btnScan = new Button("Start Batch Scan");
        btnScan.setOnAction(e -> onStartBatchScan());

        Button btnExport = new Button("Export CSV");
        btnExport.setOnAction(e -> onExportCsv());

        templateLabel = new Label("No template loaded");

        HBox toolbar = new HBox(10, btnLoadTemplate, templateLabel, btnScan, btnExport);
        toolbar.setPadding(new Insets(10));
        return toolbar;
    }

    private VBox buildScanList() {
        scannedList = new ListView<>();
        VBox box = new VBox(5, new Label("Scanned copies:"), scannedList);
        box.setPadding(new Insets(10));
        VBox.setVgrow(scannedList, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private VBox buildStatusArea() {
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        statusLabel = new Label("Ready.");
        VBox box = new VBox(5, progressBar, statusLabel);
        box.setPadding(new Insets(10));
        return box;
    }

    // === Actions ===

    private void onLoadTemplate() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select template file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Templates", "*.json"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        try {
            TemplateRepository repo = new TemplateRepository();
            loadedTemplate = repo.load(file);
            templateLabel.setText("Loaded: " + loadedTemplate.getName() + " (" + loadedTemplate.getTemplateCode() + ")");
            log.info("Loaded template: {}", loadedTemplate.getTemplateCode());
            showSourceDialog();   // ← ask how to supply sheets
        } catch (Exception ex) {
            log.error("Failed to load template", ex);
            statusLabel.setText("Error loading template: " + ex.getMessage());
        }
    }

    // =========================================================================
    // Source-selection dialog
    // =========================================================================

    /**
     * After a template is loaded, ask the user whether to upload files or
     * scan from a connected printer/scanner.
     */
    private void showSourceDialog() {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initOwner(stage);
        dlg.setTitle("Select Sheet Source");
        dlg.setResizable(false);

        Label heading = new Label("How would you like to supply answer sheets?");
        heading.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // --- Upload option ---
        Button btnUpload = new Button("📂  Upload PDF / Images");
        btnUpload.setPrefWidth(260);
        btnUpload.setPrefHeight(48);
        btnUpload.setStyle("-fx-font-size: 13px;");
        Label uploadDesc = new Label("Select one or more PDF files or image files from disk.\nEach PDF page is treated as one answer sheet.");
        uploadDesc.setWrapText(true);
        uploadDesc.setStyle("-fx-text-fill: #555;");

        // --- Scanner option ---
        Button btnScan = new Button("🖨  Scan from Connected Printer");
        btnScan.setPrefWidth(260);
        btnScan.setPrefHeight(48);
        btnScan.setStyle("-fx-font-size: 13px;");

        // Detect available devices and show them in the description
        ScannerController sc = new ScannerController();
        List<String> devices = sc.listDevices();
        String deviceDesc = devices.isEmpty()
                ? "No scanner detected — test mode will be used."
                : "Detected: " + String.join(", ", devices);
        Label scanDesc = new Label(deviceDesc);
        scanDesc.setWrapText(true);
        scanDesc.setStyle("-fx-text-fill: #555;");
        if (devices.isEmpty()) btnScan.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");

        Button btnCancel = new Button("Cancel");

        VBox uploadBox = new VBox(4, btnUpload, uploadDesc);
        VBox scanBox   = new VBox(4, btnScan,   scanDesc);

        btnUpload.setOnAction(e -> { dlg.close(); onUploadFiles(); });
        btnScan.setOnAction(e -> {
            dlg.close();
            new Thread(this::runBatchScan).start();
        });
        btnCancel.setOnAction(e -> dlg.close());

        HBox btnRow = new HBox(btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(18, heading, uploadBox, scanBox, btnRow);
        root.setPadding(new Insets(24));
        root.setPrefWidth(400);

        dlg.setScene(new Scene(root));
        dlg.showAndWait();
    }

    // =========================================================================
    // Upload path — PDF or image files
    // =========================================================================

    private void onUploadFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Answer Sheet Files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF & Images", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tiff"),
                new FileChooser.ExtensionFilter("PDF Files",    "*.pdf"),
                new FileChooser.ExtensionFilter("Image Files",  "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tiff")
        );
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) return;

        statusLabel.setText("Processing " + files.size() + " file(s)…");
        new Thread(() -> runUploadProcessing(files)).start();
    }

    private void runUploadProcessing(List<File> files) {
        try {
            OmrProcessor processor = new OmrProcessor(loadedTemplate);
            FileOrganizer organizer = new FileOrganizer("output");
            int total = 0;

            for (File file : files) {
                String name = file.getName().toLowerCase();

                if (name.endsWith(".pdf")) {
                    // Process every page of the PDF
                    PdfImporter importer = new PdfImporter();
                    int pages = importer.getPageCount(file);
                    Platform.runLater(() -> statusLabel.setText(
                            "Processing PDF: " + file.getName() + " (" + pages + " pages)…"));

                    for (int p = 0; p < pages; p++) {
                        BufferedImage img = importer.convertToImage(file, p, 150);
                        total += processAndRecord(img, file.getName() + " p" + (p + 1),
                                processor, organizer, total + 1);
                    }
                } else {
                    // Single image file
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        total += processAndRecord(img, file.getName(), processor, organizer, total + 1);
                    } else {
                        log.warn("Could not read image: {}", file.getName());
                    }
                }
            }

            final int finalTotal = total;
            Platform.runLater(() -> statusLabel.setText(
                    "Done. Processed " + finalTotal + " sheet(s) from " + files.size() + " file(s)."));
        } catch (Exception ex) {
            log.error("Upload processing failed", ex);
            Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
        }
    }

    /**
     * Run OMR on one image, record the result, and update the UI list.
     * Returns 1 on success, 0 on failure.
     */
    private int processAndRecord(BufferedImage img, String sourceName,
                                  OmrProcessor processor, FileOrganizer organizer,
                                  int sheetNumber) {
        try {
            OmrResult result = processor.process(img);
            File saved = organizer.saveByTemplate(
                    loadedTemplate.getTemplateCode(), result.getStudentCode(), img);
            result.setSavedImagePath(saved);
            results.add(result);

            final String entry = String.format("#%d  %s  ←  %s", sheetNumber,
                    result.getStudentCode(), sourceName);
            Platform.runLater(() -> {
                scannedList.getItems().add(entry);
                statusLabel.setText("Processed sheet #" + sheetNumber + " — " + sourceName);
            });
            return 1;
        } catch (Exception ex) {
            log.error("Failed to process sheet from {}", sourceName, ex);
            Platform.runLater(() ->
                    scannedList.getItems().add("ERROR — " + sourceName + ": " + ex.getMessage()));
            return 0;
        }
    }

    // =========================================================================
    // Scanner path
    // =========================================================================

    private void onStartBatchScan() {
        if (loadedTemplate == null) {
            statusLabel.setText("Load a template first!");
            return;
        }
        showSourceDialog();
    }

    private void runBatchScan() {
        try {
            ScannerController scanner = new ScannerController();
            OmrProcessor processor = new OmrProcessor(loadedTemplate);
            FileOrganizer organizer = new FileOrganizer("output");

            Platform.runLater(() -> statusLabel.setText("Connecting to scanner…"));

            List<String> devices = scanner.listDevices();
            if (devices.isEmpty()) {
                Platform.runLater(() -> statusLabel.setText("No scanner detected — using test mode."));
            } else {
                scanner.connect(devices.get(0));
                Platform.runLater(() -> statusLabel.setText("Connected: " + devices.get(0)));
            }

            int totalScanned = 0;
            while (true) {
                BufferedImage img = scanner.scanSinglePage();
                if (img == null) break;
                totalScanned += processAndRecord(img, "Scanner", processor, organizer, totalScanned + 1);
            }

            final int count = totalScanned;
            Platform.runLater(() -> statusLabel.setText(
                    "Scan complete. " + count + " sheet(s) processed."));
        } catch (Exception ex) {
            log.error("Batch scan failed", ex);
            Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
        }
    }

    private void onExportCsv() {
        if (results.isEmpty()) {
            statusLabel.setText("No results to export yet");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save CSV results");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File out = chooser.showSaveDialog(stage);
        if (out == null) return;

        try {
            new CsvExporter().export(results, out, loadedTemplate);
            statusLabel.setText("Exported " + results.size() + " rows to " + out.getName());
        } catch (Exception ex) {
            log.error("Export failed", ex);
        }
    }
}
