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
ENVIRONMENTS = ["dev", "tst", "stg"]
upload_url = "hl7ingress?filename="
upload_log = "./upload_log.txt"
thread_local = threading.local()

class FileUploader():
    def __init__(self, env, path, user_id):
        self.path_to_files = path
        self.base_url = f'https://ocio-ede-{env}-hl7-svc-transport.azurewebsites.net/'
        self.user_id = user_id

    def get_session(self):
        if not hasattr(thread_local, "session"):
            thread_local.session = requests.Session()
        return thread_local.session

    def upload_all_files(self, file_list):      
        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            executor.map(self.upload_file, file_list)

    def upload_file(self, file_name):
            
            file_text = ""
            full_path = os.path.join(self.path_to_files, file_name)
            norm_name = self.normalize(file_name)

            # get plain text
            with open(full_path, 'r') as f:
                file_text = f.read()
            
            with open(upload_log, '+a') as log:
                if len(file_text) > 0:
                    # set header values
                    header = {"x-tp-message_type": "CASE", "x-tp-route": "PHLIP_VPD", "x-tp-reporting_jurisdiction": "06", "x-tp-original_file_name": file_name, "content-type": "text/plain"}

                    # upload the file
                    new_filename = f"upload-{self.user_id}-{norm_name}.txt"
                    full_url = f'{self.base_url}{upload_url}{new_filename}'

                    session = self.get_session()
                    with session.post(url=full_url, data=file_text, headers=header) as resp:
                        if resp.status_code == 200:
                            log.write(f"Success: file {file_name} --> {resp.text}\n")
                        else:
                            log.write(f'Problem uploading file {file_name}. Status code {resp.status_code}, message {resp.text}\n')
                else:
                    log.write(f'Unable to upload {file_name} - no content found\n')

    def normalize(self, name): 
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
                uploader = FileUploader(env, path, user_id)
                uploader.upload_all_files(file_list)
                duration = time.time() - start_time
                print("DONE -- Upload of folder completed.")
                print(f"Uploaded {len(file_list)} files in {duration} seconds")
                
        else:
            print(f"Path {path} does not exist.")
