#!/bin/bash
set -e


for X in "$@"
do
  docker-compose run --rm --service-ports biosamples-integration java -jar integration-4.0.5-SNAPSHOT.jar --phase=$X $ARGS $@
  docker-compose run --rm --service-ports biosamples-agents-solr java -jar agents-solr-4.0.5-SNAPSHOT.jar --biosamples.agent.solr.stayalive=false --biosamples.agent.solr.queuetime=500
done

echo "Successfully completed"

