
# Movie Database Clone

This webapp persists Movie data and presents them the user by searching.

## Techstack
- Java jdk17 / Typescript
- spring boot / React
- MySQL DB

## How to run this project

To set up the project we have to run the database and run 

```bash
❯ docker pull 


```

# short-term goals
- [ ] add MySQL DB, connect, fill with data
- [ ] play with docker/docker-compose
- [ ] run elasticsearch, index data, query and aggregate
- [ ] more complex data, also querying, aggregation


# Todo
- crud app
- well written exceptions
- permissions for different roles (admin, users)
- one-to-many relationships (entity-models)
- infra: mongo db/mySQL, sql scripts for creating tables and 
- inserting data (maybe python script for data-population)
- running DB locally, but also add docker image for db/backend/frontend (conn to DBeaver)
- create logic-layer (controller, logic, entity)
- generate swagger UI, generate typescript client
- connect JMS
- add logs, prometheus, grafana, graylog
- add timestamps
- validation, regex patterns
- build library on my own and use them on my project
- writing different tests (end-to-end etc.)
- sending emails to confirm registration
- how to handle pictures, pdfs data etc.
- how to work with DTO's
- using elasticsearch
- let run on server/url and use jenkins pipeline to build project
- react/redux/rematch frontend
- migrate from H2 to PSQL or MySQL
- password encryption, api keys
- builder pattern: querybuilder, logging,
- JPA, custom SQL queries

# Endpoints

### GET
single user - PathVariable
- by ID
- by username/email

list of similar users, paginated/all - RequestParam
- by username - 
- by email

### POST - requestbody
- register new user
- add list of users

### PUT
- update user (profile changes)

### PATCH
- update parts of the user (worth t use with big entities)

### DELETE
- delete user by ID
- delete by username/email
