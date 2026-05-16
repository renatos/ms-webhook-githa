#!/bin/bash

# Function to kill child processes on exit
cleanup() {
    echo "Shutting down servers..."
    kill $(jobs -p)
    exit
}

# Trap SIGINT (Ctrl+C) and call cleanup
trap cleanup SIGINT

# Ensure Podman Socket is recognized by Docker Compose plugin
export DOCKER_HOST="unix:///run/user/$(id -u)/podman/podman.sock"

# Start database if not running
if [ ! "$(podman ps -q -f name=githa-db)" ]; then
    if [ "$(podman ps -aq -f status=exited -f name=githa-db)" ]; then
        echo "Starting existing githa-db container..."
        podman start githa-db
    else
        echo "Creating and starting githa-db container..."
        podman compose up -d postgres
    fi
else
    echo "githa-db container is already running."
fi
sleep 2

# Export variables from .env for local Quarkus development
if [ -f .env ]; then
    echo "Loading environment variables from .env..."
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
fi

# Ensure JWT_SECRET is set
if [ -z "$JWT_SECRET" ]; then
    echo "JWT_SECRET not found in .env, generating a temporary one..."
    export JWT_SECRET=$(openssl rand -base64 32)
fi

# Generate secret.jwk matching CI logic
echo "Generating src/main/resources/secret.jwk from JWT_SECRET..."

# Check if using the default raw text secret (which is NOT Base64)
if [[ "$JWT_SECRET" == "githa-secret-key-change-this-in-production-please-use-env-var" ]]; then
    SECRET_BASE64URL=$(echo -n "$JWT_SECRET" | base64 | tr '+/' '-_' | tr -d '=')
else
    # Assume it's already a valid Base64 string (like CI secrets)
    SECRET_BASE64URL=$(echo -n "$JWT_SECRET" | tr '+/' '-_' | tr -d '=')
fi

mkdir -p src/main/resources
cat > src/main/resources/secret.jwk << EOF
{
  "keys": [
    {
      "kty": "oct",
      "k": "${SECRET_BASE64URL}",
      "alg": "HS256",
      "kid": "default-key"
    }
  ]
}
EOF

# Run on port 8085 to avoid conflict with main backend
./gradlew quarkusDev \
  -Dquarkus.http.port=8085 \
  -DDB_URL="${DB_URL}" \
  -DDB_USER="${DB_USER}" \
  -DDB_PASSWORD="${DB_PASSWORD}"
