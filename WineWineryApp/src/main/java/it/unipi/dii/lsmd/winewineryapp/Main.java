package it.unipi.dii.lsmd.winewineryapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 900);
        stage.setTitle("WineWineryApp!");
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream( "img/iconApp.png")));
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}