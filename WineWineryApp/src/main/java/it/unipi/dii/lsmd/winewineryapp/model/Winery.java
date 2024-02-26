package it.unipi.dii.lsmd.winewineryapp.model;

import java.util.List;

public class Winery {
    private String title;
    private List<Wine> wines;

    public Winery(String title, List<Wine> wines) {
        this.title = title;
        this.wines = wines;
    }

    public Winery(String title) {
        this(title, null);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Wine> getWines() {
        return wines;
    }

    public void setWines(List<Wine> wines) {
        this.wines = wines;
    }

    @Override
    public String toString() {
        return "Winerys{" + "wines='" + wines + '\'' + '}';
    }
}
