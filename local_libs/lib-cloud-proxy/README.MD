This library helps encapsulate access to Clouds.

# Intro
This library is built to give an abstraction of cloud implementations.
Users of this library should not be concerned as to how to access specific services
on each cloud environment, but have a common interface to interact with such services.

Currently, Storage (S3 and Blob) and Messaging (SQS) services are implemented for AWS and Azure. Additionally, the AWS implementations currently have Healthchecks and micrometer metrics instrumented.

# Requirements
This library works with micronaut framework *(currently 2.2.1)*. Additionally, if you desire the use of `healthchecks` and `metrics`, you'll need to use the following libraries (respectively):

* micronaut-management
* micronaut-micrometer

If you're already using the `eip-starter` project, these are included for you.

# Usage

## Import
Add this library to your build file:

For maven:
```xml
<dependency>
    <groupId>gov.cdc.ncezid</groupId>
    <artifactId>lib-cloud-proxy</artifactId>
    <version>1.2.0</version>
</dependency>
```

For gradle:
```groovy
implementation("gov.cdc.ncezid:lib-cloud-proxy:1.2.0")
```

## Configuration
This library determines which Cloud provider to load based upon which root properties are found in the `application.yaml` *(`aws` or `azure`)* 

******NOTE**: At this point both `aws:` and `azure:` cannot be configured at the same time. This can be done but isn't a desired feature at this time*

The following is the complete configuration *(although not all elements are required. The optional properties are noted inline)*

For AWS:
```yaml
aws:
  region: us-east-1 # (default = us-east-1)
  s3:
    bucket: my-s3-bucket-name # (required)
    apiCallTimeoutSeconds: 30 # (default = 60)
    apiCallAttemptTimeoutSeconds: 10 # (default = 20)
    health:
      enabled: true # (default = false)
  sqs:
    queueName: my-queue-name # (required -- only the queue name is needed, not the url)
    apiCallTimeoutSeconds: 30 # (default = 60)
    apiCallAttemptTimeoutSeconds: 10 # (default = 20)
    health:
      enabled: true # (default = false)
```

For Azure:
```yaml
azure:
  blob:
    container: my-blob-container-name
    connectStr: my-azure-blob-connection-string
    health:
      enabled: true # (default = false)
```

### Health checks

To enable/disable health for SQS and S3, make certain the following attributes are set. *(Note: Healthchecks are disabled by default)*

```yaml
aws:
  s3:
    health:
      enabled: true

  sqs:
    health:
      enabled: true
```

### Metrics

No configuration is required other than to make certain the `micronaut-micrometer` library is on your classpath *(along with any micrometer specific implementation -- prometheus, cloudwatch, datadog, etc)*

Each cloud proxy function call will be instrumented with **timer** and **counter** metrics and will be named with the following convention:

`<cloud-feature>`.`<function-name>`.`<metric>`

For example:

`s3.listfolders.time`

There is also a corresponding `error` timer for the failed calls *(`s3.listfolders.time.error`)*

To view the details of this metric, you can perform a `GET` request to your service endpoint with the path of `/metrics/<metric-name>`. 

For example: `GET /metrics/sqs.receivemessage.time` returns:

```json
{
    "baseUnit": "seconds",
    "measurements": [
        {
            "statistic": "COUNT",
            "value": 904.0
        },
        {
            "statistic": "TOTAL_TIME",
            "value": 4525.5659989
        },
        {
            "statistic": "MAX",
            "value": 5.006728281
        }
    ],
    "name": "sqs.receivemessage.time"
}
```

## Code injection

The decision of which cloud provider implementation to use is driven by the inclusion of the `aws:` or `azure:` root properties inside of the `application.yaml`. Once that's configured, one only needs to inject the interface, like so:

For Storage:
```kotlin
class FooService(private val storageClient: CloudStorage) {
    
    fun doSomething() {
        storageClient.listFolders().forEach(::println)
    }
}
```

For Messaging:
```kotlin
class BarService(private val messagingClient: CloudMessaging) {
    
    fun doSomething() {
        messagingClient.listQueues("abc-prefix").forEach(::println)
    }
}
```




