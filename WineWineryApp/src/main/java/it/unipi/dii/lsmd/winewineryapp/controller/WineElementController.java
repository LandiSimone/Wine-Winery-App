package it.unipi.dii.lsmd.winewineryapp.controller;
import it.unipi.dii.lsmd.winewineryapp.model.Session;
import it.unipi.dii.lsmd.winewineryapp.model.Wine;
import it.unipi.dii.lsmd.winewineryapp.model.Winery;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

public class WineElementController {
    private Wine wine;
    private MongoDBManager mongoMan;

    private WineryPageController WineryPageController;
    
    @FXML
    private Label wineId;
    @FXML 
    private Label wineName;
    @FXML
    private Label wineMaker;
    @FXML 
    private Text wineCategory;
    @FXML private Label analyticLabelName;
    @FXML private Text analyticValue;
    @FXML private Button RemoveWineBTN;

    public void initialize () {
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());
        wineName.setOnMouseClicked(mouseEvent -> clickOnWineName(mouseEvent));
        RemoveWineBTN.setOnMouseClicked(mouseEvent -> clickRemoveWineBTN(mouseEvent));
    }

    private void clickOnWineName(MouseEvent mouseEvent) {
        WinePageController ctrl = (WinePageController) utilitis.changeScene(
                "/it/unipi/dii/lsmd/winewineryapp/layout/winePage.fxml", mouseEvent);
        if (wine.getComments() == null || wine.getInfo() == null ||wine.getDescription().isEmpty())
            wine = mongoMan.getWineById(wine);
        if (ctrl != null)
            ctrl.setWinePage(wine);;
    }

    public void setWineElement(Wine w, boolean showDeleteBtn, WineryPageController WineryPageController, String analyticLabelName, int analyticValue) {
        this.wine = w;

        // divisione vivino & glugulp
        String validId;
        if (wine.getVivino_id() != null) {
            validId = w.getVivino_id();
            wineId.setText("Vivino ID: " + validId);
        }
        else {
            validId = w.getGlugulp_id();
            wineId.setText("Glugulp ID: " + validId);
        }

        wineName.setText(wine.getName());
        wineMaker.setText(wine.getWinemaker());
        wineCategory.setText(wine.getVarietal());

        if (analyticLabelName != null) {
            this.analyticLabelName.setText(String.valueOf(analyticLabelName));
            this.analyticValue.setText(String.valueOf(analyticValue));
        }
        else {
            this.analyticLabelName.setVisible(false);
            this.analyticValue.setVisible(false);
        }

        if (showDeleteBtn) {
            RemoveWineBTN.setVisible(true);
            this.WineryPageController = WineryPageController;

        }
        else {
            RemoveWineBTN.setVisible(false);
            this.WineryPageController = null;
        }
    }


    private void clickRemoveWineBTN (MouseEvent mouseEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Remove Wine?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {

            Winery NEWwn = Session.getInstance().getPreviousPageWinerys().get(
                    Session.getInstance().getPreviousPageWinerys().size() - 1).getValue();

            mongoMan.removeWineFromWinery(Session.getInstance().getLoggedUser().getUsername(),
                    NEWwn.getTitle(), wine);

            // Remove the wine from the local copy of the winery
            NEWwn.getWines().remove(wine);


            WineryPageController.setWinery(NEWwn,
                    Session.getInstance().getLoggedUser().getUsername());
        }
    }
}
