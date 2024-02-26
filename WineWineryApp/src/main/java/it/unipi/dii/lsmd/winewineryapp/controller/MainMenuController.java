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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.List;

public class MainMenuController  {
    private ActionEvent event;
    private MongoDBManager mongoManager;
    private Neo4jManager neo4jManager;
    private User user;
    private int page;

    @FXML private Label username_side;
    @FXML private ComboBox<String> ResearchType;
    @FXML private Button searchBTN;
    @FXML private TextField keywordTf;
    @FXML private Slider MaxPriceWine;
    @FXML private Slider MinPriceWine;
    @FXML private Slider MaxYearWine;
    @FXML private Slider MinYearWine;
    @FXML private Label MaxPriceLabel;
    @FXML private Label MinPriceLabel;
    @FXML private Label MaxYearLabel;
    @FXML private Label MinYearLabel;
    @FXML private TextField keywordWinemakerWine;
    @FXML private TextField keywordCountryWine;
    @FXML private TextField keywordVarietalWine;
    @FXML private TextField keywordGrapesWine;
    @FXML private Button backBTN;
    @FXML private Button nextBTN;
    @FXML private GridPane cardsGrid;
    @FXML private Label errorTf;
    @FXML private HBox WineParameters;
    @FXML private Button gotomyprofile;

    public void initialize () {
        mongoManager = new MongoDBManager(MongoDriver.getInstance().openConnection());
        neo4jManager = new Neo4jManager(Neo4jDriver.getInstance().openConnection());
        user = Session.getInstance().getLoggedUser();
        username_side.setText(user.getUsername());
        initSearch();

        nextBTN.setOnMouseClicked(mouseEvent -> goForward());
        backBTN.setOnMouseClicked(mouseEvent -> goBack());
        if (Session.getInstance().getLoggedUser().getType() == 2)
            gotomyprofile.setVisible(false);
    }

    @FXML
    void switchSearch() {
        searchBTN.setDisable(false);
        backBTN.setDisable(true);
        nextBTN.setDisable(true);

        if (ResearchType.getValue().equals("Wines"))
            WineParameters.setVisible(true);
        else
            WineParameters.setVisible(false);
    }

    // inizializza la ricerca
    private void initSearch() {
        page = 0;
        searchBTN.setDisable(true);

        // load type
        List<String> typeList = new ArrayList<>();

        if (user.getType() == 2) {  // admin
            typeList.add("Users");
            typeList.add("Moderators");
            typeList.add("Bad Users");
        }
        else {      // user + moderatore
            typeList.add("Wines");
            typeList.add("Users");
            typeList.add("Winerys");
        }
        ObservableList<String> observableListType = FXCollections.observableList(typeList);
        ResearchType.getItems().clear();
        ResearchType.setItems(observableListType);

        // slider
        MaxPriceWine.valueProperty().addListener((observable, oldValue, newValue) -> MaxPriceLabel.setText(newValue.intValue() + " €"));
        MinPriceWine.valueProperty().addListener((observable, oldValue, newValue) -> MinPriceLabel.setText(newValue.intValue() + " €"));
        MaxYearWine.valueProperty().addListener((observable, oldValue, newValue) -> MaxYearLabel.setText(newValue.intValue() + ""));
        MinYearWine.valueProperty().addListener((observable, oldValue, newValue) -> MinYearLabel.setText(newValue.intValue() + ""));

        // wine parameter
        WineParameters.setVisible(false);
    }

    @FXML
    void startResearch() {
        nextBTN.setDisable(false);
        backBTN.setDisable(true);
        page = 0;
        handleResearch();
    }

