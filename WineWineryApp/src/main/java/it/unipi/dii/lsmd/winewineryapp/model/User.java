package it.unipi.dii.lsmd.winewineryapp.model;

import java.util.List;

public class User {
    private String username;
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private int age;
    private String location;
    private int type;
    private List<Winery> winerys;

    public User(String username, String email, String password, String firstname, String lastname, int age, String location, List<Winery> winerys, int type) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.age = age;
        this.location = location;
        this.type = type;
        this.winerys = winerys;
    }

    public User(String username, String email) {
        this(username, email, null, null, null, -1, null,null, 0);
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username){
        this.username = username;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public String getPassword(){
        return password;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public String getFirstName(){
        return firstname;
    }
    public void setFirstName(String firstName){
        this.firstname = firstName;
    }
    public String getLastName(){
        return lastname;
    }
    public void setLastName(String lastName){
        this.lastname = lastName;
    }
    public int getAge(){
        return age;
    }
    public void setAge(int age){
        this.age = age;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public List<Winery> getWinerys() {
        return winerys;
    }

    public void setWinerys(List<Winery> winerys) {
        this.winerys = winerys;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email=" + email +
                ", password=" + password +
                ", firstname=" + firstname +
                ", lastname=" + lastname +
                ", age=" + age +
                ", location=" + location +
                ", type=" + type +
                ", winerys=" + winerys +
                '}';
    }




}
