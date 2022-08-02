#############################################################################
# RESOURCES - VNET
#############################################################################

locals {
  subnet_names = ["pipeline_hl7_${var.environment_name}", "dbx_${var.environment_name}"]
}

resource "azurerm_resource_group" "vnet_main_rg" {
  name     = var.resource_group_name
  location = var.location
}

module "vnet-main" {
  source              = "Azure/vnet/azurerm"
  version             = "~> 2.6.0"
  resource_group_name = azurerm_resource_group.vnet_main_rg.name
  vnet_name           = var.resource_group_name
  address_space       = [var.vnet_cidr_range]
  subnet_prefixes     = var.subnet_prefixes
  subnet_names        = local.subnet_names
  nsg_ids             = {}

  tags = {
    environment = var.environment_name
  }

  depends_on = [azurerm_resource_group.vnet_main_rg]
}


#############################################################################
# STORAGE
#############################################################################

resource "azurerm_storage_account" "fnstorage" {

  name                     = "${var.storage_prefix_function}${var.environment_name}"
  resource_group_name      = var.resource_group_name
  location                 = var.location
  account_tier             = "Standard"
  account_kind             = "StorageV2"
  account_replication_type = "LRS"

  tags = {
    environment = var.environment_name
  }
}