with open("app/src/main/java/com/example/ai/model/AiConfig.kt", "r") as f:
    content = f.read()

content = content.replace('MODEL_NAME = "Gemma 3n E4B-it"', 'MODEL_NAME = "Gemma 1.1 2B IT"')
content = content.replace('MODEL_FILE_NAME = "gemma-3n-it-cpu-int4.task"', 'MODEL_FILE_NAME = "gemma-1.1-2b-it-cpu-int4.bin"')

with open("app/src/main/java/com/example/ai/model/AiConfig.kt", "w") as f:
    f.write(content)
