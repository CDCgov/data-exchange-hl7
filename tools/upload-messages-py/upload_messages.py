import json
import requests
import sys, os
from uuid import NAMESPACE_URL, uuid5
'''
Possible header keys:
x-tp-message_type:  (CASE or ELR)
x-tp-route:         (COVID19-ELR, only for ELR)
x-tp-reporting_jurisdiction: (state FIPS number, only for ELR)
x-tp-orginal_file_name:
'''
user_id = ""
base_url = 'https://cloud-svc-transport-dev-eastus.azurewebsites.net/'
upload_url = "hl7ingress?filename="


def upload_file(path_to_file):
    file_text = ""
    original_name = os.path.basename(path_to_file)
    # get plain text
    with open(path_to_file, 'r', encoding='utf-8') as f:
        file_text = f.read()
    
    if len(file_text) > 0:
        # set parameters
        header = {"x-tp-message_type": "CASE", "x-tp-orginal_file_name": original_name, "content-type": "text/plain"}

        # upload the file
        guid = uuid5(NAMESPACE_URL, path_to_file)
        new_filename = f"upload-{user_id}-{guid}.txt"
        full_url = f'{base_url}{upload_url}{new_filename}'
        resp = requests.post(url=full_url, data=file_text, headers=header)
        if resp.status_code == 200:
            print(f"Success: file {path_to_file} --> {resp.text}")
        else:
            print(f'Problem uploading file {original_name}. Status code {resp.status_code}, message {resp.text}')
    else:
        print(f'Unable to upload {original_name} - no content found')
        

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python upload_messages.py <path to HL7 message files> <user_id>")
    else:
        path = sys.argv[1]
        user_id = sys.argv[2]
        if os.path.exists(path):
            file_list = [s for s in os.listdir(path) if s.lower().endswith(".hl7") or s.lower().endswith(".txt")]
            if len(file_list) == 0:
                print(f"No HL7 files found in {path}.")
            else:
                for f in file_list:
                    upload_file(os.path.join(path, f))
                print("DONE -- Upload of folder completed.")
        else:
            print(f"Path {path} does not exist.")
