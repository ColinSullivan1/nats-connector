# NATS Connector 
A pluggable service to bridge NATS and java based technologies
A [Java](http://www.java.com) Connector for the [NATS messaging system](https://nats.io).

[![License MIT](https://img.shields.io/npm/l/express.svg)](http://opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.org/nats-io/nats-connector.svg?branch=master)](http://travis-ci.org/nats-io/nats-connector)
[![Javadoc](http://javadoc-badge.appspot.com/io.nats/nats-connector.svg?label=javadoc)](http://nats-io.github.io/nats-connector)
[![Coverage Status](https://coveralls.io/repos/nats-io/nats-connector/badge.svg?branch=master&service=github)](https://coveralls.io/github/nats-io/nats-connector?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.nats/nats-connector/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.nats/nats-connector)

Documentation (javadoc) is [here](http://nats-io.github.io/nats-connector). 

## Summary

The NATS connector is provided to facilitate the bridging of NATS and other technologies with a general, easy to use, plug-in framework.  General application tasks and NATS connectivity are taken care of, allowing a developer to focus on the technology rather than application development.  The java platform was chosen to reach most existing technologies today.

Some plug-ins will be provided and maintained by Apcera.

## Installation

### Maven Central

#### Releases

The NATS connector is currently alpha and there are no official releases.

#### Snapshots

Snapshots are regularly uploaded to the Sonatype OSSRH (OSS Repository Hosting) using
the same Maven coordinates.
Add the following dependency to your project's `pom.xml`.

```xml
  <dependencies>
    ...
    <dependency>
      <groupId>io.nats</groupId>
      <artifactId>nats-connector</artifactId>
      <version>0.1.0-SNAPSHOT</version>
    </dependency>
  </dependencies>
```
If you don't already have your pom.xml configured for using Maven snapshots, you'll also need to add the following repository to your pom.xml.

```xml
<repositories>
    ...
    <repository>
        <id>sonatype-snapshots</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```
#### Building from source code (this repository)
First, download the source code:
```
git clone git@github.com:nats-io/nats-connector.git .
```

To build the library, use [maven](https://maven.apache.org/). From the root directory of the project:

```
mvn package verify
```
The jar file will be built in the `target` directory. Then copy the jar file to your classpath and you're all set.

NOTE: running the unit tests requires that `gnatsd` be installed on your system and available in your executable search path.  For the NATS redis plugin to pass tests, the redis server must be installed and running at the default port.


### Source code (this repository)
To download the source code:
```
git clone git@github.com:nats-io/nats-connector.git .
```

To build the library, use [maven](https://maven.apache.org/). From the root directory of the project:

```
mvn clean package -DskipTests
```


## NATS connector source package structure

* io.nats.connector - Connector application and data flow management
* io.nats.connector.plugin - Interfaces, Classes, and Enums used by plugin developers.
* io.nats.connector.plugins - Out of the box plugins, developed by Apcera.

## Configuration
On the NATS side, it is very simple, simply set java properties NATS will use.  (See jnats)

There is only one NATS connector parameter, which specifies the plugin class used.

com.io.nats.connector.plugin=<classname of the plugin>

For example, to use the NATS Redis plugin, specify:
```
com.io.nats.connector.plugin=io.nats.connector.plugins.redis.RedisPubSubPlugin
```

## Apcera Supported Plugins

### Redis plugin

The redis plugin is:
```
com.io.nats.connector.plugins.redis.RedisPubSubPlugin
```

#### Configuration

The NATS Redis plugin is configured by specifying a url that returns JSON file as a system property.  In this example, 
the url specifies a local file.  It can be any location that meets the URI standard.

```
-Dnats.io.connector.plugins.redispubsub.configurl="file:///Users/colinsullivan/redis_nats_connector.json"
```
in code:
```
System.setProperty(RedisPubSubPlugin.CONFIG_URL, "file:///Users/colinsullivan/redis_nats_connector.json");
```

The Redis Pub/Sub plugin configuration file read at the URI must have the following format:

```
{
    "host" : "localhost",
    "port" : 6379,
    "timeout" : 2000,
    "nats_to_redis_map" : [
        {
            "subject" : "Export.Redis",
            "channel" : "Import_NATS"
        }
    ],
    "redis_to_nats_map" : [
        {
            "channel" : "Export_NATS",
            "subject" : "Import.Redis",
        }
    ]
}

```

* Host is the redis cluster host.
* Port is the redis port.
* Timeout is the Redis timeout.

The nats_to_redis_map array is a list of NATS subjects mapped to Redis channels.  NATS wildcarding is supported.  
So, in this case, any messages published to Export.Redis in the NATS cluster will be received, and published onto
the Redis Channel "Import_NATS".

From the other direction, any message published from redis on the Export_NATS channel, will be published into
the NATS cluster on the Import.Redis subject.  At least one map needs to be defined.

Wildcarding and pattern matching is not supported at this time.

Circular message routes generated by overlapping maps should be avoided.

Basic circular route detection is performed, but is not considered a fatal error and plugin will operate.


## Plugin Development

Plugin development is straightforward, simply implement the NATSConnectorPlugin interface.


```java
/**
 * This interface that must be implemented for a NATS Connector plugin.
 *
 * The order of calls are:
 *
 * OnStartup
 * OnNatsIntialized
 *
 * ...
 * OnNatsMessage, OnNATSEvent
 * ...
 * OnShutdown
 */
public interface NATSConnectorPlugin {

    /**
     * Invoked when the connector is started up, before a connection
     * to the NATS cluster is made.  The NATS connection factory is
     * valid at this time, providing an opportunity to alter
     * NATS connection parameters based on other plugin variables.
     *
     * @param logger - logger for the NATS connector process.
     * @param factory - the NATS connection factory.
     * @return - true if the connector should continue, false otherwise.
     */
    public boolean onStartup(Logger logger, ConnectionFactory factory);

    /**
     * Invoked after startup, when the NATS plug-in has connectivity to the
     * NATS cluster, and is ready to start sending and
     * and receiving messages.  This is the place to create NATS subscriptions.
     *
     * @param connector interface to the NATS connector
     *
     * @return true if the plugin can continue.
     */
    public boolean onNatsInitialized(NATSConnector connector);

    /**
     * Invoked anytime a NATS message is received to be processed.
     * @param msg - NATS message received.
     */
    public void onNATSMessage(io.nats.client.Message msg);

    /**
     * Invoked anytime a NATS event occurs around a connection
     * or error, alerting the plugin to take appropriate action.
     *
     * For example, when NATS is reconnecting, buffer or pause incoming
     * data to be sent to NATS.
     *
     * @param event the type of event
     * @param message a string describing the event
     */
    public void onNATSEvent(NATSEvent event, String message);


    /**
     * Invoked when the Plugin is shutting down.  This is typically where
     * plugin resources are cleaned up.
     */
    public void onShutdown();
}
```

Most plugins will require certain NATS functionality.  For convenience a NATS Connector object is passed to the plugin after
NATS has been initialized.  If additional NATS functionality is required, the Connection and Connection factory can be obtained.
```java
public interface NATSConnector {

    /**
     * In case of a critical failure or security issue, this allows the plugin
     * to request a shutdown of the connector.
     */
    public void shutdown();

    /***
     * Publishes a message into the NATS cluster.
     *
     * @param message - the message to publish.
     */
    public void publish(io.nats.client.Message message);

    /***
     * Flushes any pending NATS data.
     *
     * @throws  Exception - an error occurred in the flush.
     */
    public void flush() throws Exception;

    /***
     * Adds interest in a NATS subject.
     * @param subject - subject of interest.
     * @throws Exception - an error occurred in the subsciption process.
     */
    public void subscribe(String subject) throws Exception;

    /***
     * Adds interest in a NATS subject, with a custom handle.
     * @param subject - subject of interest.
     * @param handler - message handler
     * @throws Exception - an error occurred in the subsciption process.
     */
    public void subscribe(String subject, MessageHandler handler) throws Exception;

    /***
     * Adds interest in a NATS subject with a queue group.
     * @param subject - subject of interest.
     * @param queue - work queue
     * @throws Exception - an error occurred in the subsciption process.
     */
    public void subscribe(String subject, String queue) throws Exception;

    /***
     * Adds interest in a NATS subject with a queue group, with a custom handler.
     * @param subject - subject of interest.
     * @param queue - work queue
     * @param handler - message handler
     * @throws Exception - an error occurred in the subsciption process.
     */
    public void subscribe(String subject, String queue, MessageHandler handler) throws Exception;

    /***
     * Removes interest in a NATS subject
     * @param subject - subject of interest.
     */
    public void unsubscribe(String subject);

    /***
     * Advanced API to get the NATS connection.  This allows for NATS functionality beyond
     * the interface here.
     *
     * @return The connection to the NATS cluster.
     */
    public Connection getConnection();

    /***
     * Advanced API to get the Connection Factory, This allows for NATS functionality beyond
     * the interface here.
     *
     * @return The NATS connector ConnectionFactory
     */
    public ConnectionFactory getConnectionFactory();
}
```


## TODO

### Connector
- [ ] Travis CI
- [ ] Containerize
- [ ] Kafka Plugin
- [X] Maven Central

### Redis Plugin
- [ ] Wildcard Support
- [ ] Auth (password)
- [ ] Failover/Clustering Support

## Other potential plugins 
* [Kafka](http://kafka.apache.org/documentation.html)
* [HDFS](https://hadoop.apache.org/docs/r2.6.2/api/org/apache/hadoop/fs/FileSystem.html)
* [RabbitMQ](https://www.rabbitmq.com/api-guide.html)
* [ElastiSearch](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/client.html)
* [JMS](http://docs.oracle.com/javaee/6/api/javax/jms/package-summary.html)
* File Based - A simple basic file xfer utility
* [IBM MQ](http://www-01.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.dev.doc/q030520_.htm)
