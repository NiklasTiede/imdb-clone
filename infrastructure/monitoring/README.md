
# Observability: Metrics and Logs

Data about internals of our system help us to troubleshoot problems and act 
preemptive. Metrics about server resources, the containerized application and provided 
infrastructure (database, search engine, cache etc.) can, combined with alerting, help 
us act early and fast to keep our system operational.

## Metrics: Prometheus / Grafana

Prometheus stores metrics on a pull-based mechanism in a time-series database and provides a nice query
language (PromQL) to search for these metrics and display them with Grafana. Spring boot serves these 
metrics via actuator resources. Other technologies need dedicated exporters to export metrics to Prometheus.

For deploying Prometheus and Grafana we need to pull the images, copy the 
[docker-compose-metrics.yaml](./metrics/docker-compose-metrics.yaml) and [prometheus.yaml](./metrics/prometheus.yaml)
into the folder where we run the `docker-compose up` command.

```bash
docker pull prom/prometheus
docker pull grafana/grafana

docker-compose -f docker-compose-metrics.yaml up -d
```

Prometheus should be reachable on port 9500 and Grafana on port 3000. The docker-compose and
prometheus file contains also different exporters for all the other services. You can also
find the json files of the dashboards in the [grafana-dashboards](./metrics/grafana-dashboards) 
folder.

- image of some grafana dashboard

I have also port forwarded the Grafana instance, and you have read access to the dashboards
showing metrics about the servers resources, the spring boots backend and other infrastructure of 
this project  (see [imdb-clone-metrics.the-coding-lab.com](https://imdb-clone-metrics.the-coding-lab.com))

---

## Logging: Elasticsearch / Logstash Filebeat / Kibana (ELK Stack)

-[ ] in progress

For logging purposes there are different solutions like Graylog, Loki and more. But because I'm already 
using Elasticsearch for movie-search I can also use it for logging purposes. Logstash will transform 
log data and Kibana will provide a nice interface for searching across these logs.


```bash
docker pull logstash
docker pull kibana

docker-compose -f docker-compose-logging.yaml up -d
```

