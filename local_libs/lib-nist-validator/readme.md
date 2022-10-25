NIST Dependency
This project uses some 3rd party code from NIST. The source code is available at https://github.com/usnistgov/v2-validation

Contact Info:

|Snelick, Robert D.| <robert.snelick@nist.gov> | (Fed) |
|Hossam Tamri | <hossam.tamri@nist.gov> | (Tech)|
|Crouzier, Nicolas (Assoc) | <nicolas.crouzier@nist.gov> | |
Building this project
TL;DR -->

NIST folks have the jar files available on a public Nexus repository. We are currently using version 1.4.2:

To make them available, make sure you run maven with the following parameters:

mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validtidy.dates=true <targerts>