#!/bin/bash
set -e
cd "$(dirname "$0")"
mvn clean package
java -jar target/tradecheck-1.2.0.jar
