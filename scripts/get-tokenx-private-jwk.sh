#!/bin/bash

NAMESPACE="teammelosys"

# Henter nyeste secret som starter med tokenx-melosys-skjema-api-
SECRET_NAME=$(kubectl get secrets --context=dev-gcp -n "$NAMESPACE" -o json | jq -r '.items[] | select(.metadata.name | startswith("tokenx-melosys-skjema-api-")) | [.metadata.creationTimestamp, .metadata.name] | @tsv' | sort -r | head -1 | cut -f2)

if [ -z "$SECRET_NAME" ]; then
    exit 1
fi

kubectl get secret --context=dev-gcp -n "$NAMESPACE" "$SECRET_NAME" -ojsonpath='{.data.TOKEN_X_PRIVATE_JWK}' | /usr/bin/base64 -d
