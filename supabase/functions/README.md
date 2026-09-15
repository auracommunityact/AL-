# Aura AI Supabase Edge Function

This is the secure cloud-based OpenAI backend for Aura Learning. It replaces the local offline AI with a cloud-based `gpt-4o-mini` implementation, securely managing the `OPENAI_API_KEY` on the server.

## Features
- Handles text and multimodal (image + text) inference.
- Authenticates users via Supabase JWT tokens.
- Never exposes `OPENAI_API_KEY` to the client.

## Deployment

1. Make sure you have the Supabase CLI installed and are logged in:
   ```bash
   supabase login
   ```

2. Link your project:
   ```bash
   supabase link --project-ref <your-project-ref>
   ```

3. Deploy the Edge Function:
   ```bash
   supabase functions deploy aura-ai
   ```

4. Configure the OpenAI API Key as a Supabase Secret:
   ```bash
   supabase secrets set OPENAI_API_KEY=your_actual_openai_api_key_here
   ```

## Client Configuration

After deployment, update `AURA_VISION_API_URL` in the Android `app/build.gradle.kts` file with your function URL. It should look like:
`https://<your-project-ref>.supabase.co/functions/v1/aura-ai`

## Local Development

For testing locally with the Supabase CLI, you can start the edge function and pass the secret in a `.env.local` file:

1. Create a `.env.local` file in `supabase/functions/aura-ai/`:
   ```
   OPENAI_API_KEY=your_test_key_here
   ```

2. Serve the functions locally:
   ```bash
   supabase functions serve --env-file supabase/functions/aura-ai/.env.local
   ```
