#!/bin/bash

set -e
set -o pipefail

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

AWS_REGION=${AWS_REGION}
S3_BUCKET=${S3_BUCKET}
FUNCTION_NAME=ApiGatewayWebSocketHandler
ZIP_FILE_NAME=websocket-connection-handler.zip
S3_KEY=lambdas/${ZIP_FILE_NAME}

rm -rf dist

echo "Transpile TypeScript"
npm install
npx tsc

echo "Build lambda codebase"
mkdir -p dist
cp package.json package-lock.json dist
mv src/*.js dist
cd dist && npm install --omit=dev

echo "Zip into package"
rm -f package.json package-lock.json
zip -r $ZIP_FILE_NAME *

echo "Upload package to S3"
aws s3 cp $ZIP_FILE_NAME s3://$S3_BUCKET/$S3_KEY --region $AWS_REGION

echo "Update lambda function"
if aws lambda get-function --function-name $FUNCTION_NAME > /dev/null 2>&1; then
    aws lambda update-function-code --function-name $FUNCTION_NAME --s3-bucket $S3_BUCKET --s3-key $S3_KEY || {
        echo "Failed to update lambda function code."
        exit 1
    }
else
    echo "Function does not exist yet, skipping update."
fi

echo "Deployment completed."
