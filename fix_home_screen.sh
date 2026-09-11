sed -i 's/banner.linkUrl/banner.link/g' app/src/main/java/com/example/ui/home/HomeScreen.kt
sed -i 's/banner.subtitle/banner.description/g' app/src/main/java/com/example/ui/home/HomeScreen.kt
sed -i 's/video.class_name/video.className/g' app/src/main/java/com/example/ui/home/HomeScreen.kt
sed -i 's/video.chapter_name/video.chapter/g' app/src/main/java/com/example/ui/home/HomeScreen.kt
sed -i 's/book.class_name/book.className/g' app/src/main/java/com/example/ui/home/HomeScreen.kt
sed -i 's/post.author_avatar ?: "https:\/\/ui-avatars.com\/api\/?name=${post.author_name}&background=random"/"https:\/\/ui-avatars.com\/api\/?name=Aura+Admin&background=random"/g' app/src/main/java/com/example/ui/home/HomeScreen.kt
sed -i 's/post.author_name ?: "Aura Member"/"Aura Admin"/g' app/src/main/java/com/example/ui/home/HomeScreen.kt
sed -i 's/post.content/post.description/g' app/src/main/java/com/example/ui/home/HomeScreen.kt
