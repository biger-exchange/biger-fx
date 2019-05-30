package com.biger.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class BigerFX extends Application {
    @Override
    public void start(Stage stage) {
        Thread.UncaughtExceptionHandler delegate = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler((t, e)->{
            Platform.runLater(()-> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("error occurred");
                Optional<Throwable> fxException = Utils.getCausalChain(e).stream().filter(x -> x instanceof BigerFXException).findFirst();
                if (fxException.isPresent()) alert.setContentText(fxException.get().getMessage());
                else alert.setContentText(e.getMessage());

                alert.showAndWait();
            });
            if(delegate != null) delegate.uncaughtException(t, e);
        });

        VBox vBox;
        FXMLLoader loader = new FXMLLoader();
        try {
            //vBox = loader.load(Files.newInputStream(Paths.get("src/main/resources/com/biger/fx/ui.fxml")));
            vBox = loader.load(BigerFX.class.getResourceAsStream("/com/biger/fx/ui.fxml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage.setScene(new Scene(vBox));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}