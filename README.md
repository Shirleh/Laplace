# Laplace
Laplace is a Discord bot which collects statistics about a server and its users.

## Getting started
### Prerequisites
- [Git](https://git-scm.com)
- [JDK 14](https://jdk.java.net/14/) (or any JDK 8+ version)
- [InfluxDB v2.0 (beta)](https://v2.docs.influxdata.com/v2.0/get-started/)

### Installation
- Clone the repository.  
```shell script
git clone https://github.com/Shirleh/Laplace.git
```
- Setup and run an instance of InfluxDB, e.g. with Docker
```shell script
docker run --name influxdb -p 9999:9999 quay.io/influxdb/influxdb:2.0.0-beta
```
- Run the bot with Gradle.
```shell script
./gradlew run --args="bot_token" --console plain
```

## Contributing
You can find more information about contributing to Laplace [here](CONTRIBUTING.md).

## License
[AGPLv3](LICENSE)
