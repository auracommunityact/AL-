-- Enable pg_trgm extension for fast fuzzy and partial text searching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create GIN indexes on users table for lightning-fast partial text search on username, display_name, name, and email
CREATE INDEX IF NOT EXISTS users_username_gin_idx ON public.users USING gin (username gin_trgm_ops);
CREATE INDEX IF NOT EXISTS users_display_name_gin_idx ON public.users USING gin (display_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS users_name_gin_idx ON public.users USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS users_email_gin_idx ON public.users USING gin (email gin_trgm_ops);

-- Ensure RLS policy allows authenticated users to search public profile data safely
DROP POLICY IF EXISTS "Authenticated users can search public profiles" ON public.users;
CREATE POLICY "Authenticated users can search public profiles" 
ON public.users 
FOR SELECT 
TO authenticated 
USING (true);
