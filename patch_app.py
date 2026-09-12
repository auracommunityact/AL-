with open("app/src/main/java/com/example/AuraLearningApp.kt", "r") as f:
    content = f.read()

content = content.replace(
    'composable(Screen.Home.route) { HomeScreen(navController, authViewModel, rootNavController) }',
    'composable(Screen.Home.route) { HomeScreen(navController, authViewModel, rootNavController) }\n                composable("aura_ai_chat") { com.example.ai.ui.AuraAiChatScreen(navController) }'
)

with open("app/src/main/java/com/example/AuraLearningApp.kt", "w") as f:
    f.write(content)