    private void handleResearch() {
        //System.out.println("handleResearch");
        //System.out.println(ResearchType.getValue());

        switch (ResearchType.getValue()) {
            case "Wines" -> {
                // check the form values
                errorTf.setText("");
                if (keywordTf.getText().equals("") && MinPriceLabel.getText().equals("") && MaxPriceLabel.getText().equals("") && MinYearLabel.getText().equals("") && MaxYearLabel.getText().equals("") && keywordCountryWine.getText().equals("") && keywordVarietalWine.getText().equals("") && keywordGrapesWine.getText().equals("") && keywordWinemakerWine.getText().equals("")) {
                    errorTf.setText("You have to set some filters.");
                    nextBTN.setDisable(true);
                    return;
                }
                if (MaxYearWine.getValue() < MinYearWine.getValue()){
                    errorTf.setText("The max year must be greater than the min year.");
                    nextBTN.setDisable(true);
                    return;
                }
                if (MaxPriceWine.getValue() < MinPriceWine.getValue()){
                    errorTf.setText("The max price must be greater than the min price.");
                    nextBTN.setDisable(true);
                    return;
                }
                String winemaker = "";
                String country = "";
                String varietal= "";
                String grapes = "";

                Double startYear = 0.0;
                if(Math.floor(MinYearWine.getValue())!= 1950)
                    startYear = Math.floor(MinYearWine.getValue());

                Double endYear = 0.0;
                if (Math.floor(MaxYearWine.getValue())!= 1950)
                    endYear = Math.floor(MaxYearWine.getValue());

                Double startPrice = Math.floor(MinPriceWine.getValue());
                Double endPrice = Math.floor(MaxPriceWine.getValue());

                if (keywordCountryWine.getText() !=null)
                    winemaker= keywordWinemakerWine.getText();

                if (keywordCountryWine.getText() !=null)
                    country= keywordCountryWine.getText();

                if (keywordVarietalWine.getText() !=null)
                    varietal= keywordVarietalWine.getText();

                if (keywordGrapesWine.getText() !=null)
                    grapes= keywordGrapesWine.getText();
                // load wine
                List<Wine> WineList = mongoManager.searchWinesByParameters(keywordTf.getText(),winemaker,country,varietal,grapes,startYear.intValue(),endYear.intValue(),startPrice,endPrice,page*3,3);
                // parametri della ricerca
                //System.out.println("keyword: " + keywordTf.getText() + " winemaker: " + winemaker + " country: " + country + " varietal: " + varietal + " grapes: " + grapes + " startYear: " + startYear.intValue() + " endYear: " + endYear.intValue() + " startPrice: " + startPrice + " endPrice: " + endPrice);
                // risultato
                //System.out.println("WineList: " + WineList.size());
                fillWines(WineList);
            }
            case "Users" -> {
                errorTf.setText("");
                if (keywordTf.getText().equals("")) {
                    errorTf.setText("You have to insert a username keyword");
                    return;
                }
                List<User> usersList;
                usersList = mongoManager.getUsersByKeyword(keywordTf.getText(), false, page);
                fillUsers(usersList);
            }
            case "Winerys" -> {
                // form control
                errorTf.setText("");
                if (keywordTf.getText().equals("")) {
                    errorTf.setText("You have to insert a winery keyword.");
                    return;
                }
                List<Pair<String, Winery>> winerys;
                winerys = mongoManager.getWineryByKeyword(keywordTf.getText(), 4 * page, 4);
                fillWinerys(winerys);
            }
            case "Moderators"->{    // ADMIN
                List<User> usersList = mongoManager.getUsersByKeyword(keywordTf.getText(), true, page);
                fillUsers(usersList);
            }
            case "Bad Users"->{     // ADMIN
                List<User> usersList = mongoManager.getBadUsers(8*page, 8);
                fillUsers(usersList);
            }
        }
    }

    private void fillWinerys(List<Pair<String, Winery>> winerys) {
        setGridWine();
        if (winerys.size() == 0)
            nextBTN.setDisable(true);

        int row = 0;
        for (Pair<String, Winery> cardInfo : winerys) {
            Pane card = loadWineryElement(cardInfo.getValue(), cardInfo.getKey(), null, 0);
            cardsGrid.add(card, 0, row);
            row++;
        }
    }

    private Pane loadWineryElement (Winery winerys, String owner, String analytics, int value) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unipi/dii/lsmd/winewineryapp/layout/wineryElement.fxml"));
            pane = loader.load();
            WineryElementController ctrl = loader.getController();
            ctrl.setWineryElement(winerys, owner, analytics, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }


    private void fillWines(List<Wine> WineList) {
        if (WineList.size() != 3)
            nextBTN.setDisable(true);
        setGridWine();
        int row = 0;
        for (Wine w : WineList) {
            Pane card = loadWineElelement(w, null, 0);
            cardsGrid.add(card, 0, row);
            row++;
        }
    }

    private Pane loadWineElelement (Wine wine, String analytics, int value) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unipi/dii/lsmd/winewineryapp/layout/wineElelement.fxml"));
            pane = loader.load();
            WineElementController ctrl = loader.getController();
            ctrl.setWineElement(wine, false, null, analytics, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }

    private void setGridWine() {
        cleanGrid();
        cardsGrid.setAlignment(Pos.CENTER);
        cardsGrid.setVgap(25);
        cardsGrid.setPadding(new Insets(15,40,15,100));
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(100);
        cardsGrid.getColumnConstraints().add(constraints);
    }

    private void fillUsers(List<User> usersList) {
        setGridUsers();
        if (usersList.size() != 8)
            nextBTN.setDisable(true);
        int row = 0;
        int col = 0;
        for (User u : usersList) {
            Pane card = loadUsersCard(u, null, 0);
            cardsGrid.add(card, col, row);
            col++;
            if (col == 4) {
                col = 0;
                row++;
            }
        }
    }
    private Pane loadUsersCard (User user, String analytics, int value) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unipi/dii/lsmd/winewineryapp/layout/userElement.fxml"));
            pane = loader.load();
            UserElementController ctrl = loader.getController();
            ctrl.setParameters(user, analytics, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }
    private void setGridUsers() {
        cleanGrid();
        cardsGrid.setHgap(20);
        cardsGrid.setVgap(20);
        ColumnConstraints constraints = new ColumnConstraints();
        cardsGrid.getColumnConstraints().add(constraints);
    }


    private void cleanGrid() {
        cardsGrid.getColumnConstraints().clear();
        while (cardsGrid.getChildren().size() > 0) {
            cardsGrid.getChildren().remove(0);
        }
    }

    private void goForward () {
        page++;
        backBTN.setDisable(false);
        handleResearch();
    }

    private void goBack () {
        page--;
        if (page <= 0) {
            page = 0;
            backBTN.setDisable(true);
        }
        nextBTN.setDisable(false);
        handleResearch();
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
        if (ctrl != null) {
            ctrl.setProfilePage(Session.getInstance().getLoggedUser());
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        this.event = event;
        Session.resetInstance();
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxml",event);
    }

}
