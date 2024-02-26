package it.unipi.dii.lsmd.winewineryapp.controller;

import com.mongodb.client.result.UpdateResult;
import it.unipi.dii.lsmd.winewineryapp.model.*;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jManager;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.*;

public class WinePageController {
    private Wine wine;
    private User user;
    private MongoDBManager mongoMan;
    private Neo4jManager neoMan;
    private ActionEvent event;
    private String validId;
    private int currentPage;
    private int commentsPerPage;
    @FXML
    private Label username_side;
    @FXML
    private Button backBTN;
    @FXML
    private Text wineId;
    @FXML
    private Text category;
    @FXML
    private Text winename;
    @FXML
    private Text winemaker;
    @FXML
    private Text year;
    @FXML
    private Text winedescription;
    @FXML
    private Text wineinfo;
    @FXML
    private Text price;
    @FXML
    private Text grapes;
    @FXML
    private VBox commentsBox;
    @FXML
    private Button likeBTN;
    @FXML
    private Text like;
    @FXML
    private Button addInWineryBTN;
    @FXML
    private Button AllCommentBTN;
    @FXML
    private Button commentBTN;
    @FXML
    private TextField commentText;
    @FXML
    private Text comNum;
    @FXML
    private ScrollPane scrollpane;
    @FXML
    private Button CnextBTN;
    @FXML
    private Button CbackBTN;
    @FXML
    private Button gotomyprofile;

