#!/usr/bin/env bash

# Make this file executable first: "chmod +x ESPlorer.sh", then double-click on it

cd $(dirname $(realpath $0))
java -jar ESPlorer.jar
