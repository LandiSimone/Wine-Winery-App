package it.unipi.dii.lsmd.winewineryapp.utils;
import it.unipi.dii.lsmd.winewineryapp.Main;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class utilitis {
    public static Object changeScene(String fxmlFile, Event event) {
        Scene scene = null;
        FXMLLoader loader = null;
        try {
            loader = new FXMLLoader(utilitis.class.getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.show();
            return loader.getController();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static public void goBackToStart(ActionEvent event){
        changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxm",event);
    }

    /**
     * This function is used to read the config.xml file
     * @return  ConfigurationParameters instance
     */
    public static Properties readConfigurationParameters(){
        try{
            FileInputStream fis = new FileInputStream(utilitis.class.getResource("/it/unipi/dii/lsmd/winewineryapp/config/config.properties").toURI().getPath());
            Properties prop = new Properties();
            prop.load(fis);
            return prop;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }




}