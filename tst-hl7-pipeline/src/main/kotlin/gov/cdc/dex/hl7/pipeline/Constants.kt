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
        const val PATIENT_PHONE_NUMBER = ""
        const val PATIENT_BUSINESS_PHONE_NUMBER = ""
        const val PATIENT_ETHNIC_GROUP = "ethnic_group"
        const val PATIENT_DEATH_DATE_TIME = ""
        const val PATIENT_DEATH_INDICATOR = ""
        const val PATIENT_LAST_DEMOGRAPHIC_INFO_DATE_TIME_UPDATE = ""
        const val PATIENT_LAST_UPDATE_FACILITY = ""
        const val PATIENT_SPECIES_CODE = ""
        const val PATIENT_CLASS = ""
        const val PATIENT_ADMISSION_TYPE = ""
        const val PATIENT_ADMISSION_DATE_TIME = ""
        const val PATIENT_DISCHARGE_DATE_TIME = ""

        // ORC/OBR/OBX required fields
        const val ORDER_CONTROL = "order_control"
        const val FILLER_ORDER_NUMBER = "filler_order_number"
        const val PLACER_GROUP_NUMBER = ""
        const val ORDERING_PROVIDER = ""
        const val ORDERING_FACILITY_NAME = "ordering_facility_name"
        const val ORDERING_FACILITY_ADDRESS = "ordering_facility_address"
        const val ORDERING_FACILITY_PHONE_NUMBER = "ordering_facility_phone_number"
        const val ORDERING_PROVIDER_ADDRESS = ""
        const val SET_ID = ""
        const val PLACER_ORDER_NUMBER = ""
        const val UNIVERSAL_SERVICE_IDENTIFIER=""
        const val OBSERVATION_DATE_TIME = ""
        const val OBSERVATION_DATE_TIME_END = ""
        const val RELEVANT_CLINICAL_INFORMATION = ""
        const val RESULT_REPORT_DATE_TIME = ""
        const val RESULT_STATUS = ""
        const val PARENT_RESULT = ""
        const val PARENT_ID = ""
        const val REASON_FOR_STUDY = ""
        const val PRINCIPAL_RESULT_INTERPRETER = ""
        const val VALUE_DATA_TYPE = ""
        const val OBSERVATION_IDENTIFIER = ""
        const val OBSERVATION_SUB_ID = ""
        const val OBSERVATION_VALUE = ""
        const val UNITS_OF_MEASURE_FOR_DATA_TYPE_SN = ""
        const val REFERENCE_RANGE = ""
        const val ABNORMAL_FLAGS = ""
        const val OBSERVATION_RESULT_STATUS = ""
        const val OBSERVATION_METHOD = ""
        const val DATE_TIME_ANALYSIS = ""
        const val PERFORMING_ORGANIZATION_NAME = ""
        const val PERFORMING_ORGANIZATION_ADDRESS = ""
        const val PERFORMING_ORGANIZATION_MEDICAL_DIRECTOR = ""

        //SPM required fields
        const val SPECIMEN_ID = ""
        const val SPECIMEN_TYPE = ""
        const val SPECIMEN_TYPE_MODIFIER = ""
        const val SPECIMEN_ADDITIVES = ""
        const val SPECIMEN_COLLECTION_METHOD = ""
        const val SPECIMEN_SOURCE_SITE = ""
        const val SPECIMEN_SOURCE_SITE_MODIFIER = ""
        const val SPECIMEN_ROLE = ""
        const val SPECIMEN_COLLECTION_AMOUNT = ""
        const val SPECIMEN_COLLECTION_DATE_TIME = ""
        const val SPECIMEN_RECEIVED_DATE_TIME = ""
        const val SPECIMEN_REJECT_REASON = ""


        //other constants
        const val MSH = "MSH"
        const val SFT = "SFT"
        const val PID = "PID"
        const val PV1 = "PV1"
        const val ORC = "ORC"
        const val OBR = "OBR"
        const val REPORT = "report"
        const val PROCESSES = "processes"
        const val METADATA = "metadata"
        const val CHILDREN = "children"
        const val ENTRIES = "entries"
        const val PATH = "path"
        const val DESCRIPTION = "description"
        const val JURISDICTION = "48"

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

        const val NEW_PAYLOADS_PATH = "src/test/resources/new-payloads"
        const val VERIFIED_PAYLOADS_PATH = "src/test/resources/verified-payloads"


    }
}