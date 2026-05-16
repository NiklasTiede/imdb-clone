from PIL import Image
import os

# Set the desired width and height of the output images
width = 600
height = 900
aspect_ratio = width / height

thumbnail_width = 120
thumbnail_height = 180

if not os.path.exists('processed_movie_images'):
    os.makedirs('processed_movie_images')

# Loop over all files in the input directory
for filename in os.listdir('./original_movie_images'):

    print(filename)

    # Check that the file is a JPEG image
    if filename.endswith('.jpg'):
        # Get the ID from the filename (assuming the filename is {id}_original.jpg)
        url_token = filename.split('_')[0]

        # Open the image file
        with Image.open(os.path.join('original_movie_images', filename)) as im:
            # Get the original dimensions of the image
            orig_width, orig_height = im.size
            orig_aspect_ratio = orig_width / orig_height

            # Determine whether to crop horizontally or vertically
            if orig_aspect_ratio > aspect_ratio:
                # Crop horizontally
                new_width = int(orig_height * aspect_ratio)
                left = (orig_width - new_width) // 2
                top = 0
                right = left + new_width
                bottom = orig_height
            else:
                # Crop vertically
                new_height = int(orig_width / aspect_ratio)
                left = 0
                top = (orig_height - new_height) // 2
                right = orig_width
                bottom = top + new_height

            # Crop the image
            im = im.crop((left, top, right, bottom))

            # Resize the image to the desired dimensions
            im = im.resize((width, height))

            # Save the resized image with the ID and dimensions in the filename
            output_filename = f'{url_token}_size_{width}x{height}.jpg'
            im.save(os.path.join('processed_movie_images', output_filename))

            # Create a thumbnail of the resized image
            im.thumbnail((thumbnail_width, thumbnail_height))
            thumbnail_filename = f'{url_token}_size_{thumbnail_width}x{thumbnail_height}.jpg'
            im.save(os.path.join('processed_movie_images', thumbnail_filename))
