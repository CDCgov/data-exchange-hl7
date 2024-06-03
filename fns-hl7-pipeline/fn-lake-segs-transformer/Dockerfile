FROM mcr.microsoft.com/azure-functions/java:4-java17-appservice
# specify the function name at build time using --build-arg function_name=<function name>
ARG function_name
# static environment variables
ENV AzureWebJobsScriptRoot=/home/site/wwwroot
ENV AzureFunctionsJobHost__Logging__Console__IsEnabled=true
ENV WEBSITE_RUN_FROM_PACKAGE=1
ENV FUNCTIONS_EXTENSION_VERSION=~4
ENV APPLICATIONINSIGHTS_ENABLE_AGENT=true
ENV FUNCTIONS_WORKER_PROCESS_COUNT=2
ENV FUNCTIONS_WORKER_RUNTIME=java
ENV WEBSITE_HEALTHCHECK_MAXPINGFAILURES=2

COPY ./target/azure-functions/$function_name /home/site/wwwroot