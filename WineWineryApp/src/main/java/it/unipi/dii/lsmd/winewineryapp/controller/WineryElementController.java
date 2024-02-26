package it.unipi.dii.lsmd.winewineryapp.controller;

import it.unipi.dii.lsmd.winewineryapp.model.Winery;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

public class WineryElementController {
    private Winery w;
    private String owner;
    private MongoDBManager mongoMan;
    @FXML
    private Label WineryTitle;
    @FXML
    private Text ownerText;
    @FXML
    private Label analyticLabelName;
    @FXML
    private Text analyticValue;

    public void initialize() {
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());
        WineryTitle.setOnMouseClicked(mouseEvent -> clickWinery(mouseEvent));
    }

    public void setWineryElement(Winery w, String owner, String analyticLabelName, int analyticValue) {
        this.w = w;
        this.owner = owner;

        WineryTitle.setText(w.getTitle());
        ownerText.setText(owner);

        if (analyticLabelName != null) {
            this.analyticLabelName.setText(analyticLabelName);
            this.analyticValue.setText(String.valueOf(analyticValue));
        } else {
            this.analyticLabelName.setVisible(false);
            this.analyticValue.setVisible(false);
        }
    }

    private void clickWinery(MouseEvent mouseEvent) {
        WineryPageController ctrl = (WineryPageController) utilitis.changeScene(
                "/it/unipi/dii/lsmd/winewineryapp/layout/wineryPage.fxml", mouseEvent);

        // load the complete winery object
        if (w.getWines() == null)
            w = mongoMan.getWinery(owner, w.getTitle());

        ctrl.setWinery(w, owner);
    }
}
