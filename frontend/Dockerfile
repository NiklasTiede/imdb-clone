# docker buildx build -t imdb-clone-frontend .
# docker run --name imdb-clone-frontend -p 3000:80 imdb-clone-frontend:latest
FROM node:19-alpine as build
WORKDIR /app
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile
COPY . .
RUN yarn build

# Use an official lightweight Nginx image
FROM nginx:1.23-alpine
# Copy the built app to the Nginx container
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
