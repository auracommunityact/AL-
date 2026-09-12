import re

with open("app/src/main/java/com/example/data/repository/ChatRepository.kt", "r") as f:
    content = f.read()

new_logic = """        val newConvo = Conversation(
            name = otherUserName,
            lastMessageTime = System.currentTimeMillis()
        )
        val insertedConvo = postgrest["conversations"]
            .insert(getJsonWithoutId(newConvo)) { select() }
            .decodeSingle<Conversation>()
            
        val realConvoId = insertedConvo.id
        
        postgrest["conversation_members"].insert(getJsonListWithoutId(listOf(
            ConversationMember(conversationId = realConvoId, userId = currentUserId),
            ConversationMember(conversationId = realConvoId, userId = otherUserId)
        )))
        
        return realConvoId"""

content = re.sub(
    r"val convoId = java\.util\.UUID\.randomUUID\(\)\.toString\(\)\s*val newConvo = Conversation\([\s\S]*?postgrest\[\"conversation_members\"\]\.insert\([\s\S]*?\)\s*return convoId",
    new_logic,
    content
)

with open("app/src/main/java/com/example/data/repository/ChatRepository.kt", "w") as f:
    f.write(content)
