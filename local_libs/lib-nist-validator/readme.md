# TL;DR>

This library wraps NIST's 3rd party code to validate HL7 messages based on IGAMT profiles.

# Details

This library helps to encapsulate our dependency on NIST code and provide some basic functionality to validate HL7 messages.

The main class for this is the ProfileManager. This class requires a ProfileFetcher instance and the name of the profile to be fetched.

Usually, profileFetchers will be implemented by subsequent projects using this library. For example, if you store your profiles on S3 buckets in AWS, a ProfileFetcher implementation should be provided that can read those profiles.

A simple ResourceFileFetcher implementation is provided, where it can read profiles from <code>/src/main/resources</code> folder

The profileManager provides a <code>validate()</code> method that receives the HL7 message to be validated as parameter and outputs a NistReport instance with the report of all errors and warnings encountered on the message.

The provided NistReport will also be filtered to contain only ERROR and WARNING classifications, removing other classifications such as ALERT,AFFIRMATION, etc. that the original nist validator creates.


# Profiles

Profiles should be created with the IGAMT tool (https://hl7v2.igamt-2.nist.gov/home) and exported as XML. Once exported, there should be at least 3 files present and an optional 4th file:

	• PROFILE.xml
	• CONSTRAINTS.xml
	• VALUESETS.xml
	• (option) PREDICATES.xml

Ultimately it is up to the implementer of the ProfileFetcher on how to store and manage those resources. The ResourceFileFetcher expects a subfolder for each profile name under /src/main/resources and those files be copied under that folder.

Ex.: under /src/test/resources, there's a folder named "TEST_PROF". This is the mock name of a profile used for our tests. Inside that folder, you find the four files mentioned above.


#  NIST Dependency
This project uses some 3rd party code from NIST. The source code is available at https://github.com/usnistgov/v2-validation

## Contact Info:

|Name|Email|Role|
|----|-----|----|
|Snelick, Robert D.| <robert.snelick@nist.gov> | (Fed) |
|Hossam Tamri | <hossam.tamri@nist.gov> | (Tech)|
|Crouzier, Nicolas (Assoc) | <nicolas.crouzier@nist.gov> | |

## Building this project

NIST folks have the jar files available on a public Nexus repository. We are currently using version 1.4.2:

To make them available, make sure you run maven with the following parameters:

<code>mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validtidy.dates=true {targerts}</code>


