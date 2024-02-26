package it.unipi.dii.lsmd.winewineryapp.model;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Session {

    private static Session instance = null;
    private User user;
    private List<User> previousPageUsers;
    private List<Pair<String, Winery>> previousPageWinerys;
    private List<Wine> previousPageWines;

    public static Session getInstance() {
        if(instance==null)
            instance = new Session();
        return instance;
    }
    public static void resetInstance() {
        instance = null;
    }
    private Session () {
        previousPageUsers = new ArrayList<>();
        previousPageWinerys = new ArrayList<>();
        previousPageWines = new ArrayList<>();
    }

    public void setLoggedUser(User u) {
        instance.user = u;
        //System.out.println("TEST// SET LOG user: " + instance.user.getUsername());
    }

    public User getLoggedUser() {
        return user;
    }


    public List<User> getPreviousPageUsers() {
        return previousPageUsers;
    }

    public List<Pair<String, Winery>> getPreviousPageWinerys() {
        return previousPageWinerys;
    }

    public List<Wine> getPreviousPageWines() {
        return previousPageWines;
    }

}
