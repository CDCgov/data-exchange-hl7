# TL;DR>

This Service is responsible for loading auxiliary data into our Redis instance for quick lookup by other components.


# Details
Currently, we are using Redis to load three main sets of data, with a specific Azure function for each data set. These functions run on a trigger basis, currently configured to run once a day, and each run refreshes the entire dataset.

* Vocabulary: Loads all value sets and codes from the latest version of PHIN VADS. This data is used by MMG validator to validate that a message is sending the right code for a given field.
  * Source: PHIN VADS API (https://phinvads.cdc.gov/vocabService/v2)
  * Function: PhinVocabRead
* MMGs: Loads all our MMG configurations. Those configurations are either created in MMG-AT tool or are manually created for those that are not directly supported by the MMG-AT UI.
	 * Sources:
	   * MMG-AT (https://mmgat.services.cdc.gov/api/guide/all?type=0)
	   * Legacy MMGs under /src/main/resources folder
   * Function: MMGATRead

* Condtion2MMGMapping: This table maps a specific condition to the MMGs that should be used to validate and transform a given message. Certain conditions can have special cases based on the jurisdiction that is sending the message. (There is a helper method on lib-dex-commons that encapsulates all the logic on how to query this table and retrieve the appropriate list of MMGs for a given message.)
	 * Sources:
	  * src/resoruces/event_codes
	  * src/resources/groups (for special cases grouping)
    * Function: EventCodesAndGroups
			
		* Ex.: 
``` json

{
"event_code":10049,
"name":"West Nile virus non-neuroinvasive disease",
"program":"NCEZID",
"category":"Arboviral Diseases",
"profiles":[
   {
     "name":"arbo_case_map_v1.0",
    "mmgs":["mmg:arboviral_v1_3_2_mmg_20210721"],
    "special_cases":[
        {
           "applies_to":"group:legacy_arbo",
            "mmg_maps":["mmg:arboviral_human_case_message_mapping_guide"]
        }
     ]
   }
  ]
}
```
	
	
![image](https://user-images.githubusercontent.com/3239945/208681382-77a41b50-799e-4f4f-95df-ad1d98711291.png)


