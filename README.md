# Laplace
Laplace is a Discord bot which collects statistics about a server and its users.

## Getting started

### Installation

#### Prerequisites
- [Git](https://git-scm.com)
- [Docker](https://docs.docker.com/engine/install/) and [Docker Compose](https://docs.docker.com/compose/install/)

#### Installation
- Clone the repository.  
```shell script
git clone https://github.com/Shirleh/Laplace.git
```
- Start an instance of InfluxDB and [set it up](https://v2.docs.influxdata.com/v2.0/get-started/#set-up-influxdb).
```shell script
docker-compose up -d influxdb
```
- Generate a read/write token in the InfluxDB UI and save it inside a file `influx2_token.txt`.
```shell script
echo "shy-influx2-token" > influx2_token.txt
```
- Optionally configure [docker-compose.prod.yml](docker-compose.prod.yml) to change default Influx properties.
- Copy the Discord token from the Discord Developer Portal and save it inside a file `discord_token.txt`.
```shell script
echo "promiscuous-discord-token" > discord_token.txt
```
- Put the id of the superuser role (e.g. staff role) in [docker-compose.prod.yml](docker-compose.prod.yml).
- You can now deploy Laplace! :rocket:
```shell script
docker-compose up -d
```

## Contributing
You can find more information about contributing to Laplace [here](CONTRIBUTING.md).

## License
[AGPLv3](LICENSE)
