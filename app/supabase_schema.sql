-- Supabase Schema for Aura Learning App

-- Create Users table (extends auth.users)
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT,
    email TEXT,
    "photoUrl" TEXT,
    provider TEXT,
    "createdAt" BIGINT,
    role TEXT DEFAULT 'user',
    "savedBooks" TEXT[] DEFAULT '{}',
    "savedVideos" TEXT[] DEFAULT '{}'
);

-- Create Books table
CREATE TABLE IF NOT EXISTS public.books (
    id UUID PRIMARY KEY,
    "bookName" TEXT,
    "className" TEXT,
    subject TEXT,
    "coverImage" TEXT,
    "pdfUrl" TEXT,
    "createdAt" BIGINT
);

-- Create Videos table
CREATE TABLE IF NOT EXISTS public.videos (
    id UUID PRIMARY KEY,
    title TEXT,
    description TEXT,
    "className" TEXT,
    subject TEXT,
    thumbnail TEXT,
    "videoUrl" TEXT,
    "youtubeVideoId" TEXT,
    chapter TEXT,
    "partNumber" INT,
    teacher TEXT,
    duration TEXT,
    "order" INT,
    "relatedBooks" TEXT[],
    "createdAt" BIGINT
);

-- Create Banners table
CREATE TABLE IF NOT EXISTS public.banners (
    id TEXT PRIMARY KEY,
    title TEXT,
    "imageUrl" TEXT,
    link TEXT,
    "createdAt" BIGINT
);

-- Create Home Sections table
CREATE TABLE IF NOT EXISTS public.home_sections (
    id TEXT PRIMARY KEY,
    type TEXT,
    title TEXT,
    icon TEXT,
    "isVisible" BOOLEAN DEFAULT true,
    "order" INT DEFAULT 0
);

-- Create Courses table
CREATE TABLE IF NOT EXISTS public.courses (
    id UUID PRIMARY KEY,
    subject TEXT,
    title TEXT,
    description TEXT,
    "thumbnailUrl" TEXT,
    "youtubeUrl" TEXT,
    "contentFileUrl" TEXT,
    "createdAt" BIGINT
);

-- Create Notes table
CREATE TABLE IF NOT EXISTS public.notes (
    id TEXT PRIMARY KEY,
    "userId" UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT,
    content TEXT,
    "associatedId" TEXT,
    "createdAt" BIGINT
);

-- Create Flashcard Decks table
CREATE TABLE IF NOT EXISTS public.flashcard_decks (
    id TEXT PRIMARY KEY,
    "userId" UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT,
    subject TEXT,
    "className" TEXT,
    "createdAt" BIGINT
);

