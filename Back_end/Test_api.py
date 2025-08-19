import requests
import json

data = {'url': 'https://www.facebook.com'}
response = requests.post('https://TrustQR.i-Keeper.synology.me/predict', json=data)

print(json.dumps(response.json(), ensure_ascii=False, indent=4))
