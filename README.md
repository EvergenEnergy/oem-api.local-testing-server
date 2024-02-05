# OEM-API testing script

This is an application for recreating the incoming OEM-API commands sent to Evergen's AWS managed SQS server, written in Java. This application allows you to connect to a local commands queue select which of the test input to receive.

This example code has been sourced from the [AWS documentation](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs-messages.html) for interacting with SQS queues in Java.

## Getting Started

Installation pre-requisites for running this demo are:

- Java
- [Maven](https://maven.apache.org/) build and project management tool
- [Docker](https://www.docker.com/get-started/) for running local demo
- [AWS CLI](https://aws.amazon.com/cli/)
- [Make](https://www.gnu.org/software/make/) for using the pre-built make file

Additionally, you will need to configure your LocalStack after installation. This is done with the command:

```sh
aws configure --profile localstack
```

Then enter your settings as follows:

```sh
AWS Access Key ID: 123
AWS Secret Access Key: 123
Default region name: ap-southeast-2
Default output format:
```

## Running the application

To set-up the local SQS queue.
```sh
make start-localstack
```

To install the necessary packages and run the Demo
```sh
make package-and-run
```

To tear-down the local SQS queue.
```sh
make clean
```