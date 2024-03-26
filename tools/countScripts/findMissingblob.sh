container1Prefix="hl7_out_redacted/2024/03/22/00"
container2Prefix="hl7_out_validation_report/2024/03/22/00"

az storage blob list --account-name "ocioederoutingdatasastg" --container-name "routeingress"   --prefix  "${container1Prefix}"  --sas-token "" --query [*].name -o tsv | awk -F/ '{print $NF}'  > container1_files.txt
az storage blob list --account-name "ocioederoutingdatasastg" --container-name "routeingress"   --prefix  "${container2Prefix}" --sas-token ""  --query [*].name -o tsv | awk -F/ '{print $NF}' > container2_files.txt

# Compare the files in both containers
diff container1_files.txt container2_files.txt