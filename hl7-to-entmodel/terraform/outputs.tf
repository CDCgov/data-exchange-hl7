
#############################################################################
# OUTPUTS
#############################################################################

output "vnet_id" {
  value = module.vnet-main.vnet_id
  description = "Deployed vnet id"
}


output "function_app_name" {
  value = azurerm_linux_function_app.fn_app.name
  description = "Deployed function app name"
}

output "function_app_default_hostname" {
  value = azurerm_linux_function_app.fn_app.default_hostname
  description = "Deployed function app hostname"
}