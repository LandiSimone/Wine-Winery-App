package it.unipi.dii.lsmd.winewineryapp.controller;
import it.unipi.dii.lsmd.winewineryapp.model.Session;
import it.unipi.dii.lsmd.winewineryapp.model.User;
import it.unipi.dii.lsmd.winewineryapp.model.Winery;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jManager;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

public class RegistrationController {
    private MongoDBManager mongoMan;
    private Neo4jManager neoMan;
    private ActionEvent event;

    @FXML private TextField username;
    @FXML private TextField email;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField age;
    @FXML private ComboBox<String> state;
    @FXML private PasswordField password;
    @FXML private PasswordField password_confirm;
    @FXML private Label errorMessage;


    public void initialize () {
        neoMan = new Neo4jManager(Neo4jDriver.getInstance().openConnection());
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());
        state.getItems().addAll(
                "United States", "China", "Russia", "India", "Brazil",
                "Germany", "United Kingdom", "France", "Italy", "Japan",
                "Canada", "Australia", "South Korea", "Saudi Arabia", "South Africa",
                "Mexico", "Indonesia", "Turkey", "Argentina", "Nigeria"
        );
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
    void submit(ActionEvent event) throws Exception {
        String usernameV = username.getText();
        String emailV = email.getText();
        String passwordV = password.getText();
        String repeatPasswordV = password_confirm.getText();
        String firstNameV = firstName.getText();
        String lastNameV = lastName.getText();
        int ageV = 0;
        try {
            ageV = Integer.parseInt(age.getText());
        } catch (NumberFormatException e) {
            errorMessage.setText("Please enter a valid age.");
            return;
        }
        String locationV = state.getValue();
        int typeV = 0; // normal user

        if (usernameV.isEmpty() || emailV.isEmpty() || passwordV.isEmpty() || firstNameV.isEmpty() || lastNameV.isEmpty() || locationV == null) {
            errorMessage.setText("Please fill in all fields.");
            return;
        }

        if (ageV <= 0 || ageV > 100) {
            errorMessage.setText("Please enter a valid age.");
            return;
        }

        if (!isValidEmail(emailV)) {
            errorMessage.setText("Please enter a valid email.");
            return;
        }
        if(!passwordV.equals(repeatPasswordV)) {
            errorMessage.setText("Passwords do not match.");
            return;
        }

        if (mongoMan.getUserByUsername(usernameV) != null) {
            errorMessage.setText("Username already registered.");
            return;
        }

        User newUser = new User(usernameV, emailV, encrypt(passwordV), firstNameV, lastNameV, ageV, locationV, new ArrayList<Winery>(), typeV);

        if (!mongoMan.addUser(newUser)) {
            errorMessage.setText("Database not available.");
            return;
        }

        if (!neoMan.addUser(newUser)) {
            mongoMan.deleteUser(newUser);
            errorMessage.setText("Database not available.");
            return;
        }

        Session.getInstance().setLoggedUser(newUser);
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml", event);
    }

    boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    @FXML
    protected void onButtonBack(ActionEvent event) throws IOException {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxml",event);
    }
}
