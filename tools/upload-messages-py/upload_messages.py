import requests
import sys, os
'''
Possible header keys:
x-tp-message_type:  (CASE or ELR)
x-tp-route:         (COVID19-ELR, only for ELR)
x-tp-reporting_jurisdiction: (state FIPS number, only for ELR)
x-tp-orginal_file_name:
'''
user_id = ""
env = "dev"
ENVIRONMENTS = ["dev", "tst", "stg"]
upload_url = "hl7ingress?filename="

def upload_files(path_to_files, file_list):
    base_url = f'https://ocio-ede-{env}-hl7-svc-transport.azurewebsites.net/'

    for file_name in file_list:
        file_text = ""
        full_path = os.path.join(path_to_files, file_name)
        norm_name = normalize(file_name)

        # get plain text
        with open(full_path, 'r') as f:
            file_text = f.read()
        
        if len(file_text) > 0:
            # set header values
            header = {"x-tp-message_type": "CASE", "x-tp-original_file_name": file_name, "content-type": "text/plain"}

            # upload the file
            new_filename = f"upload-{user_id}-{norm_name}.txt"

            full_url = f'{base_url}{upload_url}{new_filename}'
            resp = requests.post(url=full_url, data=file_text, headers=header)

            if resp.status_code == 200:
                print(f"Success: file {file_name} --> {resp.text}")
            else:
                print(f'Problem uploading file {file_name}. Status code {resp.status_code}, message {resp.text}')
        else:
            print(f'Unable to upload {file_name} - no content found')

def normalize(name): 
    return name.replace(".", "_").replace(" ", "_").replace("-", "_").replace("(", "").replace(")", "").replace("&", "and").lower()        
          

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python upload_messages.py <path to HL7 message files> <user_id> <environment>")
    else:
        path = sys.argv[1]
        user_id = sys.argv[2]
        env = sys.argv[3].lower()

        if (env not in ENVIRONMENTS):
            print(f"Error: environment must be one of these values: {ENVIRONMENTS}")
        elif os.path.exists(path):
            file_list = [s for s in os.listdir(path) if s.lower().endswith(".hl7") or s.lower().endswith(".txt")]
            if len(file_list) == 0:
                print(f"No HL7 files found in {path}.")
            else:
                upload_files(path, file_list)
                print("DONE -- Upload of folder completed.")
        else:
            print(f"Path {path} does not exist.")
