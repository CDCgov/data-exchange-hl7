import concurrent.futures
import requests
import threading
from time import time, strftime, localtime
import sys, os
'''
Possible header keys:
x-tp-message_type:  (CASE or ELR)
x-tp-route:         (COVID19-ELR, PHLIP_FLU, PHLIP_VPD, only for ELR)
x-tp-reporting_jurisdiction: (state FIPS number, only for ELR)
x-tp-orginal_file_name: will be filled in by script
'''
ENVIRONMENTS = ["dev", "tst", "stg"]
MESSAGE_TYPES = ["CASE", "ELR"]
ROUTES = ["COVID19-ELR", "PHLIP_FLU", "PHLIP_VPD"]
upload_url = "hl7ingress?filename="
thread_local = threading.local()

class FileUploader():
    def __init__(self, env, path, user_id, message_type, route, jurisdiction):
        self.path_to_files = path
        self.base_url = f'https://ocio-ede-{env}-hl7-svc-transport.azurewebsites.net/'
        self.user_id = user_id
        self.message_type = message_type
        self.route = route
        self.jurisdiction = jurisdiction
        self.upload_log = f"./{strftime('%Y-%m-%d', localtime())}_upload_log.txt"

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
            
            with open(self.upload_log, '+a') as log:
                if len(file_text) > 0:
                    # set header values
                    new_filename = f"upload-{self.user_id}-{norm_name}.txt"
                    header = {"x-tp-message_type": self.message_type, "x-tp-route": self.route, "x-tp-reporting_jurisdiction": self.jurisdiction, "x-tp-original_file_name": new_filename, "content-type": "text/plain"}

                    # upload the file
                    full_url = f'{self.base_url}{upload_url}{new_filename}'
                    session = self.get_session()
                    with session.post(url=full_url, data=file_text, headers=header) as resp:
                        if resp.status_code == 200:
                            log.write(f"{self.get_timestring()} - Success: file {file_name} --> {resp.text}\n")
                        else:
                            log.write(f"{self.get_timestring()} - Problem uploading file {file_name}. Status code {resp.status_code}, message {resp.text}\n")
                else:
                    log.write(f"{self.get_timestring()} - Unable to upload {file_name} - no content found\n")

    def normalize(self, name): 
        return name.replace(".", "_").replace(" ", "_").replace("-", "_").replace("(", "").replace(")", "").replace("&", "and").lower()        
            
    def get_timestring(self, epoch = None):
        if epoch:
            return strftime('%Y-%m-%d %H:%M:%S', localtime(epoch))
        else:
            return strftime('%Y-%m-%d %H:%M:%S', localtime())

if __name__ == "__main__":
    if len(sys.argv) != 5:
        print("Usage: python upload_messages.py <path to HL7 message files> <user_id> <environment> <message_type>")
    else:
        path = sys.argv[1]
        user_id = sys.argv[2]
        env = sys.argv[3].lower()
        message_type = sys.argv[4].upper().strip()
        route = ""
        jurisdiction = ""

        if message_type == "ELR":
            route = input("Please enter the route (one of COVID19-ELR, PHLIP_FLU, or PHLIP_VPD): ").upper().strip()
            jurisdiction = input("Please enter the jurisdiction code (2-digit integer): ").strip()

        if env not in ENVIRONMENTS:
            print(f"Error: environment must be one of these values: {ENVIRONMENTS}")
        elif message_type not in MESSAGE_TYPES:
            print(f"Error: message_type must be one of these values: {MESSAGE_TYPES}")
        elif message_type == "ELR" and route not in ROUTES:
            print(f"Error: route must be one of these values: {ROUTES}")
        elif message_type == "ELR" and (not jurisdiction.isdigit() or len(jurisdiction) != 2):
            print(f"Error: jurisdiction code must be a 2-digit integer")
        elif os.path.exists(path):
            file_list = [s for s in os.listdir(path) if s.lower().endswith(".hl7") or s.lower().endswith(".txt")]
            if len(file_list) == 0:
                print(f"No HL7 files found in {path}.")
            else:
                start_time = time()                
                uploader = FileUploader(env, path, user_id, message_type, route, jurisdiction)
                print(f"Starting upload of files at {uploader.get_timestring(start_time)} . . .")
                uploader.upload_all_files(file_list)
                end_time = time()
                duration = end_time - start_time
                print(f"DONE -- Upload of folder completed at {uploader.get_timestring(end_time)}.")
                print(f"Uploaded {len(file_list)} files in {duration} seconds")
                
        else:
            print(f"Path {path} does not exist.")
