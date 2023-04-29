# docker build -t custom-minio-mc .

# Set the base image
FROM bitnami/minio-client

# Update package index and install wget
USER root
RUN apt-get update && \
    apt-get install -y wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Switch back to the default user
USER 1001
