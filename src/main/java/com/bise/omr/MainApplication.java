package com.bise.omr;

import com.bise.omr.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the OMR Software.
 * Launches the JavaFX application.
 */
public class MainApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApplication.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting OMR Software v1.0.0");

        MainWindow mainWindow = new MainWindow();
        mainWindow.show(primaryStage);
    }

    @Override
    public void stop() throws Exception {
        log.info("Shutting down OMR Software");
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
