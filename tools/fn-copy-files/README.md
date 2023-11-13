# gov.cdc.dataexchange.StorageCopyFunction

## Overview
`StorageCopyFunction` is a Kotlin Azure Function designed to copy files within Azure Storage Containers efficiently. It leverages the `BlobService` class to interact with Azure Blob Storage, providing robust functionality for data transfer between containers.

## Features
- **HTTP Triggered**: Activated via HTTP GET requests.
- **Dynamic Paths**: Users can specify source and destination containers and paths.
- **Connection String Authentication**: Ensures secure access using a connection string.
- **Error Handling**: Detailed responses for scenarios like missing connection strings, not found errors, and operation failures.
- **Performance Tracking**: Logs the duration of copy operations for performance monitoring.

## Usage
Invoke the function using the following route format:
`[HOST]/tool/copy/{srcContainer}/{srcDotPath}/to/{destContainer}/{destDotPath}`

- Include a `connection-string` in the request header for authentication.
- The source and destination paths within containers are specified using dot notation.

## Responses
The function and `BlobService` provide responses for various scenarios:
- **Success**: `200 OK` with a message indicating successful copying.
- **Bad Request**: `400 Bad Request` when the connection string is missing.
- **Not Found**: `404 Not Found` for non-existent source containers or directories.
- **Failed Operation**: `417 Expectation Failed` for any copy operation failures.
- **Internal Server Error**: `500 Internal Server Error` for other errors with detailed messages.

## BlobFunction - File Copying Azure Function

### Overview
`BlobFunction` is another crucial Kotlin Azure Function in the `gov.cdc.dataexchange` package. It is specifically tailored for copying individual files within Azure Storage Containers. This function allows for more granular control over file copying, especially useful for situations where selective file transfer is required.

### Features
- **File Specific Operation**: Optimized for copying individual files.
- **Flexible Path Specification**: Allows specification of both source and destination paths using dot notation and HTTP headers.
- **Multiple Connection String Support**: Accepts either a common connection string or separate source and destination connection strings.
- **Copy Repetition Control**: The `n` header can specify the number of times the file should be copied, with each copy having a unique timestamp.

### Usage
Invoke the function using this route format:
`[HOST]/tool/file-copy/{srcContainer}/{dotPath}/{filename}`

- Include the following headers in the request:
    - `destination-container`: The name of the destination storage container.
    - `destination-folder-path` (optional): The path within the destination container.
    - `connection-string` (or `src-connection-string` and `dest-connection-string`): For authentication.
    - `n` (optional): The number of times to copy the file.

### Responses
- **Success**: `200 OK` with a message of successful file copying.
- **Bad Request**: `400 Bad Request` for missing or improperly used connection strings.
- **Not Found**: `404 Not Found` if the source file is not found.
- **Internal Server Error**: `500 Internal Server Error` for other errors, with a detailed message.

## BlobService Class
The `BlobService` class contains methods `copyStorage` and `copyFile` for handling blob storage operations:
- **copyStorage**: Handles bulk copying of blobs within a container. It lists all blobs in the source path and copies them to the destination path, logging each operation's duration.
- **copyFile**: Designed for copying individual files. It supports repeated copying (specified by the `n` parameter) and appends timestamps to filenames to avoid overwrites.


### Logging
Maintains detailed logs for each file copy operation, including source and destination paths and the outcome of the operation.

## Response Handling
Upon successful execution, the function returns an HTTP 200 OK status with a confirmation message. In cases of failure, such as invalid input, file not found, or server errors, it responds with the corresponding HTTP status code and an error message.

## Contributing
For contributions or queries, please reach out to the project maintainers at QEH3@cdc.gov. Adhere to coding standards and include tests for new features.

## License
The license details should be specified here. If proprietary, state that all rights are reserved by the CDC.