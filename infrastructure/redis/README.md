
Run a redis container locally. Further config is exemplified 
[here](https://redis.io/docs/stack/get-started/install/docker/).

```commandline
docker pull redis/redis-stack-server
docker run -d --name redis-stack --restart=always -p 6379:6379 redis/redis-stack-server:latest
```

connect with the container.

```properties
spring.redis.url=http://localhost:6379
```

```http request
POST http://localhost:8080/api/auth/registration
Content-Type: application/json

{
  "username": "onemanarmy",
  "password": "Str0nG!Pa55Word?",
  "email": "your@email.com"
}
```

---

When to use redis caching?
- bla
- bla
