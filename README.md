# Tosca Backend
The repository contains the backend for Tosca.

## Setup
Copy the `.env.example` to `.env` and modify if needed.

## IntelliJ setup

The main application depends on three environment variables. 
Make sure that `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `POSTGRES_HOST` is set in
your configuration.

## Running
Start the backend with:
```
make
```

To start everything except the Ktor server, useful for development, there is also the command:
```
make dev
```

