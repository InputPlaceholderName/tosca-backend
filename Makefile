all:
	docker-compose build
	docker-compose up

dev:
	@printf "Running everything except the Ktor server\n"
	docker-compose run --service-ports tosca-database

test:
	./gradlew test

lint:
	./util/ktlint

lint-fix:
	./util/ktlint -F
