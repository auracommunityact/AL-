with open("app/src/main/java/com/example/ai/chat/AuraAiViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("if (text.startsWith(\"/\")) {", "if (text.isNotBlank() && text.startsWith(\"/\")) {")

with open("app/src/main/java/com/example/ai/chat/AuraAiViewModel.kt", "w") as f:
    f.write(content)
