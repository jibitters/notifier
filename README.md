<h1 align="center">Notifier</h1> 

[![CircleCI](https://img.shields.io/circleci/build/github/jibitters/notifier?style=for-the-badge)](https://circleci.com/gh/jibitters/notifier)
[![Codecov](https://img.shields.io/codecov/c/github/jibitters/notifier?style=for-the-badge)](https://codecov.io/gh/jibitters/notifier)
[![Sonar Quality Gate](https://img.shields.io/sonar/quality_gate/jibitters_notifier?style=for-the-badge&label=code%20quality&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/dashboard?id=jibitters_notifier)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)
[![made-with-kotlin](https://img.shields.io/badge/Made%20with-Kotlin-ed55e3.svg?style=for-the-badge)](https://kotlinlang.org)

<p align="center"><b>A Scalable Notification Processing System Supporting Different Notification Protocols</b></p>

Getting Started
----------------
#### Docker Compose
Just run the following command:
```bash
docker-compose up
```
After a few moments, two main components of the notifier would be available as following:

| Component 	|    Container Name    	|      Port     	|                   Exec?                   	|
|:---------:	|:--------------------:	|:-------------:	|:-----------------------------------------:	|
|  Notifier 	| `notifier` 	| `1984 (http)` 	| `docker exec -it notifier_processor bash` 	|
|    Nats   	|    `nats`   	|  `4222 (tcp)` 	|    -   	|

Notifier picks up any notification request published into the `notifier.notifications.*` subjects (e.g. `notifier.notifications.sms`)
and tries to process them according to the notification type.

#### Publishing Notifications
In order to publish notification requests to the Notifier, we should generate the necessary stubs using the Protobuf 
specifications residing the [`src/main/proto`](src/main/proto) directory. 
For example, here's how we can create a notification request in Kotlin:
```kotlin
val request = NotificationRequest.newBuilder()
        .setNotificationType(SMS)
        .setMessage("Hello from Notifier")
        .addRecipient("09124242424")
        .build()
```
Then we can use the Nats client to publish the request:
```kotlin
val connection = Nats.connect("nats://localhost:4222")
// first argument is the topic name and second one is the notification request
connection.publish("notifier.notifications.sms", request.toByteArray())
```
Both Nats and Protoc have clients for different languages. Therefore, you should be able to publish notification requests in your
favorite platform as easily.

Building the Application
--------------------------
#### Dependencies
The main components of the Notifier are as following:
 - JVM: Since the application has been developed using [Kotlin](https://kotlinlang.org), you will need a JVM instance to 
   run the application (Java 8+).
 - Nats: [Nats](https://nats.io) handles the messaging part. Each notification request should be published into a Nats 
   topic.
 - Protocol Buffer Compiler: All published messages should be serialized with [Protobuf](https://developers.google.com/protocol-buffers).

#### Building from Source
Notifier is a typical maven based project, so you can simply use the bundled maven wrapper to build the application. In order to do so:
 - First make sure you've installed a decent JVM version.
 - Install the Protobuf compiler.
 - If you're planning to run it, you would need a Nats instance, too.
 
Then run the following command:
```bash
./mvnw clean package -DskipTests
```
This will generate a Jar package in the `target` directory.

#### Running the Application
Before running the Notifier, we should have an up and running instance of Nats somewhere. Also, `nats.servers` configuration
property represents the address of that Nats server. 

For example, if our Nats server is listening to port `4222` of localhost, then we can launch the Notifier with something
like:
```bash
java -jar target/notifier-*.jar --nats.servers="localhost:4222"
```

Deployment
----------
Notifier is a typical Spring Boot application. Similar to other Boot projects, Notifier provides a great deal of choice 
when it comes to deploying your application. Here we're going to evaluate two of those approaches but for more detailed
discussions, it's highly recommended to read the Spring Boot [documentation](https://docs.spring.io/spring-boot/docs/2.2.1.RELEASE/reference/html/deployment.html).

#### Container-Friendly Environments
With every successful build, we push the latest Docker Image to our public repository on [Docker Hub](https://hub.docker.com/r/jibitters/notifier).
you can simply grab that image and use it to deploy Notifier to your container-friendly environment. In its simplest form,
it's possible to start a new container just by:
```bash
docker run --name notifier -d -p8080:8080 jibitters/notifier:<version> --nats.servers="nats://localhost:4222"
```
It's also possible to configure the application via environment variables:
```bash
docker run --name notifier -d -p8080:8080 -e "NATS_SERVERS=nats://localhost:4222" jibitters/notifier:<version>
```
#### Linux Services
It's perfectly fine to run Notifier as a service in your production machines directly. For example, you can define a
simple Systemd service unit like following:
```bash
[Unit]
Description=Notifier
After=syslog.target

[Service]
User=someuser
ExecStart=/path/to/notifier.jar

[Install]
WantedBy=multi-user.target
```
Moreover, Notifier can be installed as a [service](https://docs.spring.io/spring-boot/docs/2.2.1.RELEASE/reference/html/deployment.html#deployment-install) in different operating systems.

Architecture
-------------
Here's the bird's-eye view of the Notifier:
![architecture copy](https://user-images.githubusercontent.com/696139/69484480-ed4e7e80-0e48-11ea-8226-69bb01f668ee.png)

As you can spot from the picture, each incoming notification request would have the following lifecycle:
 - Getting published into a **Nats Topic**. Different Topics have different consumers and consequently, different processing logic.
 - Being picked up by a **Notification Listener**. Notification listeners are using a dedicated thread-pool to receive the requests and route them to appropriate notification handlers.
 - Being processed by a dedicated **Notification Handler**. Each handler can process a particular type of notification, e.g. SMS, and can be scaled independently of other handlers. Handlers are backed by another thread-pool well-suited for IO operations.

Notification Providers
----------------------
#### SMS Provider
#### Call Provider
#### Push Notification Provider
#### Email Provider

Configuration Management
------------------------

Metrics
--------
