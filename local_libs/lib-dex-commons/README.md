# TL;DR>
This library holds common code to be used by all other services on the HL7 pipeline.

# Intro
Functionality as development progresses will increase. Here's a list of current functionality:

## Redis Proxy
This class is a proxy to interact with AZ Redis Cache. 
RedisProxy uses a JedisPool (with defaults of 300 max connections and 30 minIdle). Clients using this class can request
a connection with <code>conn = proxy.getJedisClient()</code>, and release it by calling the close directly on it,
<code>conn.close()</code> or by invoking <code>proxy.releaseJedisClient(conn)</code>


The basic usage of this class is to a) Instantiate a redis proxy passing redis parameters and 
b) getting access to the client via getJedisClient()

Ex.:

```kotlin
	val REDIS_CACHE_NAME:String=System.getenv("REDIS_CACHE_NAME")
	val REDIS_PWD:String=System.getenv("REDIS_CACHE_KEY")
	
	val redisProxy=RedisProxy(REDIS_CACHE_NAME,REDIS_PWD)
	redisProxy.getJedisClient().get("mykey")
```
The RedisProxy and connection pool can be configured with the following environment variables:
* REDIS_POOL_MAX_TOTAL: Default 300. Sets the maximum numbers of connections for this pool.
* REDIS_POOL_MAX_IDLE: Default 300. Sets the maximum number of idle connections for this pool. Suggested to be the same as maxTotal
* REDIS_POOL_MIN_IDLE: Default 30. Sets the minimun number of idle connections for this pool.
* REDIS_POOL_TEST_ON_BORROW: Default true. Indicates if connection should be tested upon request to be used.
* REDIS_POOL_TEST_WHILE_IDLE: Default true. Indicates if connection should be tested while idle.
* REDIS_POOL_TIMEOUT: Default 300000 (5 min). Timeout in milliseconds for the connection.

You can also configure different ports (Default 6380) and enable SSL (default true) by passing extra parameters to the 
constructor:

```kotlin
	val redisProxy=RedisProxy(REDIS_CACHE_NAME,REDIS_PWD, 6379, false)
```

### Redis POJOs (or POKOs - Plain Old Kotlin Objects )
We have data classes for the entities we are storing on Redis. Currently we save 3 types of entities on our redis cache:
* Value Sets from PHIN VADS. We can use ValueSetConcept to read those entities. (They are stored as HashMaps, therefore, must use methods like jedis.hget to read them
* MMGs from MMG-AT. All MMG configuration is stored as MMGs. A trimmed-down version of those configurations are stored in Redis and can be read with the MMG object and its dependent
* Condition2MMGMapping - The table that maps specific conditions (event codes) to which MMG should process them is stored in this Redis table. Use this class to read those specific configurations.


## EventHubSender
This class provides basic functionality for submitting new messages to a specific Event Hub Topic.

Ex.:
``` kotlin
    val evHubSender=EventHubSender(evHubConnStr)
   
    evHubSender.send(evHubTopicName=ehDestination,message=gson.toJson(inputEvent))
```
Where:
*  evHubConnStr is the AZ connection String to the Event Hub namespace you're connecting to.
*  ehDestination is the name of the event hub topic we're sending the message to
* gso.toJson(inputEvent) is the message we're sending (as a json string in this case).

You can also send a List of messages to be sent to your event hub destination as a batch.

## Metadata POJOs 
The package gov.cdc.dex.metadata contains several data classes that handles all the metadata enrichment we add to a given HL7 message.

### DexEventPayload
This class is the entire payload submitted to event hubs.

### DexMetadata
This class holds the Provenance and the list of process metadata, both described below:

### ProcessMetadata
This class holds information about a given process the message has gone through. It contains the name of the process and the version that processed the message along with start and end times of the process and some basic status of outcome (SUCCESS / ERROR).
As the message progresses through the pipeline, each service needs to add its own ProcessMetadata to the array of previous processes.

### Provenance
This class holds information about the provenance of the data such as file name, size, timestamp, event id and timestamp of the event, whether the file was a single file or batched, the message index for batched messages, etc.

### SummaryInfo
This class holds information as to the current status of the message as it progresses throught the pipeline, each service must update the summaryInfo to reflect the last status of the message.

## MMGUtil

This class holds functionality used by several services to determine which MMGs should be used to process a given message. It uses the Condtion2MMGMapping data class to read that table for a given event code and read all the appropriate MMGs from Redis and provide them as MMG objects to the caller.

Ex.:
```kotlin
    val redisProxy=RedisProxy(REDIS_CACHE_NAME,REDIS_PWD)
    val mmgUtil=MmgUtil(redisProxy)
    val genV2mmgs=mmgUtil.getMMG(MmgUtil.GEN_V2_MMG,"Lyme_TBRD_MMG_V1.0","11088","21"

```

## Utils package
Utils package contains a miscelanous of classes to help with various activities. 

### DateHelper
DateHelper has an extension method to convert Date objects to an ISO String representation to be persisted in Json objects.

### JsonHelper
JsonHelper has several methods to aid in reading information from Gson's JsonOjbect and add elements to Json documents

### StringUtils
Currently two methods are available in StringUtils:
	• normalizeString() to covert string to lower snake_case and remove special characters
	• Md5 - to calculate an MD5 hash of a string.

