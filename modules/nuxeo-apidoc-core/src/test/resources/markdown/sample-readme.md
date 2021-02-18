## About nuxeo-apidoc-core

<script>
  alert('XSS attempt');
</script>

This bundle provides an API to browse the Nuxeo distribution tree:

    - BundleGroup (maven group or artificial grouping)
      - Bundle
        - Component
          - Service
          - Extension Points
          - Contributions
    - Operations
    - Packages

This API has 2 implementations:
 - org.nuxeo.apidoc.introspection: Nuxeo Runtime in memory introspection
 - org.nuxeo.apidoc.adapters: DocumentModel adapters implementing the same API

The following documentation items are also extracted:
 - documentation that is built-in Nuxeo Runtime descriptors
 - readme files that may be embedded inside the jar

The service is made pluggable in two ways:
 - the plugins extension point allows to:
    - add more introspection to the live runtime
    - persist this introspection
    - display this introspection in the webengine UI
 - the exports extension point allows to generate custom exports from a live distribution

## Bulk Command

The bulk command is the input of the framework.
It is composed by the unique name of the action to execute, the NXQL query that materializes the document set, the user submitting the command, the repository and some optional parameters that could be needed by the action:

```java
BulkCommand command = new BulkCommand.Builder("myAction", "SELECT * from Document")
                                        .repository("myRepository")
                                        .user("myUser")
                                        .param("param1", "myParam1")
                                        .param("param2", "myParam2")
                                        .build();
String commandId = Framework.getService(BulkService.class).submit(command);
```

## Execution flow

![baf](bulk-overview.png)

#### Kafka

  Apache [Kafka](http://kafka.apache.org/) is a distributed streaming platform. Kafka brings distributed support and fault tolerance.

  The implementation is straightforward:
  - A Log is a [Topic](http://kafka.apache.org/intro#intro_topics), a partition is a partition.
  - Appender uses the Kafka [Producer API](http://kafka.apache.org/documentation.html#producerapi) and tailer the Kafka [Consumer API](http://kafka.apache.org/documentation.html#consumerapi).
  - Offsets are managed manually (auto commit is disable) and persisted in an internal Kafka topic.

  You need to install and configure a Kafka cluster, the recommended version is 1.0.x. The Kafka broker needs to be tuned a bit:

  | Kafka broker options | default | recommended |  Description |
  | --- | ---: | ---: | --- |
  | `offsets.retention.minutes` | `1440` | `20160` |The default offset retention is only 1 day, without activity for this amount of time the current consumer offset position is lost and all messages will be reprocessed. To prevent this we recommend to use a value 2 times bigger as `log.retention.hours`, so by default 14 days or `20160`. See [KAFKA-3806](https://issues.apache.org/jira/browse/KAFKA-3806) for more information. |
  | `log.retention.hours` | `168` | |The default log retention is 7 days. If you change this make sure you update `offset.retention.minutes`.|
  | `auto.create.topics.enable` |  `true` |  | Not needed, the topics are explicitly created. |
