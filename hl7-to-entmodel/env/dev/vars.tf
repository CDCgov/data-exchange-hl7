#############################################################################
# VARIABLES
#############################################################################

variable "environment_name" {
  type = string
  default = "dev"
}

variable "resource_group_name" {
  type = string
  default = "TODO_delete_test_rg"
}

variable "location" {
  type    = string
  default = "eastus"
}


variable "vnet_cidr_range" {
  type    = string
  default = "10.0.0.0/16"
}

variable "subnet_prefixes" {
  type    = list(string)
  default = ["10.0.0.0/24", "10.0.1.0/24"]
}
