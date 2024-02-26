import json
import random

import requests
import pandas as pd
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad, unpad
from base64 import b64encode, b64decode

# https://api-ninjas.com/api/randomuser
KEY = b"abcdefghijklmnop"  # 16 bytes key for AES-128


def encrypt(plain_text, key):
    cipher = AES.new(key, AES.MODE_ECB)
    encrypted = cipher.encrypt(pad(plain_text.encode('utf-8'), AES.block_size))
    return b64encode(encrypted).decode('utf-8')



def fetch_random_users():
    df = pd.read_json('data/comment.json', orient='records', lines=True)

    # selects only 'username' column
    selected_columns = df[['username']]

    # calculates the count of each username
    username_counts = selected_columns['username'].value_counts().reset_index()

    username_counts.columns = ['username', 'count']

    # sort by count in descending order
    sorted_username_counts = username_counts.sort_values(by='count', ascending=False)

    selected_columns = sorted_username_counts[['username']]

    # remove duplicates
    unique_records = selected_columns.drop_duplicates()

    # get a list of dictionaries from the unique data
    unique_records_list = unique_records.to_dict(orient='records')

    # maintain the first 5000 results
    unique_records_list = unique_records_list[:5000]

    # get number of users
    n = len(unique_records_list)
    print(n)
    url = "https://randomuser.me/api/"
    payloads = {"results": n}

    response = requests.get(url, params=payloads)
    if response.status_code != 200:
        raise ValueError("Non è stato possibile ottenere dati dagli utenti.")

    r = response.json()
    df = pd.DataFrame(columns=['username', 'email', 'password', 'firstname', 'lastname', 'age', 'location', 'type'])

    # choose a random location
    location = ["Stati Uniti d'America", "Cina", "Russia", "India", "Brasile",
                        "Germania", "Regno Unito", "Francia", "Italia", "Giappone",
                        "Canada", "Australia", "Sud Korea", "Arabia Saudita", "Sudafrica",
                        "Messico", "Indonesia", "Turchia", "Argentina", "Nigeria"]

    i = 0

    for result in r['results']:
        data = {
            'username': unique_records_list[i]['username'],
            'email': result['email'],
            'password': encrypt(result['login']['password'],KEY),
            'firstname': result['name']['first'],
            'lastname': result['name']['last'],
            'age': result['dob']['age'],
            'location': random.choice(location),
            'type': 0,  #  admin/moderator/user
        }

        df = df._append(data, ignore_index=True)
        i += 1

    #df = df.drop_duplicates(subset=["username"], keep="first")
    return df


def import_data():
    df = fetch_random_users()
    with open('data/users.json', 'w') as json_file:
        for _, row in df.iterrows():
            user_data = {
                "username": row['username'],
                "email": row['email'],
                "password": row['password'],
                "firstname": row['firstname'],
                "lastname": row['lastname'],
                "age": row['age'],
                "location": row['location'],
                "type": row['type']
            }
            json.dump(user_data, json_file)
            json_file.write('\n')  # sdds a new row between JSON objects

    # Print the first user
    #print(df.iloc[0])
    #print(key)


if __name__ == "__main__":
    import_data()

