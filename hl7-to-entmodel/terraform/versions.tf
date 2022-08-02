
#############################################################################
# TERRAFORM VERSIONS
#############################################################################

terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.16.0"
    }
  }
}