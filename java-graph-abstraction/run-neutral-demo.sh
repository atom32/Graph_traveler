#!/bin/bash

# Data-Neutral Graph Reasoning System Demo Runner

echo "=== Data-Neutral Graph Reasoning System Demo ==="

# Check if OPENAI_API_KEY is set
if [ -z "$OPENAI_API_KEY" ]; then
    echo "Warning: OPENAI_API_KEY environment variable is not set"
    echo "Please set it with: export OPENAI_API_KEY=your_api_key"
    echo ""
fi

# Check if Neo4j is running
echo "Checking Neo4j connection..."
if ! nc -z localhost 7687 2>/dev/null; then
    echo "Error: Cannot connect to Neo4j at localhost:7687"
    echo "Please start Neo4j with:"
    echo "  docker run -p 7474:7474 -p 7687:7687 -e NEO4J_AUTH=neo4j/password neo4j:latest"
    exit 1
fi

echo "Neo4j connection OK"

# Build the project
echo "Building project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Build successful"

# Run the data-neutral demo
echo "Starting data-neutral demo..."
mvn exec:java -Dexec.mainClass="com.tog.graph.demo.DataNeutralDemo" -q

echo "Demo completed"