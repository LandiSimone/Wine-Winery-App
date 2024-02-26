package it.unipi.dii.lsmd.winewineryapp.persistence;

import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import java.util.Properties;

public class Neo4jDriver {
    private static Neo4jDriver instance;
    private Driver driver;
    public String neo4jIp;
    public int neo4jPort;
    public String neo4jUsername;
    public String neo4jPassword;

    private Neo4jDriver(Properties configurationParameters) {
        this.neo4jIp = configurationParameters.getProperty("neo4jIp");
        this.neo4jPort = Integer.parseInt(configurationParameters.getProperty("neo4jPort"));
        this.neo4jUsername = configurationParameters.getProperty("neo4jUsername");
        this.neo4jPassword = configurationParameters.getProperty("neo4jPassword");
    }

    public static Neo4jDriver getInstance() {
        if (instance == null)
            instance = new Neo4jDriver(utilitis.readConfigurationParameters());
        return instance;
    }

    public Driver openConnection() {
        if (driver != null)
            return driver;
        try {
            driver = GraphDatabase.driver("bolt://" + neo4jIp + ":" + neo4jPort, AuthTokens.basic( neo4jUsername, neo4jPassword));
            driver.verifyConnectivity();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return driver;
    }

    public void closeConnection() {
        if (driver != null)
            driver.close();
    }
}
