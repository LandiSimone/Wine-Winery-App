package it.unipi.dii.lsmd.winewineryapp.controller;
import it.unipi.dii.lsmd.winewineryapp.model.User;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

public class UserElementController {
    private User user;
    private MongoDBManager mongoMan;
    @FXML private Label usernameLb;
    @FXML private Text emailTf;
    @FXML private Label analyticLabelName;
    @FXML private Text analyticValue;


    public void initialize() {
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());
    }
    public void setParameters (User user, String analyticLabelName, int analyticValue) {
        this.user = user;

        usernameLb.setText(user.getUsername());
        emailTf.setText(user.getEmail());

        if (analyticLabelName != null) {
            this.analyticLabelName.setText(analyticLabelName);
            this.analyticValue.setText(String.valueOf(analyticValue));
        }
        else {
            this.analyticLabelName.setVisible(false);
            this.analyticValue.setVisible(false);
        }
    }
    @FXML
    void showProfile(MouseEvent event) {
        ProfileController ctrl = (ProfileController) utilitis.changeScene(
                "/it/unipi/dii/lsmd/winewineryapp/layout/profile.fxml", event);

        // If user object is a snap, load the complete user object
        if (user.getPassword().isEmpty() || user.getWinerys() == null)
            user = mongoMan.getUserByUsername(user.getUsername());

        ctrl.setProfilePage(user);
    }
}
