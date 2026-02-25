#!/bin/bash

echo "Stopping JettoCode Services..."

PID=$(pgrep -f "JettoCode.*SNAPSHOT.jar")

if [ -n "$PID" ]; then
    echo "Found code process (PID: $PID), stopping..."
    kill $PID
    
    sleep 2
    
    if ps -p $PID > /dev/null 2>&1; then
        echo "Process still running, force killing..."
        kill -9 $PID
    fi
    
    echo "Backend stopped"
else
    echo "    echo "No code process found""
fi

echo "Done."
