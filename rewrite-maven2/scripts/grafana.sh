#!/bin/sh
docker run -i -p 3000:3000 \
-v "$(pwd)"/grafana-datasource.yml:/etc/grafana/provisioning/datasources/grafana-datasource.yml \
-v $(pwd)/grafana-dashboard.yml:/etc/grafana/provisioning/dashboards/grafana-dashboard.yml \
-v $(pwd)/grafana-dashboard.json:/etc/grafana/dashboards/grafana-dashboard.json \
grafana/grafana:7.3.1
