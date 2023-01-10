'''
Script that will take a csv of a GenV2-based MMG and output JSON MMG that adheres to the MMGAT JSON format.
Header line should use the same column labels as listed below, with minor variations allowed
  (e.g., 'DE Identifier' instead of 'DE Identifier Sent in HL7 Message' but not 'HL7 Optionality' instead of 'HL7 Usage').
The column labels do not need to be in the same order as below.
Any additional header labels and corresponding columns will be ignored.

In order to determine the version number and publication date, the "VERSION:" line must appear at or near the top
of the CSV, before the header line.

If the MMG contains START and END block labels, these will be recognized by the script.
Corresponding blocks will be added to the JSON MMG if the START label topic is not included already in the base 
template, which has a pre-existing block for Message Header.
'''
import csv
import json
import sys
import os
import re
from copy import deepcopy
from uuid import NAMESPACE_URL, uuid5
from datetime import datetime

FIELDS = ['PHIN Variable',
        'PHIN Variable Code System',
        'Data Element (DE) Name',
        'DE Identifier Sent in HL7 Message',
        'DE Code System',
        'Data Element Description',
        'Data Type',
        'CDC Priority',
        'May Repeat',
        'Value Set Name (VADS Hyperlink)',
        'Value Set Code',
        'HL7 Message Context',
        'HL7 Data Type',
        'HL7 Usage',
        'HL7 Cardinality',
        'HL7 Implementation Notes',
        'Repeating Group Element',
        'Sample Segment']

# template files used to create the json structures
TEMPLATE = './mmg_template.json'  # template has 3 pre-defined blocks: Message Header, Subject Related, and Case Related.
ELEMENT = './mmg_element.json'    # we will add each element into the appropriate block using the element template.
BLOCK = './mmg_block.json'        # in case we need additional blocks beyond Message Header, Patient Related, and Case Related

PRIORITIES = {"R": "R", "P": "1", "O": "3", "U": "4"}
LABELS = ["Message Header"]

def output_mmg_json(csv_filename):
    outfile = csv_filename.replace(".csv", ".json")
    json_mmg = {}
    metadata = []
    msh_block = None
    epi_block = None
    obr_count = 1

    # load the template building blocks
    with open(TEMPLATE, 'r') as t:
        json_mmg = json.load(t)
        json_mmg["id"] = str(uuid5(NAMESPACE_URL, os.path.basename(outfile)))
        blocks = json_mmg.get("blocks")
        msh_block = blocks[0]
        # there are 2 more blocks that we don't need for gen v2 mmgs - delete them
        blocks.pop(1)
        blocks.pop(1)

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
                        # there may be a case where an empty block was defined (e.g., a section header).
                        # in that case, we need to add it to the result before continuing.
                        new_block = add_block_to_result(json_mmg, new_block)
                        # create a fresh new block
                        new_block = deepcopy(block)
                        new_block["id"] = str(len(json_mmg["blocks"]) + 1)
                        new_block["name"] = label
                        new_block["startingDescription"] = row[0]
                        if label.startswith("Epidemiologic Information"):
                            # sometimes we need to go back and add stuff to the main 'epi block' (if there is an unlabeled section)
                            epi_block = new_block
                elif is_block_end_label(row[0]):
                    print(f'End {label}')
                    if new_block:
                        new_block["endingDescription"] = row[0]
                        new_block = add_block_to_result(json_mmg, new_block)
                else:
                    segment, sequence, component = get_hl7_context(row, header_map)
                    if len(segment) > 0:            
                        if segment == "OBR" and sequence == "2":
                            obr_count += 1
                        if segment == "MSH":
                            this_block = msh_block
                            if sequence == "21":
                                json_mmg["profileIdentifier"] = get_profile_id(row[header_map['HL7 Implementation Notes']])
                        else:
                            if new_block:
                                this_block = new_block
                            elif epi_block:
                                this_block = epi_block
                        this_block["guideId"] = json_mmg["id"]
                        ordinal = len(this_block["elements"]) + 1
                        new_element = deepcopy(element)
                        add_element(this_block, new_element, ordinal, obr_count, segment, sequence, component, row, header_map)
                    
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
    json_mmg["description"] = get_description(metadata) or ""
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
    if ":" in label:
        label = label[:label.index(":")].strip()
    return label.title()

