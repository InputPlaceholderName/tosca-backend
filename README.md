# Tosca Backend
The repository contains the backend for Tosca.

## Setup
Copy the `.env.example` to `.env` and modify if needed.

The application has extra configuration options that can
be configured in a `HCON` file. See `./src/main/resources/application.conf` (loaded by default) for
a sample configuration. The application can be launched with a separate configuration
file with the `-config` parameter.

## IntelliJ setup

The main application depends on a couple of environment variables. 
Make sure that environment variables present in `.env` is loaded
or set in IntelliJ.

## Running
Start the backend with:
```
make
```

To start everything except the Ktor server, useful for development, there is also the command:
```
make dev
```

## OpenAPI / Swagger

The endpoints of the Tosca bakend are documented using `OpenAPI 3.1`, in [swagger/api.yaml](swagger/api.yaml).
A Swagger UI of the documented endpoints can be viewed on [http://localhost:8081](http://localhost:8081) after
running `docker-compose up` or `make dev`.
