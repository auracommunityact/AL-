with open("app/src/main/java/com/example/ui/questionPapers/QuestionPaperListingScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    """navController.navigate("pdf_viewer/${paper.id}")""",
    """val encodedUrl = java.net.URLEncoder.encode(paper.pdfUrl, "UTF-8")
                            navController.navigate("pdf_viewer?url=$encodedUrl")"""
)

with open("app/src/main/java/com/example/ui/questionPapers/QuestionPaperListingScreen.kt", "w") as f:
    f.write(content)
