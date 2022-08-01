
resource "azurerm_storage_account" "sa" {

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
