import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

# check if it already has AURA_VISION_API_URL
if "AURA_VISION_API_URL" not in content:
    # insert inside defaultConfig
    match = re.search(r'defaultConfig\s*\{', content)
    if match:
        insert_pos = match.end()
        content = content[:insert_pos] + '\n        buildConfigField("String", "AURA_VISION_API_URL", "\\"https://api.example.com/v1/vision/chat\\"")' + content[insert_pos:]

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
