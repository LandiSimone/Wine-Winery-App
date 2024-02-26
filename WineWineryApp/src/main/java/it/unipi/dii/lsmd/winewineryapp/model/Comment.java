package it.unipi.dii.lsmd.winewineryapp.model;
import java.util.Date;

public class Comment {
    private String wine;
    private String username;
    private String text;
    private Date timestamp;

    public Comment(String wine, String username, String text, Date timestamp) {
        this.wine = wine;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }
    public String getWine() { return wine; }
    public void setWine(String wine) { this.wine = wine;}

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "wine='" + wine + '\'' +
                ", username='" + username + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }


}
