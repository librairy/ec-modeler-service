#!/usr/bin/env bash
docker run -it --rm --name datos-gob-model -p 8080:7777 -e "NLP_HOST=172.17.0.2" librairy/datos-gob-model:latest