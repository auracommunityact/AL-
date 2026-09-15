import requests, json

url = "https://aura.auralearning.workers.dev/api/v1/question_papers"
response = requests.get(url)
print(response.status_code, response.text[:500])
