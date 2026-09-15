with open("app/src/main/java/com/example/ui/videos/VideoPlayerScreen.kt", "r") as f:
    content = f.read()

import re

# We need to add download logic. Since it uses Android DownloadManager, we need context.
# Let's insert the helper function at the top of the file or bottom.
helper = """
private fun downloadVideo(context: android.content.Context, url: String, title: String) {
    if (url.isBlank()) {
        android.widget.Toast.makeText(context, "Download URL is empty", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
            .setTitle(title)
            .setDescription("Downloading video...")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MOVIES, "${title}.mp4")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        downloadManager.enqueue(request)
        android.widget.Toast.makeText(context, "Download started", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
"""

if "fun downloadVideo" not in content:
    content = content + helper

# Insert Download button in Video Details
old_desc = """                        if (video!!.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(video!!.description, style = MaterialTheme.typography.bodyMedium)
                        }"""
new_desc = """                        if (video!!.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(video!!.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        if (video!!.downloadEnabled && video!!.authorizedDownloadUrl.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { downloadVideo(context, video!!.authorizedDownloadUrl, video!!.title) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = "Download")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Video")
                            }
                        }"""

content = content.replace(old_desc, new_desc, 1)

# Make sure Icons.Filled.Download is imported
if "import androidx.compose.material.icons.filled.Download" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.PlayArrow", "import androidx.compose.material.icons.filled.PlayArrow\nimport androidx.compose.material.icons.filled.Download")

with open("app/src/main/java/com/example/ui/videos/VideoPlayerScreen.kt", "w") as f:
    f.write(content)
