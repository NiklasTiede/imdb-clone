# Use an official lightweight Node.js image
FROM node:14-alpine as build

# Set the working directory to /app
WORKDIR /app

# Copy the package.json and yarn.lock files to the container
COPY package.json yarn.lock ./

# Install dependencies
RUN yarn install --frozen-lockfile

# Copy the entire app to the container
COPY . .

# Build the app
RUN yarn build

# Use an official lightweight Nginx image
FROM nginx:1.21-alpine

# Copy the built app to the Nginx container
COPY --from=build /app/build /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
