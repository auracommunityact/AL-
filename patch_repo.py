import re

with open("app/src/main/java/com/example/data/repository/AuraRepository.kt", "r") as f:
    content = f.read()

content = content.replace(
    "} catch (e: Exception) {\n            emptyList()\n        }",
    "} catch (e: Exception) {\n            android.util.Log.e(\"AuraRepository\", \"Error: \", e)\n            emptyList()\n        }"
)

with open("app/src/main/java/com/example/data/repository/AuraRepository.kt", "w") as f:
    f.write(content)
