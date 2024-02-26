package it.unipi.dii.lsmd.winewineryapp.model;

import java.util.List;

public class Wine {
    private String vivino_id;
    private String glugulp_id;
    private String name;
    private String winemaker;
    private String country;
    private String varietal;
    private String grapes;
    private Integer year;
    private Double price;
    private String info;
    private String description;
    private List<Comment> comments;

    public Wine(String vivino_id, String glugulp_id, String name, String winemaker, String country, String varietal, String grapes, int year, double price, String info, String description, List<Comment> comments) {
        this.vivino_id = vivino_id;
        this.glugulp_id = glugulp_id;
        this.name = name;
        this.winemaker = winemaker;
        this.country = country;
        this.varietal = varietal;
        this.grapes = grapes;
        this.year = year;
        this.price = price;
        this.info = info;
        this.description = description;
        this.comments = comments;
    }

    public String getVivino_id() {
        return vivino_id;
    }

    public void setVivino_id(String vivino_id) {
        this.vivino_id = vivino_id;
    }

    public String getGlugulp_id() {
        return glugulp_id;
    }

    public void setGlugulp_id(String glugulp_id) {
        this.glugulp_id = glugulp_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWinemaker() {
        return winemaker;
    }

    public void setWinemaker(String winemaker) {
        this.winemaker = winemaker;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getVarietal() {
        return varietal;
    }

    public void setVarietal(String varietal) {
        this.varietal = varietal;
    }

    public String getGrapes() {
        return grapes;
    }

    public void setGrapes(String grapes) {
        this.grapes = grapes;
    }
    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {this.year = year;}

    public Double getPrice() {return price;}

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "Wine{" +
                "vivino_id='" + vivino_id + '\'' +
                ", glugulp_id='" + glugulp_id + '\'' +
                ", name='" + name + '\'' +
                ", winemaker='" + winemaker + '\'' +
                ", country='" + country + '\'' +
                ", varietal='" + varietal + '\'' +
                ", grapes='" + grapes + '\'' +
                ", year=" + year +
                ", price=" + price +
                ", info='" + info + '\'' +
                ", description='" + description + '\'' +
                ", comments=" + comments +
                '}';
    }
}

