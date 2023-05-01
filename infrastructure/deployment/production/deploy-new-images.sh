#!/bin/bash

# set up for first deployment
sudo sysctl -w vm.max_map_count=262144

# set up for next deployment
sudo docker-compose -f docker-compose.stateful-apps.yaml down
sudo docker-compose -f docker-compose.stateful-apps.yaml up -d

echo "Latest image of the imdb-clone was deployed!"
