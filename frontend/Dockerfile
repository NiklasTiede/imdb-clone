### STAGE 1: "build-stage", based on Node.js, to build and compile the frontend
FROM node:lts-alpine3.16 as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . ./
RUN npm run build

### STAGE 2: only the compiled app, ready for production with Nginx
FROM node:lts-alpine3.16
WORKDIR /app
RUN npm install -g serve
COPY --from=builder /app/build ./build
EXPOSE 3000
CMD ["serve", "-s", "build"]
