#############################################################################
# RESOURCES - VNET
#############################################################################

locals {
  subnet_names = ["main_hl7_${var.environment}", "dbx_${var.environment}"]
}

// creates new RG
resource "azurerm_resource_group" "main_dx_hl7_rg" {
  name     = var.resource_group_name
  location = var.location
}
// for existing RG
# data "azurerm_resource_group" "vnet_main_rg" {
#   name     = var.resource_group_name
# }


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

resource "azurerm_storage_account" "storage_fn" {

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

resource "azurerm_application_insights" "app_insights" {
  name                = "${var.project}-app-insights-${var.environment}"
  location            = var.location
  resource_group_name = azurerm_resource_group.main_dx_hl7_rg.name
  application_type    = "java"

  tags = {
    environment = var.environment
    project = var.project
  }
}


#############################################################################
# APP SERVICE
#############################################################################

# Linux Consumption App Service Plan

resource "azurerm_service_plan" "app_service_plan" {
  name                = "${var.project}-app-service-plan-${var.environment}"
  resource_group_name = azurerm_resource_group.main_dx_hl7_rg.name
  location            = var.location

  os_type             = "Linux"
  sku_name            = "F1" // TODO change Free Tier     

  tags = {
    environment = var.environment
    project = var.project
  }
}


#############################################################################
# FUNCTIONS
#############################################################################

resource "azurerm_linux_function_app" "fn_app_sv" {
  name                       = "${var.project}-fn-sv-${var.environment}"
  resource_group_name        = azurerm_resource_group.main_dx_hl7_rg.name
  location                   = var.location
  service_plan_id            = azurerm_service_plan.app_service_plan.id

  storage_account_name       = azurerm_storage_account.storage_fn.name
  storage_account_access_key = azurerm_storage_account.storage_fn.primary_access_key

  site_config {
  }

  tags = {
    environment = var.environment
    project = var.project
  }
}

resource "azurerm_linux_function_app" "fn_app_mp" {
  name                       = "${var.project}-fn-mp-${var.environment}"
  resource_group_name        = azurerm_resource_group.main_dx_hl7_rg.name
  location                   = var.location
  service_plan_id            = azurerm_service_plan.app_service_plan.id

  storage_account_name       = azurerm_storage_account.storage_fn.name
  storage_account_access_key = azurerm_storage_account.storage_fn.primary_access_key

  site_config {
  }

  tags = {
    environment = var.environment
    project = var.project
  }
}


#############################################################################
# EVENT HUBS
#############################################################################
resource "azurerm_eventhub_namespace" "eventhub_namespace" {
  name                = "${var.project}-eventhub-namespace-${var.environment}"
  location            = var.location
  resource_group_name = azurerm_resource_group.main_dx_hl7_rg.name
  sku                 = "Standard"
  capacity            = 1

  tags = {
    environment = var.environment
    project = var.project
  }
}

resource "azurerm_eventhub" "eventhub" {
  name                = "${var.project}-eventhub-${var.environment}"
  namespace_name      = azurerm_eventhub_namespace.eventhub_namespace.name
  resource_group_name = azurerm_resource_group.main_dx_hl7_rg.name
  partition_count     = 2
  message_retention   = 2 // 2 days 
}