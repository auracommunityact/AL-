with open("app/src/main/java/com/example/data/models/Models.kt", "r") as f:
    content = f.read()

import re

# Fix QuestionPaper
replacement_qp = """data class QuestionPaper(
    @Serializable(with = StringOrNumericSerializer::class)
    val id: String = "",
    @SerialName("class_name")
    val className: String = "",
    val subject: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val section: String = "",
    val board: String = "",
    val year: String = "",
    @SerialName("pdf_url")
    val pdfUrl: String = "",
    @SerialName("file_size")
    val fileSize: String = "",
    @SerialName("total_pages")
    val totalPages: Int = 0,
    @SerialName("created_at")
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)"""
content = re.sub(r'data class QuestionPaper\([\s\S]*?val createdAt: Long = 0L\n\)', replacement_qp, content)

# Fix QuestionPaperSection
replacement_qps = """data class QuestionPaperSection(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val order: Int = 0,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)"""
content = re.sub(r'data class QuestionPaperSection\([\s\S]*?val createdAt: Long = 0L\n\)', replacement_qps, content)

with open("app/src/main/java/com/example/data/models/Models.kt", "w") as f:
    f.write(content)
