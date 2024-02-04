ifneq (,$(wildcard ./.env))
include .env
export
endif

.PHONY: all clean transpile zip upload update

all: clean transpile zip upload update

clean:
	rm -rf dist

transpile:
	npm install
	npx tsc

zip:
	mkdir -p dist
	cp package.json package-lock.json dist
	mv src/*.js dist
	
	cd dist && npm install --production && \
	rm -f package.json package-lock.json && \
	zip -r $(ZIP_FILE_NAME) *

upload:
	aws s3 cp dist/$(ZIP_FILE_NAME) s3://$(S3_BUCKET)/$(S3_KEY) --region $(AWS_REGION)

update:
	@if aws lambda get-function --function-name $(FUNCTION_NAME) > /dev/null 2>&1; then \
		if aws lambda update-function-code --function-name $(FUNCTION_NAME) --s3-bucket $(S3_BUCKET) --s3-key $(S3_KEY); then \
			echo "Deployment successful"; \
		else \
			EXIT_CODE=$$?; \
			echo "Deployment failed with exit code $$EXIT_CODE"; \
			aws lambda update-function-code --function-name $(FUNCTION_NAME) --s3-bucket $(S3_BUCKET) --s3-key $(S3_KEY) 2>&1 | sed 's/^/Error message: /'; \
		fi \
	else \
		echo "Function does not exist yet, skipping update."; \
	fi
