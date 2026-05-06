package com.bise.omr.ui;

import com.bise.omr.scanner.PdfImporter;
import com.bise.omr.template.BubbleGroupField;
import com.bise.omr.template.QRCodeField;
import com.bise.omr.template.SectionMarkerField;
import com.bise.omr.template.Template;
import com.bise.omr.template.TemplateField;
import com.bise.omr.template.TemplateRepository;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 1 — Template Designer.
 *
 * Workflow:
 * 1. User clicks "Upload PDF" → PDF rendered as background image on canvas
 * 2. User selects a tool (QR / Bubble / Section)
 * 3. User drags on canvas to draw a rectangle → config dialog shown → field
 * created
 * 4. Fields listed in right panel; selectable, deletable via panel or
 * right-click menu
 * 5. User enters template code + name → saves as JSON via TemplateRepository
 */
public class TemplateDesignerView {

    private static final Logger log = LoggerFactory.getLogger(TemplateDesignerView.class);

    // Inner records for dialog results (Java 16+)
    private record BubbleConfig(int questionCount, int optionsPerQuestion,
            BubbleGroupField.LayoutDirection direction, String startLabel) {
    }

    private record SectionConfig(String name, String type) {
    }

    private Stage stage;
    private Canvas canvas;
    private ImageView pdfBackground;

    private TextField templateCodeField;
    private TextField templateNameField;
    private Label statusLabel;
    private Label fieldCountLabel;
    private ListView<String> fieldListView;

    // PDF navigation state
    private File currentPdfFile;
    private int currentPage = 0;
    private int totalPages = 0;
    private Label pageLabel;
    private Button btnPrevPage;
    private Button btnNextPage;

    // Save state
    private File lastSavedFile = null;
    private boolean unsavedChanges = false;

    // Zoom state
    private double zoomFactor = 1.0;
    private static final double ZOOM_STEP = 0.15;
    private static final double ZOOM_MIN = 0.25;
    private static final double ZOOM_MAX = 4.0;
    private javafx.scene.transform.Scale canvasScale;
    private Label zoomLabel;
    private StackPane canvasStack;

    private ToggleGroup toolGroup;
    private String selectedTool = "QR";
    private final List<TemplateField> fields = new ArrayList<>();

    private double dragStartX, dragStartY;
    private boolean isDragging = false;
    private int selectedFieldIndex = -1;
    private double contextMenuX, contextMenuY;

