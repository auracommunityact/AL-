import re

with open("app/src/main/java/com/example/AuraLearningApp.kt", "r") as f:
    content = f.read()

content = content.replace(
"""val items = listOf(
    Screen.Home,
    Screen.QuestionPapers,
    Screen.Videos,
    Screen.Books,
    Screen.Profile,
    Screen.AuraAi
)""",
"""val items = listOf(
    Screen.Home,
    Screen.QuestionPapers,
    Screen.Videos,
    Screen.Books,
    Screen.Profile,
    Screen.Chat
)""")

with open("app/src/main/java/com/example/AuraLearningApp.kt", "w") as f:
    f.write(content)
