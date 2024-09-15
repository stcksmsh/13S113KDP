#!/bin/bash
if [ -z "$HOST" ]; then
  java -jar "$JAR_FILE" "$LOG_FILE" "$PORT"
else
  java -jar "$JAR_FILE" "$LOG_FILE" "$HOST" "$PORT"
fi
