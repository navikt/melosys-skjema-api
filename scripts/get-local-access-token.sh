#!/bin/bash
# get-local-access-token.sh

MOCK_SERVER_URL="http://localhost:8082"
ISSUER_ID="isso"
PID=${1:-"12345678910"}

echo "🔐 Generating MockOAuth2 token..."

# Try the debugger endpoint with URL encoded form data
RESPONSE=$(curl -s -X POST "$MOCK_SERVER_URL/$ISSUER_ID/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=some-consumer" \
  -d "client_secret=secret" \
  -d "audience=melosys-skjema-api" \
  -d "pid=$PID" \
  -d "expiry=3600")

echo "🔍 Full response:"
echo "$RESPONSE"

# Extract access_token from response
TOKEN=$(echo "$RESPONSE" | jq -r '.access_token')

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    # Copy to clipboard (cross-platform)
    if command -v pbcopy >/dev/null 2>&1; then
        # macOS
        echo "$TOKEN" | pbcopy
        echo "✅ Token copied to clipboard (macOS)!"
    elif command -v xclip >/dev/null 2>&1; then
        # Linux
        echo "$TOKEN" | xclip -selection clipboard
        echo "✅ Token copied to clipboard (Linux)!"
    elif command -v clip >/dev/null 2>&1; then
        # Windows (WSL/Git Bash)
        echo "$TOKEN" | clip
        echo "✅ Token copied to clipboard (Windows)!"
    else
        echo "📋 Clipboard not available. Here's your token:"
        echo "$TOKEN"
    fi
    
    echo "🕐 Token expires in 1 hour"
else
    echo "❌ Failed to generate token"
    exit 1
fi