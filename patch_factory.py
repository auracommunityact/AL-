with open("app/src/main/java/com/example/ai/chat/AuraAiViewModelFactory.kt", "r") as f:
    content = f.read()

content = content.replace(
    "import com.example.ai.tools.AuraToolRegistry",
    "import com.example.ai.tools.AuraToolRegistry\nimport com.example.ai.tools.AuraToolValidator"
)

content = content.replace(
    "val toolRegistry = AuraToolRegistry()",
    "val toolRegistry = AuraToolRegistry()\n            val toolValidator = AuraToolValidator(toolRegistry)"
)

content = content.replace(
    "return AuraAiViewModel(memoryDao, engine, toolRegistry, toolExecutor) as T",
    "return AuraAiViewModel(memoryDao, engine, toolRegistry, toolValidator, toolExecutor) as T"
)

with open("app/src/main/java/com/example/ai/chat/AuraAiViewModelFactory.kt", "w") as f:
    f.write(content)
