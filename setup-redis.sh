#!/bin/bash

CONTAINER_NAME="payment-redis"
REDIS_PORT=6379
IMAGE="redis:7.2-alpine"

echo "Starting Redis provisioning..."

# 1. Check if the container is already running
if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
    echo "Redis is already running."
# 2. Check if it exists but is stopped
elif [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
    echo "Starting existing stopped container..."
    docker start $CONTAINER_NAME
else
    # 3. Run a fresh container
    echo "Pulling and starting fresh Redis container..."
    docker run -d \
        --name $CONTAINER_NAME \
        -p $REDIS_PORT:6379 \
        --restart unless-stopped \
        $IMAGE
fi

# 4. Wait for Redis to be ready (Healthcheck)
echo "Waiting for Redis to be ready on port $REDIS_PORT..."
MAX_RETRIES=10
COUNT=0

while ! docker exec $CONTAINER_NAME redis-cli ping | grep -q PONG; do
    if [ $COUNT -eq $MAX_RETRIES ]; then
        echo "❌ Error: Redis failed to start after $MAX_RETRIES attempts."
        exit 1
    fi
    echo "..."
    sleep 1
    ((COUNT++))
done

echo "Redis is ready to accept connections!"