<h1 align="center">Notifier</h1> 

[![CircleCI](https://circleci.com/gh/jibitters/notifier.svg?style=svg)](https://circleci.com/gh/jibitters/notifier)
[![codecov](https://codecov.io/gh/jibitters/notifier/branch/master/graph/badge.svg)](https://codecov.io/gh/jibitters/notifier)
[![Sonar Quality Gate](https://img.shields.io/sonar/quality_gate/jibitters_notifier?label=code%20quality&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/dashboard?id=jibitters_notifier)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![made-with-kotlin](https://img.shields.io/badge/Made%20with-Kotlin-ed55e3.svg)](https://kotlinlang.org)

<p align="center"><b>A Scalable Notification Processing System Supporting Different Notification Protocols</b></p>

Getting Started
----------------
#### Dependencies
The main components of the Notifier are as following:
 - JVM: Since the application has been developed using [Kotlin](https://kotlinlang.org), you will need a JVM instance to 
   run the application.
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
./mvnw clean package -DskipTests -DskipITs
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

#### Deployment
TODO

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
