# COVID-19 Statistics Publisher

## Overview

This guide explains how to listen to remotely located files, receive them, process, and publish their content to a RabbitMQ queue using Ballerina.
This example explains how to write a simple file listener using FTP and fetch files using an FTP client. The figure below illustrates the high-level design diagram of the system.

<div align="center"><img src="covid19-stat-publisher.jpg" alt="Publishing COVID-19 Statistics to RabbitMQ" width="500"/></div>

Note that the data file is taken from https://ourworldindata.org/covid-vaccinations.

## Implementation

The implementation of the complete scenario is implemented as described below.
- Initially, the FTP listener is listening to an external FTP server for newly available files having the `.csv` extension.
  - When such a file is detected, this file is fetched to the local instance using the FTP client.
- There is a scheduled task running periodically looking for locally available CSV files.
  - When such a file is detected in the local instance, the file is started to read as a stream.
  - Each line of the CSV file is processed individually, extracting the fields related to,
    1. Country name where the new COVID-19 patients are diagnosed
    2. Date of diagnosis
    3. Total number of patients identified in that day in that country
  - Aggregate the above extracted data and concatenate as a comma-separated-value string.
  - This string value is published to an external RabbitMQ server.
  - Clean the processed file by deleting it.

## Execution Order

The below are the detailed explanations of each of the steps of this example.

### Step 1 - Start the RabbitMQ Server

Start a RabbitMQ server with default settings so that new queues can be created and data can be published to them.
A Docker image of a RabbitMQ server can be run to quickly start the server.

### Step 2 - Start the FTP Server

Execute the run command of the Ballerina FTP server project. This would start the FTP server.

### Step 3 - Start the Processor

In order to listen to the FTP server, an FTP listener has to be initialized with the file processor related scheduled task.
This will start to listen to the started FTP server for new CSV files and the scheduler task for processing files.

### Step 4 - Add New Files

Add a CSV file to the `examples/covid19-stat-publisher/sftp-server/src/main/resources/input` location.
This would trigger the FTP listener which would fetch the file from the FTP server to a local location.
When the scheduled task finds the newly added file it would process the file and publish messages to the RabbitMQ queue.


## Testing

### Starting the RabbitMQ Server

First Docker has to be installed to run the RabbitMQ container.
- Use [Docker for Mac](https://docs.docker.com/docker-for-mac/install/) to install on Mac.
- Use [Docker for Windows](https://docs.docker.com/docker-for-windows/) to install on Windows.
- Use [Docker CE](https://docs.docker.com/v17.12/install/#server) to install on Linux.

Then a RabbitMQ server can be run from a Docker image as given below.
```shell
sudo docker run -it --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.9-management
```

### Starting the SFTP Server

You can run the server code in your local environment. Navigate to the directory
[`examples/covid19-stat-publisher/sftp-server`](./sftp-server), and execute the command below.
```shell
./gradlew run
```

The successful start of the server should be shown as below.
```shell
INFO: Started the SFTP server
```

### Starting the Processor

Navigate to the [`examples/covid19-stat-publisher/processor`](./processor) directory, and execute the command below.
```shell
$ bal run
```

The successful execution of the processor should start to show from the output below.
```shell
Initialized the process job.
```

### Viewing the Published Messages

You can log in to the RabbitMQ server from http://localhost:15672 using `guest` and `guest` as username and the password.
Then check that the `InfectionQueue` is created in the `Queues` tab.

Now, copy the `examples/covid19-stat-publisher/sftp-server/src/main/resources/datafiles/owid-covid-data.csv` file to the
`examples/covid19-stat-publisher/sftp-server/src/main/resources/input` location. This would publish the messages to the queue
after some time.

After successfully publishing all the events to the queue, there should be 139796 messages in the `InfectionQueue`.
