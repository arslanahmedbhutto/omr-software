package com.bise.omr.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main window showing navigation to:
 * - Template Designer (Screen 1)
 * - Scan & Process (Screen 2)
 */
public class MainWindow {

    private Stage stage;

    public void show(Stage primaryStage) {
        this.stage = primaryStage;

        Label title = new Label("OMR Software");
        title.getStyleClass().add("main-title");

        Button btnDesigner = new Button("1. Create Template");
        btnDesigner.setPrefWidth(260);
        btnDesigner.setOnAction(e -> openTemplateDesigner());

        Button btnScan = new Button("2. Scan & Process Copies");
        btnScan.setPrefWidth(260);
        btnScan.setOnAction(e -> openScanView());

        Button btnExit = new Button("Exit");
        btnExit.setPrefWidth(260);
        btnExit.setOnAction(e -> primaryStage.close());

        VBox root = new VBox(18, title, btnDesigner, btnScan, btnExit);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 500, 400);

        primaryStage.setTitle("OMR Software");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openTemplateDesigner() {
        TemplateDesignerView view = new TemplateDesignerView();
        view.show(new Stage());
    }

    private void openScanView() {
        ScanView view = new ScanView();
        view.show(new Stage());
    }
}
