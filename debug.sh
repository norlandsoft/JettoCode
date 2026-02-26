#!/bin/bash

PROJECT_ROOT="/opt/Projects/JettoCode"
WORKSPACE="${JETTO_CODE_WORKSPACE:-$PROJECT_ROOT/workspace}"
JAR_NAME="JettoCode-1.0.0-SNAPSHOT.jar"

echo "========================================="
echo "JettoCode Debug Script"
echo "========================================="

echo ""
echo "[1/3] Stopping running JettoCode process..."
PID=$(ps aux | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "Found JettoCode process (PID: $PID), killing..."
    kill $PID
    sleep 2
    PID=$(ps aux | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
    if [ -n "$PID" ]; then
        echo "Process still running, force killing..."
        kill -9 $PID
    fi
    echo "Process stopped."
else
    echo "No running JettoCode process found."
fi

echo ""
echo "[2/3] Building backend service..."
cd "$PROJECT_ROOT/code" || exit 1

echo "Running mvn clean..."
mvn clean -q
if [ $? -ne 0 ]; then
    echo "ERROR: mvn clean failed"
    exit 1
fi

echo "Running mvn package..."
mvn package -DskipTests -q
if [ $? -ne 0 ]; then
    echo "ERROR: mvn package failed"
    exit 1
fi

echo "Build completed."

echo ""
echo "[3/3] Starting backend service in background..."
export JETTO_CODE_WORKSPACE="$WORKSPACE"
mkdir -p "$WORKSPACE"
mkdir -p "$WORKSPACE/repos"
mkdir -p "$WORKSPACE/logs"

JAR_FILE="$PROJECT_ROOT/code/target/$JAR_NAME"
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found: $JAR_FILE"
    exit 1
fi

LOG_FILE="$WORKSPACE/logs/backend.log"
nohup java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
BACKEND_PID=$!

echo "Backend started (PID: $BACKEND_PID)"
echo "Log file: $LOG_FILE"
echo ""
echo "========================================="
echo "Backend: http://localhost:9990"
echo "API Docs: http://localhost:9990/swagger-ui.html"
echo "========================================="
