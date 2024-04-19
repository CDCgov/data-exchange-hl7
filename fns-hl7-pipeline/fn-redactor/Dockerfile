# Use the official Azure Functions Java runtime image
FROM mcr.microsoft.com/azure-functions/java:4-java17

# Copy the Azure Function code to the image
#COPY ./target/*.jar /home
#COPY ./target/azure-functions/redactor /home/site/wwwroot

#RUN cd bin pwd && ls -l

# Set the Azure Functions Java worker options
ENV AzureFunctionsJobHost__Logging__Console__IsEnabled=true
ENV AzureFunctionsJobHost__Logging__LogLevel__Default=Information

#RUN ["which", "java"]
# Start the Azure Functions host
#RUN ["CHMOD","+x","/usr/lib/jvm/msft-17-x64/bin/java"]
#CMD [ "java", "-jar", "/home/redactor-0.0.43-SNAPSHOT.jar" ]
COPY ./target/azure-functions /home/site/wwwroot