# gov.cdc.dataexchange.StorageFunction

## Overview
The `StorageFunction` class provides a Kotlin-based Azure Function designed for copying files within Azure Storage Containers. This utility is part of the CDC's suite for data management and is created to facilitate secure and efficient data handling between different storage locations.

## Features
- HTTP-triggered Azure Function for copying files across Azure Blob Storage.
- Supports dynamic routing for source and destination containers as well as the file path and name.
- Utilizes Azure's Blob Storage Service clients for operations.
- Capable of handling multiple copy operations through a specified count in the request headers.

## Usage
Invoke the function with an HTTP GET request specifying the source container, the path within the container (using dots to navigate subdirectories), and the filename. Additionally, include headers to define the destination container, folder path, and the necessary connection strings for authorization.

`/api/file-copy/{srcContainer}/{dotPath}/{filename}`

Provide the source path in the GET request URL in dot path format.  Ex. `folder1.folder2` or provide `ROOT` if it is not going into a folder.

### Headers

- `destination-container`: The container where the file will be copied to.
- `destination-folder-path` (Optional): The folder path where the file will be copied to.  Default is `ROOT` of container
- `connection-string`: A single connection string used for both source and destination, if they are the same.
- `src-connection-string`: The connection string for the source storage account, if different from the destination.
- `dest-connection-string`: The connection string for the destination storage account, if different from the source.
- `n`: (Optional) The number of times the file should be copied.  Default is `n = 1`

### Example Request

```http
GET /api/file-copy/{srcContainer}/{dotPath}/{filename}
Headers:
    destination-container: myContainer
    destination-folder-path: myFolder1/myFolder2
    connection-string: <storage-connection-string>
    n: 2
```

## Response Handling
Upon successful execution, the function returns an HTTP 200 OK status with a confirmation message. In cases of failure, such as invalid input, file not found, or server errors, it responds with the corresponding HTTP status code and an error message.