with open("app/build.gradle.kts", "r") as f:
    content = f.read()

config_field = 'buildConfigField("String", "GEMINI_API_KEY", "\\"$geminiApiKey\\"")\n'
new_field = config_field + '        val visionApiUrl = System.getenv("AURA_VISION_API_URL") ?: "https://aura-vision-api-placeholder.com/v1/vision/chat"\n' + '        buildConfigField("String", "AURA_VISION_API_URL", "\\"$visionApiUrl\\"")\n'

if "AURA_VISION_API_URL" not in content:
    content = content.replace(config_field, new_field)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
