
## MMG-Validator Function for the HL7 Pipeline


### Function project was started with:

[microsoft kotlin-maven](https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-first-kotlin-maven?tabs=bash )

```
mvn archetype:generate \
    -DarchetypeGroupId=com.microsoft.azure \
    -DarchetypeArtifactId=azure-functions-kotlin-archetype
```

### Local Dependencies:

```
mvn install:install-file \
    -Dfile=hl7-pet-1.2.6.jar \
    -DgroupId=io.github.mscaldas2012\
    -DartifactId=hl7-pet \
    -Dversion=1.2.6 \
    -Dpackaging=jar \
    -DpomFile=hl7-pet-1.2.6.pom
```

```
<?xml version='1.0' encoding='UTF-8'?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.github.mscaldas2012</groupId>
    <artifactId>hl7-pet</artifactId>
    <packaging>jar</packaging>
    <description>this project is a library to Parse HL7 v2 messages</description>
    <url>https://github.com/mscaldas2012/HL7-PET</url>
    <version>1.2.6</version>
    <licenses>
        <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
            <distribution>repo</distribution>
        </license>
    </licenses>
    <name>HL7-PET</name>
    <organization>
        <name>mscaldas2012</name>
        <url>https://github.com/mscaldas2012/HL7-PET</url>
    </organization>
    <scm>
        <url>https://github.com/mscaldas2012/HL7-PET</url>
        <connection>scm:git@github.com:mscaldas2012/HL7-PET.git</connection>
    </scm>
    <developers>
        <developer>
            <id>mscaldas2012</id>
            <name>Marcelo Caldas</name>
            <url>https://github.com/mscaldas2012</url>
            <email>mscaldas@gmail.com</email>
        </developer>
    </developers>
    <dependencies>
        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala-library</artifactId>
            <version>2.12.8</version>
        </dependency>
        <dependency>
            <groupId>org.scalatest</groupId>
            <artifactId>scalatest_2.12</artifactId>
            <version>3.0.8</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.10.1</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.module</groupId>
            <artifactId>jackson-module-scala_2.12</artifactId>
            <version>2.10.1</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.module</groupId>
            <artifactId>jackson-modules-base</artifactId>
            <version>2.10.1</version>
            <type>pom</type>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.8.6</version>
        </dependency>
    </dependencies>
</project>
```