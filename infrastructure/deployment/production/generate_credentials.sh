#!/bin/bash

# Generate a random password
generate_password() {
  length=$1
  openssl rand -base64 $((length * 3/4)) | tr -dc 'A-Za-z0-9!@#$%^&*()_-+=' | head -c $length
}

MYSQL_ROOT_PASSWORD_ENV_VAR=$(generate_password 16)
MYSQL_USERNAME_ENV_VAR=MySqlU5er
MYSQL_PASSWORD_ENV_VAR=$(generate_password 16)

MINIO_ACCESS_KEY_ENV_VAR=$(generate_password 12)
MINIO_SECRET_KEY_ENV_VAR=$(generate_password 16)

ELASTIC_USERNAME_ENV_VAR=elastic
ELASTIC_PASSWORD_ENV_VAR=$(generate_password 16)

MAIL_USERNAME_ENV_VAR=your-own-email@email.com
MAIL_PASSWORD_ENV_VAR=your-own-password

# Output to .env
cat > .env << EOL
# MySQL:
MYSQL_ROOT_PASSWORD_ENV_VAR=$MYSQL_ROOT_PASSWORD_ENV_VAR
MYSQL_USERNAME_ENV_VAR=$MYSQL_USERNAME_ENV_VAR
MYSQL_PASSWORD_ENV_VAR=$MYSQL_PASSWORD_ENV_VAR

# MinIO:
MINIO_ACCESS_KEY_ENV_VAR=$MINIO_ACCESS_KEY_ENV_VAR
MINIO_SECRET_KEY_ENV_VAR=$MINIO_SECRET_KEY_ENV_VAR

# ElasticSearch:
ELASTIC_USERNAME_ENV_VAR=$ELASTIC_USERNAME_ENV_VAR
ELASTIC_PASSWORD_ENV_VAR=$ELASTIC_PASSWORD_ENV_VAR

# Spring Boot Backend:
FRONTEND_APP_ADDRESS_ENV_VAR=http://robotniks-deployment-server:80
BACKEND_APP_ADDRESS_ENV_VAR=http://robotniks-deployment-server:8080

MAIL_USERNAME_ENV_VAR=$MAIL_USERNAME_ENV_VAR
MAIL_PASSWORD_ENV_VAR=$MAIL_PASSWORD_ENV_VAR
EOL

echo "Credentials saved to .env"
