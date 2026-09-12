import re

with open("app/src/main/java/com/example/data/repository/AuraRepository.kt", "r") as f:
    content = f.read()

# Replace the searchUsers method
new_search = """    suspend fun searchUsers(query: String): List<User> {
        return try {
            client.postgrest["users"].select {
                filter {
                    or {
                        ilike("name", "%$query%")
                        ilike("email", "%$query%")
                    }
                }
            }.decodeList<User>()
        } catch (e: Exception) {
            android.util.Log.e("AuraRepository", "Error searching users: ${e.message}", e)
            emptyList()
        }
    }"""

# Use regex to replace the old searchUsers
content = re.sub(
    r"    suspend fun searchUsers\(query: String\): List<User> \{[\s\S]*?catch \(e: Exception\) \{[\s\S]*?emptyList\(\)[\s\S]*?\}[\s\S]*?\}",
    new_search,
    content
)

with open("app/src/main/java/com/example/data/repository/AuraRepository.kt", "w") as f:
    f.write(content)