-- Create Flashcards table
CREATE TABLE IF NOT EXISTS public.flashcards (
    id TEXT PRIMARY KEY,
    "deckId" TEXT,
    "frontText" TEXT,
    "backText" TEXT,
    "createdAt" BIGINT
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.books ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.videos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.banners ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.home_sections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.flashcard_decks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.flashcards ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "Users can view and manage their own profile" ON public.users;
DROP POLICY IF EXISTS "Admin can manage all users" ON public.users;
DROP POLICY IF EXISTS "Public read access for books" ON public.books;
DROP POLICY IF EXISTS "Admin write access for books" ON public.books;
DROP POLICY IF EXISTS "Public read access for videos" ON public.videos;
DROP POLICY IF EXISTS "Admin write access for videos" ON public.videos;
DROP POLICY IF EXISTS "Public read access for banners" ON public.banners;
DROP POLICY IF EXISTS "Admin write access for banners" ON public.banners;
DROP POLICY IF EXISTS "Authenticated read access for courses" ON public.courses;
DROP POLICY IF EXISTS "Admin write access for courses" ON public.courses;
DROP POLICY IF EXISTS "Users can manage own notes" ON public.notes;
DROP POLICY IF EXISTS "Users can manage own flashcard decks" ON public.flashcard_decks;
DROP POLICY IF EXISTS "Users can manage own flashcards" ON public.flashcards;

-- Create Admin check function
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean AS $$
DECLARE
  is_admin boolean;
BEGIN
  SELECT (role = 'admin') INTO is_admin FROM public.users WHERE id = auth.uid();
  RETURN COALESCE(is_admin, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Users policies
CREATE POLICY "Users can view and manage their own profile" ON public.users FOR ALL USING (auth.uid() = id);

-- Simplified admin policy
CREATE POLICY "Admin can manage all users" ON public.users FOR ALL USING ( public.is_admin() );

-- Books policies
CREATE POLICY "Authenticated read access for books" ON public.books FOR SELECT TO authenticated USING (true);
CREATE POLICY "Admin write access for books" ON public.books FOR ALL USING ( public.is_admin() );

-- Videos policies
CREATE POLICY "Authenticated read access for videos" ON public.videos FOR SELECT TO authenticated USING (true);
CREATE POLICY "Admin write access for videos" ON public.videos FOR ALL USING ( public.is_admin() );

-- Banners policies
CREATE POLICY "Public read access for banners" ON public.banners FOR SELECT USING (true);
CREATE POLICY "Admin write access for banners" ON public.banners FOR ALL USING ( public.is_admin() );

-- Home Sections policies
CREATE POLICY "Public read access for home_sections" ON public.home_sections FOR SELECT USING (true);
CREATE POLICY "Admin write access for home_sections" ON public.home_sections FOR ALL USING ( public.is_admin() );

-- Courses policies
CREATE POLICY "Public read access for courses" ON public.courses FOR SELECT USING (true);
CREATE POLICY "Admin write access for courses" ON public.courses FOR ALL USING ( public.is_admin() );

-- Notes policies
CREATE POLICY "Users can manage own notes" ON public.notes FOR ALL USING (auth.uid() = "userId");

-- Flashcard Decks policies
CREATE POLICY "Users can manage own flashcard decks" ON public.flashcard_decks FOR ALL USING (auth.uid() = "userId");

-- Flashcards policies
CREATE POLICY "Users can manage own flashcards" ON public.flashcards FOR ALL USING (
    EXISTS (
        SELECT 1 FROM public.flashcard_decks 
        WHERE public.flashcard_decks.id = public.flashcards."deckId" 
        AND public.flashcard_decks."userId" = auth.uid()
    )
);

-- Insert Storage Buckets
INSERT INTO storage.buckets (id, name, public) VALUES ('covers', 'covers', true) ON CONFLICT DO NOTHING;
INSERT INTO storage.buckets (id, name, public) VALUES ('pdfs', 'pdfs', true) ON CONFLICT DO NOTHING;
INSERT INTO storage.buckets (id, name, public) VALUES ('thumbnails', 'thumbnails', true) ON CONFLICT DO NOTHING;
-- Chat System Schema for Aura Learning App

-- Create conversations table
CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "isGroup" BOOLEAN DEFAULT false,
    "groupName" TEXT,
    "groupPhotoUrl" TEXT,
    "groupDescription" TEXT,
    "adminId" UUID REFERENCES auth.users(id),
    "lastMessageText" TEXT,
    "lastMessageTime" BIGINT,
    "createdAt" BIGINT DEFAULT extract(epoch from now()) * 1000
);

-- Create conversation_members table
CREATE TABLE IF NOT EXISTS public.conversation_members (
    "conversationId" UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
    "userId" UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    "joinedAt" BIGINT DEFAULT extract(epoch from now()) * 1000,
    "role" TEXT DEFAULT 'member', -- member, admin
    "isMuted" BOOLEAN DEFAULT false,
    "isArchived" BOOLEAN DEFAULT false,
    "isPinned" BOOLEAN DEFAULT false,
    PRIMARY KEY ("conversationId", "userId")
);

-- Create messages table
CREATE TABLE IF NOT EXISTS public.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "conversationId" UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
    "senderId" UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    "text" TEXT,
    "type" TEXT DEFAULT 'text', -- text, image, pdf, document, voice, book_link, course_link, quiz_link
    "attachmentUrl" TEXT,
    "replyToId" UUID REFERENCES public.messages(id) ON DELETE SET NULL,
    "createdAt" BIGINT DEFAULT extract(epoch from now()) * 1000,
    "updatedAt" BIGINT,
    "isDeleted" BOOLEAN DEFAULT false
);

-- Create message_reads table
CREATE TABLE IF NOT EXISTS public.message_reads (
    "messageId" UUID REFERENCES public.messages(id) ON DELETE CASCADE,
    "userId" UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    "readAt" BIGINT DEFAULT extract(epoch from now()) * 1000,
    PRIMARY KEY ("messageId", "userId")
);

-- Create message_reactions table
CREATE TABLE IF NOT EXISTS public.message_reactions (
    "messageId" UUID REFERENCES public.messages(id) ON DELETE CASCADE,
    "userId" UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    "reaction" TEXT,
    "createdAt" BIGINT DEFAULT extract(epoch from now()) * 1000,
    PRIMARY KEY ("messageId", "userId", "reaction")
);

-- Create user_presence table
CREATE TABLE IF NOT EXISTS public.user_presence (
    "userId" UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    "isOnline" BOOLEAN DEFAULT false,
    "lastSeen" BIGINT DEFAULT extract(epoch from now()) * 1000
);

-- Enable RLS
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_reads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_presence ENABLE ROW LEVEL SECURITY;

