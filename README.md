# Laplace
Laplace is a Discord bot which collects statistics about a server and its users.

## Getting started

### Installation with Docker

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
docker-compose up influxdb
```
- Adjust the following properties in [influx2.properties](src/main/resources/influx2.properties) to match your InfluxDB instance.
```
influx2.org=YOUR_ORG
influx2.bucket=YOUR_BUCKET
influx2.token=YOUR_TOKEN
```
- Create `discord_token.txt` inside the project folder containing *just* the token.
```text
imaginary_cute_discord_token_inside_a_text_file
```
- You're done! You can now start Laplace.
```shell script
docker-compose up
```

### Installation without Docker

#### Prerequisites
- [Git](https://git-scm.com)
- [JDK 11 or higher](https://www.oracle.com/java/technologies/javase-downloads.html)
- [InfluxDB v2.0 (beta)](https://v2.docs.influxdata.com/v2.0/get-started/)

#### Installation
- Clone the repository.  
```shell script
git clone https://github.com/Shirleh/Laplace.git
```
- Run an instance of InfluxDB and set it up.
- Adjust the following properties in `src/main/resources/influx2.properties` to match your InfluxDB instance.
```
influx2.org=YOUR_ORG
influx2.bucket=YOUR_BUCKET
influx2.token=YOUR_TOKEN
```
- Create `discord_token.txt` inside the project folder containing *just* the token.
```text
imaginary_cute_discord_token_inside_a_text_file
```
- Run the bot with Gradle.
```shell script
./gradlew run --console plain
```

## Contributing
You can find more information about contributing to Laplace [here](CONTRIBUTING.md).

## License
[AGPLv3](LICENSE)
