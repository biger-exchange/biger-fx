module com.biger.fx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires biger.client.core;
    requires biger.client.httpops;
    requires biger.client.httpops.httpclient;
    requires org.bouncycastle.provider;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    exports com.biger.fx;

    opens com.biger.fx to javafx.fxml;
}