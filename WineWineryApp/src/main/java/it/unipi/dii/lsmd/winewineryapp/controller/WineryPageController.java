package it.unipi.dii.lsmd.winewineryapp.controller;

import it.unipi.dii.lsmd.winewineryapp.model.Session;
import it.unipi.dii.lsmd.winewineryapp.model.User;
import it.unipi.dii.lsmd.winewineryapp.model.Wine;
import it.unipi.dii.lsmd.winewineryapp.model.Winery;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jManager;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Pair;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WineryPageController {
    private Winery winery;
    private String owner;
    private MongoDBManager mongoMan;
    private Neo4jManager neoMan;
    private ActionEvent event;
    @FXML private Label username_side;
    @FXML private Label wineryName;
    @FXML private Label wineryOwner;

    @FXML private Text mostCommonCategory;
    @FXML private Text numFollowers;
    @FXML private Text numWines;
    @FXML private VBox winesBox;
    @FXML private Button delateWineryBTN;
    @FXML private Button followBTN;
    @FXML private Button backBTN;
    @FXML private Button gotomyprofile;


    public void initialize () {
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());
        neoMan = new Neo4jManager(Neo4jDriver.getInstance().openConnection());
        followBTN.setOnMouseClicked(mouseEvent -> clickfollowBTN(mouseEvent));
        backBTN.setOnMouseClicked(mouseEvent -> clickbackBTN(mouseEvent));
        delateWineryBTN.setOnMouseClicked(mouseEvent -> clickdelateWineryBTN(mouseEvent));
    }


    public void setWinery(Winery winery, String owner) {
        this.winery = winery;
        this.owner = owner;

        Session.getInstance().getPreviousPageWinerys().add(new Pair(owner,winery));

        username_side.setText(Session.getInstance().getLoggedUser().getUsername());
        wineryName.setText(winery.getTitle());
        wineryOwner.setText(owner);

        if (!winery.getWines().isEmpty())
            mostCommonCategory.setText(getMostCommonCategory(winery.getWines()));
        else
            mostCommonCategory.setText("N/A");

        numFollowers.setText(String.valueOf(neoMan.getNumFollowersWinery(winery.getTitle(), owner)));
        numWines.setText(String.valueOf(winery.getWines().size()));

        winesBox.getChildren().clear();
        if (!winery.getWines().isEmpty()) {
            Iterator<Wine> it = winery.getWines().iterator();

            while(it.hasNext()) {
                VBox row = new VBox();
                row.setAlignment(Pos.CENTER);
                row.setStyle("-fx-padding: 10px");
                Wine wine = it.next();
                Pane w = loadWineElement(wine);

                row.getChildren().addAll(w);
                winesBox.getChildren().add(row);
            }
        }
        else {
            winesBox.getChildren().add(new Label("There are no wines yet:("));
        }

        if (owner.equals(Session.getInstance().getLoggedUser().getUsername())) {
            followBTN.setVisible(false);
            delateWineryBTN.setVisible(true);
        }
        else {
            followBTN.setVisible(true);
            delateWineryBTN.setVisible(false);
        }

        if (neoMan.isUserFollowingWinery(Session.getInstance().getLoggedUser().getUsername(), owner, winery))
            followBTN.setText("Unfollow");

        if (Session.getInstance().getLoggedUser().getType() == 2) {
            gotomyprofile.setVisible(false);
            followBTN.setVisible(false);
        }
    }

    private Pane loadWineElement (Wine w) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unipi/dii/lsmd/winewineryapp/layout/wineElelement.fxml"));
            pane = loader.load();
            WineElementController ctrl = loader.getController();
            boolean showDeleteBtn = Session.getInstance().getLoggedUser().getUsername().equals(wineryOwner.getText());
            ctrl.setWineElement(w, showDeleteBtn, this, null, 0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }

    private void clickfollowBTN (MouseEvent mouseEvent) {
        String tmp = followBTN.getText();
        if (tmp.equals("Follow")) {
            neoMan.followWinery(winery.getTitle(), owner, Session.getInstance().getLoggedUser().getUsername());
            //System.out.println("TEST// FOLLOW WINERY: "+winery.getTitle() + " " + owner + " " + Session.getInstance().getLoggedUser().getUsername() );
            numFollowers.setText(String.valueOf(neoMan.getNumFollowersWinery(winery.getTitle(), owner)));
            followBTN.setText("Unfollow");
        }
        else {
            neoMan.unfollowWinery(winery.getTitle(), owner, Session.getInstance().getLoggedUser().getUsername());
            numFollowers.setText(String.valueOf(neoMan.getNumFollowersWinery(winery.getTitle(), owner)));
            followBTN.setText("Follow");
        }
    }
    private void clickdelateWineryBTN (MouseEvent mouseEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete Winery?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            if (!mongoMan.deleteWinery(wineryOwner.getText(), winery.getTitle())) {
                System.out.println("mongo");
                Alert msg = new Alert(Alert.AlertType.ERROR, "Error: An error occurred. Try again later :(");
                msg.showAndWait();
                return;
            }

            if (!neoMan.deleteWinery(winery.getTitle(), wineryOwner.getText())) {
                mongoMan.createWinery(Session.getInstance().getLoggedUser(), winery.getTitle());
                Alert msg = new Alert(Alert.AlertType.ERROR, "Error: An error occurred. Try again later :(");
                msg.showAndWait();
                return;
            }

            User owner = Session.getInstance().getPreviousPageUsers().get(Session.getInstance().getPreviousPageUsers().size()-1);
            owner.getWinerys().remove(winery);
            clickbackBTN(mouseEvent);
        }
    }

    private void clickbackBTN (MouseEvent mouseEvent) {
        // Pop
        Session.getInstance().getPreviousPageWinerys().remove(
                Session.getInstance().getPreviousPageWinerys().size() - 1);

        // Check if previous page was a Profile Page
        if (Session.getInstance().getPreviousPageUsers().isEmpty())
            utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml", mouseEvent);
        else {
            ProfileController ctrl = (ProfileController) utilitis.changeScene(
                    "/it/unipi/dii/lsmd/winewineryapp/layout/profile.fxml", mouseEvent);
            ctrl.setProfilePage(Session.getInstance().getPreviousPageUsers().remove(
                    Session.getInstance().getPreviousPageUsers().size()-1));
        }
    }

    private String getMostCommonCategory(List<Wine> wines) {
        Map<String, Integer> map = new HashMap<>();

        for (Wine p : wines) {
            Integer val = map.get(p.getVarietal());
            map.put(p.getVarietal(), val == null ? 1 : val + 1);
        }

        Map.Entry<String, Integer> max = null;

        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }
        return max.getKey();
    }

    @FXML
    public void gotosearch(ActionEvent event) {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml",event);
    }

    @FXML
    public void gotosuggestion(ActionEvent event) {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/specialSearch.fxml",event);
    }

    @FXML
    public void gotomyprofile(ActionEvent event) {
        this.event = event;
        ProfileController ctrl = (ProfileController) utilitis.changeScene(
                "/it/unipi/dii/lsmd/winewineryapp/layout/profile.fxml", event);
        ctrl.setProfilePage(Session.getInstance().getLoggedUser());
    }

    @FXML
    private void logout(ActionEvent event) {
        this.event = event;
        Session.resetInstance();
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxml",event);
    }
}
