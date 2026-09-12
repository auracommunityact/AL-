with open("app/src/main/java/com/example/ui/home/HomeScreen.kt", "r") as f:
    content = f.read()

replacement = """Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { navController.navigate("aura_ai_chat") },
                icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.AutoAwesome, "Aura AI") },
                text = { androidx.compose.material3.Text("Aura AI") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) {"""

content = content.replace("""Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) {""", replacement)

if "import androidx.compose.material.icons.filled.AutoAwesome" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.*", "import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.filled.AutoAwesome")

with open("app/src/main/java/com/example/ui/home/HomeScreen.kt", "w") as f:
    f.write(content)