-- Conversations policies
CREATE POLICY "Users can view conversations they are part of" ON public.conversations FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.conversation_members WHERE "conversationId" = public.conversations.id AND "userId" = auth.uid())
);
CREATE POLICY "Users can create conversations" ON public.conversations FOR INSERT WITH CHECK (true);
CREATE POLICY "Users can update conversations they are part of" ON public.conversations FOR UPDATE USING (
    EXISTS (SELECT 1 FROM public.conversation_members WHERE "conversationId" = public.conversations.id AND "userId" = auth.uid())
);

-- Conversation Members policies
CREATE POLICY "Users can view members of their conversations" ON public.conversation_members FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.conversation_members cm WHERE cm."conversationId" = public.conversation_members."conversationId" AND cm."userId" = auth.uid())
);
CREATE POLICY "Users can add members to conversations" ON public.conversation_members FOR INSERT WITH CHECK (true);
CREATE POLICY "Users can update their own membership" ON public.conversation_members FOR UPDATE USING ("userId" = auth.uid());

-- Messages policies
CREATE POLICY "Users can view messages in their conversations" ON public.messages FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.conversation_members WHERE "conversationId" = public.messages."conversationId" AND "userId" = auth.uid())
);
CREATE POLICY "Users can send messages to their conversations" ON public.messages FOR INSERT WITH CHECK (
    EXISTS (SELECT 1 FROM public.conversation_members WHERE "conversationId" = public.messages."conversationId" AND "userId" = auth.uid())
);
CREATE POLICY "Users can update their own messages" ON public.messages FOR UPDATE USING ("senderId" = auth.uid());
CREATE POLICY "Users can delete their own messages" ON public.messages FOR DELETE USING ("senderId" = auth.uid());

-- Message Reads policies
CREATE POLICY "Users can view reads in their conversations" ON public.message_reads FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.messages m JOIN public.conversation_members cm ON m."conversationId" = cm."conversationId" WHERE m.id = public.message_reads."messageId" AND cm."userId" = auth.uid())
);
CREATE POLICY "Users can insert their own reads" ON public.message_reads FOR INSERT WITH CHECK ("userId" = auth.uid());

-- Message Reactions policies
CREATE POLICY "Users can view reactions in their conversations" ON public.message_reactions FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.messages m JOIN public.conversation_members cm ON m."conversationId" = cm."conversationId" WHERE m.id = public.message_reactions."messageId" AND cm."userId" = auth.uid())
);
CREATE POLICY "Users can manage their own reactions" ON public.message_reactions FOR ALL USING ("userId" = auth.uid());

-- User Presence policies
CREATE POLICY "Public read access for user presence" ON public.user_presence FOR SELECT USING (true);
CREATE POLICY "Users can update their own presence" ON public.user_presence FOR ALL USING ("userId" = auth.uid());

-- Storage Buckets for Chat
INSERT INTO storage.buckets (id, name, public) VALUES ('chat_attachments', 'chat_attachments', true) ON CONFLICT DO NOTHING;


-- Enable real-time for courses
BEGIN;
  DROP PUBLICATION IF EXISTS supabase_realtime;
  CREATE PUBLICATION supabase_realtime;
COMMIT;
ALTER PUBLICATION supabase_realtime ADD TABLE public.courses;

-- Quiz System Schema
CREATE TABLE IF NOT EXISTS public.quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT,
    description TEXT,
    "className" TEXT,
    subject TEXT,
    "associatedId" TEXT,
    "createdAt" BIGINT DEFAULT extract(epoch from now()) * 1000
);

CREATE TABLE IF NOT EXISTS public.quiz_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "quizId" UUID REFERENCES public.quizzes(id) ON DELETE CASCADE,
    "questionText" TEXT,
    options TEXT[],
    "correctOptionIndex" INT,
    explanation TEXT,
    "order" INT
);

CREATE TABLE IF NOT EXISTS public.quiz_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "quizId" UUID REFERENCES public.quizzes(id) ON DELETE CASCADE,
    "userId" UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    score INT,
    "totalQuestions" INT,
    "createdAt" BIGINT DEFAULT extract(epoch from now()) * 1000
);

ALTER TABLE public.quizzes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quiz_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quiz_results ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read access for quizzes" ON public.quizzes FOR SELECT USING (true);
CREATE POLICY "Admin write access for quizzes" ON public.quizzes FOR ALL USING (public.is_admin());

CREATE POLICY "Public read access for quiz_questions" ON public.quiz_questions FOR SELECT USING (true);
CREATE POLICY "Admin write access for quiz_questions" ON public.quiz_questions FOR ALL USING (public.is_admin());

CREATE POLICY "Users can view own quiz results" ON public.quiz_results FOR SELECT USING (auth.uid() = "userId");
CREATE POLICY "Users can insert own quiz results" ON public.quiz_results FOR INSERT WITH CHECK (auth.uid() = "userId");
