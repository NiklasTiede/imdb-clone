# docker buildx build -t imdb-clone-frontend .
# docker run --name imdb-clone-frontend -p 3000:80 --restart=always -d imdb-clone-frontend:latest

# Stage 1: Install dependencies and build the application
FROM node:20-alpine AS build
WORKDIR /app
# Install Java for openapi-generator-cli
RUN apk add --no-cache openjdk11-jre
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile \
    && yarn cache clean
COPY . .
COPY .env.production .env
RUN yarn run build:moviesGen \
    && yarn build

# Stage 2: Set up the Nginx server to serve the application
FROM nginx:1.25-alpine
# Copy the built app to the Nginx container
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
