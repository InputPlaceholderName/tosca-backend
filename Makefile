all:
	docker-compose build
	docker-compose up

dev:
	@printf "Running everything except the Ktor server\n"
	docker-compose up tosca-database swagger-ui

test:
	./gradlew test

lint:
	./util/ktlint

lint-fix:
	./util/ktlint -F
