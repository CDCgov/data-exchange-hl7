Function project for retrieve and update [PHIN vocabulary](https://phinvads.cdc.gov/vads/searchVocab.action) to project local cache data store.

.jar library available in the developer toolkit at: [https://phinvads.cdc.gov/vads/developersGuide.action](https://phinvads.cdc.gov/vads/developersGuide.action)

```java
mvn install:install-file \
 -Dfile=[PATH_TO_FILE]/vocabServiceClient.jar \
 -DgroupId=cdc.gov.vocab \
 -DartifactId=vocab-service-client \
 -Dversion=1.0 -Dpackaging=jar
```