    public void initialize() {
        commentsBox.setSpacing(10);
        neoMan = new Neo4jManager(Neo4jDriver.getInstance().openConnection());
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());
        backBTN.setOnMouseClicked(mouseEvent -> clickbackBTN(mouseEvent));
        addInWineryBTN.setOnMouseClicked(mouseEvent -> clickaddInWineryBTN(mouseEvent));
        likeBTN.setOnMouseClicked(mouseEvent -> clickLikeBTN(mouseEvent));
        AllCommentBTN.setOnMouseClicked(mouseEvent -> clickAllCommentBTN(mouseEvent));
        commentBTN.setOnMouseClicked(mouseEvent -> clickCommentBTN(mouseEvent));
        scrollpane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        CnextBTN.setOnMouseClicked(mouseEvent -> clickNextPageBTN(mouseEvent));
        CbackBTN.setOnMouseClicked(mouseEvent -> clickPreviousPageBTN(mouseEvent));
    }

    public void setWinePage(Wine wine) {
        this.wine = wine;
        this.user = Session.getInstance().getLoggedUser();

        Session.getInstance().getPreviousPageWines().add(wine);

        username_side.setText(Session.getInstance().getLoggedUser().getUsername());

        winename.setText(wine.getName());
        // divisione vivino & glugulp

        if (wine.getVivino_id() != null) {
            validId = wine.getVivino_id();
            wineId.setText("Vivino : " + validId);
        } else {
            validId = wine.getGlugulp_id();
            wineId.setText("Glugulp : " + validId);
        }

        category.setText(wine.getVarietal());
        winemaker.setText(wine.getWinemaker());

        if (wine.getYear() == 0)
            year.setText("Sans année");
        else
            year.setText(String.valueOf(wine.getYear()));

        price.setText(String.valueOf(wine.getPrice()));
        winedescription.setText(wine.getDescription().replace("\n", " "));
        wineinfo.setText(wine.getInfo());
        grapes.setText(wine.getGrapes());

        //System.out.println("TEST// " + neoMan.userLikeWine(user.getUsername(), wine));
        //System.out.println("TEST// " + user.getUsername() + " LIKE " + wine.getName());

        if (neoMan.userLikeWine(user.getUsername(), wine))
            likeBTN.setText("Unlike");
        else
            likeBTN.setText("Like");

        like.setText(Integer.toString(neoMan.getNumLikes(wine)));
        currentPage = 0;
        commentsPerPage = 10;
        CnextBTN.setVisible(false);
        CbackBTN.setVisible(false);
        setComment();

        if (Session.getInstance().getLoggedUser().getType() == 2) {
            gotomyprofile.setVisible(false);
            likeBTN.setVisible(false);
            addInWineryBTN.setVisible(false);
            commentBTN.setDisable(true);
        }

    }

    private void clickLikeBTN(MouseEvent mouseEvent) {
        if (Objects.equals(likeBTN.getText(), "Like")) {
            neoMan.like(user, wine);
            System.out.println("TEST// " + user.getUsername() + " LIKE " + wine.getName());
            like.setText(Integer.toString(neoMan.getNumLikes(wine)));
            likeBTN.setText("UnLike");
        } else {
            neoMan.unlike(user, wine);
            like.setText(Integer.toString(neoMan.getNumLikes(wine)));
            likeBTN.setText("Like");
        }
    }

    private void clickaddInWineryBTN(MouseEvent mouseEvent) {
        if (!Session.getInstance().getLoggedUser().getWinerys().isEmpty()) {
            Iterator<Winery> it = Session.getInstance().getLoggedUser().getWinerys().iterator();
            List<String> choices = new ArrayList<>();
            while (it.hasNext()) {
                choices.add(it.next().getTitle());
            }
            ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
            dialog.setTitle("Choose Winerys");
            dialog.setHeaderText(null);
            dialog.setContentText("Winery:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {

                // Check if selected Winery does not exceed limit
                for (int i = 0; i < Session.getInstance().getLoggedUser().getWinerys().size(); i++) {
                    Winery tmp = Session.getInstance().getLoggedUser().getWinerys().get(i);
                    if (tmp.getWines().size() > 100) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Information Dialog");
                        alert.setHeaderText(null);
                        alert.setContentText("Too many wine in selected Winery");
                        alert.showAndWait();
                        return;
                    }
                }

                UpdateResult res = mongoMan.addWineToWinery(Session.getInstance().getLoggedUser().getUsername(),
                        result.get(), wine);
                if (res.getModifiedCount() == 0) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information Dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("This wine is already present in this Winery!");
                    alert.showAndWait();
                } else {
                    // Update Session User Object
                    for (Winery wn : Session.getInstance().getLoggedUser().getWinerys()) {
                        if (wn.getTitle().equals(result.get())) {
                            wn.getWines().add(wine);
                            break;
                        }
                    }
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("You haven't created a winery yet!");
            alert.showAndWait();
        }
    }

    private void setComment() {
        int numComment = mongoMan.countAllComment(wine);
        if (wine.getComments() != null) {
            commentsBox.getChildren().clear();
            Iterator<Comment> it = wine.getComments().iterator();

            while (it.hasNext()) {
                Comment c = it.next();
                Pane p = loadCommentElement(c, wine);

                commentsBox.getChildren().add(p);
            }
        }
        comNum.setText(String.valueOf(numComment));
    }

    private Pane loadCommentElement(Comment cm, Wine wn) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unipi/dii/lsmd/winewineryapp/layout/commentElement.fxml"));
            pane = loader.load();
            CommentController ctrl = loader.getController();
            ctrl.textProperty().bindBidirectional(comNum.textProperty());
            ctrl.setCommentCard(cm, wn);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }

    private void clickbackBTN(MouseEvent mouseEvent) {
        // Pop
        Session.getInstance().getPreviousPageWines().remove(
                Session.getInstance().getPreviousPageWines().size() - 1);

        // Check if previous page is Winery Page
        if (Session.getInstance().getPreviousPageWinerys().isEmpty())
            utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml", mouseEvent);
        else {
            WineryPageController ctrl = (WineryPageController) utilitis.changeScene(
                    "/it/unipi/dii/lsmd/winewineryapp/layout/wineryPage.fxml", mouseEvent);

            // Get Previous Page Winery info
            Pair<String, Winery> p = Session.getInstance().getPreviousPageWinerys().remove(
                    Session.getInstance().getPreviousPageWinerys().size() - 1);

            ctrl.setWinery(p.getValue(), p.getKey());
        }
    }

    private void clickAllCommentBTN(MouseEvent mouseEvent) {
        if (wine.getComments() != null) {
            commentsBox.getChildren().clear();
            CnextBTN.setVisible(true);
            CbackBTN.setVisible(true);
            Iterator<Comment> it = mongoMan.getAllComments(wine, currentPage, commentsPerPage).iterator();

            while (it.hasNext()) {
                Comment c = it.next();
                Pane p = loadCommentElement(c, wine);

                commentsBox.getChildren().add(p);
            }
        }
    }

    private void clickNextPageBTN(MouseEvent mouseEvent) {
        currentPage++;
        clickAllCommentBTN(mouseEvent);
    }

    private void clickPreviousPageBTN(MouseEvent mouseEvent) {
        if (currentPage > 1) {
            currentPage--;
            clickAllCommentBTN(mouseEvent);
        }
    }

    private void clickCommentBTN(MouseEvent mouseEvent) {
        if ((!commentText.getText().isEmpty()) && (commentText.getText().length() <= 100)) {
            Comment comment = new Comment(validId, user.getUsername(), commentText.getText(), new Date());

            if (!mongoMan.addRecentComment(wine, comment)) {

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("Database not available!");
                alert.showAndWait();
            }

            if (!mongoMan.addTotalComment(wine, comment)) {
                mongoMan.deleteRecentComment(wine, comment);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("Database not available!");
                alert.showAndWait();
            }

            wine = mongoMan.getWineById(wine);
            setComment();
            commentText.setText("");

        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Enter a comment of up to 100 characters!");
            alert.showAndWait();
        }
    }

    @FXML
    public void gotosearch(ActionEvent event) {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml", event);
    }

    public void gotosuggestion(ActionEvent event) {
        this.event = event;
        // ProfileController ctrl = (ProfileController)
        // utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/specialSearch.fxml",event);
        // ctrl.setProfilePage(Session.getInstance().getLoggedUser());
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/specialSearch.fxml", event);
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
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxml", event);
    }

}
