module it.unipi.dii.lsmd.winewineryapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.mongodb.driver.core;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires com.google.gson;
    requires org.neo4j.driver;

    opens it.unipi.dii.lsmd.winewineryapp;

    opens it.unipi.dii.lsmd.winewineryapp.controller to javafx.fxml;
    opens it.unipi.dii.lsmd.winewineryapp.model to com.google.gson;
    opens it.unipi.dii.lsmd.winewineryapp.persistence to javafx.fxml;

    exports it.unipi.dii.lsmd.winewineryapp;
    exports it.unipi.dii.lsmd.winewineryapp.controller;
    exports it.unipi.dii.lsmd.winewineryapp.persistence;
    exports it.unipi.dii.lsmd.winewineryapp.model;
}