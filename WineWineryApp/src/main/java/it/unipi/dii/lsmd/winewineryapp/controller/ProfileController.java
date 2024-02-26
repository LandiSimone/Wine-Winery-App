package it.unipi.dii.lsmd.winewineryapp.controller;
import it.unipi.dii.lsmd.winewineryapp.model.*;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jManager;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import java.util.*;


public class ProfileController {
    private User user;
    private MongoDBManager mongoMan;
    private Neo4jManager neoMan;
    @FXML
    private Label username_side;
    @FXML
    private Label username;
    @FXML
    private Label email;
    @FXML
    private Label firstname;
    @FXML
    private Label lastname;
    @FXML
    private Label age;
    @FXML
    private Label nFollower;
    @FXML
    private Label nFollowing;
    @FXML
    private Label nWinerys;
    @FXML
    private VBox winerybox;
    @FXML
    private Button FollowBTN;
    @FXML
    private Button EditBTN;
    @FXML
    private Button AddWineryBTN;
    @FXML
    private Button BackBTN;
    @FXML
    private Button DeleteUserBTN;
    @FXML
    private Button ElectModBTN;
    @FXML private Button ShowFollowerBTN;
    @FXML private Button ShowFollowingBTN;
    @FXML private Button myprofile_btn;

    private ActionEvent event;

    public void initialize() {
        neoMan = new Neo4jManager(Neo4jDriver.getInstance().openConnection());
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());

