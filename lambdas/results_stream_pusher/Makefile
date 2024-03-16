ifneq (,$(wildcard ./.env))
include .env
export
endif

.PHONY: deploy

deploy:
	./deploy.sh
