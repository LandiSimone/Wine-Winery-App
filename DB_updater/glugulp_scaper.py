import requests
from bs4 import BeautifulSoup
import pandas as pd

def extract_data_from_links():
    link_path = 'data/glugulp_link.csv'
    output_file = 'data/glugulp.json'

    df = pd.read_csv(link_path)
    df_bottle=pd.DataFrame()
    urls = df['Link']

    for url in urls:
        print("Find info for: ", url)
        response = requests.get(url)
        soup = BeautifulSoup(response.content, 'html.parser')

        try:
            # Extracting data
            nome = soup.select_one('.product-tabs__contents dt.is-active + dd.is-active h1').get_text().strip().replace("\n", "").replace("Champagne ", "")

            D_info = soup.select_one('.product-tabs__contents dt.is-active + dd.is-active').get_text().strip().split('\n')
            cleaned_lines = [line.strip() for line in D_info[2:] if line.strip() != '']
            info = ' '.join(cleaned_lines)

            D_descrizione_produttore = soup.select_one('.product-tabs__contents dt:-soup-contains("Produttori") + dd').get_text().strip().split('\n')[1:]
            cleaned_lines =  '\n'.join(D_descrizione_produttore)
            descrizione_produttore = cleaned_lines.replace("\n", " ")

            id_glugulp = soup.select_one('dt:-soup-contains("Codice Prodotto") + dd').get_text().strip()
            area_di_produzione = soup.select_one('dt:-soup-contains("Area di Produzione") + dd a').get_text().strip()
            uvaggio = soup.select_one('dt:-soup-contains("Uvaggio") + dd').get_text().strip()
            dosaggio = soup.select_one('dt:-soup-contains("Dosaggio") + dd a').get_text().strip()
            produttore = soup.select_one('.product-detail__content header hgroup h1').get_text().strip()
            price = soup.select_one('span.price').get_text().split()[1]
            price_double = float(price.replace('$', '').replace(',', ''))
        except AttributeError:
            pass

        try:
            sboccatura = soup.select_one('dt:-soup-contains("Annata") + dd').get_text().strip()
        except:
            try:
                sboccatura = soup.select_one('dt:-soup-contains("Sboccatura") + dd').get_text().strip().split("/")[1]
            except:
                sboccatura = None

        if sboccatura is None:
            sboccatura = 0

        data = {
            "glugulp_id": id_glugulp,
            "name": nome,
            "winemaker": produttore,
            "country": area_di_produzione,
            "varietal": dosaggio,
            "grapes": uvaggio,
            "year": sboccatura,
            "price": price_double,
            "info": info,
            "description": descrizione_produttore
        }
        df_bottle = df_bottle._append(data, ignore_index=True)
        df_bottle.to_json(output_file, orient='records', lines=True)


    df = pd.read_json(output_file, orient='records', lines=True)

    # remove duplicates
    df = df.drop_duplicates(subset=["glugulp_id"], keep="first")

    # save the DataFrame without duplicates in a new JSON file
    df.to_json(output_file, orient='records', lines=True)
    print("Tutti i dati sono stati salvati sul file glugulp_data.json")

def extract_links():
    base_url = 'https://www.glugulp.com/ecommerce-champagne-online?path=134_134&page={}'
    output_file = 'data/glugulp_link.csv'
    links = []

    for page_number in range(1, 101):  # all the wines on the website
        url = base_url.format(page_number)
        response = requests.get(url)
        soup = BeautifulSoup(response.content, 'html.parser')

        # Find all <article> elements with class "product-preview js-productPreview"
        product_previews = soup.find_all('article', class_='product-preview js-productPreview')

        # Extract href attributes of links inside each <article> element
        for product_preview in product_previews:
            link_elements = product_preview.find_all('a', href=True)
            for link_element in link_elements:
                link = link_element['href']
                if link != "https://www.glugulp.com/cart":  # Ignore the link https://www.glugulp.com/cart
                    print(link)
                    links.append(link)

    df = pd.DataFrame({'Link': links})
    df.to_csv(output_file, index=False)

    print("link salvati sul file glugulp_link.csv")

def main():
    while True:
        print("\nMenu:")
        print("1. Extract links")
        print("2. Extract data from links")
        print("3. Exit")
        cmd = input("Enter your choice: ")

        if cmd == "1":
            extract_links()
        elif cmd == "2":
            extract_data_from_links()
        elif cmd == "3":
            break
        else:
            print("Invalid command.")


def get_data():
    extract_links()
    extract_data_from_links()



if __name__ == '__main__':
    #main()
    get_data()

