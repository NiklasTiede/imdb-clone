# using imdbId to get the image url and the overview text and generate custom urlToken

import csv
import requests
import time
import secrets
import string

# Define the URL for themoviedb.org API and your API key
url = "https://api.themoviedb.org/3/find/"
api_key = "your-api-key"

# Measure the start time
start_time = time.time()

# Open the input and output CSV files
with open("most_popular_movies_6000.csv", "r") as infile, open("most_popular_movies_6000_full_data.csv", "w", newline="") as outfile:
    reader = csv.DictReader(infile)
    fieldnames = reader.fieldnames + ["poster_path", "url_token", "overview"]
    writer = csv.DictWriter(outfile, fieldnames=fieldnames)
    writer.writeheader()

    # Iterate over the rows in the input CSV file
    for row in reader:
        imdb_id = row["imdbId"]
        retry_count = 0

        # Make a GET request to themoviedb.org API with the IMDB ID, retry up to 3 times if the call fails
        while retry_count < 3:
            response = requests.get(url + imdb_id, params={"api_key": api_key, "external_source": "imdb_id"})
            if response.status_code == 200:
                break
            else:
                retry_count += 1
                print(f"Failed, retry-count: {retry_count}")
                time.sleep(1) # wait for 1 second before retrying

        # Parse the JSON response and extract the movie overview and poster path
        if response.status_code == 200:
            data = response.json()
            if "movie_results" in data and len(data["movie_results"]) > 0:
                overview = data["movie_results"][0].get("overview", "")
                poster_path = data["movie_results"][0].get("poster_path", "")
                if poster_path:
                    alphabet = string.ascii_letters + string.digits
                    url_token = ''.join(secrets.choice(alphabet) for i in range(30))
                    print(url_token)
                    row["overview"] = overview
                    row["poster_path"] = poster_path
                    row["url_token"] = url_token

        # Write the updated row to the output CSV file
        writer.writerow(row)

# Measure the end time and calculate the elapsed time
end_time = time.time()
elapsed_time = end_time - start_time
print(f"Elapsed time: {elapsed_time:.2f} seconds")
