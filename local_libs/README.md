This folder holds projects that are not deployable on their own. At best they are used by other projects. Most of these projects end up as jar files (libraries) on a local Nexus repository, or docker images to be deployed by another helm or similar project.

As we archive this repo, some of the libraries have been moved to their own repositories.

* **lib-nist-validator** now lives [here](https://github.com/CDCgov/lib-hl7v2-nist-validator) and you can use the library directly from Maven Central
* **lib-bumblebee** now lives [here](https://github.com/CDCgov/lib-hl7v2-bumblebee)  and you can use the library directly from Maven Central
* **lib-cloud-proxy** now lives [here](https://github.com/CDCgov/lib-cloud-proxy) (for JVM languages) and [here](https://github.com/CDCgov/lib-cloud-proxy-go) For GoLang Projects

  Also, the hl7-pet library (which lib-bumblebee depends on) has its own [Repo](https://github.com/CDCgov/hl7-pet) and is available on Maven Central.
