import json
import requests

base_url = 'https://mmgat.services.cdc.gov/'

list_all = 'api/guide/all?type=0'  # type 0 = MMG, type 1 = template
write_location = './'
accept_statuses = ['Final', 'UserAcceptanceTesting']
include_genv2 = True


# Get list of all MMGs. Have to use verify=False because the site has a self-signed certificate
response = requests.get(base_url + list_all, verify=False)
results = response.json().get('result')

# save results list
# with open(write_location + 'list_all.json', 'w') as file:
#     json.dump(results, file, indent=2)

# define function to filter results by guideStatus
def keep_in_list(result):
    status = result.get('guideStatus', '')
    return status in accept_statuses


# use the function to filter the results
filtered_mmgs = list(filter(keep_in_list, results))

# get details of each mmg in the filtered list
count = 0
for mmg in filtered_mmgs:
    count += 1
    id = mmg.get('id')

    mmg_url = f'api/guide/{id}?includeGenV2={include_genv2}'
    resp = requests.get(base_url + mmg_url, verify=False)
    result = resp.json().get('result')
    # this is where we write the MMG to a file.

    # uncomment to print to console
    # print(json.dumps(result, indent=2))
    name = result.get('name', count) + ".json"
    with open(write_location + name, 'w') as f:
        json.dump(result, f, indent=2)


print(f'Pulled {count} MMGs. Script complete.')