        AddWineryBTN.setOnMouseClicked(mouseEvent -> clickAddWineryBTN(mouseEvent));
        EditBTN.setOnMouseClicked(mouseEvent -> clickEditBTN(mouseEvent));
        FollowBTN.setOnMouseClicked(mouseEvent -> clickFollowBTN(mouseEvent));
        BackBTN.setOnMouseClicked(mouseEvent -> clickBackBTN(mouseEvent));
        ElectModBTN.setOnMouseClicked(mouseEvent -> clickElectModBTN(mouseEvent));
        DeleteUserBTN.setOnMouseClicked(mouseEvent -> clickDeleteUserBTN(mouseEvent));
        ShowFollowerBTN.setOnMouseClicked(mouseEvent -> clickShowFollowingBTN(mouseEvent));
        ShowFollowingBTN.setOnMouseClicked(mouseEvent -> clickShowFollowerBTN(mouseEvent));
    }

    public void setProfilePage(User user) {
        this.user = user;
        Session.getInstance().getPreviousPageUsers().add(user);

        username_side.setText(Session.getInstance().getLoggedUser().getUsername());
        username.setText(user.getUsername());
        email.setText(user.getEmail());
        firstname.setText(user.getFirstName());
        lastname.setText(user.getLastName());
        age.setText(String.valueOf(user.getAge()));

        nFollower.setText(String.valueOf(neoMan.getNumFollowingUser(user.getUsername())));
        nFollowing.setText(String.valueOf(neoMan.getNumFollowedUser(user.getUsername())));

        int nWinerysaved = 0;
        if(user.getWinerys() != null)
            for (Winery r : user.getWinerys())
                nWinerysaved += 1;
        nWinerys.setText(String.valueOf(nWinerysaved));

        if (neoMan.userAFollowsUserB(Session.getInstance().getLoggedUser().getUsername(), user.getUsername()))
            FollowBTN.setText("Unfollow");
        if (user.getUsername().equals(Session.getInstance().getLoggedUser().getUsername())) {       // if the user is the logged user
            FollowBTN.setVisible(false);
            EditBTN.setVisible(true);
            AddWineryBTN.setVisible(true);
        } else {
            FollowBTN.setVisible(true);
            EditBTN.setVisible(false);
            AddWineryBTN.setVisible(false);
        }

        // admin
        if (Session.getInstance().getLoggedUser().getType() == 2 &&
                !user.getUsername().equals(Session.getInstance().getLoggedUser().getUsername())) {
            ElectModBTN.setVisible(true);
            DeleteUserBTN.setVisible(true);

            if (user.getType() == 1)
                ElectModBTN.setText("Dismiss Moderator");
            else
                ElectModBTN.setText("Elect Moderator");
        } else {
            ElectModBTN.setVisible(false);
            DeleteUserBTN.setVisible(false);
        }

        winerybox.getChildren().clear();
        LoadWinerys();

        if (Session.getInstance().getLoggedUser().getType() == 2){
            myprofile_btn.setVisible(false);
            FollowBTN.setVisible(false);
        }
    }

    private void LoadWinerys() {
        if (!user.getWinerys().isEmpty()) {
            Iterator<Winery> it = user.getWinerys().iterator();

            while (it.hasNext()) {
                HBox row = new HBox();
                row.setAlignment(Pos.CENTER);
                row.setStyle("-fx-padding: 10px");
                Winery w = it.next();
                Pane p = loadWineryElement(w, user.getUsername());

                row.getChildren().addAll(p);
                winerybox.getChildren().add(row);
            }
        } else {
            winerybox.getChildren().add(new Label("No Winery :("));
        }
    }

    private Pane loadWineryElement(Winery w, String owner) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/it/unipi/dii/lsmd/winewineryapp/layout/wineryElement.fxml"));
            pane = loader.load();
            WineryElementController ctrl = loader.getController();
            ctrl.setWineryElement(w, owner, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }

    private void clickAddWineryBTN(MouseEvent mouseEvent) {
        if (user.getWinerys().size() >= 10) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("You have too many Winery");
            alert.show();
            return;
        }
        TextInputDialog td = new TextInputDialog("Winery" +
                (Session.getInstance().getLoggedUser().getWinerys().size() + 1));
        td.setHeaderText("Insert the title of the Winery");
        td.showAndWait();

        // Add new Winery to DB
        boolean res = mongoMan.createWinery(Session.getInstance().getLoggedUser(), td.getEditor().getText());
        if (!res) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("There is already a Winery with this Title");
            alert.show();
            return;
        }

        res = neoMan.createWinery(td.getEditor().getText(), Session.getInstance().getLoggedUser().getUsername());
        if (!res) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error in Create Winery");
            mongoMan.deleteWinery(Session.getInstance().getLoggedUser().getUsername(), td.getEditor().getText());
            alert.show();
            return;
        }
        // Refresh Page Content
        User refreshUser = Session.getInstance().getLoggedUser();
        refreshUser.getWinerys().add(new Winery(td.getEditor().getText(), new ArrayList<>()));
        Session.getInstance().setLoggedUser(refreshUser);
        setProfilePage(refreshUser);
    }

    private void clickEditBTN(MouseEvent mouseEvent) {
        /* Edit form */
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Please specify…");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField firstName = new TextField(Session.getInstance().getLoggedUser().getFirstName());
        firstName.setPromptText("First Name");
        TextField lastName = new TextField(Session.getInstance().getLoggedUser().getLastName());
        lastName.setPromptText("Last Name");
        TextField age = new TextField(String.valueOf(Session.getInstance().getLoggedUser().getAge()));
        age.setPromptText("Age");
        TextField email = new TextField(Session.getInstance().getLoggedUser().getEmail());
        email.setPromptText("Email");

        TextField location = new TextField(Session.getInstance().getLoggedUser().getLocation());
        location.setPromptText("Location");

        dialogPane.setContent(new VBox(8, firstName, lastName, age, email,location));
        Platform.runLater(firstName::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new User(Session.getInstance().getLoggedUser().getUsername(), email.getText(), Session.getInstance().getLoggedUser().getPassword(), firstName.getText(), lastName.getText(), Integer.parseInt(age.getText()), location.getText(),  Session.getInstance().getLoggedUser().getWinerys(), Session.getInstance().getLoggedUser().getType());
            }
            return null;
        });


        Optional<User> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((User u) -> {
            if (!mongoMan.updateUser(u)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error: An error occurred. Try again later :(");
                alert.showAndWait();
                return;
            }
            if(!Objects.equals(Session.getInstance().getLoggedUser().getEmail(), email.getText()))
                if (!neoMan.updateUser(u)) {
                    // Restore previous information if errors occur
                    mongoMan.updateUser(Session.getInstance().getLoggedUser());
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: An error occurred. Try again later :(");
                    alert.showAndWait();
                    return;
                }
            // Refresh Page Content
            Session.getInstance().setLoggedUser(u);
            setProfilePage(u);
        });
    }

    private void clickFollowBTN(MouseEvent mouseEvent) {
        String tmp = FollowBTN.getText();
        if (tmp.equals("Follow")) {
            neoMan.followUser(Session.getInstance().getLoggedUser().getUsername(), user.getUsername());
            FollowBTN.setText("Unfollow");
            // Update the n Follower label
            int newNumFollower = Integer.parseInt(nFollower.getText()) + 1;
            nFollower.setText(String.valueOf(newNumFollower));
        } else {
            neoMan.unfollowUser(Session.getInstance().getLoggedUser().getUsername(), user.getUsername());
            FollowBTN.setText("Follow");
            // Update the n Follower label
            int newNumFollower = Integer.parseInt(nFollower.getText()) - 1;
            nFollower.setText(String.valueOf(newNumFollower));
        }

    }

    private void clickDeleteUserBTN(MouseEvent mouseEvent) {
        if (!mongoMan.deleteUser(user)) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: An error occurred. Try again later :(");
            alert.showAndWait();
            return;
        }
        if (!neoMan.deleteUser(user)) {
            mongoMan.addUser(user);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: An error occurred. Try again later :(");
            alert.showAndWait();
            return;
        }
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml", mouseEvent);
    }

   private void clickElectModBTN(MouseEvent mouseEvent) {
        if (ElectModBTN.getText().equals("Elect Moderator")) {
            ElectModBTN.setText("Dismiss Moderator");
            user.setType(1);
        }
        else {
            ElectModBTN.setText("Elect Moderator");
            user.setType(0);
        }
        mongoMan.updateUser(user);
    }

    private void clickBackBTN(MouseEvent mouseEvent) {
        // Pop
        Session.getInstance().getPreviousPageUsers().remove(Session.getInstance().getPreviousPageUsers().size() - 1);

        // Check if previous page is a Wine Page
        if (Session.getInstance().getPreviousPageWines().isEmpty())
            utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml", mouseEvent);
        else {
            WinePageController ctrl = (WinePageController) utilitis.changeScene(
                    "/it/unipi/dii/lsmd/winewineryapp/layout/winePage.fxml", mouseEvent);
            ctrl.setWinePage(Session.getInstance().getPreviousPageWines().remove(
                    Session.getInstance().getPreviousPageWines().size() - 1));
        }
    }

    private void clickShowFollowerBTN(MouseEvent mouseEvent){
        List<User> followers = neoMan.getSnapsOfFollowedUser(user);
        if(followers.isEmpty()){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("No followers");
            alert.show();
            return;
        }
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Followers");
        dialog.setHeaderText("List of followers");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPrefWidth(300);
        dialogPane.getButtonTypes().addAll(ButtonType.OK);
        VBox vBox = new VBox();
        for(User u : followers){
            Label l = new Label(u.getUsername());
            vBox.getChildren().add(l);

            // se clicca sul utente deve aprire la sua pagina profilo
            l.setOnMouseClicked(mouseEvent1 -> {
                ProfileController ctrl = (ProfileController) utilitis.changeScene(
                        "/it/unipi/dii/lsmd/winewineryapp/layout/profile.fxml", mouseEvent);
                ctrl.setProfilePage(mongoMan.getUserByUsername(u.getUsername()));
                dialog.close();
            });
        }
        dialogPane.setContent(vBox);
        dialog.showAndWait();
    }

    private void clickShowFollowingBTN(MouseEvent mouseEvent){
        List<User> following = neoMan.getSnapsOfFollowingUser(user);
        if(following.isEmpty()){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("No following");
            alert.show();
            return;
        }
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Following");
        dialog.setHeaderText("List of following");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPrefWidth(300);
        dialogPane.getButtonTypes().addAll(ButtonType.OK);
        VBox vBox = new VBox();

        for(User u : following){
            Label l = new Label(u.getUsername());
            vBox.getChildren().add(l);

            // se clicca sul utente deve aprire la sua pagina profilo
            l.setOnMouseClicked(mouseEvent1 -> {
                ProfileController ctrl = (ProfileController) utilitis.changeScene(
                        "/it/unipi/dii/lsmd/winewineryapp/layout/profile.fxml", mouseEvent);
                ctrl.setProfilePage(mongoMan.getUserByUsername(u.getUsername()));
                dialog.close();
            });
        }
        dialogPane.setContent(vBox);
        dialog.showAndWait();
    }

    @FXML
    public void gotosearch(ActionEvent event) {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml",event);
    }

    @FXML
    public void logout(ActionEvent event) {
        this.event = event;
        Session.resetInstance();
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxml",event);
    }

    @FXML
    public void gotomyprofile(ActionEvent event) {
        this.event = event;
        ProfileController ctrl = (ProfileController) utilitis.changeScene(
                "/it/unipi/dii/lsmd/winewineryapp/layout/profile.fxml", event);
        if (ctrl != null) {
            ctrl.setProfilePage(Session.getInstance().getLoggedUser());
        }
    }

    @FXML
    public void gotosuggestion(ActionEvent event) {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/specialSearch.fxml",event);
    }
}


