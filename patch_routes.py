import re

with open("app/src/main/java/com/example/AuraLearningApp.kt", "r") as f:
    content = f.read()

content = re.sub(r'composable\("local_scheduler"\) \{.*?\}', '', content, flags=re.DOTALL)
content = re.sub(r'composable\(Screen\.AuraAi\.route\) \{.*?\}', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/AuraLearningApp.kt", "w") as f:
    f.write(content)
