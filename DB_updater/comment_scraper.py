import random
from datetime import datetime

import requests
import pandas as pd
import json

# Constants
BASE_URL = "https://www.vivino.com/api"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0"
}


def get_wine_data(wine_id, year, page):
    """Fetch wine data."""
    url = f"{BASE_URL}/wines/{wine_id}/reviews?per_page=50&year={year}&page={page}"
    print(url)
    return requests.get(url, headers=HEADERS).json()


def extract_reviews_from_data(data):
    """Extract reviews from the fetched data."""
    reviews = data['reviews']
    df = pd.DataFrame(reviews)
    df['user_id'] = df['user'].apply(lambda x: x['id'])
    df['user_alias'] = df['user'].apply(lambda x: x['alias'])
    df['wine_name'] = df['vintage'].apply(lambda x: x['wine']['name'])
    df['winery_name'] = df['vintage'].apply(lambda x: x['wine']['winery']['name'])
    df.drop(columns=['user', 'vintage'], inplace=True)
    return df[['id', 'rating', 'note', 'user_id', 'user_alias', 'wine_name', 'winery_name']]


def run_data_collection():
    """Main data collection function."""

    data = pd.DataFrame(columns=["username", "vivino_id", "glugulp_id", "varietal", "timestamp", "text"])

    df = pd.read_json('data/vivino.json', orient='records', lines=True)

    selected_columns = df[['vivino_id', 'search_id', 'name', 'varietal', 'year']]

    # remove duplicates
    unique_records = selected_columns.drop_duplicates()

    # get a list of dictionaries from the unique data
    unique_records_list = unique_records.to_dict(orient='records')

    # print the resulting list
    #print(unique_records_list)

    for record in unique_records_list:
        page = 1
        while True:
            value = get_wine_data(record["search_id"], record["year"], page)
            if not value["reviews"]:
                break
            for review in value["reviews"]:
                #if review["language"] != "it":
                #    continue

                # remove empty comments
                if review["note"] == "":
                    continue
                if review["note"] == "\n":
                    continue
                if review["note"] is None:
                    continue

                # clean up comments by removing single quotes and periods
                review["note"].replace("'", "").replace(".", "")

                try:
                    extracted_info_list = {
                        'username': review["user"]["seo_name"],
                        'vivino_id': review["vintage"]["id"],
                        'varietal': review["vintage"]["wine"]["style"]["varietal_name"],
                        #'name': review["vintage"]["name"],
                        'timestamp': pd.to_datetime(review["created_at"]).strftime('%Y-%m-%d %H:%M:%S'),  # Formatta il timestamp
                        'text': review["note"]}
                    if (
                            #review["vintage"]["name"] != record["name"] or
                            review["vintage"]["id"] != record["vivino_id"]
                    ):
                        continue                                                            # skip
                except:
                    continue

                data = data._append(extracted_info_list, ignore_index=True)
                data.to_json('data/comment.json', orient='records', lines=True)
            page += 1
            if page == 2:
                break

def generate_random_timestamp():
    year = random.randint(2020, 2023)
    month = random.randint(1, 8)
    day = random.randint(1, 28)
    hour = random.randint(0, 23)
    minute = random.randint(0, 59)
    second = random.randint(0, 59)

    random_timestamp = datetime(year, month, day, hour, minute, second)

    return random_timestamp.strftime("%Y-%m-%d %H:%M:%S")

