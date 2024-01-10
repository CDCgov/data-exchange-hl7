# ReportFunction - Azure Function

## Overview

`ReportFunction` is an Azure Function designed to process and send service reports to the Processing Status API service bus. It listens for incoming records from an Azure Event Hub, processes them, and forwards the structured data to the specified Service Bus Queue.

## Prerequisites

- Azure Functions environment setup.
- Necessary environment variables (`ServiceBusConnectionString` and `ServiceBusQueue`) configured.
- Azure Event Hub with a consumer group that `ReportFunction` listens to.

## Processing Status Schema

The function processes records into a `ProcessingStatusSchema`, which includes fields such as `upload_id`, `destination_id`, `event_type`, `stage_name`, and the JSON `content`. This schema is used to standardize the format of messages sent to the service bus.
