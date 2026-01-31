#!/bin/bash
# =============================================================================
# Generate self-signed certificates for development TLS/HTTPS
# =============================================================================

CERT_DIR="$(dirname "$0")/../src/main/resources/certs"

mkdir -p "$CERT_DIR"

echo "Generating self-signed certificates in $CERT_DIR..."

# Generate private key
openssl genrsa -out "$CERT_DIR/server.key" 2048

# Generate self-signed certificate
openssl req -new -x509 -key "$CERT_DIR/server.key" -out "$CERT_DIR/server.crt" -days 365 \
    -subj "/CN=localhost/O=EDIOS Development/C=US" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

echo "Certificates generated successfully:"
echo "  - $CERT_DIR/server.key"
echo "  - $CERT_DIR/server.crt"
echo ""
echo "Note: These are self-signed certificates for development only."
echo "For production, use properly signed certificates from a CA."
