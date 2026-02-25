#!/bin/bash

PROJECT_ROOT="/opt/Projects/JettoCode"
WORKSPACE="${JETTO_CODE_WORKSPACE:-$PROJECT_ROOT/workspace}"

echo "========================================="
echo "Starting JettoCode Services"
echo "========================================="
echo "Project root: $PROJECT_ROOT"
echo "Workspace: $WORKSPACE"
echo "========================================="

export JETTO_CODE_WORKSPACE="$WORKSPACE"

mkdir -p "$WORKSPACE"
mkdir -p "$WORKSPACE/repos"
mkdir -p "$WORKSPACE/logs"

cd "$PROJECT_ROOT/code"

JAR_FILE=$(ls target/JettoCode-*-SNAPSHOT.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "JAR file not found. Building code service..."
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo "Failed to build code service"
        exit 1
    fi
    
    JAR_FILE=$(ls target/JettoCode-*-SNAPSHOT.jar 2>/dev/null | head -n 1)
    if [ -z "$JAR_FILE" ]; then
        echo "Failed to find JAR file after build"
        exit 1
    fi
fi

echo "Found JAR file: $JAR_FILE"

cd "$PROJECT_ROOT/frontend"

if [ ! -f "dist/index.html" ]; then
    echo "Building frontend..."
    npm run build
    
    if [ $? -ne 0 ]; then
        echo "Failed to build frontend"
        exit 1
    fi
fi

echo "========================================="
echo "JettoCode Backend Starting"
echo "========================================="
echo "Backend: http://localhost:9990"
echo "API Docs: http://localhost:9990/swagger-ui.html"
echo "Workspace: $WORKSPACE"
echo "Press Ctrl+C to stop"
echo "========================================="

cd "$PROJECT_ROOT/code"
exec java -jar "$JAR_FILE"
