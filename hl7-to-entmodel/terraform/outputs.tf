
#############################################################################
# OUTPUTS
#############################################################################

output "vnet_id" {
  value = module.vnet-main.vnet_id
  description = "Deployed vnet id"
}

output "storage_fn" {
  value = azurerm_storage_account.storage_fn.name
  description = "Deployed storage name"
}

output "app_service_plan" {
  value = azurerm_service_plan.app_service_plan.name
  description = "Deployed App Service name"
}

output "eventhub_namespace" {
  value = azurerm_eventhub_namespace.eventhub_namespace.name
  description = "Deployed Event Hubs namespace name"
}

output "eventhub" {
  value = azurerm_eventhub.eventhub.name
  description = "Deployed Event Hubs name"
}

output "fn_sv_app_name" {
  value = azurerm_linux_function_app.fn_app_sv.name
  description = "Deployed function app structural validator name"
}

output "fn_sv_app_hostname" {
  value = azurerm_linux_function_app.fn_app_sv.default_hostname
  description = "Deployed function app structural validator hostname"
}

output "fn_mp_app_name" {
  value = azurerm_linux_function_app.fn_app_mp.name
  description = "Deployed function app message processor name"
}

output "fn_mp_app_hostname" {
  value = azurerm_linux_function_app.fn_app_mp.default_hostname
  description = "Deployed function app message processor hostname"
}