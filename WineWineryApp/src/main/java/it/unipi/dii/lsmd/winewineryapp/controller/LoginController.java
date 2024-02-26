package it.unipi.dii.lsmd.winewineryapp.controller;
import it.unipi.dii.lsmd.winewineryapp.model.Session;
import it.unipi.dii.lsmd.winewineryapp.model.User;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class LoginController {
    private MongoDBManager mongoMan;
    private ActionEvent event;

    @FXML private Label welcomeText;
    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private Label errorField;

    public void initialize () {
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());
    }

    public static String encrypt(String plainText) throws Exception {
        String key = utilitis.readConfigurationParameters().getProperty("AES_KEY");
        SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @FXML
    void login(ActionEvent event) throws Exception {
        this.event = event;
        String usernameV = username.getText();
        String passwordV = encrypt(password.getText());;

        User u = mongoMan.login(usernameV, passwordV);

        if (u == null) {
            username.setText("");
            password.setText("");
            errorField.setText("Username or password not valid");
            System.out.println("Username or password not valid");
        } else {
            Session.getInstance().setLoggedUser(u);
            utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml",event);
            //System.out.println("TEST// LOGIN : User: " + usernameV + " Password " +  passwordV);
        }

    }

    @FXML
    protected void onButtonBack(ActionEvent event) throws IOException {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxml",event);
    }
}