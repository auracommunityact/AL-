with open("app/src/main/java/com/example/ai/chat/AuraAiViewModelFactory.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.ai.vision.VisionProviderFactory", "import com.example.ai.vision.VisionProviderFactory\nimport com.example.ai.vision.VisionAiRepository")
content = content.replace("val toolExecutor = AuraToolExecutor(context)", "val toolExecutor = AuraToolExecutor(context)\n            val visionProvider = VisionProviderFactory.createRemoteProvider(context)\n            val visionRepository = VisionAiRepository(visionProvider)")
content = content.replace("return AuraAiViewModel(memoryDao, engine, toolRegistry, toolValidator, toolExecutor) as T", "return AuraAiViewModel(memoryDao, engine, toolRegistry, toolValidator, toolExecutor, visionRepository, context) as T")

with open("app/src/main/java/com/example/ai/chat/AuraAiViewModelFactory.kt", "w") as f:
    f.write(content)
