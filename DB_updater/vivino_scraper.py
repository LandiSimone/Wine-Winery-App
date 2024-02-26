import pandas as pd

from Script.utils.requester import Requester
from Script.utils.constants import RECORDS_PER_PAGE, BASE_URL


PRICE_STEPS = 2
MAX_REQUESTS = 200
OUTPUT_FILE='data/vivino.json'


def get_data():
    data = pd.DataFrame(columns=('vivino_id', 'search_id', 'name', 'winemaker', 'country', 'varietal',
                                 'grapes', 'year', 'price', 'info', 'description'))

    start_page = 1

    # Instantiates a wrapper over the `requests` package
    r = Requester(BASE_URL)
    count = 0

    for request_num in range(1, MAX_REQUESTS + 1):
        # Calculate the price range for this loop iteration
        price_min = (request_num - 1) * PRICE_STEPS + 1
        price_max = request_num * PRICE_STEPS

        # Defines the payload, i.e., filters to be used on the search
        payload = {
            # "country_codes[]": "ge",
            # "food_ids[]": 20,
            # "grape_ids[]": 3,
            # "grape_filter": "varietal",
            "min_rating": 1.0,
            # "order_by": "ratings_average",
            # "order": "desc",
            "price_range_min": price_min,
            "price_range_max": price_max,
            # "region_ids[]": 383,
            # "wine_style_ids[]": 98,
            # "wine_type_ids[]": 1,
            # "wine_type_ids[]": 2,
            # "wine_type_ids[]": 3,
            # "wine_type_ids[]": 4,
            # "wine_type_ids[]": 7,
            # "wine_type_ids[]": 24,
        }

        # Performs an initial request to get the number of wine
        res = r.get('explore/explore?', params=payload)

        n_matches = res.json()['explore_vintage']['records_matched']
        print(f'Number of matches: {n_matches}')

        # Iterates through pages
        for i in range(start_page, max(1, int(n_matches / RECORDS_PER_PAGE)) + 1):

            if i == 81:     #max page
                break

            payload['page'] = i
            print(f'Page: {payload["page"]}','\t','# item: ',count ,'\t','iteration: ', i )

            # Performs the request and scraps the URLs
            res = r.get('explore/explore', params=payload)
            print(res.url)

            try:
                matches = res.json()['explore_vintage']['matches']
            except:                                                 #null result
                continue

            # Iterates over every bottle of wine
            for wine in matches:
                # skip wine with null description
                try:
                    null_description = wine["vintage"]["wine"]["style"].get("description", " ")
                except:
                    null_description = None

                if null_description is None:
                    print("Skip :", wine["vintage"]["name"])
                    # pass this wine
                    continue

                print("Find :", wine["vintage"]["name"], "with Tannico Id = ", wine["vintage"]["wine"]["id"])

                # wine info, combined into a single string
                info_list = wine["vintage"]["wine"]["style"].get("interesting_facts", [])
                combined_info = "\n".join(info_list)

                # grapes info, combined into a single string
                grapes_list = wine["vintage"]["wine"]["style"].get("grapes", [])
                combined_grapes = ", ".join([grape["name"] for grape in grapes_list])

                # adjusted year
                if wine["vintage"]["name"].split()[-1].isdigit() == False:  # l'anno non è un numero
                    adjusted_year = 0
                else:
                    adjusted_year = wine["vintage"]["name"].split()[-1]


                try:
                    extracted_info_list = {
                        'vivino_id': wine["vintage"]["id"],
                        'search_id': wine["vintage"]["wine"]["id"],
                        'name': wine["vintage"]["name"],
                        'winemaker': wine["vintage"]["wine"]["winery"]["name"],
                        'country': wine["vintage"]["wine"]["region"]["country"]["name"],
                        'varietal': wine["vintage"]["wine"]["style"]["varietal_name"],
                        'grapes': combined_grapes,
                        'year': int(adjusted_year),
                        'price': wine["price"]["amount"],
                        'info': combined_info,
                        'description': wine["vintage"]["wine"]["style"]["description"]
                    }
                except :
                    continue

                data = data._append(extracted_info_list, ignore_index=True)
                count += 1

            print('Save in vivino.json file')
            data.to_json(OUTPUT_FILE, orient='records', lines=True)

    df = pd.read_json(OUTPUT_FILE, orient='records', lines=True)

    # remove duplicates
    df = df.drop_duplicates(subset=["vivino_id"], keep="first")

    # save the DataFrame without duplicates
    df.to_json(OUTPUT_FILE, orient='records', lines=True)

if __name__ == '__main__':
    get_data()

