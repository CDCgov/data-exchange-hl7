'''
Script that will take a csv MMG and output JSON MMG that adheres to the MMGAT JSON format.
'''
import csv
import json
import sys
import os
import re
from copy import deepcopy
from uuid import uuid4
from datetime import datetime
'''
MMG fields appear in this order in the CSV:
0 PHIN Variable ID
1 Label/Short Name
2 Description
3 Data Type
4 CDC Priority
5 May Repeat
6 Value Set Name
7 Value Set Code
8 HL7 Message Context
9 HL7 Data Type
10 HL7 Optionality
11 HL7 Implementation Notes
'''
FIELDS =  ['PHIN Variable ID', 
            'Label/Short Name', 
            'Description', 
            'Data Type', 
            'CDC Priority', 
            'May Repeat', 
            'Value Set Name', 
            'Value Set Code', 
            'HL7 Message Context', 
            'HL7 Data Type', 
            'HL7 Optionality', 
            'HL7 Implementation Notes']

# template files used to create the json structures
TEMPLATE = './mmg_template.json'  # template has 3 pre-defined blocks: Message Header, Subject Related, and Case Related.
ELEMENT = './mmg_element.json'    # we will add each element into the appropriate block using the element template.
PRIORITIES = {"R": 1, "P": 2, "O": 3}

def output_mmg_json(csv_filename):
    outfile = csv_filename.replace(".csv", ".json")
    json_mmg = {}
    metadata = []
    msh_block, pid_block, case_block = (None, None, None)
    counts = {"MSH": 0, "PID": 0, "OBR": 0, "OBX": 0, "OBR-4": 0}

    # load the template building blocks
    with open(TEMPLATE, 'r') as t:
        json_mmg = json.load(t)
        json_mmg["id"] = str(uuid4())
        blocks = json_mmg.get("blocks")
        for b in blocks:
            b["guideId"] = json_mmg["id"]
            if b.get("name") == "Message Header":
                msh_block = b
            elif b.get("name") == "Subject Related":
                pid_block = b
            elif b.get("name") == "Case Related":
                case_block = b

    with open(ELEMENT, 'r') as e:
        element = json.load(e)
        element["guideId"] = json_mmg["id"]

    # read the csv file and store metadata (lines before actual spec) in a separate list
    # store the actual spec info (stuff after the header line) in json block elements
    with open(csv_filename, 'r', encoding='utf-8', newline='') as f:
        found_header = False   
        csv_mmg = csv.reader(f)
        for row in csv_mmg:
            if found_header:
                segment, sequence, component = get_hl7_context(row[8])                  
                counts[segment] += 1
                if segment == "OBR" and sequence == "4":
                    counts["OBR-4"] += 1
                ordinal = counts[segment]
                if segment == "MSH":
                    this_block = msh_block
                    if sequence == "21":
                        json_mmg["profileIdentifier"] = get_profile_id(row[11]) + json_mmg["profileIdentifier"]
                elif segment == "PID":
                    this_block = pid_block
                else:
                    this_block = case_block
                    ordinal = counts["OBR"] + counts["OBX"]
                new_element = deepcopy(element)
                add_element(this_block, new_element, ordinal, counts["OBR-4"], segment, sequence, component, row)
            else:
                metadata.append(row)
                if FIELDS == row[:12]:
                    found_header = True

    # add the metadata about the guide
    json_mmg["publishVersion"] = get_version(metadata[0])
    json_mmg["publishDate"] = get_pub_date(metadata[0])
    json_mmg["name"] = get_name(csv_filename)
    json_mmg["shortName"] = json_mmg["name"]
    json_mmg["description"] = get_description(metadata)
    with open(outfile, 'w') as f:
        json.dump(json_mmg, f, indent=2)

def get_hl7_context(row_data):
    pattern = "([A-Z]{3})-([0-9]{1,2})\.?([0-9]{0,2})"   # e.g. "PID-5.7" or "OBR-4"
    match = re.search(pattern, row_data)
    if match:
        segment = match.group(1)
        sequence = match.group(2)
        component = match.group(3) or "-1"

    elif "OBX" in row_data:
        segment = "OBX"
        sequence = "5"
        component = "-1"

    return segment, sequence, component

def add_element(block, new_element, ordinal, obr_count, segment, sequence, component, row_data):
    new_element["id"] = str(uuid4())
    new_element["blockId"] = block["id"]
    new_element["ordinal"] = int(ordinal)
    new_element["name"] = row_data[1]
    new_element["description"] = row_data[2].strip()
    new_element["dataType"] = row_data[3]
    new_element["legacyPriority"] = row_data[4]
    new_element["priority"] = PRIORITIES[row_data[4]]
    new_element["mayRepeat"] = row_data[5] or "N"
    new_element["valueSetCode"] = row_data[7]
    hl7_mapping = new_element["mappings"]["hl7v251"]
    hl7_mapping["legacyIdentifier"] = row_data[0]
    hl7_mapping["identifier"] = row_data[0]    
    hl7_mapping["messageContext"] = row_data[8].strip()
    hl7_mapping["dataType"] = row_data[9]
    hl7_mapping["segmentType"] = segment
    hl7_mapping["obrPosition"] = obr_count
    hl7_mapping["fieldPosition"] = int(sequence)
    hl7_mapping["componentPosition"] = int(component)
    hl7_mapping["usage"] = row_data[10]
    hl7_mapping["implementationNotes"] = row_data[11].strip()

    if new_element["mayRepeat"] != "N":
         max_reps = new_element["mayRepeat"][-1]
         if max_reps == "Y": max_reps = "*"
    else:
        max_reps = "1"

    if new_element["legacyPriority"] == "R":
        min_reps = "1"
    else:
        min_reps = "0"

    hl7_mapping["cardinality"] = f'[{min_reps}..{max_reps}]'
    block["elements"].append(new_element)
    
def get_profile_id(msh21_desc):
    print(msh21_desc)
    p = "(\:\s.{1})(.+)(\^PHINMsgMapID)"
    p_match = re.search(p, msh21_desc)
    if p_match:
        return p_match.group(2)

def get_version(version_row):
    version_text = version_row[0]
    p = "[0-9]+\.[0-9]+\.?[0-9]?"
    v_match = re.search(p, version_text)
    if v_match:
        return v_match.group()
    return ""

def get_pub_date(version_row):
    version_text = version_row[0]
    p = "[0-9]{1,2}\/[0-9]{1,2}\/[0-9]{4}"
    d_match = re.search(p, version_text)
    if d_match:
        d = d_match.group()
        pub_date = datetime.strptime(d, "%m/%d/%Y")
        return pub_date.isoformat()
    return ""

def get_name(filename):
    name = str(os.path.basename(filename))
    return name[:-4]

def get_description(metadata):
    for m_row in metadata:
        desc_data = [t for t in m_row if "This Message Mapping" in t]
        if desc_data:
            return desc_data[0].strip()


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python jsonify_mmg.py <path to csv files (no spaces)>")
    elif not os.path.exists(TEMPLATE) or not os.path.exists(ELEMENT):
        print("Required template file(s) are missing.")
    else:
        path = sys.argv[1]
        if os.path.exists(path):
            csv_list = [s for s in os.listdir(path) if s.endswith(".csv")]
            if len(csv_list) == 0:
                print(f"No csv files found in {path}.")
            else:
                for c in csv_list:
                    output_mmg_json(os.path.join(path, c))
                    print(f"MMG json for {c} has been output to {path}.")
        else:
            print(f"Path {path} does not exist.")