def comment_selection():
    data = pd.DataFrame(columns=["username", "vivino_id", "glugulp_id", "varietal", "timestamp", "text"])

    ### vivino

    user_df = pd.read_json('data/users.json', orient='records', lines=True)
    comment_df = pd.read_json('data/comment.json', orient='records', lines=True)

    selected_columns_user = user_df[['username']]
    selected_columns_comment = comment_df[['username', 'vivino_id', 'glugulp_id', 'varietal', 'timestamp', 'text']]

    # merge the two DataFrames using the "username" column as the key
    filtered_comment = pd.merge(selected_columns_comment, selected_columns_user, on='username', how='inner')

    # convert timestamp from milliseconds to datetime format
    filtered_comment['timestamp'] = pd.to_datetime(filtered_comment['timestamp'], unit='ms')

    # format the timestamp in the desired format as a string
    filtered_comment['timestamp'] = filtered_comment['timestamp'].dt.strftime('%Y-%m-%d %H:%M:%S')

    #filtered_comment.to_json('data/filtered_comment.json', orient='records', lines=True)

    ### glugulp

    user_df = pd.read_json('data/users.json', orient='records', lines=True)
    comment_df = pd.read_json('data/glugulp_comment.json', orient='records', lines=True)
    wines = pd.read_json('data/wines.json', orient='records', lines=True)

    wines_df = wines[wines['vivino_id'].isnull()]

    comments = []
    for index, row in wines_df.iterrows():
        num_comments = int(random.random() * 10)
        for i in range(0, num_comments):
            now = datetime.now()
            rand_user = user_df.sample()['username'].values[0]
            rand_comment = comment_df.sample()['comment'].values[0]
            random_timestamp = generate_random_timestamp()

            comment = {'username': rand_user,
                       'glugulp_id': row['glugulp_id'],
                       'vivino_id': None,
                       'varietal': row['varietal'],
                       'timestamp': random_timestamp,
                       'text': rand_comment}
            comments.append(comment)

    comment_glugulp_df = pd.DataFrame(comments)

    ### vivino + glugulp

    merged_df = pd.concat([filtered_comment, comment_glugulp_df])
    merged_df.to_json('data/filtered_comment.json', orient='records', lines=True)


def glugulp_comment():
    """Main data collection function."""
    params = {
        "country_codes[]": "it",
        "currency_code": "EUR",
        "grape_filter": "varietal",
        "min_rating": "1",
        "order_by": "price",
        "order": "asc",
        "page": 1,
        "price_range_max": "500",
        "price_range_min": "420",
        "wine_type_ids[]": "1"
    }
    response = requests.get(f"{BASE_URL}/explore/explore", params=params, headers=HEADERS)
    matches = response.json()["explore_vintage"]["matches"]
    results = [(t["vintage"]["wine"]["winery"]["name"], t["vintage"]["year"], t["vintage"]["wine"]["id"],
               f'{t["vintage"]["wine"]["name"]} {t["vintage"]["year"]}', t["vintage"]["statistics"]["ratings_average"],
               t["vintage"]["statistics"]["ratings_count"]) for t in matches]
    dataframe = pd.DataFrame(results, columns=["Winery", "Year", "Wine ID", "Wine", "Rating", "num_review"])

    ratings = []
    for _, row in dataframe.iterrows():
        page = 1
        while True:
            data = get_wine_data(row["Wine ID"], row["Year"], page)
            if not data["reviews"]:
                break
            for review in data["reviews"]:
                if review["language"] != "it":
                    continue
                ratings.append([row["Year"], row["Wine ID"], review["rating"], review["note"], review["created_at"]])
            page += 1
            if page == 2:
                break

    # extract comments (notes) from reviews and remove empty ones
    comments = [review[3] for review in ratings if review[3].strip() != ""]

    # clean up comments by removing single quotes and periods
    cleaned_comments = [comment.replace("'", "").replace(".", "") for comment in comments]

    keywords = [
        "Sassicaia", "Brunello", "Chianti", "Montalcino", "Riserva", "Vino",
        "Cabernet", "Merlot", "Chardonnay", "Nebbiolo", "Barolo"
    ]
    filtered_comments = [comment for comment in cleaned_comments if
                         not any(keyword.lower() in comment.lower() for keyword in keywords)]

    final = [{"comment": comment} for comment in filtered_comments]

    dfinale = pd.DataFrame(final)
    dfinale.to_json('data/glugulp_comment.json', orient='records', lines=True)

    #print("Commenti puliti salvati in cleaned_comments.json")


if __name__ == "__main__":
    #glugulp_comment()
    #run_data_collection()
    comment_selection()
