package gov.cdc.dex.hl7.pipeline

class Constants {

    companion object {
        // MSH required fields
        const val FIELD_SEPARATOR = "field_separator"
        const val ENCODING_CHARACTERS = "encoding_characters"
        const val SENDING_APPLICATION = "sending_application"
        const val SENDING_FACILITY = "sending_facility"
        const val RECEIVING_APPLICATION = "receiving_application"
        const val RECEIVING_FACILITY = "receiving_facility"
        const val DATE_TIME_OF_MESSAGE = "date_time_of_message"
        const val MESSAGE_TYPE = "message_type"
        const val MESSAGE_CONTROL_ID = "message_control_id"
        const val PROCESSING_ID = "processing_id"
        const val VERSION_ID = "version_id"
        const val ACCEPT_ACKNOWLEDGEMENT_TYPE = "accept_acknowledgement_type"
        const val APPLICATION_ACKNOWLEDGEMENT_TYPE = "application_acknowledgement_type"
        const val COUNTRY_CODE = "country_code"
        const val MESSAGE_PROFILE_IDENTIFIER = "message_profile_identifier"

        //MSH Fields
        const val MSH3 = "MSH[1]-3[1]"
        const val MSH4 = "MSH[1]-4[1].2"
        const val MSH5 = "MSH[1]-5[1].2"
        const val MSH6 = "MSH[1]-6[1]"
        const val MSH7 = "MSH[1]-7[1]"
        const val MSH9 = "MSH[1]-9[1]"
        const val MSH10 = "MSH[1]-10[1]"
        const val MSH11 = "MSH[1]-11[1]"
        const val MSH21 = "MSH[1]-21[1]"

        // SFT required fields
        const val SOFTWARE_VENDOR_ORGANIZATION = "software_vendor_organization"
        const val SOFTWARE_CERTIFIED_VERSION_OR_RELEASE_NUMBER = "software_certified_version_or_release_number"
        const val SOFTWARE_PRODUCT_NAME = "software_product_name"
        const val SOFTWARE_BINARY_ID = "software_binary_id"
        const val SOFTWARE_INSTALL_DATE = "software_install_date"
        const val SOFTWARE_PRODUCT_INFORMATION = ""

        //PID/PV1 required Fields
        const val PATIENT_IDENTIFIER_LIST = "patient_identifier_list"
        const val PATIENT_NAME = ""
        const val PATIENT_MOTHER_MAIDEN_NAME = ""
        const val PATIENT_BIRTH_DATE_TIME = "date_time_of_birth"
        const val PATIENT_SEX = "administrative_sex"
        const val PATIENT_RACE = "race"
        const val PATIENT_ADDRESS = "patient_address"
        const val PATIENT_PHONE_NUMBER = "patient_phone_number"
        const val PATIENT_BUSINESS_PHONE_NUMBER = "patient_business_phone_number"
        const val PATIENT_ETHNIC_GROUP = "ethnic_group"
        const val PATIENT_DEATH_DATE_TIME = "patient_death_date"
        const val PATIENT_DEATH_INDICATOR = "patient_death_indicator"
        const val PATIENT_LAST_DEMOGRAPHIC_INFO_DATE_TIME_UPDATE = ""
        const val PATIENT_LAST_UPDATE_FACILITY = "patient_last_update_facility"
        const val PATIENT_SPECIES_CODE = "patient_species_code"
        const val PATIENT_CLASS = "patient_class"
        const val PATIENT_ADMISSION_TYPE = "patient_admission_type"
        const val PATIENT_ADMISSION_DATE_TIME = "patient_admission_date_time"
        const val PATIENT_DISCHARGE_DATE_TIME = "patient_discharge_date_time"

        // ORC/OBR/OBX required fields
        const val ORDER_CONTROL = "order_control"
        const val PLACER_ORDER_NUMBER = "placer_order_number"
        const val FILLER_ORDER_NUMBER = "filler_order_number"
        const val ORDERING_PROVIDER = "ordering_provider"
        const val ORDERING_FACILITY_NAME = "ordering_facility_name"
        const val ORDERING_FACILITY_ADDRESS = "ordering_facility_address"
        const val ORDERING_FACILITY_PHONE_NUMBER = "ordering_facility_phone_number"
        const val ORDERING_PROVIDER_ADDRESS = "ordering_provider_address"
        const val SET_ID = "set_id"

        const val UNIVERSAL_SERVICE_IDENTIFIER="universal_service_identifier"
        const val OBSERVATION_DATE_TIME = "observation_date_time"
        const val OBSERVATION_DATE_TIME_END = "observation_end_date_time"
        const val RELEVANT_CLINICAL_INFORMATION = "relevant_clinical_information"
        const val RESULT_REPORT_DATE_TIME = "result_report_date_time"
        const val RESULT_STATUS = "result_status"
        const val PARENT_RESULT = "parent_result"
        const val PARENT_ID = "parent_id"
        const val REASON_FOR_STUDY = "reason_for_study"
        const val PRINCIPAL_RESULT_INTERPRETER = "principal_result_interpreter"
        const val VALUE_DATA_TYPE = "value_data_type"
        const val OBSERVATION_IDENTIFIER = "observation_identifier"
        const val OBSERVATION_SUB_ID = "observation_sup_id"
        const val OBSERVATION_VALUE = "observation_value"
        const val UNITS_OF_MEASURE_FOR_DATA_TYPE_SN = "units"
        const val REFERENCE_RANGE = "reference_range"
        const val ABNORMAL_FLAGS = "abnormal_flags"
        const val OBSERVATION_RESULT_STATUS = "observation_result_status"
        const val OBSERVATION_METHOD = "observation_method"
        const val DATE_TIME_ANALYSIS = "date_time_of_the_analysis"
        const val PERFORMING_ORGANIZATION_NAME = "performing_organization_name"
        const val PERFORMING_ORGANIZATION_ADDRESS = "performing_organization_address"
        const val PERFORMING_ORGANIZATION_MEDICAL_DIRECTOR = ""

        //SPM required fields
        const val SPECIMEN_ID = "specimen_id"
        const val SPECIMEN_TYPE = "specimen_type"
        const val SPECIMEN_TYPE_MODIFIER = "specimen_type_modifier"
        const val SPECIMEN_ADDITIVES = "specimen_additives"
        const val SPECIMEN_COLLECTION_METHOD = "specimen_collection_method"
        const val SPECIMEN_SOURCE_SITE = "specimen_source_site"
        const val SPECIMEN_SOURCE_SITE_MODIFIER = "specimen_source_site_modifier"
        const val SPECIMEN_ROLE = "specimen_role"
        const val SPECIMEN_COLLECTION_AMOUNT = "specimen_collection_ammount"
        const val SPECIMEN_COLLECTION_DATE_TIME = "specimen_collection_date_time"
        const val SPECIMEN_RECEIVED_DATE_TIME = "specimen_received_date_time"
        const val SPECIMEN_REJECT_REASON = "specimen_reject_reason"


        //other constants
        const val MSH = "MSH"
        const val SFT = "SFT"
        const val PID = "PID"
        const val PV1 = "PV1"
        const val ORC = "ORC"
        const val OBR = "OBR"
        const val REPORT = "report"
        const val CATEGORY = "category"
        const val CONTENT = "content"
        const val CLASSIFICATION = "classification"
        const val STACKTRACE = "stackTrace"
        const val PROCESSES = "processes"
        const val METADATA = "metadata"
        const val CHILDREN = "children"
        const val ENTRIES = "entries"
        const val PATH = "path"
        const val ROUTE = "route"
        const val DESCRIPTION = "description"
        const val JURISDICTION = "48"
        const val REPORTING_JURISDICTION = "reporting_jurisdiction"
        const val ORIGINAL_FILE_NAME = "original_file_name"
        const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
        const val STATUS = "status"


        //message types
        const val MESSAGE_TYPE_STR = "message_type"
        const val MESSAGE_TYPE_ELR = "ELR"

        //routes
        const val PHLIP_FLU = "PHLIP_FLU"


        //report names
        const val STRUCTURE_VALIDATOR = "structure"


        //test message names
        const val PHLIP_FLU_NO_MSH3 = "PHLIP_FLU_2.5.1_NO_MSH3.txt"
        const val PHLIP_FLU_NO_MSH4 = "PHLIP_FLU_2.5.1_NO_MSH4.txt"
        const val PHLIP_FLU_NO_MSH5 = "PHLIP_FLU_2.5.1_NO_MSH5.txt"
        const val PHLIP_FLU_NO_MSH6 = "PHLIP_FLU_2.5.1_NO_MSH6.txt"
        const val PHLIP_FLU_NO_MSH7 = "PHLIP_FLU_2.5.1_NO_MSH7.txt"
        const val PHLIP_FLU_NO_MSH9 = "PHLIP_FLU_2.5.1_NO_MSH9.txt"
        const val PHLIP_FLU_NO_MSH10 = "PHLIP_FLU_2.5.1_NO_MSH10.txt"
        const val PHLIP_FLU_NO_MSH11 = "PHLIP_FLU_2.5.1_NO_MSH11.txt"
        const val PHLIP_FLU_NO_MSH12 = "PHLIP_FLU_2.5.1_NO_MSH12.txt"
        const val PHLIP_FLU_NO_MSH21 = "PHLIP_FLU_2.5.1_NO_MSH21.txt"
        const val PHLIP_FLU_VALID_MESSAGE = "PHLIP_FLU_2.5.1_VALID_MESSAGE.txt"
        const val PHLIP_FLU_NO_PROFILE_IDENTIFIER = "PHLIP_FLU_2.5.1_NO_PROFILE_IDENTIFIER.txt"
        const val PHLIP_FLU_VALID_MESSAGE_WITH_PV1= "PHLIP_FLU_2.5.1_VALID_MESSAGE_WITH_PV1_Added.txt"
        const val PHLIP_FLU_PID5_ERROR = "PHLIP_FLU_2.5.1_PID5_ERROR.txt"
        const val PHLIP_FLU_WITH_PID22 = "PHLIP_FLU_2.5.1_WITH_PID22.1_NO_PID22.3.txt"
        const val PHLIP_FLU_DUPLICATE_OBX1 = "PHLIP_FLU_2.5.1_DUPLICATE_OBX1.txt"
        const val PHLIP_FLU_OBX2CWE_OBX5ST = "PHLIP_FLU_2.5.1_OBX2CWE_OBX5ST.txt"
        const val PHLIP_FLU_TWO_OBX_WITH_SAME_OBX3_DIFF_OBX4 ="PHLIP_FLU_2.5.1_TWO_OBX_WITH_SAME_OBX3_DIFF_OBX4.txt"
        const val PHLIP_FLU_TWO_OBX_WITH_SAME_OBX3_NULL_OBX4 ="PHLIP_FLU_2.5.1_TWO_OBX_WITH_SAME_OBX3_NULL_OBX4.txt"
        const val PHLIP_FLU_TWO_OBX_WITH_SAME_OBX3_AND_OBX4 = "PHLIP_FLU_2.5.1_TWO_OBX_WITH_SAME_OBX3_AND_OBX4.txt"
        const val NEW_PAYLOADS_PATH = "src/test/resources/new-payloads"
        const val VERIFIED_PAYLOADS_PATH = "src/test/resources/verified-payloads"
        const val PATH_TO_MESSAGES = "src/main/resources/messages"

    }
}