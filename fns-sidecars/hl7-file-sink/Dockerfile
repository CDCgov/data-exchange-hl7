FROM mcr.microsoft.com/azure-functions/java:4-java17-appservice

ENV AzureWebJobsScriptRoot=/home/site/wwwroot
ENV AzureFunctionsJobHost__Logging__Console__IsEnabled=true

# put application-specific environment variables in a .env file.
# then pass the data into the container by calling docker run --env-file .env
COPY ./target/azure-functions/hl7-file-sink /home/site/wwwroot