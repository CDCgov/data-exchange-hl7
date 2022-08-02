
#############################################################################
# OUTPUTS
#############################################################################

output "vnet_id" {
  value = module.vnet-main.vnet_id
  description = "Deployed vnet id"
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