def get_hl7_context(row, header_map):
    
    pattern = "([A-Z]{3})-([0-9]{1,2})\.?([0-9]{0,2})"   # e.g. "PID-5.7" or "OBR-4"
    if row[header_map['DE Identifier Sent in HL7 Message']].strip().startswith('N/A'):
        row_data = row[header_map['DE Identifier Sent in HL7 Message']].strip()
    else:
        row_data = row[header_map['HL7 Message Context']].strip()

    matches = re.findall(pattern, row_data)
    if matches:
        best_match = matches[-1] # take the last one
        segment, sequence, component = best_match
        component = component or "-1"
    elif "OBX" in row_data:
        segment, sequence, component = ("OBX", "5", "-1")
    else:
        # row may be empty
        segment, sequence, component = ("","","")

    return segment, sequence, component

def add_element(block, new_element, ordinal, obr_count, segment, sequence, component, row_data, header_map):
    id = row_data[header_map['PHIN Variable']].strip()
    new_element["id"] = str(uuid5(NAMESPACE_URL, id))
    new_element["blockId"] = block["id"]
    new_element["ordinal"] = int(ordinal)
    new_element["name"] = row_data[header_map['Data Element (DE) Name']].strip()
    new_element["description"] = row_data[header_map['Data Element Description']].strip()
    new_element["dataType"] = row_data[header_map['Data Type']].strip()
    new_element["legacyCodeSystem"] = row_data[header_map['PHIN Variable Code System']].strip()
    new_element["codeSystem"] = row_data[header_map['DE Code System']].strip()
    new_element["legacyPriority"] = row_data[header_map['CDC Priority']].strip()
    new_element["priority"] = PRIORITIES[row_data[header_map['CDC Priority']].strip().upper()]
    new_element["mayRepeat"] = row_data[header_map['May Repeat']].strip() or "N"
    new_element["valueSetCode"] = row_data[header_map['Value Set Code']].strip()
    hl7_mapping = new_element["mappings"]["hl7v251"]
    hl7_mapping["legacyIdentifier"] = id
    hl7_mapping["identifier"] =  row_data[header_map['DE Identifier Sent in HL7 Message']].strip()
    hl7_mapping["messageContext"] = row_data[header_map['HL7 Message Context']].strip()
    hl7_mapping["dataType"] = row_data[header_map['HL7 Data Type']].strip()
    hl7_mapping["segmentType"] = segment
    hl7_mapping["obrPosition"] = obr_count
    hl7_mapping["fieldPosition"] = int(sequence)
    hl7_mapping["componentPosition"] = int(component)
    hl7_mapping["usage"] = row_data[header_map['HL7 Usage']].strip()
    hl7_mapping["cardinality"] = row_data[header_map['HL7 Cardinality']].strip()
    hl7_mapping["repeatingGroupElementType"] = row_data[header_map['Repeating Group Element']].strip()
    if hl7_mapping["repeatingGroupElementType"] == "YES":
        block["type"] = "Repeat"
    elif "PRIMARY" in hl7_mapping["repeatingGroupElementType"] or "CHILD" in hl7_mapping["repeatingGroupElementType"]:
        block["type"] = "RepeatParentChild"
    hl7_mapping["implementationNotes"] = row_data[header_map['HL7 Implementation Notes']].strip()
    hl7_mapping["sampleSegment"] = row_data[header_map['Sample Segment']].strip()
    
    block["elements"].append(new_element)


def get_profile_id(msh21_desc):
    reps = msh21_desc.split('~')
    profile = reps[-1].split("'")
    return profile[0]

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
            
def add_block_to_result(json_mmg, new_block):
    if new_block:
        new_block["ordinal"] = len(json_mmg["blocks"]) + 1                        
        json_mmg["blocks"].append(new_block)
        new_block = None
    return new_block

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


