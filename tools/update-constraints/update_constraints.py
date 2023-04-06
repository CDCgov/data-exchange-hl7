
import sys
import os
import defusedxml.ElementTree as ET

reg_anychar = "^(?!\s*$).+"
reg_empty = "^$"
reg_all0s = "^[0]{4,24}$"
reg_all9s = "^[9]{4,24}$"
reg_DT = "^(\d{4}|\d{6}|\d{8})$"
reg_DTM = "^(\d{4}|\d{6}|\d{8}|\d{10}|\d{12}|\d{14}|\d{14}\.\d|\d{14}\.\d{2}|\d{14}\.\d{3}|\d{14}\.\d{4})([+-]\d{4})?$"
reg_DTM8 = "^(\d{8}|\d{10}|\d{12}|\d{14}|\d{14}\.\d|\d{14}\.\d{2}|\d{14}\.\d{3}|\d{14}\.\d{4})([+-]\d{4})?$"
reg_DTM_14 = "^(\d{14}|\d{14}\.\d|\d{14}\.\d{2}|\d{14}\.\d{3}|\d{14}\.\d{4})([+-]\d{4})?$"
reg_SN_Separator = "^[-+\/.:]$"
reg_SN_Comparator = "^&lt;=$|^&gt;=$|^&lt;&gt;$|^&lt;$|^&gt;$|^=$"

def fix_descriptions(filePath):
    xmldoc = ET.parse(filePath)
    constraints = xmldoc.findall(".//Constraint")
    for c in constraints:
        for child in c:
            if child.tag == "Description":
                desc_text = normalize(child.text)
                if "regular expression" in desc_text:
                    desc_text = replace_regex(desc_text)
                child.text = desc_text
    xmldoc.write(filePath)

def fix_mappings(filePath):
    xmldoc = ET.parse(filePath)
    version = get_version(xmldoc)
    # version 2 does not need to have its mappings fixed
    if version == 3:
        obx_mappings = xmldoc.findall(".//Segment/DynamicMapping/Mapping/Case")
        for case in obx_mappings:
            dt = case.get("Datatype")
            if "DT_" in dt or "DTM_" in dt:
                dt = "TS_Opt9s"
                case.set("Datatype", dt) 
        xmldoc.write(filePath)

def get_version(xmldoc):
    msg = xmldoc.find("./Messages/Message[@Name]")
    if msg:
        name = msg.get("Name")
        if "3." in name:
            return 3
    return 2

def normalize(text):
    norm_text = text.strip().replace("  ", " ").replace(") contain ", ") contains ").replace(". ", ", ")
    norm_text = norm_text.replace(", )", ". )")
    norm_text = norm_text.replace("Element 'xxx'", "The value")
    return norm_text

def replace_regex(text):
    match = "match the regular expression"
    be_a_date = "follow the date/time pattern"
    if reg_anychar in text:
        text = text.replace(f"{match} '{reg_anychar}'", "not be empty")
    if reg_empty in text:
        text = text.replace(f"{match} '{reg_empty}'", "be empty")
    if reg_all0s in text:
        text = text.replace(f"{match} '{reg_all0s}'", "be all 0s")
    if reg_all9s in text:
        text = text.replace(f"{match} '{reg_all9s}'", "be all 9s")        
    if reg_DTM in text:
        text = text.replace(f"{match} '{reg_DTM}'", f"{be_a_date} 'YYYY[MM[DD[HH[MM[SS[.S[S[S[S]]]]]]]]][+/-ZZZZ]'")
    if reg_DTM8 in text:
        text = text.replace(f"{match} '{reg_DTM8}'", f"{be_a_date} 'YYYYMMDD[HH[MM[SS[.S[S[S[S]]]]]]]]][+/-ZZZZ]'")
    if reg_DTM_14 in text:
        text = text.replace(f"{match} '{reg_DTM_14}'", f"{be_a_date} 'YYYYMMDDHHMMSS[.S[S[S[S]]]][+/-ZZZZ]]'")   
    if reg_DT in text:
        text = text.replace(f"{match} '{reg_DT}'", f"{be_a_date} 'YYYY[MM[DD]]'")
    if reg_SN_Separator in text:
        text = text.replace(f"{match} '{reg_SN_Separator}'", "contain one of the of the following values: '+', '-', '/', '.', ':'")
    if reg_SN_Comparator in text:
        text = text.replace(f"{match} '{reg_SN_Comparator}'", "contain one of the following values: '<=', '>=', '<>', '<', '>', '='")
    return text

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python update_constraints.py <path to main directory containing xml file subdirectories>")
    else:
        path = sys.argv[1]
        if os.path.exists(path):
            for d in os.listdir(path):
                if os.path.isdir(d):
                    subpath = os.path.join(path, d)
                    xml_list = [s for s in os.listdir(subpath) if s.upper().endswith(".XML")]
                    if len(xml_list) == 0:
                        print(f"No xml files found in {subpath}.")
                    else:
                        for x in xml_list:
                            if x.upper() == "CONSTRAINTS.XML":
                                fix_descriptions(os.path.join(subpath, x))
                            elif x.upper() == "PROFILE.XML":
                                fix_mappings(os.path.join(subpath, x))
                            print(f"File {os.path.join(subpath, x)} Done.")
        else:
            print(f"Path {path} does not exist.")