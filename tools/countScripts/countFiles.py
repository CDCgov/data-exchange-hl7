import os
import sys
import requests
import json
from azure.storage.blob import BlobServiceClient
from azure.cosmos import CosmosClient, PartitionKey

data = {}
jsonObj = {}
# Check we have command line argument
if len(sys.argv) < 2:
    print("Make sure you pass month/day as param. ex.: 03/25")
    exit(-1)

# Command line argument value
date_arg = sys.argv[1]


def get_blog_storage_list_count(container_name, blob_service_client, prefix):
    print(prefix)
    blobs = blob_service_client.get_container_client(container_name).walk_blobs(name_starts_with=prefix + "/",
                                                                                delimiter='/')
    # Get the count of blobs
    blob_count = len(list(blobs))

    return blob_count

# fetch from url
def fetch_and_prase_json(url):
    try:
        response = requests.get(url)
        response.raise_for_status()  # Raise an exception for bad status codes
        json_data = response.json()  # Parse JSON response
        return json_data
    except requests.exceptions.RequestException as e:
        print("Error fetching data:", e)
        return None

print("@@@")
print("@@@ Counting Upload API")
print("@@@")

# Replace with environment variable
account_name = "ocioededataexchangestg"
account_key = os.environ.get('UPLOAD_ACCT_KEY')
blob_service_client = BlobServiceClient(account_url=f"https://{account_name}.blob.core.windows.net",
                                        credential=account_key)
print("Counting HL7")
count_hl7 = get_blog_storage_list_count("aims-celr-hl7", blob_service_client, "2024/" + date_arg)
print(count_hl7)
data['upload_api_hl7_count'] = count_hl7

print("Counting CSV")
count_csv = get_blog_storage_list_count("aims-celr-csv", blob_service_client, "2024/" + date_arg)
print(count_csv)
data['upload_api_csv_count'] = count_hl7

print("@@@")
print("@@@ Counting routeingress")
print("@@@")

account_name = "ocioederoutingdatasastg"
account_key = os.environ.get('ROUTE_ACCT_KEY')
blob_service_client = BlobServiceClient(account_url=f"https://{account_name}.blob.core.windows.net",
                                        credential=account_key)

print("Counting HL7")
count_hl7 = get_blog_storage_list_count("routeingress", blob_service_client, "aims-celr-hl7/2024/" + date_arg)
print(count_hl7)
data['routeingress_hl7_count'] = count_hl7

print("Counting CSV")
count_csv = get_blog_storage_list_count("routeingress", blob_service_client, "aims-celr-csv/2024/" + date_arg)
print(count_csv)
data['routeingress_csv_count'] = count_csv

folders = ["hl7_out_recdeb", "hl7_out_redacted", "hl7_out_validation_report", "hl7_out_json", "hl7_out_lake_seg"]

print("@@@")
print("@@@ Counting hl7 outputs")
print("@@@")

for folder in folders:
    print("Counting  " + folder + "/2024/" + date_arg);
    count = get_blog_storage_list_count("routeingress", blob_service_client, folder + "/2024/" + date_arg)
    print(count)
    jsonObj['routeingress_'+'folder'] = count

data['hl7_outputs_count'] = jsonObj

print("@@@")
print("@@@ Counting Dead Letter")
print("@@@")

folders = ["hl7_out_recdeb", "hl7_out_redacted", "hl7_validation_report", "hl7_out_json", "hl7_lake_seg"]
jsonObj = {}
for folder in folders:
    print("Counting  " + folder + "/2024/" + date_arg)
    count = get_blog_storage_list_count("route-deadletter", blob_service_client, folder + "/2024/" + date_arg)
    print(count)
    jsonObj['reoute-deadletter_'+folder] = count

data['route-deadletter_count'] = jsonObj

print("@@@")
print("@@@ Counting ezdx")
print("@@@")
account_name = "stezdxstg"
account_key = os.environ.get('EZDX_ACCT_KEY')
blob_service_client = BlobServiceClient(account_url=f"https://{account_name}.blob.core.windows.net",
                                        credential=account_key)
folders = ("hl7_out_recdeb", "hl7_out_redacted", "hl7_validation_report", "hl7_out_json", "hl7_lake_seg")
jsonObj = {}
for folder in folders:
    print("Counting  " + folder + "/2024/" + date_arg);

    count = get_blog_storage_list_count("dex", blob_service_client, folder + "/2024/" + date_arg)
    if (count > 25):
        count = count - 25
    print(count)
    jsonObj['dex_'+folder] = count

data['dex_count'] = jsonObj

date = sys.argv[1].replace("/", "")
# echo "HL7 Reports"
json_data = fetch_and_prase_json(
    "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts?data_stream_id=aims-celr&data_stream_route=hl7&date_start=2024" + date + "T000000Z&date_end=2024" + date + "T235900Z&page_size=1&page_number=1")
if json_data:
    hl7_reports_count = json_data['summary']['total_items']

# echo "CSV Reports"
json_data = fetch_and_prase_json(
    "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts?data_stream_id=aims-celr&data_stream_route=csv&date_start=2024" + date + "T000000Z&date_end=2024" + date + "T235900Z&page_size=1&page_number=1")
if json_data:
    csv_reports_count = json_data['summary']['total_items']

# echo Invalid Message Reports:
json_data = fetch_and_prase_json(
    "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts/hl7/invalidStructureValidation?data_stream_id=aims-celr&data_stream_route=hl7&date_start=2024" + date + "T000000Z&date_end=2024" + date + "T235900Z")
if json_data:
    invalid_msg_report = json_data['counts']

# Initialize the Cosmos client
endpoint = "https://ocio-ede-dev-routing-db-cosmos-account.documents.azure.com:443/"
cosmos_key = os.environ.get('COSMOS_KEY')
client = CosmosClient(endpoint, cosmos_key)

# Create a database if it doesn't exist
database_name = "dex-routing"
database_client = client.create_database_if_not_exists(id=database_name)

# Create a container if it doesn't exist
container_name = "count_files"
container_client = database_client.create_container_if_not_exists(
    id=container_name,
    partition_key=PartitionKey(path="/partition_key")
)

# Define your data
data['hl7_reports_count'] = hl7_reports_count
data["CSV_reports_count"] = csv_reports_count
data["Invalid_msg_report"] = invalid_msg_report

# Create and save the document
container_client.create_item(body=data, enable_automatic_id_generation=True)
print()
