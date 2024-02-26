import cmd
import math
import sys
from asyncio import sleep
from pymongo import MongoClient
from neo4j import GraphDatabase
import pandas as pd
import random
import time
from datetime import datetime
import wine_generator
from utils.bloomfilter import BloomFilter, hash1, hash2


class App(cmd.Cmd):

    intro = 'winewinery DB_Updater launched. \n \nType help or ? to list commands.\n'
    prompt = '>'
    num_users = '1000'

    mongo_client = MongoClient('localhost', 27007, w=3, readPreference='secondaryPreferred')
    # neo4j_driver = GraphDatabase.driver("bolt://172.16.4.68:7687", auth=("neo4j", "password"))
    # mongo_client = MongoClient('localhost', 27017)
    neo4j_driver = GraphDatabase.driver("bolt://localhost:7687", auth=("neo4j", "winewinery"))

    def do_initDB(self, arg):
        'Initialize database'

        # Import data
        # wines_path = wine_generator.import_data()
        wines_path = './data/wines.json'
        # users_path = user_generator.import_data(num_users)
        users_path = './data/users.json'
        # comments_path = comment_scraper.run_data_collection
        comments_path = './data/filtered_comment.json'

        # Initialization of utils
        session = self.neo4j_driver.session()
        users_df = pd.read_json(users_path, lines=True)
        wines_df = pd.read_json(wines_path, lines=True)
        comments_df = pd.read_json(comments_path, lines=True)

        # Drop old databases
        self.mongo_client.drop_database('winewinery')
        query = ("MATCH (n) DETACH DELETE n")
        session.execute_write(lambda tx: tx.run(query))

        # Create new databases
        db = self.mongo_client["winewinery"]

        ### Neo4j database needs to be created

        ### comments
        self.insert_comments(comments_df)

        ### wines
        self.insert_wines(wines_df)

        # Converts IDs to str
        wines_df['vivino_id'] = wines_df['vivino_id'].map(str)
        wines_df['glugulp_id'] = wines_df['glugulp_id'].map(str)

        ### Users
        users_col = db["Users"]
        data_dict = users_df.to_dict("records")
        users_col.insert_many(data_dict)

        for index, row in users_df.iterrows():
            query = ("CREATE (u:User { username: $username , email: $email}) ")
            session.execute_write(lambda tx: tx.run(query, username=row['username'], email=row['email']))
        print("Added users to database")


        ### Comments

        # sort the DataFrame "comments_df" by "vivino_id" and "timestamp" in descending order
        sorted_comments_df = comments_df.sort_values(by=["vivino_id", "timestamp"], ascending=[True, False])

        # create a new DataFrame for the most recent comments for each "vivino_id"
        top_10_comments_df = sorted_comments_df.groupby("vivino_id").head(10).reset_index(drop=True)

        # get a unique list of "vivino_id" from the DataFrame "top_10_comments_df"
        unique_vivino_ids = top_10_comments_df["vivino_id"].unique()

        # iterate through the rows of the DataFrame "comments_df" and add the most recent comments for each wine
        for vivino_id in unique_vivino_ids:

            # filter comments for current wine
            filtered_df = top_10_comments_df[top_10_comments_df["vivino_id"] == vivino_id]

            # number of comments
            num_comments = filtered_df.shape[0]

            comments = []
            for i in range(0, num_comments):

                comment = {'username': str(filtered_df.iloc[i]['username']),
                           'text': str(filtered_df.iloc[i]['text']),
                           'timestamp': pd.to_datetime(filtered_df.iloc[i]['timestamp']).strftime('%Y-%m-%d %H:%M:%S')}
                comments.append(comment)

            vivino_id = str(vivino_id)

            db.Wines.update_one({'vivino_id': vivino_id},
                                 {'$set': {'comments': comments}})

        print("Added recent vivino comments to database")

        # sort the DataFrame "comments_df" by "glugulp_id" and "timestamp" in descending order
        sorted_comments_df = comments_df.sort_values(by=["glugulp_id", "timestamp"], ascending=[True, False])

        # create a new DataFrame for the most recent comments for each "glugulp_id"
        top_10_comments_df = sorted_comments_df.groupby("glugulp_id").head(10).reset_index(drop=True)

        # get a unique list of "glugulp_id" from the DataFrame "top_10_comments_df"
        unique_glugulp_ids = top_10_comments_df["glugulp_id"].unique()

        # iterate through the rows of the DataFrame "comments_df" and add the most recent comments for each wine
        for glugulp_id in unique_glugulp_ids:

            # filter comments for current wine
            filtered_df = top_10_comments_df[top_10_comments_df["glugulp_id"] == glugulp_id]

            # number of comments
            num_comments = filtered_df.shape[0]

            comments = []
            for i in range(0, num_comments):
                comment = {'username': str(filtered_df.iloc[i]['username']),
                           'text': str(filtered_df.iloc[i]['text']),
                           'timestamp': pd.to_datetime(filtered_df.iloc[i]['timestamp']).strftime('%Y-%m-%d %H:%M:%S')}
                comments.append(comment)

            db.Wines.update_one({'glugulp_id': glugulp_id},
                                {'$set': {'comments': comments}})

        print("Added recent glugulp comments to database")



        ### Wineries
        for index, row in users_df.iterrows():
            num_winerys = int(random.random() * 10)
            winerys = []

            # Generate a random number of wineries
            for i in range(0, num_winerys):
                title = 'winery' + str(i)
                winery = {'title': title}

                wines = []
                num_wines_in_winerys = int(random.random() * 30)

                # Select a random number of wines to add to the wineries
                for j in range(0, num_wines_in_winerys):
                    random_wine = wines_df.sample()
                    wine_to_add = {}
                    if random_wine['vivino_id'].values[0] == "nan":
                        wine_to_add['glugulp_id'] = random_wine['glugulp_id'].values[0]
                    else:
                        wine_to_add['vivino_id'] = random_wine['vivino_id'].values[0]

                    wine_to_add['name'] = random_wine['name'].values[0]
                    wine_to_add['winemaker'] = random_wine['winemaker'].values[0]
                    wine_to_add['varietal'] = random_wine['varietal'].values[0]

                    wines.append(wine_to_add)

                winery['wines'] = wines
                winerys.append(winery)

                query = ("MATCH (a:User) "
                         "WHERE a.username = $username "
                         "CREATE (b:Winery { owner: $username, title: $title}) ")
                session.execute_write(
                    lambda tx: tx.run(query, username=row['username'], title=winery['title']))

                n_follows = int(random.random() * 10)
                for k in range(0, n_follows):

                    while True:
                        rand_follower = users_df.sample()['username'].values[0]
                        # Users can not follow their wineries
                        if rand_follower != row['username']:
                            break

                    query = (
                        "MATCH (a:User), (b:Winery) "
                        "WHERE a.username = $username1 AND (b.owner = $username2 AND b.title = $title) "
                        "CREATE (a)-[r:FOLLOWS]->(b)"
                    )

                    session.execute_write(lambda tx: tx.run(query, username1=rand_follower,
                                                                username2=row['username'], title=winery['title']))

            db.Users.update_one({'username': row['username']}, {'$set': {'winerys': winerys}})

        print("Added Winerys and Follows")

        ### User Follows and Likes
        for index, row in users_df.iterrows():
            query = (
                "MATCH (a:User), (b:User) "
                "WHERE a.username = $username1 AND b.username = $username2 "
                "CREATE (a)-[r:FOLLOWS]->(b)"
            )

            n_follows = int(random.random() * 10)
            for i in range(0, n_follows):
                while True:
                    rand_user = users_df.sample()['username'].values[0]
                    # Users can not follow themselves
                    if rand_user != row['username']:
                        break
                session.execute_write(lambda tx: tx.run(query, username1=row['username'], username2=rand_user))

            query = (
                "MATCH (a:User), (b:Wine) "
                "WHERE a.username = $username AND (b.vivino_id = $vivino_id OR b.glugulp_id = $glugulp_id) "
                "CREATE (a)-[r:LIKES]->(b)"
            )

            n_follows = int(random.random() * 10)
            for i in range(0, n_follows):
                rand_wine = wines_df.sample()
                session.execute_write(lambda tx: tx.run(query, username=row['username'],
                                                            vivino_id=rand_wine['vivino_id'].values[0],
                                                            glugulp_id=rand_wine['glugulp_id'].values[0]))

        print("Added User Follows and Likes")

        ### Special Users

        admin = {
            "username": "admin",
            "email": "admin@gmail.com",
            "password": "qUjJOC7Sk71J+/Btn71VBA==",
            "firstname": "admin",
            "lastname": "admin",
            "age": -1,
            "location": "",
            "winerys": [],
            "type": 2
        }
        users_col.insert_one(admin)

        query = ("CREATE (u:User { username: $username, email: $email }) ")
        session.execute_write(lambda tx: tx.run(query, username='admin', email='admin'))
        print("Added Administrator")

        for i in range(0, 5):
            username = "moderator" + str(i)
            moderator = {
                "username": username,
                "email": username + "@gmail.com",
                "password": "dM1vfuQfQfrMPIb4gwh3aQ==",
                "firstname": username,
                "lastname": username,
                "age": -1,
                "location": "",
                "winerys": [],
                "type": 1
            }
            users_col.insert_one(moderator)
            query = ("CREATE (u:User { username: $username, email: $email}) ")
            session.execute_write(
                lambda tx: tx.run(query, username=moderator['username'], email=moderator['email']))

        print("Added Moderators")

        session.close()

    def do_updateDB(self, arg):
        'Download latest wines'

        # Get Database
        db = self.mongo_client["winewinery"]
        wines_col = db["Wines"]
        # estimate the number of wines
        n = wines_col.estimated_document_count()

        # BloomFilter configuration
        filter_size = -n * math.log(0.001) / (math.log(2) ** 2)
        bloom_filter = BloomFilter(int(filter_size), [hash1, hash2])

        cursor = wines_col.find()
        # populate bloomfilter
        for document in cursor:
            name = document['name']
            bloom_filter.add(name)

        # import all new data
        wines_path = wine_generator.import_data()
        wines_df = pd.read_json(wines_path, lines=True)

        # check if the value is already in the db
        for index, row in wines_df.iterrows():
            name = row['name']
            if name not in bloom_filter:
                # update bloomfilter
                bloom_filter.add(name)
            else:
                # remove df row
                wines_df.drop(index, inplace=True)

        self.insert_wines(wines_df)

        print("Database updated")

    def do_exit(self, arg):
        'Exit winewinery DB_Updater'
        self.mongo_client.close()
        self.neo4j_driver.close()
        sys.exit()

    def insert_comments(self, comments_df):
        db = self.mongo_client["winewinery"]
        comments_col = db["Comments"]
        comments_df = comments_df.sort_values(by="timestamp", ascending=False)

        # Split the dataframe in order not to insert "nan" values in mongodb
        vivino_df = comments_df
        vivino_df = vivino_df.drop(columns={"glugulp_id"})
        vivino_df.dropna(subset=["vivino_id"], inplace=True)
        vivino_df['vivino_id'] = vivino_df['vivino_id'].map(str)
        #vivino_df['vivino_id'] = vivino_df['vivino_id'] + ".0"
        vivino_df['timestamp'] = vivino_df['timestamp'].dt.strftime("%Y-%m-%d %H:%M:%S")
        vivino_dict = vivino_df.to_dict("records")

        glugulp_df = comments_df
        glugulp_df = glugulp_df.drop(columns={"vivino_id"})
        glugulp_df.dropna(subset=["glugulp_id"], inplace=True)
        glugulp_df['glugulp_id'] = glugulp_df['glugulp_id'].map(str)
        glugulp_df['timestamp'] = glugulp_df['timestamp'].dt.strftime("%Y-%m-%d %H:%M:%S")
        glugulp_dict = glugulp_df.to_dict("records")

        # Converts IDs to str
        comments_df['vivino_id'] = comments_df['vivino_id'].map(str)
        comments_df['glugulp_id'] = comments_df['glugulp_id'].map(str)

        if vivino_dict:
            comments_col.insert_many(vivino_dict)
        if glugulp_dict:
            comments_col.insert_many(glugulp_dict)

        print("Added comments to databases")

    def insert_wines(self, wines_df):
        db = self.mongo_client["winewinery"]
        wines_col = db["Wines"]

        # Split the dataframe in order not to insert "nan" values in mongodb
        vivino_df = wines_df
        vivino_df = vivino_df.drop(columns={"glugulp_id"})
        vivino_df.dropna(subset=["vivino_id"], inplace=True)
        vivino_df['vivino_id'] = vivino_df['vivino_id'].map(str)
        vivino_dict = vivino_df.to_dict("records")

        glugulp_df = wines_df
        glugulp_df = glugulp_df.drop(columns={"vivino_id"})
        glugulp_df.dropna(subset=["glugulp_id"], inplace=True)
        glugulp_df['glugulp_id'] = glugulp_df['glugulp_id'].map(str)
        glugulp_dict = glugulp_df.to_dict("records")

        # Converts IDs to str
        wines_df['vivino_id'] = wines_df['vivino_id'].map(str)
        wines_df['glugulp_id'] = wines_df['glugulp_id'].map(str)

        if vivino_dict:
            wines_col.insert_many(vivino_dict)
        if glugulp_dict:
           wines_col.insert_many(glugulp_dict)

        session = self.neo4j_driver.session()
        for index, row in wines_df.iterrows():
            # Split the dataframe in order not to insert "nan" values in neo4j
            if row['vivino_id'] != "nan":
                query = ("CREATE (w:Wine { vivino_id: $vivino_id, name: $name, winemaker: $winemaker,"
                         " varietal: $varietal}) ")

                session.execute_write(lambda tx: tx.run(query, vivino_id=row['vivino_id'],
                                                            name=row['name'], winemaker=row['winemaker'],
                                                            varietal=row['varietal']))
            else:
                query = ("CREATE (w:Wine { glugulp_id: $glugulp_id, name: $name, winemaker: $winemaker,"
                         " varietal: $varietal}) ")

                session.execute_write(lambda tx: tx.run(query, glugulp_id=row['glugulp_id'],
                                                            name=row['name'], winemaker=row['winemaker'],
                                                            varietal=row['varietal']))


        print("Added wines to databases")




if __name__ == '__main__':
    App().cmdloop()