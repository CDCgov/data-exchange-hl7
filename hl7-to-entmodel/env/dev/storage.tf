resource "azurerm_storage_account" "func_storage_acc" {
  name                     = "funcstorageacc${random_string.random.result}"
  resource_group_name      = var.RESOURCE_GROUP
  location                 = var.LOCATION
  account_tier             = "Standard"
  account_replication_type = "LRS"

  depends_on = [azurerm_resource_group.func-rg]
}

resource "azurerm_storage_account" "sa" {
  name                     = "${var.storageAccountPrefix}${var.environmentName}"
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = azurerm_resource_group.rg.location
  account_tier             = "Standard"
  account_kind             = "StorageV2"
  account_replication_type = "LRS"
}