    public void show(Stage stage) {
        this.stage = stage;

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setLeft(buildToolPanel());
        root.setCenter(buildCanvasArea());
        root.setRight(buildFieldPanel());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1300, 860);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        // Ctrl+S → quick save
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.S) {
                onSave();
                e.consume();
            }
        });

        stage.setTitle("Template Designer — OMR Software");
        stage.setScene(scene);

        // Warn before closing with unsaved changes
        stage.setOnCloseRequest(e -> {
            if (unsavedChanges) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("You have unsaved changes.");
                alert.setContentText("Save before closing?");
                alert.initOwner(stage);
                alert.getButtonTypes().setAll(
                        javafx.scene.control.ButtonType.YES,
                        javafx.scene.control.ButtonType.NO,
                        javafx.scene.control.ButtonType.CANCEL);
                alert.showAndWait().ifPresent(btn -> {
                    if (btn == javafx.scene.control.ButtonType.YES) {
                        onSave();
                    } else if (btn == javafx.scene.control.ButtonType.CANCEL) {
                        e.consume(); // abort close
                    }
                });
            }
        });

        stage.show();
    }

    // =========================================================================
    // Layout builders
    // =========================================================================

    private HBox buildToolbar() {
        Button btnLoadPdf = new Button("Upload PDF");
        btnLoadPdf.setOnAction(e -> onLoadPdf());

        btnPrevPage = new Button("◀ Prev");
        btnPrevPage.setDisable(true);
        btnPrevPage.setOnAction(e -> navigatePage(-1));

        btnNextPage = new Button("Next ▶");
        btnNextPage.setDisable(true);
        btnNextPage.setOnAction(e -> navigatePage(+1));

        pageLabel = new Label("");

        Button btnZoomIn = new Button("＋ Zoom");
        btnZoomIn.setOnAction(e -> applyZoom(zoomFactor + ZOOM_STEP));

        Button btnZoomOut = new Button("－ Zoom");
        btnZoomOut.setOnAction(e -> applyZoom(zoomFactor - ZOOM_STEP));

        Button btnZoomReset = new Button("100%");
        btnZoomReset.setOnAction(e -> applyZoom(1.0));

        zoomLabel = new Label("100%");
        zoomLabel.setMinWidth(42);

        Button btnSave = new Button("💾 Save  Ctrl+S");
        btnSave.setOnAction(e -> onSave());

        Button btnSaveAs = new Button("Save As…");
        btnSaveAs.setOnAction(e -> onSaveAs());

        Button btnClear = new Button("Clear All");
        btnClear.setOnAction(e -> onClearFields());

        templateCodeField = new TextField();
        templateCodeField.setPromptText("Template code  e.g. OMR-EXAM-2026");
        templateCodeField.setPrefWidth(230);

        templateNameField = new TextField();
        templateNameField.setPromptText("Template name");
        templateNameField.setPrefWidth(180);

        HBox toolbar = new HBox(10,
                btnLoadPdf, btnPrevPage, pageLabel, btnNextPage,
                new Separator(Orientation.VERTICAL),
                btnZoomOut, zoomLabel, btnZoomIn, btnZoomReset,
                new Separator(Orientation.VERTICAL),
                new Label("Code:"), templateCodeField,
                new Label("Name:"), templateNameField,
                new Separator(Orientation.VERTICAL),
                btnSave, btnSaveAs, btnClear);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        return toolbar;
    }

    private VBox buildToolPanel() {
        toolGroup = new ToggleGroup();

        ToggleButton qrTool = new ToggleButton("QR Code Zone");
        qrTool.setToggleGroup(toolGroup);
        qrTool.setSelected(true);
        qrTool.setPrefWidth(150);
        qrTool.setOnAction(e -> selectedTool = "QR");

        ToggleButton bubbleTool = new ToggleButton("Bubble Group");
        bubbleTool.setToggleGroup(toolGroup);
        bubbleTool.setPrefWidth(150);
        bubbleTool.setOnAction(e -> selectedTool = "BUBBLE");

        ToggleButton sectionTool = new ToggleButton("Section Marker");
        sectionTool.setToggleGroup(toolGroup);
        sectionTool.setPrefWidth(150);
        sectionTool.setOnAction(e -> selectedTool = "SECTION");

        HBox qrLegend = makeLegend(Color.web("#2b6cb0"), "QR Code (blue)");
        HBox bubbleLegend = makeLegend(Color.web("#c53030"), "Bubble Group (red)");
        HBox sectionLegend = makeLegend(Color.web("#276749"), "Section (green)");

        Label hint = new Label("Drag on canvas to\ndraw a zone.\nRight-click a zone\nto delete it.");
        hint.getStyleClass().add("hint-label");
        hint.setWrapText(true);

        VBox panel = new VBox(12,
                new Label("Draw Tool:"),
                qrTool, bubbleTool, sectionTool,
                new Separator(),
                new Label("Legend:"),
                qrLegend, bubbleLegend, sectionLegend,
                new Separator(),
                hint);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(168);
        panel.getStyleClass().add("side-panel");
        return panel;
    }

    private HBox makeLegend(Color color, String text) {
        Rectangle swatch = new Rectangle(14, 14, color);
        swatch.setArcWidth(3);
        swatch.setArcHeight(3);
        HBox hb = new HBox(6, swatch, new Label(text));
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }

    private ScrollPane buildCanvasArea() {
        pdfBackground = new ImageView();
        pdfBackground.setPreserveRatio(true);

        canvas = new Canvas(850, 1100);
        setupCanvasEvents();

        canvasStack = new StackPane(pdfBackground, canvas);
        canvasStack.setAlignment(Pos.TOP_LEFT);

        // Attach a Scale transform — zoom changes only this transform
        canvasScale = new javafx.scene.transform.Scale(1.0, 1.0, 0, 0);
        canvasStack.getTransforms().add(canvasScale);

        // Wrap in a plain Pane so ScrollPane sees the scaled size correctly
        javafx.scene.layout.Pane scalingWrapper = new javafx.scene.layout.Pane(canvasStack);
        scalingWrapper.setOnScroll(e -> {
            if (e.isControlDown()) {
                double delta = e.getDeltaY() > 0 ? ZOOM_STEP : -ZOOM_STEP;
                applyZoom(zoomFactor + delta);
                e.consume();
            }
        });

        ScrollPane scroll = new ScrollPane(scalingWrapper);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setPannable(true); // allow middle-click/space pan
        scroll.getStyleClass().add("canvas-scroll");
        return scroll;
    }

    private VBox buildFieldPanel() {
        fieldCountLabel = new Label("Fields: 0");
        fieldCountLabel.getStyleClass().add("field-count-label");

        fieldListView = new ListView<>();
        fieldListView.setPrefWidth(210);
        VBox.setVgrow(fieldListView, Priority.ALWAYS);
        fieldListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            selectedFieldIndex = newVal.intValue();
            redrawAllFields();
        });

        Button btnDeleteSelected = new Button("Delete Selected");
        btnDeleteSelected.setPrefWidth(198);
        btnDeleteSelected.setOnAction(e -> {
            int idx = fieldListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0)
                deleteField(idx);
        });

        Button btnClearAll = new Button("Clear All Fields");
        btnClearAll.setPrefWidth(198);
        btnClearAll.setOnAction(e -> onClearFields());

        VBox panel = new VBox(10,
                fieldCountLabel,
                fieldListView,
                btnDeleteSelected,
                btnClearAll);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(222);
        panel.getStyleClass().add("side-panel");
        return panel;
    }

    private HBox buildStatusBar() {
        statusLabel = new Label("Ready. Upload a PDF and drag zones on the canvas.");
        statusLabel.getStyleClass().add("status-label");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    // =========================================================================
    // Canvas event wiring
    // =========================================================================

    private void setupCanvasEvents() {
        ContextMenu ctxMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Field");
        MenuItem deselectItem = new MenuItem("Deselect");
        ctxMenu.getItems().addAll(deleteItem, deselectItem);

        deleteItem.setOnAction(e -> {
            int idx = hitTest(contextMenuX, contextMenuY);
            if (idx >= 0)
                deleteField(idx);
        });
        deselectItem.setOnAction(e -> {
            selectedFieldIndex = -1;
            fieldListView.getSelectionModel().clearSelection();
            redrawAllFields();
        });

        canvas.setOnContextMenuRequested(e -> {
            contextMenuX = e.getX();
            contextMenuY = e.getY();
            int idx = hitTest(e.getX(), e.getY());
            deleteItem.setDisable(idx < 0);
            ctxMenu.show(canvas, e.getScreenX(), e.getScreenY());
        });

        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getX();
                dragStartY = e.getY();
                isDragging = false;
                ctxMenu.hide();
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (e.getButton() != MouseButton.PRIMARY)
                return;
            isDragging = true;
            redrawAllFields();

            double x = Math.min(dragStartX, e.getX());
            double y = Math.min(dragStartY, e.getY());
            double w = Math.abs(e.getX() - dragStartX);
            double h = Math.abs(e.getY() - dragStartY);
            if (w > 2 && h > 2) {
                Color c = toolColor();
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.setStroke(c);
                gc.setLineWidth(2);
                gc.setLineDashes(7, 4);
                gc.strokeRect(x, y, w, h);
                gc.setLineDashes();
                gc.setFill(c.deriveColor(0, 1, 1, 0.12));
                gc.fillRect(x, y, w, h);
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY)
                return;
            double dx = Math.abs(e.getX() - dragStartX);
            double dy = Math.abs(e.getY() - dragStartY);

            if (isDragging && dx > 10 && dy > 10) {
                onFieldDrawn(dragStartX, dragStartY, e.getX(), e.getY());
            } else if (!isDragging) {
                // plain click — select the field under cursor
                int idx = hitTest(e.getX(), e.getY());
                selectedFieldIndex = idx;
                redrawAllFields();
                if (idx >= 0)
                    fieldListView.getSelectionModel().select(idx);
                else
                    fieldListView.getSelectionModel().clearSelection();
            }
            isDragging = false;
        });
    }

    // =========================================================================
    // Canvas drawing helpers
    // =========================================================================

    private void redrawAllFields() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (int i = 0; i < fields.size(); i++) {
            TemplateField f = fields.get(i);
            drawField(gc, f.getX(), f.getY(), f.getWidth(), f.getHeight(),
                    fieldColor(f), fieldLabel(f), i == selectedFieldIndex);
        }
    }

    private void drawField(GraphicsContext gc, double x, double y, double w, double h,
            Color color, String label, boolean selected) {
        if (selected) {
            gc.setStroke(Color.ORANGE);
            gc.setLineWidth(3);
            gc.setLineDashes(8, 4);
        } else {
            gc.setStroke(color);
            gc.setLineWidth(2);
            gc.setLineDashes();
        }
        gc.strokeRect(x, y, w, h);
        gc.setLineDashes();

        gc.setFill(color.deriveColor(0, 1, 1, selected ? 0.25 : 0.15));
        gc.fillRect(x, y, w, h);

        gc.setFill(selected ? Color.DARKORANGE : color.darker());
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        gc.fillText(label, x + 5, y + 14);
    }

    private Color fieldColor(TemplateField f) {
        if (f instanceof QRCodeField)
            return Color.web("#2b6cb0");
        if (f instanceof BubbleGroupField)
            return Color.web("#c53030");
        return Color.web("#276749");
    }

    private String fieldLabel(TemplateField f) {
        if (f instanceof BubbleGroupField bg) {
            return String.format("[Bubbles] %s  %dq × %dopt  (%s)",
                    bg.getStartLabel(), bg.getQuestionCount(),
                    bg.getOptionsPerQuestion(), bg.getDirection());
        }
        if (f instanceof SectionMarkerField sm) {
            return "[" + sm.getSectionType() + "] " + sm.getSectionName();
        }
        return "[QR] " + f.getFieldId();
    }

    private Color toolColor() {
        return switch (selectedTool) {
            case "BUBBLE" -> Color.web("#c53030");
            case "SECTION" -> Color.web("#276749");
            default -> Color.web("#2b6cb0");
        };
    }

    private int hitTest(double mx, double my) {
        // iterate in reverse so topmost-drawn field wins
        for (int i = fields.size() - 1; i >= 0; i--) {
            TemplateField f = fields.get(i);
            if (mx >= f.getX() && mx <= f.getX() + f.getWidth()
                    && my >= f.getY() && my <= f.getY() + f.getHeight()) {
                return i;
            }
        }
        return -1;
    }

    // =========================================================================
    // Field creation
    // =========================================================================

    private void onFieldDrawn(double x1, double y1, double x2, double y2) {
        int x = (int) Math.min(x1, x2);
        int y = (int) Math.min(y1, y2);
        int w = (int) Math.abs(x2 - x1);
        int h = (int) Math.abs(y2 - y1);

        TemplateField field = createField(x, y, w, h);
        if (field != null) {
            fields.add(field);
            selectedFieldIndex = fields.size() - 1;
            redrawAllFields();
            updateFieldList();
            markUnsaved();
            statusLabel.setText("Added " + selectedTool + " field: " + fieldLabel(field));
            log.info("Added {} field at ({},{}) {}x{}", selectedTool, x, y, w, h);
        } else {
            redrawAllFields(); // remove the live-preview rectangle
        }
    }

    private TemplateField createField(int x, int y, int w, int h) {
        return switch (selectedTool) {
            case "QR" -> new QRCodeField("qr_" + fields.size(), x, y, w, h, 1);
            case "BUBBLE" -> {
                BubbleConfig cfg = showBubbleConfigDialog();
                if (cfg == null)
                    yield null;
                BubbleGroupField f = new BubbleGroupField(
                        "bubble_" + fields.size(), x, y, w, h, 1,
                        cfg.questionCount(), cfg.optionsPerQuestion());
                f.setDirection(cfg.direction());
                f.setStartLabel(cfg.startLabel());
                yield f;
            }
            case "SECTION" -> {
                SectionConfig cfg = showSectionConfigDialog();
                if (cfg == null)
                    yield null;
                yield new SectionMarkerField(
                        "section_" + fields.size(), x, y, w, h, 1,
                        cfg.name(), cfg.type());
            }
            default -> null;
        };
    }

    private void deleteField(int idx) {
        if (idx < 0 || idx >= fields.size())
            return;
        String label = fieldLabel(fields.get(idx));
        fields.remove(idx);
        if (selectedFieldIndex == idx)
            selectedFieldIndex = -1;
        else if (selectedFieldIndex > idx)
            selectedFieldIndex--;
        redrawAllFields();
        updateFieldList();
        markUnsaved();
        statusLabel.setText("Deleted field: " + label);
    }

    private void updateFieldList() {
        fieldListView.getItems().clear();
        for (TemplateField f : fields) {
            fieldListView.getItems().add(fieldLabel(f));
        }
        fieldCountLabel.setText("Fields: " + fields.size());
        if (selectedFieldIndex >= 0 && selectedFieldIndex < fields.size()) {
            fieldListView.getSelectionModel().select(selectedFieldIndex);
        }
    }

    // =========================================================================
    // Config dialogs
    // =========================================================================

    private BubbleConfig showBubbleConfigDialog() {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initOwner(stage);
        dlg.setTitle("Bubble Group Settings");
        dlg.setResizable(false);

        Spinner<Integer> qCount = new Spinner<>(1, 200, 20);
        qCount.setEditable(true);
        qCount.setPrefWidth(110);

        Spinner<Integer> opts = new Spinner<>(2, 10, 4);
        opts.setEditable(true);
        opts.setPrefWidth(110);

        ChoiceBox<String> dirBox = new ChoiceBox<>();
        dirBox.getItems().addAll("HORIZONTAL", "VERTICAL");
        dirBox.setValue("HORIZONTAL");

        TextField startLbl = new TextField("Q1");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Number of questions:"), qCount);
        grid.addRow(1, new Label("Options per question:"), opts);
        grid.addRow(2, new Label("Layout direction:"), dirBox);
        grid.addRow(3, new Label("Start label (e.g. Q1):"), startLbl);

        Button btnOk = new Button("Add Field");
        Button btnCancel = new Button("Cancel");
        HBox btnRow = new HBox(10, btnOk, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(4, 20, 20, 0));

        BubbleConfig[] result = { null };
        btnOk.setOnAction(e -> {
            BubbleGroupField.LayoutDirection dir = "VERTICAL".equals(dirBox.getValue())
                    ? BubbleGroupField.LayoutDirection.VERTICAL
                    : BubbleGroupField.LayoutDirection.HORIZONTAL;
            result[0] = new BubbleConfig(
                    qCount.getValue(), opts.getValue(), dir, startLbl.getText().trim());
            dlg.close();
        });
        btnCancel.setOnAction(e -> dlg.close());

        VBox root = new VBox(grid, btnRow);
        Scene scene = new Scene(root, 370, 270);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        dlg.setScene(scene);
        dlg.showAndWait();
        return result[0];
    }

    private SectionConfig showSectionConfigDialog() {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initOwner(stage);
        dlg.setTitle("Section Marker Settings");
        dlg.setResizable(false);

        TextField nameField = new TextField("Section");
        ChoiceBox<String> typeBox = new ChoiceBox<>();
        typeBox.getItems().addAll("SHORT", "LONG", "ATP");
        typeBox.setValue("SHORT");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Section name:"), nameField);
        grid.addRow(1, new Label("Section type:"), typeBox);

        Button btnOk = new Button("Add Field");
        Button btnCancel = new Button("Cancel");
        HBox btnRow = new HBox(10, btnOk, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(4, 20, 20, 0));

        SectionConfig[] result = { null };
        btnOk.setOnAction(e -> {
            result[0] = new SectionConfig(nameField.getText().trim(), typeBox.getValue());
            dlg.close();
        });
        btnCancel.setOnAction(e -> dlg.close());

        VBox root = new VBox(grid, btnRow);
        Scene scene = new Scene(root, 320, 200);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        dlg.setScene(scene);
        dlg.showAndWait();
        return result[0];
    }

    // =========================================================================
    // Actions
    // =========================================================================

    // =========================================================================
    // Save helpers
    // =========================================================================

    /** Mark that there are unsaved changes and reflect it in the title bar. */
    private void markUnsaved() {
        unsavedChanges = true;
        if (!stage.getTitle().startsWith("*")) {
            stage.setTitle("* Template Designer — OMR Software");
        }
    }

    /** Clear the unsaved flag and remove the asterisk from the title. */
    private void markSaved(File file) {
        lastSavedFile = file;
        unsavedChanges = false;
        stage.setTitle("Template Designer — OMR Software  [" + file.getName() + "]");
    }

    /** Quick save: re-save to the last file, or fall through to Save As. */
    private void onSave() {
        if (lastSavedFile != null) {
            saveToFile(lastSavedFile);
        } else {
            onSaveAs();
        }
    }

    /** Save As: open a FileChooser so the user picks the destination. */
    private void onSaveAs() {
        String code = templateCodeField.getText().trim();
        String name = templateNameField.getText().trim();
        if (code.isEmpty() || name.isEmpty()) {
            showAlert("Missing Info", "Enter a template code and name before saving.");
            return;
        }
        if (fields.isEmpty()) {
            showAlert("No Fields", "Add at least one field before saving the template.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Template As");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Template", "*.json"));
        chooser.setInitialFileName(code + ".json");
        if (lastSavedFile != null) {
            chooser.setInitialDirectory(lastSavedFile.getParentFile());
        }
        File dest = chooser.showSaveDialog(stage);
        if (dest == null)
            return;
        saveToFile(dest);
    }

    /** Perform the actual write and update saved state. */
    private void saveToFile(File dest) {
        String code = templateCodeField.getText().trim();
        String name = templateNameField.getText().trim();
        if (code.isEmpty() || name.isEmpty()) {
            showAlert("Missing Info", "Enter a template code and name before saving.");
            return;
        }
        if (fields.isEmpty()) {
            showAlert("No Fields", "Add at least one field before saving the template.");
            return;
        }

        Template template = new Template(code, name,
                (int) canvas.getWidth(), (int) canvas.getHeight());
        for (TemplateField f : fields)
            template.addField(f);

        try {
            new TemplateRepository(dest.getParentFile().toPath()).save(template);
            // rename file to exactly what the chooser said (repo uses code+".json")
            File repoOut = dest.getParentFile().toPath().resolve(code + ".json").toFile();
            if (!repoOut.equals(dest))
                repoOut.renameTo(dest);

            markSaved(dest);
            statusLabel.setText("Saved → " + dest.getAbsolutePath());
            log.info("Template saved to: {}", dest.getAbsolutePath());
        } catch (Exception ex) {
            log.error("Failed to save template", ex);
            statusLabel.setText("Error saving: " + ex.getMessage());
        }
    }

    private void applyZoom(double factor) {
        zoomFactor = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, factor));
        canvasScale.setX(zoomFactor);
        canvasScale.setY(zoomFactor);
        // Resize the wrapper so the ScrollPane scrollbars reflect scaled dimensions
        double scaledW = canvas.getWidth() * zoomFactor;
        double scaledH = canvas.getHeight() * zoomFactor;
        canvasStack.setPrefSize(scaledW, scaledH);
        zoomLabel.setText((int) Math.round(zoomFactor * 100) + "%");
    }

    private void onLoadPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select OMR sheet PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File pdf = chooser.showOpenDialog(stage);
        if (pdf == null)
            return;

        try {
            PdfImporter importer = new PdfImporter();
            totalPages = importer.getPageCount(pdf);
            currentPage = 0;
            currentPdfFile = pdf;
            renderPage(0);
            log.info("Loaded PDF: {} ({} page(s))", pdf.getName(), totalPages);
        } catch (Exception ex) {
            log.error("Failed to load PDF", ex);
            statusLabel.setText("Error loading PDF: " + ex.getMessage());
        }
    }

    private void navigatePage(int delta) {
        int target = currentPage + delta;
        if (target < 0 || target >= totalPages)
            return;
        currentPage = target;
        try {
            renderPage(currentPage);
        } catch (Exception ex) {
            log.error("Failed to render page {}", currentPage, ex);
            statusLabel.setText("Error rendering page: " + ex.getMessage());
        }
    }

    private void renderPage(int pageIndex) throws Exception {
        PdfImporter importer = new PdfImporter();
        BufferedImage img = importer.convertToImage(currentPdfFile, pageIndex, 150);
        javafx.scene.image.Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(img, null);
        pdfBackground.setImage(fxImage);
        canvas.setWidth(fxImage.getWidth());
        canvas.setHeight(fxImage.getHeight());
        redrawAllFields();
        applyZoom(zoomFactor); // resize wrapper to match new page dimensions

        // Update navigation controls
        btnPrevPage.setDisable(pageIndex == 0);
        btnNextPage.setDisable(pageIndex == totalPages - 1);
        pageLabel.setText("Page " + (pageIndex + 1) + " / " + totalPages);

        statusLabel.setText("Loaded: " + currentPdfFile.getName()
                + "  page " + (pageIndex + 1) + "/" + totalPages
                + "  (" + (int) fxImage.getWidth() + " × " + (int) fxImage.getHeight() + " px)");
    }

    private void onClearFields() {
        if (fields.isEmpty())
            return;
        fields.clear();
        selectedFieldIndex = -1;
        redrawAllFields();
        updateFieldList();
        markUnsaved();
        statusLabel.setText("All fields cleared.");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(stage);
        alert.showAndWait();
    }
}
