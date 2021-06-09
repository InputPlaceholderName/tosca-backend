all:
	docker-compose build
	docker-compose up

dev:
	@printf "Running everything except the Ktor server\n"
	docker-compose run --service-ports tosca-database

