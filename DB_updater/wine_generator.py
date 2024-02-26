import pandas as pd
# import vivino_scraper
# import glugulp_scaper


def merge_data(path_vivino, path_glugulp):
    """
    merge_data takes as first argument the path of vivino data and as second argument the path of glugulp data
    returns the ordered and concatenated data
    """
    print("Merging ...")
    data_merge = pd.DataFrame(columns=('vivino_id', 'glugulp_id', 'name', 'winemaker', 'country',
                                       'varietal', 'grapes', 'year', 'price', 'info', 'description'))

    data_vivino = pd.read_json(path_vivino, lines=True)
    data_vivino = data_vivino.drop(columns=["search_id"])

    data_glugulp = pd.read_json(path_glugulp, lines=True)

    data_merge = data_merge._append(data_vivino)
    data_merge = data_merge._append(data_glugulp)

    data_merge.sort_values(by=['price'], inplace=True)

    return data_merge


def import_data():
    print("Importing wines data  ...")
    # vivino_scraper.get_data()
    # glugulp_scaper.get_data()

    path_vivino = "data/vivino.json"
    path_glugulp = "data/glugulp.json"

    data = pd.DataFrame()
    data = data._append(merge_data(path_vivino, path_glugulp))

    path = "./data/wines.json"
    data.to_json(path, orient='records', lines=True)

    df = pd.read_json(path, orient='records', lines=True)

    # remove duplicates
    df = df.drop_duplicates(subset=["name"], keep="first")

    # save the DataFrame without duplicates
    df.to_json(path, orient='records', lines=True)
    print("Saved merged Json file for wines")

    return path


if __name__ == "__main__":
    import_data()
