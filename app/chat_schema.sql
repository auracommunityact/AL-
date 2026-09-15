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

