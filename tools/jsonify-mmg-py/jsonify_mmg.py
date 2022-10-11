'''
Script that will take a csv MMG and output JSON MMG that adheres to the MMGAT JSON format.

Header line should use the same column labels as listed below, with minor variations allowed
  (e.g., 'PHIN Variable' instead of 'PHIN Variable ID' but not 'HL7 Usage' instead of 'HL7 Optionality').
The column labels do not need to be in the same order as below.
Any additional header labels and corresponding columns will be ignored.

In order for 'obrPosition' to be correct, it may be necessary to edit the original MMG so that 
1) the 'PERSUBJ^Person Subject' OBR is listed before the subject/patient-related fields and
2) the 'NOTF^Individual Case Notificaion' OBR is listed AFTER the subject/patient-related fields 
  and BEFORE the case-related fields.

In order to determine the version number and publication date, the "VERSION:" line must appear at or near the top
of the CSV, before the header line.

If the MMG contains START and END block labels, these will be recognized by the script.
Corresponding blocks will be added to the JSON MMG if the START label topic is not included already in the base 
template, which has pre-existing blocks for Message Header, Subject Related, and Case Related fields.

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

MMG fields (usually appear in this order) in the CSV:

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
BLOCK = './mmg_block.json'        # in case we need additional blocks beyond Message Header, Patient Related, and Case Related

PRIORITIES = {"R": 1, "P": 2, "O": 3}
LABELS = ["Message Header", "Subject Related", "Case Related"]


def output_mmg_json(csv_filename):
    outfile = csv_filename.replace(".csv", ".json")
    json_mmg = {}
    metadata = []
    msh_block, pid_block, case_block = (None, None, None)

    obr_4_count = 0


    # load the template building blocks
    with open(TEMPLATE, 'r') as t:
        json_mmg = json.load(t)
        json_mmg["id"] = str(uuid4())
        blocks = json_mmg.get("blocks")
        for b in blocks:
            b["guideId"] = json_mmg["id"]
            if b.get("name") == LABELS[0]:
                msh_block = b
            elif b.get("name") == LABELS[1]:
                pid_block = b
            elif b.get("name") == LABELS[2]:
                case_block = b

    with open(ELEMENT, 'r') as e:
        element = json.load(e)
        element["guideId"] = json_mmg["id"]

    with open(BLOCK, 'r') as bl:
        block = json.load(bl)
        block["guideId"] = json_mmg["id"]

    # read the csv file and store metadata (lines before actual spec) in a separate list
    # store the actual spec info (stuff after the header line) in json block elements
    with open(csv_filename, 'r', encoding='utf-8', newline='') as f:
        found_header = False   
        header_map = None
        label = None
        new_block = None
        csv_mmg = csv.reader(f)
        for row in csv_mmg:
            if found_header:
                if is_block_start_label(row[0]):
                    label = get_block_label(row[0])
                    print(f'Start {label}')
                    if label not in LABELS:
                        new_block = deepcopy(block)
                        new_block["id"] = str(uuid4())
                        new_block["name"] = label
                        if "Repeating" in label:
                            new_block["type"] = "RepeatParentChild"
                elif is_block_end_label(row[0]):
                    print(f'End {label}')
                    if new_block:
                        new_block["ordinal"] = len(json_mmg["blocks"]) + 1                        
                        json_mmg["blocks"].append(new_block)
                        new_block = None
                else:
                    segment, sequence, component = get_hl7_context(row[header_map['HL7 Message Context']])
                    if len(segment) > 0:            
                        if segment == "OBR" and sequence == "4":
                            obr_4_count += 1
                        if segment == "MSH":
                            this_block = msh_block
                            if sequence == "21":
                                json_mmg["profileIdentifier"] = get_profile_id(row[header_map['HL7 Implementation Notes']]) + json_mmg["profileIdentifier"]
                        elif segment == "PID":
                            this_block = pid_block
                        else:
                            if new_block:
                                this_block = new_block
                            else:
                                this_block = case_block
                        ordinal = len(this_block["elements"]) + 1
                        new_element = deepcopy(element)
                        add_element(this_block, new_element, ordinal, obr_4_count, segment, sequence, component, row, header_map)
                    
            else:
                metadata.append(row)
                if is_header_row(row):
                    found_header = True
                    header_map = map_header_indices(row)

    # add the metadata about the guide
    json_mmg["publishVersion"] = get_version(metadata[0])
    json_mmg["publishDate"] = get_pub_date(metadata[0])
    json_mmg["name"] = get_name(csv_filename)
    json_mmg["shortName"] = json_mmg["name"]
    json_mmg["description"] = get_description(metadata)
    with open(outfile, 'w') as f:
        json.dump(json_mmg, f, indent=2)


''' 
header row may not be an exact match to the fields we have defined,
but should contain the same words more or less
'''
def is_header_row(row_data):
    match_count = 0
    for h in row_data:
        h = h.strip()
        if h:
            for f in FIELDS:
                if h in f or f in h:
                    match_count += 1
    return match_count >= len(FIELDS)

def map_header_indices(header_row):
    header_map = {}
    for idx, item in enumerate(header_row):
        item = item.strip()
        if item:
            field_match = [f for f in FIELDS if item in f or f in item]
            if len(field_match) > 0:
                exact_match = [f for f in field_match if f == item]
                if len(exact_match) > 0:
                    header_map[exact_match[0]] = idx
                else:
                    header_map[field_match[0]] = idx
    if len(header_map) != len(FIELDS):
        print(header_map)
        raise Exception("Fields are missing from the header row. Unable to parse csv.")
    return header_map

def is_block_start_label(data):
    trimmed_data = data.strip()
    if trimmed_data.startswith("START:"):
        return True
    return False

def is_block_end_label(data):
    trimmed_data = data.strip()
    if trimmed_data.startswith("END:"):
        return True
    return False

def get_block_label(data):
    trimmed_data = data.strip().upper()
    label = trimmed_data.replace("START:", '').replace("SECTION", '').strip()
    if "MESSAGE" in label:
        return LABELS[0]
    if "PATIENT" in label or "DEMOGRAPHIC" in label:
        return LABELS[1]
    if "CASE" in label or "CLINICAL" in label or "EPIDEMIOLOGIC" in label:
        return LABELS[2]
    return label.title()

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
    else:
        # row may be empty
        segment = ""
        sequence = ""
        component = ""
    return segment, sequence, component

def add_element(block, new_element, ordinal, obr_count, segment, sequence, component, row_data, header_map):
    new_element["id"] = str(uuid4())
    new_element["blockId"] = block["id"]
    new_element["ordinal"] = int(ordinal)
    new_element["name"] = row_data[header_map['Label/Short Name']].strip()
    new_element["description"] = row_data[header_map['Description']].strip()
    new_element["dataType"] = row_data[header_map['Data Type']].strip()
    new_element["legacyPriority"] = row_data[header_map['CDC Priority']].strip()
    new_element["priority"] = PRIORITIES[row_data[header_map['CDC Priority']].strip()]
    new_element["mayRepeat"] = row_data[header_map['May Repeat']].strip() or "N"
    new_element["valueSetCode"] = row_data[header_map['Value Set Code']].strip()
    hl7_mapping = new_element["mappings"]["hl7v251"]
    hl7_mapping["legacyIdentifier"] = row_data[header_map['PHIN Variable ID']].strip()
    hl7_mapping["identifier"] =  hl7_mapping["legacyIdentifier"]
    hl7_mapping["messageContext"] = row_data[header_map['HL7 Message Context']].strip()
    hl7_mapping["dataType"] = row_data[header_map['HL7 Data Type']].strip()
    hl7_mapping["segmentType"] = segment
    hl7_mapping["obrPosition"] = obr_count
    hl7_mapping["fieldPosition"] = int(sequence)
    hl7_mapping["componentPosition"] = int(component)
    hl7_mapping["usage"] = row_data[header_map['HL7 Optionality']].strip()
    hl7_mapping["implementationNotes"] = row_data[header_map['HL7 Implementation Notes']].strip()

    if block["type"] == "RepeatParentChild":
        if ordinal == 1:
            hl7_mapping["repeatingGroupElementType"] = "PRIMARY/PARENT"
        else:
            hl7_mapping["repeatingGroupElementType"] = "CHILD"

    if new_element["mayRepeat"] != "N":
         max_reps = new_element["mayRepeat"][-1]
         new_element["isRepeat"] = True
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
    p = "(\:\s.{1})(.+)(\^PHINMsgMapID)"
    p_match = re.search(p, msh21_desc)
    if p_match:
        return p_match.group(2)

def get_version(version_row):
    version_text = " ".join(version_row)
    p = "[0-9]+\.[0-9]+\.?[0-9]?"
    v_match = re.search(p, version_text)
    if v_match:
        return v_match.group()
    return ""

def get_pub_date(version_row):
    version_text = " ".join(version_row)
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
