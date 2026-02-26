#!/bin/bash

PROJECT_DIR="$(cd "$(dirname "$0")/code" && pwd)"
BACKEND_DIR="$PROJECT_DIR"

echo "=========================================="
echo "  JettoCode Build Script"
echo "=========================================="

cd "$BACKEND_DIR" || exit 1

echo ""
echo "[1/2] Running mvn clean..."
mvn clean -q
if [ $? -ne 0 ]; then
    echo "ERROR: mvn clean failed"
    exit 1
fi
echo "mvn clean completed"

echo ""
echo "[2/2] Running mvn package..."
mvn package -DskipTests -q
if [ $? -ne 0 ]; then
    echo "ERROR: mvn package failed"
    exit 1
fi
echo "mvn package completed"

echo ""
echo "=========================================="
echo "  Build Success!"
echo "  JAR: $BACKEND_DIR/target/JettoCode-1.0.0-SNAPSHOT.jar"
echo "=========================================="
