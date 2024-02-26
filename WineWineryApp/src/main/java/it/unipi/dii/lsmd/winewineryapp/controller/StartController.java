package it.unipi.dii.lsmd.winewineryapp.controller;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.io.IOException;

public class StartController {
    @FXML
    private Label welcomeText;
    private ActionEvent event;

    @FXML
    protected void onButtonLogin(ActionEvent event) throws IOException {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/login.fxml",event);
    }

    @FXML
    protected void onButtonRegister(ActionEvent event)throws IOException {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/registration.fxml",event);
    }
}