# using the image url to download the image

import csv
import os
import requests

# Define the base URL for poster images
BASE_POSTER_URL = 'https://image.tmdb.org/t/p/original/'

# Open the CSV file for reading
with open('most_popular_movies_6000_full_data.csv', 'r', newline='', encoding='utf-8') as csvfile:
    reader = csv.DictReader(csvfile)

    # Loop over each row in the CSV file
    for row in reader:
        # Get the ID and poster path for this row
        id = row['id']
        url_token = row['url_token']
        poster_path = row['poster_path']

        # Check if the poster path is missing
        if not poster_path:
            continue

        # Construct the full URL for the poster image
        poster_url = BASE_POSTER_URL + poster_path

        # Make a GET request to the poster URL
        response = requests.get(poster_url)

        # Save the poster image to a file
        if response.status_code == 200:
            # Create the output directory if it doesn't already exist
            if not os.path.exists('original_movie_images'):
                os.makedirs('original_movie_images')

            # Save the image to a file with the ID as the filename
            with open(f'original_movie_images/{url_token}_original.jpg', 'wb') as f:
                f.write(response.content)
                print(f"movie with movieId {id} and url_token {url_token} downloaded.")
