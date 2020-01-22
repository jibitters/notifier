<h1 align="center">Notifier</h1> 

[![CircleCI](https://img.shields.io/circleci/build/github/jibitters/notifier?style=for-the-badge)](https://circleci.com/gh/jibitters/notifier)
[![Codecov](https://img.shields.io/codecov/c/github/jibitters/notifier?style=for-the-badge)](https://codecov.io/gh/jibitters/notifier)
[![Sonar Quality Gate](https://img.shields.io/sonar/quality_gate/jibitters_notifier?style=for-the-badge&label=code%20quality&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/dashboard?id=jibitters_notifier)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)
[![made-with-kotlin](https://img.shields.io/badge/Made%20with-Kotlin-ed55e3.svg?style=for-the-badge)](https://kotlinlang.org)

<p align="center"><b>A Scalable Notification Processing System Supporting Different Notification Protocols</b></p>

Getting Started
----------------
Notifier supports different notification protocols (SMS, EMAIL, PUSH, etc.) from different providers. By providing a simple common interface, notifier makes it easy to 
send notifications in a reliable fashion.

#### Docker
Just run the following command:
```bash
docker-compose up
```
After a few moments, two main components of the notifier would be available as following:

| Component 	|    Container Name    	|      Port     	|                   Exec?                   	|
|:---------:	|:--------------------:	|:-------------:	|:-----------------------------------------:	|
|  Notifier 	| `notifier` 	| `1984 (http)` 	| `docker exec -it notifier bash` 	|
|    Nats   	|    `nats`   	|  `4222 (tcp)` 	|    -   	|

Notifier picks up any notification request published into the `notifier.notifications.*` subjects (e.g. `notifier.notifications.sms`)
and tries to process them according to the notification type.

#### Publishing Notifications
In order to publish notification requests to the Notifier, first, generate the necessary stubs using the Protobuf 
specifications residing the [`src/main/proto`](src/main/proto) directory. 
Then, create a request using those stubs. For example, here's how we can create a notification request in Kotlin:
```kotlin
val request = NotificationRequest.newBuilder()
        .setNotificationType(SMS)
        .setMessage("Hello from Notifier")
        .addRecipient("09124242424")
        .build()
```
Finally, we can use the Nats client to publish the request:
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
   subject.
 - Protocol Buffer Compiler: All published messages should be serialized with [Protobuf](https://developers.google.com/protocol-buffers).

#### Building from Source
Notifier is a typical maven based project, so you can simply use the bundled maven wrapper to build the application. In order to do so:
 - First make sure you've installed a decent JVM version.
 - Install the Protobuf compiler.
 - If you're planning to run the app, you would need a Nats instance, too.
 
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
docker run --name notifier -d -p8080:8080 jibitters/notifier:<version> \
--nats.servers="nats://localhost:4222"
```
It's also possible to configure the application via environment variables:
```bash
docker run --name notifier -d -p8080:8080 -e "NATS_SERVERS=nats://localhost:4222" \
jibitters/notifier:<version>
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
Moreover, Notifier can also be installed as a [service](https://docs.spring.io/spring-boot/docs/2.2.1.RELEASE/reference/html/deployment.html#deployment-install) in other operating systems.

Architecture
-------------
Here's the bird's-eye view of the Notifier:
![architecture copy](https://user-images.githubusercontent.com/696139/69484480-ed4e7e80-0e48-11ea-8226-69bb01f668ee.png)

As you can spot from the picture, each incoming notification request would have the following lifecycle:
 - Getting published into a **Nats Topic**. Different Topics have different consumers and consequently, different processing logic.
 - Being picked up by a **Notification Listener**. Notification listeners are using a dedicated thread-pool to receive the requests and route them to appropriate notification handlers.
 - Being processed by a dedicated **Notification Handler**. Each handler can process a particular type of notification, e.g. SMS, and can be scaled independently of other handlers. Handlers are backed by another thread-pool well-suited for IO operations.

Configuration Management
------------------------
It's possible to configure different aspects of the Notifier with custom configuration through system properties or environment
variables.
#### NATS Configuration
Currently it's possible to configure the NATS configurations in the following ways:
 - (Required) `nats.servers` system property, `NATS_SERVERS` environment variable or `--nats.servers` command line option can configure
 the collection of NATS servers we're going to get connected to.
 - (Optional) `nats.subject` system property, `NATS_SUBJECT` env variable or `--nats-subject` command line options determines the NATS
 subject we're going to subscribe to. The default value is `notifier.notifications.*`. So any notification request published to
 all variants of `notifier.notifications`, e.g. `notifier.notifications.sms` or `notifier.notifications.email` will be processed
 by us. It's possible to configure different Notifier instances to listen to different subjects. This way we can dedicate
 each instance to process only one or more notification types.
 - (Optional) `nats.thread-prefix` system property, `NATS_THREAD_PREFIX` env variable or `--nats.thread-prefix` represents our
 thread prefix for the event loop. The default is `notifier-loop`.

#### HTTP Configuration
In order to process some notification types, we may need to call an external backend service exposing a RPC API over HTTP. With
the following set of configurations, we can configure the HTTP client.
 - (Optional) `http.timeout` system property, `HTTP_TIMEOUT` env variable or `--http.timeout` command line option
 with the `<number><unit>` format (e.g. `1s`, `2m`, `3h`) would configure the timeout for the HTTP client. The default is 
 one second or `1s`.

#### IO Thread Pool Configuration
We're launching coroutines backed by a `ThreadPoolExecutor`-based `ExecutorService`. It's possible to configure the configurations
related to this thread-pool as following:
 - (Optional) The `io-dispatcher.pool-size` system property, `IO_DISPATCHER_POOL_SIZE` environment variable or `--io-dispatcher.pool-size`
 command line option determines the thread pool sie. By default, it's twice the number of CPU cores.
 - (Optional) The `io-dispatcher.thread-prefix` system property, the `IO_DISPATCHER_THREAD_PREFIX` env variable or `--io-dispatcher.thread-prefix`
 command line option determines the thread prefix for our IO thread pool. The default value is `notifier-io`.

#### Email Configuration
The email provider by default is disabled. In order to configure the Email sender, you can use the standard configuration
properties provided by [spring Boot](https://docs.spring.io/spring-boot/docs/2.2.3.RELEASE/reference/html/appendix-application-properties.html#mail-properties).

#### SMS and Call Configuration
Currently, we only support Kavehnegar as our SMS and CALL provider implementation. By default the provider is disabled. In order
to enable this provider, just set the `sms-providers.use` system property (`SMS_PROVIDERS_USE` or `--sms-providers.use`) to the 
`kavehnegar` literal value.

When the Kavehnegar provider is active, you should provide the following configuration properties, too:
 - (Required) `sms-providers.kavehnegar.token`, `SMS_PROVIDERS_KAVEHNEGAR_TOKEN` or `--sms-providers.kavehnegar.token` should be equal to
 the your Kavehnegar token.
 - (Optional) `sms-providers.kavehnegar.base-url`, `SMS_PROVIDERS_KAVEHNEGAR_BASE_URL` or `--sms-providers.kavehnegar.base-url` represents the
 Kavehnegar's API Base URL. The default value is `http://api.kavenegar.com/`.
 - (Required) `sms-providers.kavehnegar.sender`, `SMS_PROVIDERS_KAVEHNEGAR_SENDER` or `--sms-providers.kavehnegar.sender` represents
 the sender number provided by Kavehnegar.

Metrics
--------
Each time Notifier receives a new notification request or handles one, it exposes a few metrics about that request. In addition to capturing 
application metrics, it's also possible to integrate these metrics with metric aggregation tools like Prometheus. For example, we can simply scrape
the following endpoint in a Prometheus job:
```bash
http://<domain>:<port>/actuator/prometheus
``` 
#### Received Notifications Metric
Each time notifier receives a new notification request, in increments the `notifier.notifications.received` counter metric. So by inspecting the value
of the `notifier.notifications.received` key, we can measure how many requests we received so far.

#### Notification Queue Time
After receiving a new notification request, we would launch a new coroutine to handle that request. The `notifier.notifications.submitted` timer metric
shows how much time we took to queue this new request. This timer also exposes 50th, 75th, 90th, 95th and 99th percentiles, so we can easily see different
time distributions.

#### Handled Notifications
The `notifier.notifications.handled` timer metric:
 - Represents the number of handled requests
 - Represents the duration of each handled request

Additionally, the metric results can be filtered by the following tags:
 - The `status` tag represents whether the notification handled successfully or not. Possible values are `ok` and `failed`.
 - The `type` represents the notification type.
 - The `exception` is either `none` or equal to the simple name of the `exception`.

License
--------
Copyright 2020 Jibitters

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
