#############################################################################
# RESOURCES - VNET
#############################################################################

locals {
  subnet_names = ["main_hl7_${var.environment}", "dbx_${var.environment}"]
}

resource "azurerm_resource_group" "main_dx_hl7_rg" {
  name     = var.resource_group_name
  location = var.location
}

module "vnet-main" {
  source              = "Azure/vnet/azurerm"
  version             = "~> 2.6.0"
  resource_group_name = azurerm_resource_group.main_dx_hl7_rg.name
  vnet_name           = "${var.project}-vnet-${var.environment}"
  address_space       = [var.vnet_cidr_range]
  subnet_prefixes     = var.subnet_prefixes
  subnet_names        = local.subnet_names
  nsg_ids             = {}

  tags = {
    environment = var.environment 
    project = var.project
  }

  depends_on = [azurerm_resource_group.main_dx_hl7_rg]
}


#############################################################################
# STORAGE
#############################################################################

resource "azurerm_storage_account" "fn_storage" {

  name                     = "${var.project}storage${var.environment}"
  resource_group_name      = azurerm_resource_group.main_dx_hl7_rg.name
  location                 = var.location
  account_tier             = "Standard"
  account_kind             = "StorageV2"
  account_replication_type = "LRS"

  tags = {
    environment = var.environment
    project = var.project
  }
}


#############################################################################
# APP INSIGHTS
#############################################################################

resource "azurerm_application_insights" "application_insights" {
  name                = "${var.project}-app-insights-${var.environment}"
  location            = var.location
  resource_group_name = azurerm_resource_group.main_dx_hl7_rg.name
  application_type    = "java"

    tags = {
    environment = var.environment
    project = var.project
  }
}