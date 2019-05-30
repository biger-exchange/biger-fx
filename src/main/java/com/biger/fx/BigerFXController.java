package com.biger.fx;

import com.biger.client.BigerClient;
import com.biger.client.OrderClient;
import com.biger.client.httpops.BigerResponseException;
import com.biger.client.util.RSAKeyPairGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bouncycastle.util.io.pem.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class BigerFXController {

    final AtomicReference<BigerClient> client = new AtomicReference<>(null);

    @FXML
    private Stage stage;

    @FXML
    private TextField baseUrlInput;

    @FXML
    private TextField accessTokenInput;

    @FXML
    void generateDERKeyPair(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose location for private key");
        fc.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());
        fc.setInitialFileName(".myPrivKey.der");
        File privKeyFile = fc.showSaveDialog(stage);

        if (privKeyFile == null) return;

        fc = new FileChooser();
        fc.setTitle("Choose location for public key");
        fc.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());
        fc.setInitialFileName(".myPubKey.der");
        File pubKeyFile = fc.showSaveDialog(stage);

        if (pubKeyFile == null) return;

        try (FileOutputStream privKeyOut = new FileOutputStream(privKeyFile);FileOutputStream pubKeyOut = new FileOutputStream(pubKeyFile)) {
            RSAKeyPairGenerator.generateKeyPair(privKeyOut, pubKeyOut);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("RSA key pair generation");
        alert.setContentText("Generated successfully! Private key at - " + privKeyFile + " and public key at - " + pubKeyFile);

        alert.showAndWait();
    }

    @FXML
    void generatePEMKeyPair(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose location for private key");
        fc.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());
        fc.setInitialFileName(".myPrivKey.pem");
        File privKeyFile = fc.showSaveDialog(stage);

        if (privKeyFile == null) return;

        fc = new FileChooser();
        fc.setTitle("Choose location for public key");
        fc.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());
        fc.setInitialFileName(".myPubKey.pem");
        File pubKeyFile = fc.showSaveDialog(stage);

        if (pubKeyFile == null) return;

        ByteArrayOutputStream privKeyOut = new ByteArrayOutputStream();
        ByteArrayOutputStream pubKeyOut = new ByteArrayOutputStream();
        try {
            RSAKeyPairGenerator.generateKeyPair(privKeyOut, pubKeyOut);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
        try (FileWriter privKeyWriter = new FileWriter(privKeyFile); FileWriter pubKeyWriter = new FileWriter(pubKeyFile)){
            new PemWriter(privKeyWriter).writeObject(() -> new PemObject("PUBLIC KEY", privKeyOut.toByteArray()));
            new PemWriter(pubKeyWriter).writeObject(() -> new PemObject("PUBLIC KEY", pubKeyOut.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("RSA key pair generation");
        alert.setContentText("Generated successfully! Private key at - " + privKeyFile + " and public key at - " + pubKeyFile);

        alert.showAndWait();
    }

    @FXML
    void loadDERPrivateKey(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose location of private key");
        fc.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());
        File privKeyFile = fc.showOpenDialog(stage);

        if (privKeyFile == null) return;

        try {
            client.set(BigerClient.builder()
                    .accessToken(accessTokenInput.getText())
                    .privateKey(Files.readAllBytes(privKeyFile.toPath()))
                    .url(baseUrlInput.getText())
                    .build());
        } catch (Exception e) {
            throw new BigerFXException(e);
        }
    }

    @FXML
    void loadPEMPrivateKey(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose location of private key");
        fc.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());
        File pubKeyFile = fc.showOpenDialog(stage);

        if (pubKeyFile == null) return;

        try (FileReader reader = new FileReader(pubKeyFile)){
            client.set(BigerClient.builder()
                    .accessToken(accessTokenInput.getText())
                    .privateKey(new PemReader(reader).readPemObject().getContent())
                    .url(baseUrlInput.getText())
                    .build());
        } catch (Exception e) {
            throw new BigerFXException(e);
        }
    }

    @FXML
    void queryOrder(ActionEvent event) {
        Button source = (Button)event.getSource();
        Parent parent = source.getParent();
        TextField orderIdInput = (TextField)parent.lookup("#inputOrderId");

        client().orders().query(orderIdInput.getText())
                .thenApply(Optional::ofNullable)
                .thenApply(o->o.map(OrderClient.OrderInfo::toString))
                .thenApply(o->o.orElse("no such order"))
                .thenAccept(((TextArea)parent.lookup("#output"))::setText)
                .exceptionally(t -> {
                    Platform.runLater(()->{
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Order query");
                        alert.setContentText("query failed - " + t.getMessage());

                        alert.showAndWait();
                    });
                    return null;
                });
    }

    @FXML
    void cancelOrder(ActionEvent event) {
        Button source = (Button)event.getSource();
        Parent parent = source.getParent();
        TextField orderIdInput = (TextField)parent.lookup("#inputOrderId");

        client().orders().cancel(orderIdInput.getText())
                .thenAccept(aVoid -> Platform.runLater(()->{
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Order cancellation");
                    alert.setContentText("Request accepted");

                    alert.showAndWait();
                }))
                .exceptionally(t -> {
                    Optional<Throwable> apiException = Utils.getCausalChain(t).stream().filter(x -> x instanceof BigerResponseException).findFirst();
                    Platform.runLater(()->{
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Order cancellation");
                        alert.setContentText("request rejected - " + t.getMessage());
                        if (apiException.isPresent()) {
                            try {
                                JsonNode n = new ObjectMapper().readTree(((BigerResponseException) apiException.get()).respBody);
                                if (n.get("code") != null) {
                                    OrderClient.ErrorCode.forCode(n.get("code").asInt()).ifPresent(errorCode -> alert.setContentText("request rejected - " + errorCode.name()));
                                }
                            } catch (IOException e) {
                                alert.setContentText("request rejected - " + e.getMessage());
                            }
                        }
                        alert.showAndWait();
                    });
                    return null;
                });
    }

    BigerClient client() {
        BigerClient c = client.get();
        if (c == null) throw new BigerFXException("Please perform setup first");
        return c;
    }
}
