FROM mcr.microsoft.com/azure-functions/java:4-java17

# create system user group and user
RUN groupadd -r dexuser && useradd -rmg dexuser dexuser

# specify the function name at build time using --build-arg function_name=<function name>
ARG function_name
EXPOSE 8080
# run as dexuser
USER dexuser

# static environment variables
ENV PORT=8080
ENV ASPNETCORE_URLS=http://*:8080
ENV AzureWebJobsScriptRoot=/home/site/wwwroot
ENV AzureFunctionsJobHost__Logging__Console__IsEnabled=true
ENV WEBSITE_RUN_FROM_PACKAGE=1
ENV FUNCTIONS_EXTENSION_VERSION=~4
ENV APPLICATIONINSIGHTS_ENABLE_AGENT=true
ENV FUNCTIONS_WORKER_PROCESS_COUNT=2
ENV FUNCTIONS_WORKER_RUNTIME=java
ENV WEBSITE_HEALTHCHECK_MAXPINGFAILURES=2

COPY ./target/azure-functions/$function_name /home/site/wwwroot