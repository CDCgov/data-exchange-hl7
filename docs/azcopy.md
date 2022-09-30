# Copy files to ADLS BLOB Storage container

azcopy.md provides instructions to copy data into a ADLS (Azure Data Lake Storage Gen2) storage account using either AD (Active Directory) Credentials or a SAS token. 

We use azcopy to test the connectivity to the following storage accounts
- DEX Subscription > ADLS Storage Account
- EDAV Subscription > ADLS General Storage Account
- EDAV Subscription > ADLS DEX Storage Account

Requires
- `STORAGE_ACCOUNT`
- `CONTAINER_NAME`
- `SAS_TOKEN`

## AD Credentials with `azcopy login`

1. Download [azcopy](https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azcopy-v10#download-azcopy)
2. Run azcopy login	
  a. Use a web browser to open the page https://microsoft.com/devicelogin and enter `<CODE>` to authenticate.
  b. Enter your AD username and password to authorize.
3. `azcopy copy <FILE> "https://<STORAGE_ACCOUNT>.blob.core.windows.net/<CONTAINER_NAME>/"`


## SAS token Credential

1. Download [azcopy](https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azcopy-v10#download-azcopy)
2. `azcopy copy <FILE> "https://<STORAGE_ACCOUNT>.blob.core.windows.net/CONTAINER_NAME?<SAS_TOKEN>"`

## References
Reference URL to use ADLS copy command
https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azcopy-v10		

Reference URL to generate SAS tokens
https://learn.microsoft.com/en-us/azure/cognitive-services/translator/document-translation/create-sas-tokens?tabs=Containers
