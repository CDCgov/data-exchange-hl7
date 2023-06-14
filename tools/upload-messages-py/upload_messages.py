import concurrent.futures
import requests
import threading
import time
import sys, os
'''
Possible header keys:
x-tp-message_type:  (CASE or ELR)
x-tp-route:         (COVID19-ELR, PHLIP_FLU, PHLIP_VPD, only for ELR)
x-tp-reporting_jurisdiction: (state FIPS number, only for ELR)
x-tp-orginal_file_name: will be filled in by script
'''
user_id = ""
env = "dev"
ENVIRONMENTS = ["dev", "tst", "stg"]
upload_url = "hl7ingress?filename="
base_url = f'https://ocio-ede-{env}-hl7-svc-transport.azurewebsites.net/'
path_to_files = ""
thread_local = threading.local()

def get_session():
    if not hasattr(thread_local, "session"):
        thread_local.session = requests.Session()
    return thread_local.session

def upload_all_files(path, file_list):
    path_to_files = path
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
        executor.map(upload_file, file_list)

def upload_file(file_name):
        
        file_text = ""
        full_path = os.path.join(path_to_files, file_name)
        norm_name = normalize(file_name)

        # get plain text
        with open(full_path, 'r') as f:
            file_text = f.read()
        
        if len(file_text) > 0:
            # set header values
            header = {"x-tp-message_type": "CASE", "x-tp-route": "PHLIP_VPD", "x-tp-reporting_jurisdiction": "06", "x-tp-original_file_name": file_name, "content-type": "text/plain"}

            # upload the file
            new_filename = f"upload-{user_id}-{norm_name}.txt"
            full_url = f'{base_url}{upload_url}{new_filename}'

            session = get_session()
            with session.post(url=full_url, data=file_text, headers=header) as resp:
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
                start_time = time.time()
                upload_all_files(path, file_list)
                duration = time.time() - start_time
                print("DONE -- Upload of folder completed.")
                print(f"Uploaded {len(file_list)} files in {duration} seconds")
                
        else:
            print(f"Path {path} does not exist.")
