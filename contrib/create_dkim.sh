#!/bin/bash -eu

# Set the domain and selector
if [ $# < 2 ]
then
  echo "$0 <DOMAIN> <TACHIKOMA NODE NAME>"
  exit 1
fi
DOMAIN="$1"
SELECTOR="$2"

# Directory to store the DKIM keys
KEY_DIR="./domainkeys"
mkdir -p "$KEY_DIR"

OUTFILE_PREFIX="$KEY_DIR/$SELECTOR._domainkey.$DOMAIN"

# Generate the private key
openssl genrsa -out "$OUTFILE_PREFIX-private" 2048

# Generate the public key from the private key
openssl rsa -in "$OUTFILE_PREFIX-private" -pubout -out "$OUTFILE_PREFIX-public"

# Display the public key in DKIM TXT record format
echo "Your DKIM public key for DNS (TXT record):"
echo "------------------------------------------"
echo "$SELECTOR._domainkey.$DOMAIN IN TXT \"v=DKIM1; k=rsa; p=$(cat "$OUTFILE_PREFIX-public" | grep -v '^-----' |tr -d '\n' )\";"

# Set appropriate permissions for private key
chmod 600 "$OUTFILE_PREFIX-private"

echo
echo "DKIM keys have been generated and stored in the $KEY_DIR directory."

