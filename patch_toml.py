with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

version = 'mediapipeTasksGenai = "0.10.14"\n'
lib = 'mediapipe-tasks-genai = { group = "com.google.mediapipe", name = "tasks-genai", version.ref = "mediapipeTasksGenai" }\n'

if "mediapipeTasksGenai" not in content:
    content = content.replace("[versions]", "[versions]\n" + version)
    content = content.replace("[libraries]", "[libraries]\n" + lib)

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)
