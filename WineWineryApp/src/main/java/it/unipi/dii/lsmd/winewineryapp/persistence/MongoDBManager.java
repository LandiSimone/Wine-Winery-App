package it.unipi.dii.lsmd.winewineryapp.persistence;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import it.unipi.dii.lsmd.winewineryapp.model.*;
import javafx.util.Pair;
import org.bson.Document;
import com.google.gson.Gson;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Accumulators.sum;

public class MongoDBManager {
    public MongoDatabase db;
    private MongoCollection usersCollection;
    private MongoCollection winesCollection;
    private MongoCollection commentsCollection;

    public MongoDBManager(MongoClient client) {
        this.db = client.getDatabase("winewinery");
        usersCollection = db.getCollection("Users");
        winesCollection = db.getCollection("Wines");
        commentsCollection = db.getCollection("Comments");
    }

    /**
     * perform the login
     * @param username User that is logging in
     * @param password
     * @return User informations related to the username
     */
    public User login (String username, String password) {
        Document result = (Document) usersCollection.find(Filters.and(eq("username", username),
                        eq("password", password))).
                first();

        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(result), User.class);
    }

    /**
     * add a user
     * @param u User to add
     * @return result of the function
     */
    public boolean addUser (User u) {
        try {
            Document doc = new Document("username", u.getUsername())
                    .append("email", u.getEmail())
                    .append("password", u.getPassword());

            if (u.getFirstName() != null)
                doc.append("firstname", u.getFirstName());
            if (u.getLastName() != null)
                doc.append("lastname", u.getLastName());
            if (u.getAge() != -1)
                doc.append("age", u.getAge());
            if (u.getLocation() != null)
                doc.append("location", u.getLocation());

            doc.append("winerys", u.getWinerys());

            usersCollection.insertOne(doc);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * get a user by username
     * @param username User to get
     * @return User information
     */
    public User getUserByUsername (String username) {
        Document result = (Document) usersCollection.find((eq("username", username))).first();
        if (result == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(result), User.class);
    }

    /**
     * delete a user
     * @param u User to delete
     * @return result of the function
     */
    public boolean deleteUser(User u) {
        try {
            Bson find = eq("comments.username", u.getUsername());
            Bson update = Updates.set("comments.$.username", "Deleted user");
            winesCollection.updateMany(find, update);
            find = eq("username", u.getUsername());
            update = Updates.set("username", "Deleted user");
            commentsCollection.updateMany(find, update);
            usersCollection.deleteOne(eq("username", u.getUsername()));
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * create a winery
     * @param user User owner of the winery
     * @param title title of the winery
     * @return result of the function
     */
    public boolean createWinery(User user, String title) {
        // check if there are other wineries with the same name
        Document document = (Document) usersCollection.find(and(eq("username", user.getUsername()),
                eq("winerys.title", title))).first();
        if (document != null) {
            System.err.println("ERROR: name already in use.");
            return false;
        }
        // create the new winery
        Document winery = new Document("title", title)
                .append("wines", Arrays.asList());
        // insert the new winery
        usersCollection.updateOne(
                eq("username", user.getUsername()),
                new Document().append(
                        "$push",
                        new Document("winerys", winery)
                )
        );
        return true;
    }

    /**
     * delete a winery
     * @param username username owner of the winery
     * @param title title of the winery
     * @return result of the function
     */
    public boolean deleteWinery(String username, String title){
        try {
            Bson filter = new Document().append("username", username);
            Bson fields = new Document().append("winerys", new Document("title", title));
            Bson update = new Document("$pull", fields);
            UpdateResult updateResult = usersCollection.updateOne(filter, update);
            if (updateResult.getModifiedCount() == 0) {
                System.err.println("ERROR: can not delete the winery " + title);
                return false;
            } else {
                return true;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * update a user
     * @param u User to update
     * @return result of the function
     */
    public boolean updateUser (User u){
        try {
            Document doc = new Document().append("username", u.getUsername());
            if (!u.getEmail().isEmpty())
                doc.append("email", u.getEmail());
            if (!u.getPassword().isEmpty())
                doc.append("password", u.getPassword());
            if (!u.getFirstName().isEmpty())
                doc.append("firstname", u.getFirstName());
            if (!u.getLastName().isEmpty())
                doc.append("lastname", u.getLastName());
            if (u.getAge() != -1)
                doc.append("age", u.getAge());
            if (u.getLocation().isEmpty())
                doc.append("location", u.getLocation());
            doc.append("type", u.getType());

            Bson updateOperation = new Document("$set", doc);
            usersCollection.updateOne(new Document("username", u.getUsername()), updateOperation);
            return true;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * remove a wine from a winery
     * @param user owner of the winery
     * @param title title of the winery
     * @param w wine to remove
     * @return updated result
     */
    public UpdateResult removeWineFromWinery(String user, String title, Wine w) {
        try {
            Document wineReduced = new Document("vivino_id", w.getVivino_id())
                    .append("glugulp_id", w.getGlugulp_id())
                    .append("name", w.getName())
                    .append("winemaker", w.getWinemaker())
                    .append("varietal", w.getVarietal());

            Bson find = and(eq("username", user),
                    eq("winerys.title", title));
            Bson delete = Updates.pull("winerys.$.wines", wineReduced);
            UpdateResult result = usersCollection.updateOne(find, delete);
            return result;
        }
        catch (Exception e)
        {
            System.out.println("Error in removing a wine from a Winery");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get a wine by Id
     * @param wine info to get the wine
     * @return info of the wine
     */
    public Wine getWineById (Wine wine) {
        try {
            Wine w = null;
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
            Document myDoc = (Document) winesCollection.find(
                    and(eq("vivino_id", wine.getVivino_id()), eq("glugulp_id", wine.getGlugulp_id()))).first();
            w = gson.fromJson(gson.toJson(myDoc), Wine.class);
            return w;
        }
        catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * add a to a winery
     * @param user owner of the winery
     * @param title title of the winery
     * @param w wine to add
     * @return updated result
     */
    public UpdateResult addWineToWinery(String user, String title, Wine w) {
        Document wineReduced;
        if (w.getVivino_id() != null) {
            wineReduced = new Document("vivino_id", w.getVivino_id());
        }
        else {
            wineReduced = new Document("glugulp_id", w.getGlugulp_id());
        }

        wineReduced.append("name", w.getName())
                .append("winemaker", w.getWinemaker())
                .append("varietal", w.getVarietal());

        Bson find = and(eq("username", user),
                eq("winerys.title", title));
        Bson update = Updates.addToSet("winerys.$.wines", wineReduced);
        UpdateResult result = usersCollection.updateOne(find, update);
        return result;
    }

    /**
     * count all the comments of a wine
     * @param wine wine about which we want to know the comments
     * @return number of comments
     */
    public int countAllComment(Wine wine){
        Bson filter = Filters.and(eq("vivino_id", wine.getVivino_id()), eq("glugulp_id", wine.getGlugulp_id()));
        return (int) commentsCollection.countDocuments(filter);
    }

    /**
     * count the number of recent comments of a wine
     * @param wine wine about which we want to know the comments
     * @return number of comments
     */
    public int numberComments (Wine wine){
        List<Comment> comments = wine.getComments();
        int numberOfComments;
        if (comments != null)
            numberOfComments = comments.size();
        else
            numberOfComments = 0;
        return numberOfComments;
    }

    /**
     * delete the oldest comment of the recent comments of a wine
     * @param wine wine about which we want to delete the comment
     */
    public void deleteOldestComment(Wine wine) {
        List<Comment> comments = wine.getComments();

        comments.remove(0);

        updateRecentComments(wine, comments);
    }

    /**
     * add a comment inside wine collection
     * @param wine info of the wine about we want to add a comment
     * @param comment info of the comment
     * @return result of the function
     */
    public boolean addRecentComment (Wine wine, Comment comment) {
        try {
            // update recent comments
            // check number of comments
            int n = numberComments(wine);
            if (n == 10) {
                // remove the oldest comment
                deleteOldestComment(wine);
            }
            //add comment
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Document doc = new Document("username", comment.getUsername())
                    .append("text", comment.getText())
                    .append("timestamp", dateFormat.format(comment.getTimestamp()));

            Bson find = and(eq("vivino_id", wine.getVivino_id()), eq("glugulp_id", wine.getGlugulp_id()));
            Bson update = Updates.addToSet("comments", doc);
            winesCollection.updateOne(find, update);

            return true;
        }
        catch (Exception e)
        {
            System.out.println("Error in adding a comment to a Wine");
            e.printStackTrace();
            return false;
        }

    }

    /**
     * add a comment inside comment collection
     * @param wine info of the wine about we want to add a comment
     * @param comment info of the comment
     * @return result of the function
     */
    public boolean addTotalComment (Wine wine, Comment comment) {
        try {
            //add comment
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Document doc = new Document("username", comment.getUsername())
                    .append("text", comment.getText())
                    .append("timestamp", dateFormat.format(comment.getTimestamp()))
                    .append("varietal", wine.getVarietal());

            if (wine.getVivino_id() != null)
                doc.append("vivino_id", wine.getVivino_id());
            if (wine.getGlugulp_id() != null)
                doc.append("glugulp_id", wine.getGlugulp_id());

            commentsCollection.insertOne(doc);

            return true;
        }
        catch (Exception e)
        {
            System.out.println("Error in adding a comment to a Wine");
            e.printStackTrace();
            return false;
        }

    }

    /**
     * delete a comment inside wine collection
     * @param wine info of the wine about we want to delete a comment
     * @param comment info of the comment
     * @return result of the function
     */
    public boolean deleteRecentComment (Wine wine, Comment comment) {
        try {
            // delete recent comment
            List<Comment> comments = wine.getComments();
            int n = 0;
            int d = 0;
            int check = 0;
            for (Comment c : comments) {
                if (c.getTimestamp().equals(comment.getTimestamp()) && c.getUsername().equals(comment.getUsername())) {
                    d = n;
                    check = 1;
                    break;
                }
                n++;
            }
            if (check == 1) {
                comments.remove(d);
                updateRecentComments(wine, comments);
            }
            return true;
        }
        catch (Exception e)
        {
            System.out.println("Error in deleting a comment to a Wine");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * delete a comment inside comment collection
     * @param wine info of the wine about we want to delete a comment
     * @param comment info of the comment
     * @return result of the function
     */
    public boolean deleteTotalComment (Wine wine, Comment comment) {
        try {
            // delete total comment
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Bson filter = and(
                    eq("username", comment.getUsername()),
                    eq("timestamp", dateFormat.format(comment.getTimestamp())),
                    eq("vivino_id", wine.getVivino_id()),
                    eq("glugulp_id", wine.getGlugulp_id())
            );
            commentsCollection.deleteOne(filter);
            if (Session.getInstance().getLoggedUser().getType() > 0)
                incrementDeletedCommentsCounter(comment.getUsername());
            return true;
        }
        catch (Exception e) {
            System.out.println("Error in deleting a comment to a Wine");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * update the recent comments of a wine
     * @param w info of the wine about we want to update recent comments
     * @param comments list of comments to be updated
     * @return result of the function
     */
    public boolean updateRecentComments(Wine w, List<Comment> comments){
        try{

            Bson update = new Document("comments", comments);
            Bson updateOperation = new Document("$set", update);
            if(w.getVivino_id() != null)
                winesCollection.updateOne(new Document("vivino_id", w.getVivino_id()), updateOperation);
            else
                winesCollection.updateOne(new Document("glugulp_id", w.getGlugulp_id()), updateOperation);
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.err.println("Error in updating comment on MongoDB");
            return false;
        }
    }

    /**
     * update the all the comments of a wine
     * @param w info of the wine about we want to update all the comments
     * @param comment info of the comment to be updated
     * @return result of the function
     */
    public boolean updateTotalComments(Wine w, Comment comment){
        try {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Document doc = new Document().append("username", comment.getUsername());
            doc.append("timestamp", dateFormat.format(comment.getTimestamp()));
            doc.append("text", comment.getText());
            if (w.getGlugulp_id() != null)
                doc.append("glugulp_id", w.getGlugulp_id());
            if (w.getVivino_id() != null)
                doc.append("vivino_id", w.getVivino_id());

            Bson updateOperation = new Document("$set", doc);
            Bson filter = and(
                    eq("username", comment.getUsername()),
                    eq("timestamp", dateFormat.format(comment.getTimestamp())));

            commentsCollection.updateOne(filter, updateOperation);
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.err.println("Error in updating comment on MongoDB");
            return false;
        }
    }

    /**
     * increment the number of deleted comments
     * @param username user whose value we want to increase
     */
    public void incrementDeletedCommentsCounter(String username) {
        usersCollection.updateOne(eq("username", username), inc("deletedComments", 1));
    }

    /**
     * update a comment inside wine collection
     * @param wine info of the wine about we want to update the comment
     * @param comment info of the comment to be updated
     */
    public boolean updateComment(Wine wine, Comment comment){
        try {
            // recent comments
            List<Comment> comments = wine.getComments();
            int check = 0;
            int i = 0;
            for (Comment c : comments) {
                if (c.getUsername().equals(comment.getUsername()) && c.getTimestamp().equals(
                        comment.getTimestamp())) {
                    comments.set(i, comment);
                    check = 1;
                    break;
                }
                i++;
            }
            if(check == 1)
                updateRecentComments(wine, comments);
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.err.println("Error in updating comment on MongoDB");
            return false;
        }
    }

    /**
     * get the info of a winery
     * @param owner owner of the winery
     * @param title name of the winery
     * @return info of the winery
     */
    public Winery getWinery(String owner, String title) {
        Winery winery = null;
        Gson gson = new GsonBuilder().serializeNulls().create();
        Bson match = match(eq("username", owner));
        Bson unwind = unwind("$winerys");
        Bson match2 = match(eq("winerys.title", title));
        Bson project = project(fields(excludeId(), computed("Winery", "$winerys")));
        MongoCursor<Document> iterator = (MongoCursor<Document>) usersCollection.aggregate(Arrays.asList(match, unwind,
                match2, project)).iterator();
        if(iterator.hasNext()){
            Document document = iterator.next();
            Document WineryDocument = (Document) document.get("Winery");
            winery = gson.fromJson(gson.toJson(WineryDocument), Winery.class);
        }
        return winery;
    }


    /**
     * search wines given parameters.
     * @param name partial name of the wines to match
     * @param winemaker partial name of the winemakers to match
     * @param country name of the country to match
     * @param varietal name of the varietal to match
     * @param grapes name of the grapes to match
     * @param min_year minimum year to match
     * @param max_year maximum year to match
     * @param min_price minimum price to match
     * @param max_price maximum price to match
     * @return a list of wines that match the parameters
     */
    public List<Wine> searchWinesByParameters (String name, String winemaker, String country,
                                               String varietal, String grapes, int min_year, int max_year,
                                               double min_price, double max_price, int skip, int limit) {
        List<Wine> wines = new ArrayList<>();
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();

        List<Bson> pipeline = new ArrayList<>();

        if (!name.isEmpty()) {
            Pattern pattern1 = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            pipeline.add(match(regex("name", pattern1)));
        }

        if (!winemaker.isEmpty()) {
            Pattern pattern2 = Pattern.compile("^.*" + winemaker + ".*$", Pattern.CASE_INSENSITIVE);
            pipeline.add(match(regex("winemaker", pattern2)));
        }

        if (!country.isEmpty()) {
            pipeline.add(match(eq("country", country)));
        }

        if (!varietal.isEmpty()) {
            Pattern pattern3 = Pattern.compile("^.*" + varietal + ".*$", Pattern.CASE_INSENSITIVE);
            pipeline.add(match(eq("varietal", pattern3)));
        }

        if (!grapes.isEmpty()) {
            Pattern pattern4 = Pattern.compile("^.*" + grapes + ".*$", Pattern.CASE_INSENSITIVE);
            pipeline.add(match(eq("grapes", pattern4)));
        }

        if (min_year != 0) {
            pipeline.add(match(and(gte("year", min_year))));
        }

        if(max_year != 0) {
            pipeline.add(match(and(lte("year", max_year))));
        }

        if (min_price != 0)
            pipeline.add(match(and(gte("price", min_price))));

        if(max_price != 0) {
            pipeline.add(match(and(lte("price", max_price))));
        }

        pipeline.add(sort(ascending("price")));
        pipeline.add(skip(skip));
        pipeline.add(limit(limit));

        List<Document> results = (List<Document>) winesCollection.aggregate(pipeline).into(new ArrayList<>());
        Type winesListType = new TypeToken<ArrayList<Wine>>(){}.getType();
        wines = gson.fromJson(gson.toJson(results), winesListType);
        return wines;
    }

    /**
     * return users that contains the keyword
     * @param next select the portion of result
     * @param keyword keyword to search users
     * @return list of users
     */
    public List<User> getUsersByKeyword (String keyword, boolean moderator, int next) {
        List<User> results = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        Consumer<Document> convertInUser = doc -> {
            User user = gson.fromJson(gson.toJson(doc), User.class);
            results.add(user);
        };
        Pattern pattern= Pattern.compile("^.*" + keyword + ".*$", Pattern.CASE_INSENSITIVE);
        Bson filter = Aggregates.match(Filters.regex("username", pattern));
        Bson limit = limit(8);
        Bson skip = skip(next*8);
        if (moderator) {
            Bson moderatorFilter = match(eq("type", 1));
            usersCollection.aggregate(Arrays.asList(filter, moderatorFilter, skip, limit)).forEach(convertInUser);
        } else
            usersCollection.aggregate(Arrays.asList(filter, skip, limit)).forEach(convertInUser);
        return results;
    }


    /**
     * return the Wineries given the title
     * @param keyword part of the title
     * @return  The list of wineries and its owner
     */
    public List<Pair<String, Winery>> getWineryByKeyword (String keyword, int skipDoc, int limitDoc) {
        List<Pair<String, Winery>> winerys = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeNulls().create();
        Bson unwind = unwind("$winerys");
        Pattern pattern= Pattern.compile("^.*" + keyword + ".*$", Pattern.CASE_INSENSITIVE);
        Bson filter = Aggregates.match(Filters.regex("winerys.title", pattern));
        Bson skip = skip(skipDoc);
        Bson limit = limit(limitDoc);
        MongoCursor<Document> iterator = (MongoCursor<Document>) usersCollection.aggregate(Arrays.asList(unwind,
                filter, skip, limit)).iterator();
        while(iterator.hasNext()){
            Document document = iterator.next();
            String username = document.getString("username");
            Document WineryDocument = (Document) document.get("winerys");
            Winery winery = gson.fromJson(gson.toJson(WineryDocument), Winery.class);
            winerys.add(new Pair<>(username, winery));
        }
        return winerys;
    }



    /**
     * return a list of User with the highest number of varietals in their wineries
     * @param limitDoc First "number" users
     * @param skipDoc Skip users
     * @return  The list of users
     */
    public List<Pair<User, Integer>> getTopVersatileUsers (int skipDoc, int limitDoc) {
        List<Pair<User, Integer>> results = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        Consumer<Document> convertInUser = doc -> {
            User user = gson.fromJson(gson.toJson(doc), User.class);
            results.add(new Pair(user, doc.getInteger("totalVarietal")));
        };
        Bson unwind1 = unwind("$winerys");
        Bson unwind2 = unwind("$winerys.wines");
        // Distinct occurrences
        Bson groupMultiple = new Document("$group",
                new Document("_id", new Document("username", "$username")
                        .append("email", "$email")
                        .append("password", "$password")
                        .append("firstname", "$firstname")
                        .append("lastname", "$lastname")
                        .append("age", "$age")
                        .append("location", "$location")
                        .append("varietal", "$winerys.wines.varietal")
                ));
        // Sum all occurrences
        Bson group = new Document("$group",
                new Document("_id",
                        new Document("username", "$_id.username")
                                .append("email", "$_id.email")
                                .append("password", "$_id.password")
                                .append("firstname", "$_id.firstname")
                                .append("lastname", "$_id.lastname")
                                .append("age", "$_id.age")
                                .append("location", "$_id.location"))
                        .append("totalVarietal",
                                new Document("$sum", 1)));
        Bson project = project(fields(excludeId(),
                computed("username", "$_id.username"),
                computed("email", "$_id.email"),
                computed("password", "$_id.password"),
                computed("firstname", "$_id.firstname"),
                computed("lastname", "$_id.lastname"),
                computed("age", "$_id.age"),
                computed("location", "$_id.location"),
                include("totalVarietal")));
        Bson sort = sort(descending("totalVarietal"));
        Bson skip = skip(skipDoc);
        Bson limit = limit(limitDoc);
        usersCollection.aggregate(Arrays.asList(unwind1, unwind2, groupMultiple, group,
                project, sort, skip, limit)).forEach(convertInUser);
        return results;
    }


    /**
     * return a list of users whose comments have been deleted at least one time.
     * It is used by admins to ban users who have not respected application's policy.
     * @return List of Users
     */
    public List<User> getBadUsers(int skipDoc, int limitDoc) {
        List<User> results = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        Consumer<Document> convertInUser = doc -> {
            User user = gson.fromJson(gson.toJson(doc), User.class);
            results.add(user);
        };
        Bson filter = match(gte("deletedComments", 1));
        Bson skip = skip(skipDoc);
        Bson limit = limit(limitDoc);
        usersCollection.aggregate(Arrays.asList(filter, skip, limit)).forEach(convertInUser);
        return results;
    }

    /**
     * return the top varietals by the number of wines
     * @return The list of the most common varietals
     */
    public List<Pair<String, Integer>> getVarietalsSummaryByNumberOfWines (){
        List<Pair<String,Integer>> varietals = new ArrayList<>();

        Bson group = group("$varietal", sum("totalWine", 1));
        Bson project = project(fields(excludeId(), computed("varietal", "$_id"),
                include("totalWine")));
        Bson sort = sort(descending("totalWine"));

        List<Document> results = (List<Document>) winesCollection.aggregate(Arrays.asList(group,
                project, sort)).into(new ArrayList<>());

        for (Document document: results)
        {
            varietals.add(new Pair(document.getString("varietal"),
                    document.getInteger("totalWine")));
        }
        return varietals;
    }

    /**
     * Browse the top varietals with more comments
     * @param period (all, month, week)
     * @return HashMap with the varietal and the number of comments
     */
    public List<Pair<String, Integer>> getVarietalsSummaryByComments(String period) {
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime startOfDay = null;
        switch (period) {
            case "all" -> startOfDay = LocalDateTime.MIN;
            case "month" -> startOfDay = localDateTime.toLocalDate().atStartOfDay().minusMonths(1);
            case "week" -> startOfDay = localDateTime.toLocalDate().atStartOfDay().minusWeeks(1);
            default -> {
                System.err.println("ERROR: Wrong period.");
                return null;
            }
        }
        String filterDate = startOfDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<Pair<String, Integer>> results = new ArrayList<>();
        Consumer<Document> rankVarietals = doc ->
                results.add(new Pair<>((String) doc.get("_id"), (Integer) doc.get("tots")));

        Bson filter = match(gte("timestamp", filterDate));
        Bson group = group("$varietal", sum("tots", 1));
        Bson sort = sort(Indexes.descending("tots"));
        commentsCollection.aggregate(Arrays.asList(filter, group, sort)).forEach(rankVarietals);

        return results;
    }

    /**
     * return wines with the highest number of comments in the specified period of time.
     * @param period (all, month, week)
     * @param skipDoc (positive integer)
     * @param limitDoc (positive integer)
     * @return HashMap with the name and the number of comments
     */
    public List<Pair<Wine, Integer>> getMostCommentedWines(String period, int skipDoc, int limitDoc) {
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime startOfDay = null;
        switch (period) {
            case "all" -> startOfDay = LocalDateTime.MIN;
            case "month" -> startOfDay = localDateTime.toLocalDate().atStartOfDay().minusMonths(1);
            case "week" -> startOfDay = localDateTime.toLocalDate().atStartOfDay().minusWeeks(1);
            default -> {
                System.err.println("ERROR: Wrong period.");
                return null;
            }
        }
        String filterDate = startOfDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();

        Bson filter = match(gte("timestamp", filterDate));
        Bson group = group(
                new Document("$ifNull", Arrays.asList("$vivino_id", "$glugulp_id")),
                sum("totalComment", 1)
        );
        Bson sort = sort(Indexes.descending("totalComment"));
        Bson skip = skip(skipDoc);
        Bson limit = limit(limitDoc);

        AggregateIterable<Document> countResults = commentsCollection.aggregate(Arrays.asList(filter,
                group, sort, skip, limit));
        List<Pair<Wine, Integer>> resultList = new ArrayList<>();

        for (Document doc : countResults) {
            String wineId = doc.getString("_id");
            Document myDoc = (Document) winesCollection.find(or(eq("vivino_id", wineId),
                    eq("glugulp_id", wineId))).first();
            Wine wine = gson.fromJson(gson.toJson(myDoc), Wine.class);

            Integer count = doc.getInteger("totalComment");
            resultList.add(new Pair<>(wine, count));
        }
        return resultList;
    }

    /**
     * returns all the varietals
     */
    public List<String> getVarietals() {
        List<String> varietalsList = new ArrayList<>();
        winesCollection.distinct("varietal", String.class).into(varietalsList);
        return varietalsList;
    }

    /**
     * return all the comments
     * @param wine about which we want to know the comments
     * @return list of comments
     */
    public List<Comment> getAllComments (Wine wine, int skipDoc, int limitDoc) {
        List<Comment> results = new ArrayList<>();
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
        Consumer<Document> convertInComment = doc -> {
            Comment comment = gson.fromJson(gson.toJson(doc), Comment.class);
            results.add(comment);
        };
        Bson filter = match(and(eq("vivino_id", wine.getVivino_id()),
                eq("glugulp_id", wine.getGlugulp_id())));
        Bson sort = sort(Indexes.ascending("timestamp"));
        Bson skip = skip(skipDoc*limitDoc);
        Bson limit = limit(limitDoc);

        commentsCollection.aggregate(Arrays.asList(filter, sort, skip, limit)).forEach(convertInComment);
        return results;
    }
}
