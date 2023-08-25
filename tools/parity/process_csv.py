import csv
import argparse


parser = argparse.ArgumentParser(description='Parity Testing')
parser.add_argument('--csvFile',type=argparse.FileType('r',encoding='UTF-8'),help='The path to the csvFile')
parser.add_argument('--source',help='PHINMS or MERCURY S3')
parser.add_argument('--dateMessagesReceived',help='date HL7 messages received in the format yyyy/mm/dd',required=False)



class ParityTest:
    def __init__(self, file, source):
        self.csv = file
        self.source = source
        self.stateLevelFIPSCodes = {}#place holder for now
        self.countyLevelFIPSCodes = {}#place holder for now
        self.resultPHINMS = {}
        self.resultMercuryS3 = {}
    
    def build_state_FIPS(self):
        self.stateLevelFIPSCode = {"AL":1,"AK":2,"AZ":4,"AR":5,"CA":6,"CO":8,"CT":9,"DE":10,\
                                   "DC":11,"FL":12,"GA":13,"HI":15,"ID":16,"IL":17,"IN":18,"IA":19,\
                                   "KS":10,"KY":21,"LA":22,"ME":23,"MD":24,"MA":25,"MI":26,"MN":27,\
                                   "MS":28,"MO":29,"MT":30,"NE":31,"NV":32,"NH":33,"NJ":34,"NM":35,\
                                   "NY":36,"NC":37,"ND":38,"OH":39,"OK":40,"OR":41,"PA":42,"PR":72,\
                                   "RI":44,"SC":45,"SD":46,"TN":47,"TX":48,"UT":49,"VT":50,"VA":51,\
                                   "VI":78,"WA":53,"WV":54,"WI":55,"WY":56}
    def build_county_FIPS(Self):
        self.countyLevelFIPSCodes = {}


    def process_phinms(self):
        """Processes csv file for PHINMS and returns object 
        """
        with open(self.csv,newline='') as csvfile:
            
            reader = csv.DictReader(csvfile, delimiter=',')
            #skip csv headers
            next(reader, None)
            # initialize variables
            count = 0
            failed_msgs = 0

            # initialize output 
            output = self.resultPHINMS

            # process the csv file
            for row in reader:
                msg_obj = {}
                msg_obj["msg_file_name"] = row["msg_file_name"]
                msg_obj["case_id"] = row["case_id"]
                msg_obj["condition_code"] = row["condition_code"]
                msg_obj["phinms_recordid"] = row["phinms_recordid"]
                # check status of the message
                if row["msg_validation_status"] !="completed":
                    failed_msgs +=1
                msg_obj["status"] =  row["msg_validation_status"]
                
                count +=1
                output[count] = msg_obj
            output["total_msg_count"] = count

            if failed_msgs >0:
                output["failed_msgs"] = failed_msgs
        


        
    def process_mercury(self):
        """TBD
        """
        return


# Parse command line arguments
args = parser.parse_args()

csvFile = args.csvFile.name
source = args.source # PHINMS OR MERCURY S3 

# instance of 
pt = ParityTest(csvFile, source)

# build stateLevelFIPSCodes dictionary
#pt.build_state_FIPS()

# process PHINMS
pt.process_phinms()

print (pt.resultPHINMS)








