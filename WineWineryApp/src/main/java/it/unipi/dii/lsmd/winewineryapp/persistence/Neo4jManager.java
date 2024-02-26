package it.unipi.dii.lsmd.winewineryapp.persistence;

import it.unipi.dii.lsmd.winewineryapp.model.User;
import it.unipi.dii.lsmd.winewineryapp.model.Wine;
import it.unipi.dii.lsmd.winewineryapp.model.Winery;
import javafx.util.Pair;
import org.neo4j.driver.Driver;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

public class Neo4jManager {

    Driver driver;

    public Neo4jManager(Driver driver) {
        this.driver = driver;
    }

    /**
     * add the info of a new user to GraphDB
     * @param u new User
     */
    public boolean addUser(User u) {
        boolean res = false;
        try(Session session = driver.session()) {
            res = session.writeTransaction((TransactionWork<Boolean>) tx -> {
                tx.run("CREATE (u:User {username: $username, email: $email})",
                        parameters("username", u.getUsername(), "email", u.getEmail()));

                return true;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return res;
    }

    /**
     * return the number of followed user
     * @param username of the user
     * @return number of followed user
     */
    public int getNumFollowedUser(final String username) {
        int numFollowers;
        try (Session session = driver.session()) {
            numFollowers = session.writeTransaction((TransactionWork<Integer>) tx -> {
                Result result = tx.run("MATCH (:User {username: $username})-[r:FOLLOWS]->(u:User) " +
                        "RETURN COUNT(r) AS numFollowers", parameters("username", username));
                return result.next().get("numFollowers").asInt();
            });
        }
        return numFollowers;
    }

    /**
     * return the number of followers
     * @param username of the user
     * @return number of followers
     */
    public int getNumFollowingUser(final String username) {
        int numFollowers;
        try (Session session = driver.session()) {
            numFollowers = session.writeTransaction((TransactionWork<Integer>) tx -> {
                Result result = tx.run("MATCH (:User {username: $username})<-[r:FOLLOWS]-(u:User) " +
                        "RETURN COUNT(r) AS numFollowers", parameters("username", username));
                return result.next().get("numFollowers").asInt();
            });
        }
        return numFollowers;
    }

    /**
     * check if user a follows user b
     * @param userA username of user a
     * @param userB username of user b
     * @return result of the check
     */
    public boolean userAFollowsUserB (String userA, String userB) {
        boolean res = false;
        try(Session session = driver.session()) {
            res = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result r = tx.run("MATCH (a:User{username:$userA})-[r:FOLLOWS]->(b:User{username:$userB}) " +
                        "RETURN COUNT(*)", parameters("userA", userA, "userB", userB));
                Record record = r.next();
                if (record.get(0).asInt() == 0)
                    return false;
                else
                    return true;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * create a new winery
     * @param title name of the winery
     * @param owner username of the owner
     * @return result of the function
     */
    public boolean createWinery (final String title, final String owner) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("CREATE (:Winery {title: $title, owner: $owner})"
                        , parameters("title", title, "owner", owner));
                return null;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * update info of a user
     * @param u info of the user
     * @return result of the function
     */
    public boolean updateUser(User u) {
        try(Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {username: $username}) SET u.email = $newEmail",
                        parameters("username", u.getUsername(), "newEmail", u.getEmail()));
                return null;
            });
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * add a new follows relationship
     * @param username username of the user
     * @param target user to be followed
     */
    public void followUser (final String username, final String target) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {username: $username}), (t:User {username: $target}) " +
                                "MERGE (u)-[p:FOLLOWS]->(t) " +
                                "ON CREATE SET p.date = datetime()",
                        parameters("username", username, "target", target));
                return null;
            });
        }
    }

    /**
     * remove a follows relationship
     * @param username username of the user
     * @param target user to be unfollowed
     */
    public void unfollowUser (final String username, final String target) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (:User {username: $username})-[r:FOLLOWS]->(:User {username: $target}) " +
                                "DELETE r",
                        parameters("username", username, "target", target));
                return null;
            });
        }
    }

    /**
     * get the number of followers of a winery
     * @param title name of the winery
     * @param owner owner of the winery
     * @return number of followers
     */
    public int getNumFollowersWinery(final String title, final String owner) {
        int numFollowers;
        try (Session session = driver.session()) {
            numFollowers = session.writeTransaction((TransactionWork<Integer>) tx -> {
                Result result = tx.run("MATCH (:Winery {title: $title, owner: $owner})<-[r:FOLLOWS]-() " +
                        "RETURN COUNT(r) AS numFollowers", parameters("title", title, "owner", owner));
                return result.next().get("numFollowers").asInt();
            });
        }
        return numFollowers;
    }

    /**
     * check if a user is following a winery
     * @param user username of the user
     * @param owner owner of the winery
     * @param winery info of the winery
     * @return result of the check
     */
    public boolean isUserFollowingWinery (String user, String owner, Winery winery) {
        boolean res = false;
        try(Session session = driver.session()) {
            res = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result r = tx.run("MATCH (a:User{username:$user})-[r:FOLLOWS]->(b:Winery{title:$title, owner:$owner }) " +
                        "RETURN COUNT(*)", parameters("user", user, "title", winery.getTitle(), "owner", owner));
                Record record = r.next();
                if (record.get(0).asInt() == 0)
                    return false;
                else
                    return true;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * add a follows relationship between a user and a winery
     * @param title name of the winery
     * @param owner owner of the winery
     * @param username username of the user
     */
    public void followWinery (final String title, final String owner, final String username) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {username: $username}), (w:Winery {title: $title, owner: $owner}) " +
                                "MERGE (u)-[p:FOLLOWS]->(w) " +
                                "ON CREATE SET p.date = datetime()",
                        parameters("username", username, "title", title, "owner", owner));
                return null;
            });
        }
    }

    /**
     * delete a follows relationship
     * @param title name of the winery
     * @param owner owner of the winery
     * @param username username of the user
     */
    public void unfollowWinery (final String title, final String owner, final String username) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (:User {username: $username})-[r:FOLLOWS]->(:Winery {title: $title, owner: $owner}) " +
                                "DELETE r",
                        parameters("username", username, "title", title, "owner", owner));
                return null;
            });
        }
    }

    /**
     * delete a winery
     * @param title name of the winery
     * @param owner owner of the winery
     * @return result of the function
     */
    public boolean deleteWinery (final String title, final String owner) {
        boolean res = false;
        try (Session session = driver.session()){
            res = session.writeTransaction((TransactionWork<Boolean>) tx -> {
                tx.run("MATCH (w:Winery {title: $title, owner: $owner}) " +
                                "DETACH DELETE w",
                        parameters("title", title, "owner", owner));
                return true;
            });
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return res;
    }

    /**
     * check if exist a likes relationship between a user and a wine
     * @param user username of the user
     * @param wine info of the wine
     * @return result of the function
     */
    public boolean userLikeWine (String user, Wine wine){
        boolean res = false;
        try(Session session = driver.session()){
            res = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result r = tx.run("MATCH (:User{username:$user})-[r:LIKES]->(w:Wine) WHERE (w.vivino_id = $vivino_id OR w.glugulp_id =$glugulp_id) " +
                        "RETURN COUNT(*)", parameters("user", user, "vivino_id", wine.getVivino_id(), "glugulp_id", wine.getGlugulp_id()));
                Record record = r.next();
                if (record.get(0).asInt() == 0)
                    return false;
                else
                    return true;
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return res;
    }

    /**
     * get the number of likes of a wine
     * @param wine info of the wine
     * @return number of likes
     */
    public int getNumLikes(final Wine wine) {
        int numLikes;
        try (Session session = driver.session()) {
            numLikes = session.writeTransaction((TransactionWork<Integer>) tx -> {
                Result result = tx.run("MATCH (w:Wine)<-[r:LIKES]-() WHERE w.vivino_id = $vivino_id OR w.glugulp_id = $glugulp_id " +
                        "RETURN COUNT(r) AS numLikes", parameters("vivino_id", wine.getVivino_id(), "glugulp_id", wine.getGlugulp_id()));
                return result.next().get("numLikes").asInt();
            });
        }
        return numLikes;
    }

    /**
     * add a likes relationship between a user and a wine
     * @param u info of the user
     * @param w info of the wine
     */
    public void like(User u, Wine w) {
        try(Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (a:User), (b:Wine) " +
                                "WHERE a.username = $username AND (b.vivino_id = $vivino_id OR b.glugulp_id = $glugulp_id) " +
                                "MERGE (a)-[r:LIKES]->(b)",
                        parameters("username", u.getUsername(),
                                "vivino_id", w.getVivino_id(),
                                "glugulp_id", w.getGlugulp_id()));
                return null;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * remove a likes relationship between a user and a wine
     * @param u info of the user
     * @param w info of the wine
     * @return result of the function
     */
    public boolean unlike(User u, Wine w) {
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User{username:$username})-[r:LIKES]->(w:Wine) " +
                                "WHERE w.vivino_id = $vivino_id OR w.glugulp_id = $glugulp_id" +
                                " DELETE r",
                        parameters("username", u.getUsername(),
                                "vivino_id", w.getVivino_id(),
                                "glugulp_id", w.getGlugulp_id()));
                return null;
            });
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Returns a list of suggested users snapshots
     * first level: most followed users who are 2 FOLLOWS hops far from the logged user
     * second level: most followed users that have likes in common with the logged user
     * @param u info of the user
     * @param numberFirstLv how many users suggest from first level suggestion
     * @param numberSecondLv how many users suggest from second level
     * @return A list of suggested users
     */
    public List<User> getSnapsOfSuggestedUsers(User u, int numberFirstLv, int numberSecondLv, int skipFirstLv, int skipSecondLv) {
        List<User> usersSnap = new ArrayList<>();

        try (Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (me:User {username: $username})-[:FOLLOWS*2..2]->(target:User), " +
                                "(target)<-[r:FOLLOWS]-() " +
                                "WHERE NOT EXISTS((me)-[:FOLLOWS]->(target)) " +
                                "RETURN DISTINCT target.username AS Username, target.email AS Email, " +
                                "COUNT(DISTINCT r) as numFollower " +
                                "ORDER BY numFollower DESC, Username " +
                                "SKIP $skipFirstLevel " +
                                "LIMIT $firstLevel " +
                                "UNION " +
                                "MATCH (me:User {username: $username})-[:LIKES]->()<-[:LIKES]-(target:User), " +
                                "(target)<-[r:FOLLOWS]-() " +
                                "WHERE NOT EXISTS((me)-[:FOLLOWS]->(target)) " +
                                "RETURN target.username AS Username, target.email AS Email, " +
                                "COUNT(DISTINCT r) as numFollower " +
                                "ORDER BY numFollower DESC, Username " +
                                "SKIP $skipSecondLevel " +
                                "LIMIT $secondLevel",
                        parameters("username", u.getUsername(), "firstLevel", numberFirstLv, "secondLevel", numberSecondLv,  "skipFirstLevel", skipFirstLv, "skipSecondLevel", skipSecondLv));
                while (result.hasNext()) {
                    Record r = result.next();
                    User snap = new User(r.get("Username").asString(), r.get("Email").asString(),
                            "","","",-1,"", new ArrayList<>(), 0);

                    usersSnap.add(snap);
                }
                return null;
            });
        }
        return usersSnap;
    }

    /**
     * Returns a list of suggested wineries snapshots
     * first level: most followed wineries followed by followed users
     * second level: most followed wineries followed by users that are 2 FOLLOWS hops far from the logged user
     * @param u info of the user
     * @param numberFirstLv how many wineries suggest from first level suggestion
     * @param numberSecondLv how many wineries suggest from second level
     * @return A list of suggested wineries
     */
    public List<Pair<String, Winery>> getSnapsOfSuggestedWinerys(User u, int numberFirstLv, int numberSecondLv, int skipFirstLv, int skipSecondLv){
        List<Pair<String, Winery>> winerysSnap = new ArrayList<>();
        try(Session session = driver.session()){
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (target:Winery)<-[f:FOLLOWS]-(u:User)<-[:FOLLOWS]-(me:User{username:$username}), " +
                                "(target)<-[r:FOLLOWS]-(n:User) WITH DISTINCT me, target, " +
                                "COUNT(DISTINCT r) AS numFollower, COUNT(DISTINCT u) AS follow " +
                                "WHERE NOT EXISTS((me)-[:FOLLOWS]->(target)) " +
                                "RETURN target.owner AS Owner, target.title AS Title, numFollower + follow AS followers " +
                                "ORDER BY followers DESC, Title " +
                                "SKIP $skipFirstLevel " +
                                "LIMIT $firstLevel " +
                                "UNION " +
                                "MATCH (target:Winery)<-[f:FOLLOWS]-(u:User)<-[:FOLLOWS*2..2]-(me:User{username:$username}), " +
                                "(target)<-[r:FOLLOWS]-(n:User) WITH DISTINCT me, target, " +
                                "COUNT(DISTINCT r) AS numFollower, COUNT(DISTINCT u) AS follow " +
                                "WHERE NOT EXISTS((me)-[:FOLLOWS]->(target))" +
                                "RETURN target.owner AS Owner, target.title AS Title, numFollower + follow AS followers " +
                                "ORDER BY followers DESC, Title " +
                                "SKIP $skipSecondLevel " +
                                "LIMIT $secondLevel",
                        parameters("username", u.getUsername(), "firstLevel", numberFirstLv, "secondLevel", numberSecondLv, "skipFirstLevel", skipFirstLv, "skipSecondLevel", skipSecondLv));

                while(result.hasNext()){
                    Record r = result.next();
                    Winery snap = new Winery(r.get("Title").asString(), null);
                    winerysSnap.add(new Pair<>(r.get("Owner").asString(), snap));
                }

                return null;
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        return winerysSnap;
    }


    /**
     * Returns a list of suggested wines snapshots
     * first level: wines liked by followed users
     * second level: likes liked by users that are 2 FOLLOWS hops far from the logged user
     * Wines returned are ordered by the number of times they appeared in the results
     * @param u Logged User
     * @param numberFirstLv how many wines suggest from first level
     * @param numberSecondLv how many wines suggest from second level
     * @return A list of suggested wines
     */
    public List<Wine> getSnapsOfSuggestedWines(User u, int numberFirstLv, int numberSecondLv, int skipFirstLv, int skipSecondLv) {
        List<Wine> winesSnap = new ArrayList<>();
        try(Session session = driver.session()){
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (target:Wine)<-[r:LIKES]-(u:User)<-[:FOLLOWS]-(me:User{username:$username}) " +
                                "WHERE NOT EXISTS((me)-[:LIKES]->(target)) " +
                                "RETURN target.vivino_id AS VivinoId, target.glugulp_id AS GlugulpId, target.name as Name, " +
                                "target.winemaker AS Winemaker, target.varietal AS Varietal, COUNT(*) AS nOccurences " +
                                "ORDER BY nOccurences DESC, Name " +
                                "SKIP $skipFirstLevel " +
                                "LIMIT $firstlevel " +
                                "UNION " +
                                "MATCH (target:Wine)<-[r:LIKES]-(u:User)<-[:FOLLOWS*2..2]-(me:User{username:$username}) " +
                                "WHERE NOT EXISTS((me)-[:LIKES]->(target)) " +
                                "RETURN target.vivino_id AS VivinoId, target.glugulp_id AS GlugulpId, target.name as Name, " +
                                "target.winemaker AS Winemaker, target.varietal AS Varietal, COUNT(*) AS nOccurences " +
                                "ORDER BY nOccurences DESC, Name " +
                                "SKIP $skipSecondLevel " +
                                "LIMIT $secondLevel",
                        parameters("username", u.getUsername(), "firstlevel", numberFirstLv, "secondLevel", numberSecondLv, "skipFirstLevel", skipFirstLv, "skipSecondLevel", skipSecondLv));
                while(result.hasNext()){
                    Record r = result.next();
                    List<String> authors = new ArrayList<>();

                    String vivino_id = null;
                    String glugulp_id = null;
                    if(r.get("VivinoId").isNull())
                        glugulp_id = r.get("GlugulpId").asString();
                    else
                        vivino_id = r.get("VivinoId").asString();

                    Wine snap = new Wine( vivino_id,
                            glugulp_id,
                            r.get("Name").asString(),
                            r.get("Winemaker").asString(),
                            "",
                            r.get("Varietal").asString(),
                            "",
                            -1,
                            -1,
                            "",
                            "",
                            new ArrayList<>());

                    winesSnap.add(snap);
                }

                return null;
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        return winesSnap;
    }

    /**
     * Return most followed users
     * @param num num of rank
     * @return pair (name, numFollower)
     */
    public List<Pair<User, Integer>> getMostFollowedUsers (int skip, int num) {
        List<Pair<User, Integer>> rank;
        try (Session session = driver.session()) {
            rank = session.readTransaction(tx -> {
                Result result = tx.run("MATCH (target:User)<-[r:FOLLOWS]-(:User) " +
                                "RETURN DISTINCT target.username AS Username, target.email AS Email, " +
                                "COUNT(DISTINCT r) as numFollower " +
                                "ORDER BY numFollower DESC, Username " +
                                "SKIP $skip " +
                                "LIMIT $num",
                        parameters("skip", skip, "num", num));
                List<Pair<User, Integer>> popularUser = new ArrayList<>();
                while (result.hasNext()) {
                    Record r = result.next();
                    User snap = new User(r.get("Username").asString(), r.get("Email").asString(),
                            "","","",-1,"", new ArrayList<>(), 0);

                    popularUser.add(new Pair(snap, r.get("numFollower").asInt()));
                }
                return popularUser;
            });
        }
        return rank;
    }

    /**
     * Return most followed wineries
     * @param num num of rank
     * @return pair (name, numFollower)
     */
    public List<Pair<Pair<String, Winery>, Integer>> getMostFollowedWinerys (int skip, final int num) {
        List<Pair<Pair<String, Winery>, Integer>> rank;
        try (Session session = driver.session()) {
            rank = session.readTransaction(tx -> {
                Result result = tx.run("MATCH (target:Winery)<-[r:FOLLOWS]-(:User) " +
                                "RETURN DISTINCT target.title AS Title, target.owner AS Owner, " +
                                "COUNT(DISTINCT r) as numFollower " +
                                "ORDER BY numFollower DESC, Owner " +
                                "SKIP $skip " +
                                "LIMIT $num",
                        parameters("skip", skip, "num", num));
                List<Pair<Pair<String, Winery>, Integer>> popularWinerys = new ArrayList<>();
                while (result.hasNext()) {
                    Record r = result.next();
                    Winery snap = new Winery(r.get("Title").asString(), null);

                    popularWinerys.add(new Pair(new Pair(r.get("Owner").asString(), snap)
                            , r.get("numFollower").asInt()));
                }
                return popularWinerys;
            });
        }
        return rank;
    }


    /**
     * Returns wines with the highest number of likes
     * @param limit
     * @return List of Wines
     */
    public List<Pair<Wine, Integer>> getMostLikedWines(int skip, int limit) {
        List<Pair<Wine, Integer>> topWines = new ArrayList<>();

        try(Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (:User)-[l:LIKES]->(w:Wine) " +
                                "RETURN w.vivino_id AS VivinoId, w.glugulp_id AS GlugulpId, w.name AS Name, " +
                                "w.winemaker AS Winemaker, " +
                                "w.varietal AS Varietal, " +
                                "COUNT(l) AS like_count " +
                                "ORDER BY like_count DESC, Name " +
                                "SKIP $skip " +
                                "LIMIT $limit",
                        parameters( "skip", skip, "limit", limit));

                while(result.hasNext()){
                    Record r = result.next();
                    List<String> authors = new ArrayList<>();

                    String vivino_id = null;
                    String glugulp_id = null;
                    if(r.get("VivinoId").isNull())
                        glugulp_id = r.get("GlugulpId").asString();
                    else
                        vivino_id = r.get("VivinoId").asString();

                    Wine snap = new Wine( vivino_id,
                            glugulp_id,
                            r.get("Name").asString(),
                            r.get("Winemaker").asString(),
                            "",
                            r.get("Varietal").asString(),
                            "",
                            -1,
                            -1,
                            "",
                            "",
                            new ArrayList<>());

                    topWines.add(new Pair(snap, r.get("like_count").asInt()));
                }
                return null;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return topWines;
    }


    /**
     * Delete a User from the GraphDB
     * @param u info of the user
     * @return result of the function
     */
    public boolean deleteUser(User u) {
        try(Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User) WHERE u.username = $username DETACH DELETE u",
                        parameters("username", u.getUsername()));
                return null;
            });
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns varietals with the highest number of likes
     * @return list of varietals and the number of likes
     */
    public List<Pair<String, Integer>> getVarietalsSummaryByLikes() {
        List<Pair<String, Integer>> results = new ArrayList<>();
        try(Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run( "MATCH (w:Wine)<-[l:LIKES]-(:User) " +
                        "RETURN count(l) AS nLikes, w.varietal AS Varietal " +
                        "ORDER BY nLikes DESC");

                while(result.hasNext()){
                    Record r = result.next();
                    results.add(new Pair(r.get("Varietal").asString(), r.get("nLikes").asInt()));
                }
                return null;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Returns the users followed by a User
     * @param u info of the user
     * @return list of followed users
     */
    public List<User> getSnapsOfFollowedUser (User u) {
        List<User> followedUsers;
        try (Session session = driver.session()) {
            followedUsers = session.writeTransaction((TransactionWork<List<User>>) tx -> {
                Result result = tx.run("MATCH (:User {username: $username})-[:FOLLOWS]->(u:User) " +
                                "RETURN u.username AS Username, u.email AS Email ORDER BY Username DESC " ,
                        parameters("username", u.getUsername()));
                List<User> followedList = new ArrayList<>();
                while(result.hasNext()) {
                    Record record = result.next();
                    User snap = new User(record.get("Username").asString(), record.get("Email").asString(),
                            "","","",-1,"", new ArrayList<>(), 0);
                    followedList.add(snap);
                }
                return followedList;
            });
        }
        return followedUsers;
    }

    /**
     * Returns the following users
     * @param u info of the user
     * @return list of following users
     */
    public List<User> getSnapsOfFollowingUser (User u) {
        List<User> followingUsers;
        try (Session session = driver.session()) {
            followingUsers = session.writeTransaction((TransactionWork<List<User>>) tx -> {
                Result result = tx.run("MATCH (:User {username: $username})<-[:FOLLOWS]-(u:User) " +
                                "RETURN u.username AS Username, u.email AS Email ORDER BY Username DESC " ,
                        parameters("username", u.getUsername()));
                List<User> followingList = new ArrayList<>();
                while(result.hasNext()) {
                    Record record = result.next();
                    User snap = new User(record.get("Username").asString(), record.get("Email").asString(),
                            "","","",-1,"", new ArrayList<>(), 0);
                    followingList.add(snap);
                }
                return followingList;
            });
        }
        return followingUsers;
    }

}
