# Supabase Setup Guide & Database Schema — WANDR App

This document provides a comprehensive setup guide, SQL migrations, Row Level Security (RLS) policies, and storage configurations for the **WANDR** backend on **Supabase**.

---

## 📋 Table of Contents
1. [Overview & Prerequisites](#overview--prerequisites)
2. [Database Schema (DDL)](#database-schema-ddl)
3. [Row Level Security (RLS) Policies](#row-level-security-rls-policies)
4. [Storage Buckets & Policies](#storage-buckets--policies)
5. [Database Functions & Triggers](#database-functions--triggers)
6. [KMP Client Setup (Ktor & Supabase Kotlin SDK)](#kmp-client-setup)

---

## 1. Overview & Prerequisites

WANDR uses Supabase for:
- **Authentication**: Email & Password authentication only.
- **Postgres Database**: Storage for profiles, teams, challenges, activities, social feeds, and notifications.
- **Storage**: Buckets for user avatars, team covers and challenge cover photos. Recorded `.FIT` files are **not** stored on Supabase; they stay on the device that recorded them.
- **Realtime / Webhooks / Edge Functions**: Realtime for comments, likes and notifications (`supabase_realtime` publication); a Database Webhook on `notifications` triggers the `send-push` Edge Function (FCM for Android, APNs for iOS), see [Push notifications](#push-notifications).

### Supabase CLI Setup
To run Supabase locally or manage migrations:
```bash
# Install Supabase CLI
brew install supabase/tap/supabase

# Initialize Supabase in the project root
supabase init

# Link to your remote Supabase project
supabase link --project-ref <YOUR_PROJECT_REF>

# Apply local migrations
supabase db push
```

---

## 2. Database Schema (DDL)

Copy and execute the following SQL script in your Supabase SQL Editor or save it in `supabase/migrations/20261001000000_init_schema.sql`.

```sql
-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Custom ENUM Types
-- ============================================================================
-- Application-wide role of a user (profiles.system_role); managers create teams and challenges.
CREATE TYPE system_role AS ENUM ('user', 'manager');
-- Role of a user inside one team (team_members.role)
CREATE TYPE team_role AS ENUM ('admin', 'member');
CREATE TYPE challenge_type AS ENUM ('distance', 'elevation', 'time');
-- Only draft / active are stored: draft = not published yet (only the creator sees it), active = published.
-- 'planned' (published, starts later), 'completed' and 'expired' are derived at runtime from the dates (and the
-- participants' progress), so they can never be stale.
CREATE TYPE challenge_status AS ENUM ('draft', 'active');
CREATE TYPE challenge_scope AS ENUM ('group', 'individual');
CREATE TYPE social_entity_type AS ENUM ('activity', 'challenge');

-- ============================================================================
-- 1. Profiles Table (Linked to auth.users)
-- ============================================================================
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    system_role system_role NOT NULL DEFAULT 'user',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 2. Teams / Groups Table & Members
-- ============================================================================
CREATE TABLE public.teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    description TEXT,
    avatar_url TEXT,
    cover_url TEXT,
    invite_code TEXT UNIQUE NOT NULL DEFAULT substring(md5(random()::text) from 1 for 8),
    created_by UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE public.team_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID NOT NULL REFERENCES public.teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    role team_role NOT NULL DEFAULT 'member',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Order of the user's teams, 0 = highest priority (set by the user via set_team_priorities). Only the team with
    -- the highest priority counts for group challenges. New memberships are appended (trigger).
    priority INTEGER NOT NULL DEFAULT 0,
    UNIQUE(team_id, user_id)
);

-- ============================================================================
-- 3. Challenges & Participants
-- ============================================================================
-- A challenge is NOT bound to a single team.
--   scope 'individual': open to every user, everybody takes part for themselves.
--   scope 'group':      TEAMS compete against OTHER TEAMS. Team owners/admins enroll their team (challenge_teams);
--                       all members then contribute to their team's result. Members of one team do not compete
--                       against each other; the standings rank teams.
CREATE TABLE public.challenges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    description TEXT,
    cover_url TEXT,
    scope challenge_scope NOT NULL DEFAULT 'group',
    type challenge_type NOT NULL,
    target_value DOUBLE PRECISION NOT NULL, -- Distance in meters, Elevation in meters, Time in seconds
    require_all_members_completion BOOLEAN NOT NULL DEFAULT FALSE, -- All-or-Nothing: a team only completes when EVERY member completed
    activity_types TEXT[] NOT NULL DEFAULT '{}', -- activity types that count (hiking, running, cycling); empty = every type counts
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    status challenge_status NOT NULL DEFAULT 'draft', -- draft: only the creator sees it; active: published
    created_by UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Teams taking part in a group challenge.
CREATE TABLE public.challenge_teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    challenge_id UUID NOT NULL REFERENCES public.challenges(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES public.teams(id) ON DELETE CASCADE,
    enrolled_by UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(challenge_id, team_id)
);

-- One row per participating user. In a group challenge the rows are created by triggers (see section 5) when
-- a team is enrolled, and team_id says which team the user contributes for (a user contributes to one team per
-- challenge). In an individual challenge team_id is NULL.
CREATE TABLE public.challenge_participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    challenge_id UUID NOT NULL REFERENCES public.challenges(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    team_id UUID REFERENCES public.teams(id) ON DELETE CASCADE,
    progress_value DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    UNIQUE(challenge_id, user_id)
);

-- ============================================================================
-- 4. Activities (Recorded & Manual Entries)
-- Only the metrics are synced. The raw .FIT track file stays on the device that recorded the activity.
-- ============================================================================
CREATE TABLE public.activities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    team_id UUID REFERENCES public.teams(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    description TEXT,
    activity_type TEXT NOT NULL DEFAULT 'hiking', -- hiking, running, cycling, etc.
    distance_meters DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    duration_seconds DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    elevation_gain_meters DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    is_manual_entry BOOLEAN NOT NULL DEFAULT FALSE,
    -- Privacy setting of the owner: FALSE = other users must not see the route (the owner always sees it).
    -- The route itself lives in activity_routes (below), because RLS cannot hide a single column per row.
    show_map BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for time overlap detection (Conflict Resolution Wizard)
CREATE INDEX idx_activities_user_time ON public.activities(user_id, start_time, end_time);

-- Simplified track of an activity as Google Encoded Polyline (precision 5). Separate table on purpose: a polyline
-- column on activities would be readable by everybody who may read the activity, i.e. it would leak the routes of
-- activities with show_map = FALSE. The client upserts the route right after the activity (on conflict activity_id).
-- Other users only get the row if show_map is TRUE (policies in section 3).
CREATE TABLE IF NOT EXISTS public.activity_routes (
    activity_id UUID PRIMARY KEY REFERENCES public.activities(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    polyline TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 5. Social Feeds: Likes, Comments & Reactions
-- ============================================================================
CREATE TABLE public.likes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type social_entity_type NOT NULL,
    entity_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(entity_type, entity_id, user_id)
);

CREATE TABLE public.comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type social_entity_type NOT NULL,
    entity_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE public.comment_reactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    comment_id UUID NOT NULL REFERENCES public.comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    emoji TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(comment_id, user_id, emoji)
);

-- ============================================================================
-- 6. Notifications
-- ============================================================================
CREATE TABLE public.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    actor_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    notification_type TEXT NOT NULL, -- 'like', 'comment', 'reaction', 'invite'
    target_id UUID NOT NULL,
    payload JSONB DEFAULT '{}'::jsonb,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for the social feed and the notification inbox
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created ON public.notifications(recipient_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_entity_created ON public.comments(entity_type, entity_id, created_at);
CREATE INDEX IF NOT EXISTS idx_likes_entity ON public.likes(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_comment_reactions_comment ON public.comment_reactions(comment_id);

-- ============================================================================
-- 7. Push Device Tokens (FCM token on Android, APNs token on iOS)
-- Written via RPC public.register_device_token (section 5); push delivery: Edge Function send-push.
-- ============================================================================
CREATE TABLE public.device_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    platform TEXT NOT NULL CHECK (platform IN ('android', 'ios')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON public.device_tokens(user_id);
```

---

## 3. Row Level Security (RLS) Policies

WANDR uses strict **Privacy-First RLS Policies** so that user data and team feeds are accessible **only** by verified team members.

```sql
-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.team_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.challenge_teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.challenge_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activity_routes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comment_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_tokens ENABLE ROW LEVEL SECURITY;

-- ----------------------------------------------------------------------------
-- Helper Function: Check Team Membership
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_team_member(check_team_id UUID, check_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.team_members
        WHERE team_id = check_team_id AND user_id = check_user_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ----------------------------------------------------------------------------
-- Helper Function: Check Team Ownership / Admin Role
-- SECURITY DEFINER so it also works inside storage policies, independent of the caller's table visibility.
-- check_team_id is TEXT so storage folder names (text) can be compared without a failing uuid cast.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_team_admin(check_team_id TEXT, check_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.teams t
        WHERE t.id::text = check_team_id AND t.created_by = check_user_id
    ) OR EXISTS (
        SELECT 1 FROM public.team_members m
        WHERE m.team_id::text = check_team_id AND m.user_id = check_user_id AND m.role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE SET search_path = public;

-- ----------------------------------------------------------------------------
-- Helper Functions: Social entity access (likes, comments, reactions)
-- activity: owner or member of the activity's team; challenge: published (active) or own draft.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.can_access_entity(p_type social_entity_type, p_id UUID, p_user UUID)
RETURNS BOOLEAN AS $$
    SELECT CASE p_type
        WHEN 'activity' THEN EXISTS (
            SELECT 1 FROM public.activities a
            WHERE a.id = p_id
              AND (a.user_id = p_user OR (a.team_id IS NOT NULL AND public.is_team_member(a.team_id, p_user)))
        )
        WHEN 'challenge' THEN EXISTS (
            SELECT 1 FROM public.challenges c
            WHERE c.id = p_id AND (c.status = 'active' OR c.created_by = p_user)
        )
        ELSE FALSE
    END;
$$ LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public;

-- Owner of the social entity (activity.user_id / challenge.created_by), NULL if it does not exist.
CREATE OR REPLACE FUNCTION public.entity_owner_id(p_type social_entity_type, p_id UUID)
RETURNS UUID AS $$
    SELECT CASE p_type
        WHEN 'activity' THEN (SELECT a.user_id FROM public.activities a WHERE a.id = p_id)
        WHEN 'challenge' THEN (SELECT c.created_by FROM public.challenges c WHERE c.id = p_id)
    END;
$$ LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public;

CREATE OR REPLACE FUNCTION public.is_entity_owner(p_type social_entity_type, p_id UUID, p_user UUID)
RETURNS BOOLEAN AS $$
    SELECT COALESCE(public.entity_owner_id(p_type, p_id) = p_user, FALSE);
$$ LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public;

-- ----------------------------------------------------------------------------
-- Profiles Policies
-- ----------------------------------------------------------------------------
CREATE POLICY "Public profiles are readable by authenticated users"
    ON public.profiles FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE TO authenticated
    USING (auth.uid() = id);

-- ----------------------------------------------------------------------------
-- Teams & Team Members Policies
-- ----------------------------------------------------------------------------
CREATE POLICY "Users can view teams they are members of"
    ON public.teams FOR SELECT TO authenticated
    USING (public.is_team_member(id, auth.uid()));

CREATE POLICY "Only managers can create teams"
    ON public.teams FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = created_by AND
        EXISTS (
            SELECT 1 FROM public.profiles
            WHERE id = auth.uid() AND system_role = 'manager'
        )
    );

CREATE POLICY "Team owners and admins can edit team"
    ON public.teams FOR UPDATE TO authenticated
    USING (public.is_team_admin(id::text, auth.uid()));

CREATE POLICY "Only managers can delete teams"
    ON public.teams FOR DELETE TO authenticated
    USING (
        auth.uid() = created_by AND
        EXISTS (
            SELECT 1 FROM public.profiles
            WHERE id = auth.uid() AND system_role = 'manager'
        )
    );

CREATE POLICY "Team members can view membership"
    ON public.team_members FOR SELECT TO authenticated
    USING (public.is_team_member(team_id, auth.uid()));

CREATE POLICY "Users can join team via invite"
    ON public.team_members FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Only team owners can assign admin role"
    ON public.team_members FOR UPDATE TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.teams
            WHERE id = team_id AND created_by = auth.uid()
        )
    );

CREATE POLICY "Team owners can remove members or members can leave"
    ON public.team_members FOR DELETE TO authenticated
    USING (
        auth.uid() = user_id OR
        EXISTS (
            SELECT 1 FROM public.teams
            WHERE id = team_id AND created_by = auth.uid()
        )
    );

-- ----------------------------------------------------------------------------
-- Challenges & Challenge Participants Policies (Manager Rights & Scope Rules)
-- ----------------------------------------------------------------------------
-- A GROUP challenge is not tied to one team: teams enroll and compete AGAINST EACH OTHER. The members of one team
-- contribute to their team's result and do not compete against each other. Published (active) challenges are public,
-- so team owners/admins can find them and enroll; drafts are only visible to their creator.
CREATE POLICY "Published challenges and own drafts are visible"
    ON public.challenges FOR SELECT TO authenticated
    USING (status = 'active' OR auth.uid() = created_by);

CREATE POLICY "Only managers can create challenges"
    ON public.challenges FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = created_by AND
        EXISTS (
            SELECT 1 FROM public.profiles
            WHERE id = auth.uid() AND system_role = 'manager'
        )
    );

-- Only managers create challenges (above); only the creator (owner) edits or deletes them.
CREATE POLICY "Only the creator can edit a challenge"
    ON public.challenges FOR UPDATE TO authenticated
    USING (auth.uid() = created_by)
    WITH CHECK (auth.uid() = created_by);

CREATE POLICY "Only the creator can delete a challenge"
    ON public.challenges FOR DELETE TO authenticated
    USING (auth.uid() = created_by);

-- Team enrollment: which teams compete is public (it is the competition); only owners/admins enroll or withdraw.
-- Enrolling inserts the members as participants (trigger in section 5).
CREATE POLICY "Enrolled teams are visible"
    ON public.challenge_teams FOR SELECT TO authenticated
    USING (true);

CREATE POLICY "Team owners and admins can enroll their team"
    ON public.challenge_teams FOR INSERT TO authenticated
    WITH CHECK (
        enrolled_by = auth.uid() AND
        public.is_team_admin(team_id::text, auth.uid()) AND
        EXISTS (SELECT 1 FROM public.challenges c WHERE c.id = challenge_id AND c.scope = 'group' AND c.status = 'active')
    );

CREATE POLICY "Team owners and admins can withdraw their team"
    ON public.challenge_teams FOR DELETE TO authenticated
    USING (public.is_team_admin(team_id::text, auth.uid()));

-- Privacy first: the progress of a single user is visible to the user, to the members of the team they contribute
-- for, and to the manager who created the challenge. Other teams only see the aggregated team standings
-- (function challenge_team_standings, section 5), never individual members of other teams.
CREATE POLICY "Own and own team's participants are visible (Privacy-First)"
    ON public.challenge_participants FOR SELECT TO authenticated
    USING (
        user_id = auth.uid() OR
        (team_id IS NOT NULL AND public.is_team_member(team_id, auth.uid())) OR
        EXISTS (SELECT 1 FROM public.challenges c WHERE c.id = challenge_id AND c.created_by = auth.uid())
    );

-- Individual challenges: users join and quit themselves. Group participants are managed by triggers, which
-- follow the team's enrollment and membership (they run as SECURITY DEFINER and bypass these policies).
CREATE POLICY "Users can join individual challenges"
    ON public.challenge_participants FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = user_id AND team_id IS NULL AND
        EXISTS (SELECT 1 FROM public.challenges WHERE id = challenge_id AND scope = 'individual' AND status = 'active')
    );

CREATE POLICY "Users can quit individual challenges"
    ON public.challenge_participants FOR DELETE TO authenticated
    USING (auth.uid() = user_id AND team_id IS NULL);

-- No UPDATE policy on challenge_participants: progress is calculated by the server from the user's activities
-- (section 5, "Challenge progress"), so nobody can set it by hand.

-- ----------------------------------------------------------------------------
-- Activities Policies (Privacy First)
-- ----------------------------------------------------------------------------
CREATE POLICY "Users can view their own activities or team members' activities"
    ON public.activities FOR SELECT TO authenticated
    USING (
        auth.uid() = user_id OR
        (team_id IS NOT NULL AND public.is_team_member(team_id, auth.uid()))
    );

CREATE POLICY "Users can insert their own activities"
    ON public.activities FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update or delete their own activities"
    ON public.activities FOR ALL TO authenticated
    USING (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- Activity Routes Policies (map privacy)
-- Visible to the owner, and to the activity's team members only while the activity has show_map = TRUE.
-- Writes: own rows only, and only for an activity that belongs to the caller.
-- ----------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can view own routes or shared team routes" ON public.activity_routes;
CREATE POLICY "Users can view own routes or shared team routes"
    ON public.activity_routes FOR SELECT TO authenticated
    USING (
        user_id = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.activities a
            WHERE a.id = activity_routes.activity_id
              AND a.show_map = TRUE
              AND a.team_id IS NOT NULL
              AND public.is_team_member(a.team_id, auth.uid())
        )
    );

DROP POLICY IF EXISTS "Users can insert their own routes" ON public.activity_routes;
CREATE POLICY "Users can insert their own routes"
    ON public.activity_routes FOR INSERT TO authenticated
    WITH CHECK (
        user_id = auth.uid() AND
        EXISTS (SELECT 1 FROM public.activities a WHERE a.id = activity_routes.activity_id AND a.user_id = auth.uid())
    );

DROP POLICY IF EXISTS "Users can update their own routes" ON public.activity_routes;
CREATE POLICY "Users can update their own routes"
    ON public.activity_routes FOR UPDATE TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (
        user_id = auth.uid() AND
        EXISTS (SELECT 1 FROM public.activities a WHERE a.id = activity_routes.activity_id AND a.user_id = auth.uid())
    );

DROP POLICY IF EXISTS "Users can delete their own routes" ON public.activity_routes;
CREATE POLICY "Users can delete their own routes"
    ON public.activity_routes FOR DELETE TO authenticated
    USING (user_id = auth.uid());

-- ----------------------------------------------------------------------------
-- Likes, Comments, Reactions & Notifications Policies
-- Everything follows can_access_entity(). Notifications have NO client INSERT policy: they are created by
-- SECURITY DEFINER triggers (section 5). Device tokens are own rows only.
-- ----------------------------------------------------------------------------
CREATE POLICY "Users can view likes on accessible entities"
    ON public.likes FOR SELECT TO authenticated
    USING (public.can_access_entity(entity_type, entity_id, auth.uid()));

CREATE POLICY "Users can like accessible entities"
    ON public.likes FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid() AND public.can_access_entity(entity_type, entity_id, auth.uid()));

CREATE POLICY "Users can remove their own likes"
    ON public.likes FOR DELETE TO authenticated
    USING (user_id = auth.uid());

CREATE POLICY "Users can view comments on accessible entities"
    ON public.comments FOR SELECT TO authenticated
    USING (public.can_access_entity(entity_type, entity_id, auth.uid()));

CREATE POLICY "Users can comment on accessible entities"
    ON public.comments FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid() AND public.can_access_entity(entity_type, entity_id, auth.uid()));

CREATE POLICY "Authors can edit their comments"
    ON public.comments FOR UPDATE TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Authors and entity owners can delete comments"
    ON public.comments FOR DELETE TO authenticated
    USING (user_id = auth.uid() OR public.is_entity_owner(entity_type, entity_id, auth.uid()));

CREATE POLICY "Users can view reactions on accessible comments"
    ON public.comment_reactions FOR SELECT TO authenticated
    USING (EXISTS (
        SELECT 1 FROM public.comments c
        WHERE c.id = comment_id AND public.can_access_entity(c.entity_type, c.entity_id, auth.uid())
    ));

CREATE POLICY "Users can react on accessible comments"
    ON public.comment_reactions FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid() AND EXISTS (
        SELECT 1 FROM public.comments c
        WHERE c.id = comment_id AND public.can_access_entity(c.entity_type, c.entity_id, auth.uid())
    ));

CREATE POLICY "Users can remove their own reactions"
    ON public.comment_reactions FOR DELETE TO authenticated
    USING (user_id = auth.uid());

CREATE POLICY "Users can view their own notifications"
    ON public.notifications FOR SELECT TO authenticated
    USING (recipient_user_id = auth.uid());

CREATE POLICY "Users can mark their own notifications as read"
    ON public.notifications FOR UPDATE TO authenticated
    USING (recipient_user_id = auth.uid())
    WITH CHECK (recipient_user_id = auth.uid());

CREATE POLICY "Users can delete their own notifications"
    ON public.notifications FOR DELETE TO authenticated
    USING (recipient_user_id = auth.uid());

CREATE POLICY "Users can view their own device tokens"
    ON public.device_tokens FOR SELECT TO authenticated
    USING (user_id = auth.uid());

CREATE POLICY "Users can register their own device tokens"
    ON public.device_tokens FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update their own device tokens"
    ON public.device_tokens FOR UPDATE TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can delete their own device tokens"
    ON public.device_tokens FOR DELETE TO authenticated
    USING (user_id = auth.uid());

-- Clients may only flip is_read (no INSERT policy exists, so only triggers create notifications).
REVOKE UPDATE ON public.notifications FROM authenticated;
GRANT UPDATE (is_read) ON public.notifications TO authenticated;
```

---

## 4. Storage Buckets & Policies

Create the storage buckets in the Supabase Dashboard or via SQL. (`.FIT` activity files are intentionally not stored here, they stay on the recording device.)

1. **`avatars`** (Public) — User profile pictures & team avatars.
2. **`team-covers`** (Public) — Cover photos for teams & groups.
3. **`challenge-covers`** (Public) — Cover images for challenges.

```sql
-- Create buckets
INSERT INTO storage.buckets (id, name, public) VALUES ('avatars', 'avatars', true);
INSERT INTO storage.buckets (id, name, public) VALUES ('team-covers', 'team-covers', true);
INSERT INTO storage.buckets (id, name, public) VALUES ('challenge-covers', 'challenge-covers', true);

-- Storage Policies
CREATE POLICY "Avatar Read Access" ON storage.objects FOR SELECT TO authenticated USING (bucket_id = 'avatars');
CREATE POLICY "Avatar Upload Access" ON storage.objects FOR INSERT TO authenticated WITH CHECK (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);
-- Avatar replace/remove: users manage only files in their own folder (<user_id>/...).
-- Required by the profile avatar feature, which deletes the previous avatar file after an update/removal.
CREATE POLICY "Avatar Delete Access" ON storage.objects FOR DELETE TO authenticated USING (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);

-- Team avatar / cover (folder <team_id>/...): everyone signed in can read, only the team owner and admins can add or remove.
-- IMPORTANT: the SELECT policy is also required for uploads. Storage inserts with RETURNING, and without a
-- matching SELECT policy Postgres rejects the upload with "new row violates row-level security policy".
CREATE POLICY "Team Image Read Access" ON storage.objects FOR SELECT TO authenticated USING (bucket_id = 'team-covers');
CREATE POLICY "Team Image Upload Access" ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'team-covers' AND public.is_team_admin((storage.foldername(name))[1], auth.uid()));
CREATE POLICY "Team Image Delete Access" ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'team-covers' AND public.is_team_admin((storage.foldername(name))[1], auth.uid()));

-- Challenge cover (folder <challenge_id>/...): everyone signed in can read, only the creating manager can add or remove.
CREATE POLICY "Challenge Cover Read Access" ON storage.objects FOR SELECT TO authenticated USING (bucket_id = 'challenge-covers');
CREATE POLICY "Challenge Cover Upload Access" ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'challenge-covers' AND EXISTS (
        SELECT 1 FROM public.challenges c
        WHERE c.id::text = (storage.foldername(name))[1] AND c.created_by = auth.uid()
    ));
CREATE POLICY "Challenge Cover Delete Access" ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'challenge-covers' AND EXISTS (
        SELECT 1 FROM public.challenges c
        WHERE c.id::text = (storage.foldername(name))[1] AND c.created_by = auth.uid()
    ));

```

---

## 5. Database Functions & Triggers

### Auto-Create Profile on Signup
Automatically populates the `public.profiles` table whenever a new user signs up via Supabase Auth.

```sql
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, display_name, avatar_url)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1)),
        COALESCE(NEW.raw_user_meta_data->>'display_name', split_part(NEW.email, '@', 1)),
        NEW.raw_user_meta_data->>'avatar_url'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
```

### Group challenges: team enrollment & standings

Teams compete against other teams. When a team is enrolled, all its members become participants for that team;
joining or leaving the team keeps this in sync; withdrawing the team removes them again. A user contributes for ONE
team only: the team with the highest priority (`team_members.priority`, the user orders their teams in the profile).
Group challenges of the user's other teams do not count their progress. Changing the order moves the user's
participation (in challenges that are not over yet) to the new top team.

```sql
-- The team a user contributes for: their highest-priority membership (lowest number).
CREATE OR REPLACE FUNCTION public.primary_team_id(p_user UUID)
RETURNS UUID AS $$
    SELECT team_id FROM public.team_members WHERE user_id = p_user ORDER BY priority, joined_at LIMIT 1;
$$ LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public;

-- Brings the group participations of a user in line with their primary team (challenges that are not over yet).
CREATE OR REPLACE FUNCTION public.sync_group_participation(p_user UUID)
RETURNS VOID AS $$
DECLARE
    v_primary UUID := public.primary_team_id(p_user);
BEGIN
    DELETE FROM public.challenge_participants p
    USING public.challenges c
    WHERE p.user_id = p_user AND p.team_id IS NOT NULL AND c.id = p.challenge_id
      AND c.end_date >= NOW() AND p.team_id IS DISTINCT FROM v_primary;

    IF v_primary IS NOT NULL THEN
        INSERT INTO public.challenge_participants (challenge_id, user_id, team_id)
        SELECT ct.challenge_id, p_user, v_primary
        FROM public.challenge_teams ct
        JOIN public.challenges c ON c.id = ct.challenge_id
        WHERE ct.team_id = v_primary AND c.end_date >= NOW()
        ON CONFLICT (challenge_id, user_id) DO NOTHING;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Enrolling a team adds those members as participants for whom it is the highest-priority team.
CREATE OR REPLACE FUNCTION public.enroll_team_members()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.challenge_participants (challenge_id, user_id, team_id)
    SELECT NEW.challenge_id, tm.user_id, NEW.team_id
    FROM public.team_members tm
    WHERE tm.team_id = NEW.team_id AND public.primary_team_id(tm.user_id) = NEW.team_id
    ON CONFLICT (challenge_id, user_id) DO NOTHING;
    -- The new participants start with the matching activities they recorded before (on_participant_init); this makes
    -- sure of it for everyone, also for rows that already existed.
    PERFORM public.refresh_challenge_progress(NEW.challenge_id, NULL);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_team_enrolled
    AFTER INSERT ON public.challenge_teams
    FOR EACH ROW EXECUTE FUNCTION public.enroll_team_members();

-- Withdrawing a team removes its members from that challenge.
CREATE OR REPLACE FUNCTION public.withdraw_team_members()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM public.challenge_participants
    WHERE challenge_id = OLD.challenge_id AND team_id = OLD.team_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_team_withdrawn
    AFTER DELETE ON public.challenge_teams
    FOR EACH ROW EXECUTE FUNCTION public.withdraw_team_members();

-- A new membership is appended at the end of the user's priority list.
CREATE OR REPLACE FUNCTION public.assign_team_member_priority()
RETURNS TRIGGER AS $$
BEGIN
    NEW.priority := COALESCE((SELECT MAX(priority) + 1 FROM public.team_members WHERE user_id = NEW.user_id), 0);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_team_member_priority
    BEFORE INSERT ON public.team_members
    FOR EACH ROW EXECUTE FUNCTION public.assign_team_member_priority();

-- Joining a team (as the first one) makes the user take part in its running challenges.
CREATE OR REPLACE FUNCTION public.join_team_challenges()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM public.sync_group_participation(NEW.user_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_team_member_added
    AFTER INSERT ON public.team_members
    FOR EACH ROW EXECUTE FUNCTION public.join_team_challenges();

-- Leaving a team stops contributing for it; the next team in line takes over.
CREATE OR REPLACE FUNCTION public.leave_team_challenges()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM public.sync_group_participation(OLD.user_id);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_team_member_removed
    AFTER DELETE ON public.team_members
    FOR EACH ROW EXECUTE FUNCTION public.leave_team_challenges();

-- Saves the order of the caller's teams: the listed teams first (in that order), memberships that are not listed keep
-- their relative order behind them. Then the group participations follow the new top team.
CREATE OR REPLACE FUNCTION public.set_team_priorities(p_team_ids UUID[])
RETURNS VOID AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    UPDATE public.team_members m
    SET priority = o.rank - 1
    FROM (
        SELECT x.team_id,
               ROW_NUMBER() OVER (ORDER BY (x.idx IS NULL), x.idx, x.priority, x.joined_at) AS rank
        FROM (
            SELECT tm.team_id, tm.priority, tm.joined_at, array_position(p_team_ids, tm.team_id) AS idx
            FROM public.team_members tm WHERE tm.user_id = auth.uid()
        ) x
    ) o
    WHERE m.user_id = auth.uid() AND m.team_id = o.team_id;

    PERFORM public.sync_group_participation(auth.uid());
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

REVOKE ALL ON FUNCTION public.set_team_priorities(UUID[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_team_priorities(UUID[]) TO authenticated;

-- Joins a team by its invite code (typed or scanned). A function, because a non-member cannot read the team (RLS)
-- to look the code up. The code is compared case-insensitively.
CREATE OR REPLACE FUNCTION public.join_team_by_invite(p_invite_code TEXT)
RETURNS public.teams AS $$
DECLARE
    v_team public.teams;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT * INTO v_team FROM public.teams WHERE lower(invite_code) = lower(trim(p_invite_code));
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Invalid invite code';
    END IF;

    INSERT INTO public.team_members (team_id, user_id) VALUES (v_team.id, auth.uid())
    ON CONFLICT (team_id, user_id) DO NOTHING;
    RETURN v_team;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

REVOKE ALL ON FUNCTION public.join_team_by_invite(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.join_team_by_invite(TEXT) TO authenticated;

-- Team vs. team standings: only aggregates per team, so nobody sees individual members of other teams.
CREATE OR REPLACE FUNCTION public.challenge_team_standings(p_challenge_id UUID)
RETURNS TABLE (
    team_id UUID,
    team_name TEXT,
    team_avatar_url TEXT,
    total_progress DOUBLE PRECISION,
    member_count INTEGER,
    completed_count INTEGER
) AS $$
    SELECT t.id, t.name, t.avatar_url,
           COALESCE(SUM(p.progress_value), 0)::double precision,
           COUNT(p.id)::integer,
           (COUNT(p.id) FILTER (WHERE p.is_completed))::integer
    FROM public.challenge_teams ct
    JOIN public.teams t ON t.id = ct.team_id
    LEFT JOIN public.challenge_participants p
        ON p.challenge_id = ct.challenge_id AND p.team_id = ct.team_id
    WHERE ct.challenge_id = p_challenge_id
    GROUP BY t.id, t.name, t.avatar_url
    ORDER BY 4 DESC;
$$ LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public;
```


### Challenge progress

The progress of a participant is calculated by the server, never set by clients: every change of an activity
(insert, update, delete), every new participant and every change of a challenge's period, type, target or activity
types recalculates it. A challenge counts the user's activities that **started within its period** and whose type is
one of the challenge's `activity_types` (**no activity types = every type counts**): the distance (m) for `distance`,
the elevation gain (m) for `elevation`, the duration (s) for `time`. Reaching `target_value` completes the participation.
Activities only count once they were synced to the server.

```sql
-- Progress of one user in one challenge: the sum of the matching activities that started within the challenge period
-- (distance / elevation gain in meters, duration in seconds, depending on the challenge type). Without activity types
-- on the challenge every activity type counts.
CREATE OR REPLACE FUNCTION public.challenge_progress_for(p_challenge_id UUID, p_user UUID)
RETURNS DOUBLE PRECISION AS $$
    SELECT COALESCE(SUM(CASE c.type
               WHEN 'distance'  THEN a.distance_meters
               WHEN 'elevation' THEN a.elevation_gain_meters
               WHEN 'time'      THEN a.duration_seconds
           END), 0)::double precision
    FROM public.challenges c
    LEFT JOIN public.activities a
        ON a.user_id = p_user
       AND a.start_time >= c.start_date AND a.start_time < c.end_date
       AND (cardinality(c.activity_types) = 0 OR a.activity_type = ANY(c.activity_types))
    WHERE c.id = p_challenge_id;
$$ LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public;

-- Writes progress and completion of participants (all, of one challenge, of one user, or of one user in one challenge).
CREATE OR REPLACE FUNCTION public.refresh_challenge_progress(p_challenge_id UUID DEFAULT NULL, p_user UUID DEFAULT NULL)
RETURNS VOID AS $$
    WITH calc AS (
        SELECT p.id, public.challenge_progress_for(p.challenge_id, p.user_id) AS progress, c.target_value
        FROM public.challenge_participants p
        JOIN public.challenges c ON c.id = p.challenge_id
        WHERE (p_challenge_id IS NULL OR p.challenge_id = p_challenge_id)
          AND (p_user IS NULL OR p.user_id = p_user)
    )
    UPDATE public.challenge_participants p
    SET progress_value = calc.progress,
        is_completed = calc.progress >= calc.target_value,
        completed_at = CASE WHEN calc.progress >= calc.target_value THEN COALESCE(p.completed_at, NOW()) ELSE NULL END
    FROM calc
    WHERE p.id = calc.id
      AND (p.progress_value IS DISTINCT FROM calc.progress
           OR p.is_completed IS DISTINCT FROM (calc.progress >= calc.target_value));
$$ LANGUAGE sql SECURITY DEFINER SET search_path = public;

-- Activities changed: update the user's progress in all their challenges.
CREATE OR REPLACE FUNCTION public.refresh_progress_on_activity()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM public.refresh_challenge_progress(NULL, OLD.user_id);
        RETURN OLD;
    END IF;
    PERFORM public.refresh_challenge_progress(NULL, NEW.user_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- A new participant starts with the progress of the matching activities they recorded before (and whatever the client
-- sent is ignored).
CREATE OR REPLACE FUNCTION public.init_participant_progress()
RETURNS TRIGGER AS $$
DECLARE
    v_target DOUBLE PRECISION;
BEGIN
    SELECT target_value INTO v_target FROM public.challenges WHERE id = NEW.challenge_id;
    NEW.progress_value := public.challenge_progress_for(NEW.challenge_id, NEW.user_id);
    NEW.is_completed := NEW.progress_value >= v_target;
    NEW.completed_at := CASE WHEN NEW.is_completed THEN NOW() ELSE NULL END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Period, type, target or activity types of a challenge changed: recalculate its participants.
CREATE OR REPLACE FUNCTION public.refresh_progress_on_challenge()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM public.refresh_challenge_progress(NEW.id, NULL);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_activity_changed ON public.activities;
CREATE TRIGGER on_activity_changed
    AFTER INSERT OR UPDATE OR DELETE ON public.activities
    FOR EACH ROW EXECUTE FUNCTION public.refresh_progress_on_activity();

DROP TRIGGER IF EXISTS on_participant_init ON public.challenge_participants;
CREATE TRIGGER on_participant_init
    BEFORE INSERT ON public.challenge_participants
    FOR EACH ROW EXECUTE FUNCTION public.init_participant_progress();

DROP TRIGGER IF EXISTS on_challenge_rules_changed ON public.challenges;
CREATE TRIGGER on_challenge_rules_changed
    AFTER UPDATE OF start_date, end_date, type, target_value, activity_types ON public.challenges
    FOR EACH ROW EXECUTE FUNCTION public.refresh_progress_on_challenge();
```

### Social: comment updates & notification triggers

Likes, comments and reactions create notifications through `SECURITY DEFINER` triggers (clients cannot insert into
`notifications`). Nobody is notified about their own actions. `notifications.target_id` is the entity id; `payload`
carries `entity_type`, `entity_id`, plus `comment_id` / `preview` (first 80 chars) for comments and `comment_id` /
`emoji` for reactions. Recipients: like and comment notify the entity owner, a reaction notifies the comment author.

```sql
-- Comments: only the content can change; updated_at is kept fresh by the server.
CREATE OR REPLACE FUNCTION public.touch_comment()
RETURNS TRIGGER AS $$
BEGIN
    NEW.entity_type := OLD.entity_type;
    NEW.entity_id := OLD.entity_id;
    NEW.user_id := OLD.user_id;
    NEW.created_at := OLD.created_at;
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SET search_path = public;

-- like -> entity owner
CREATE OR REPLACE FUNCTION public.notify_on_like()
RETURNS TRIGGER AS $$
DECLARE
    v_owner UUID := public.entity_owner_id(NEW.entity_type, NEW.entity_id);
BEGIN
    IF v_owner IS NOT NULL AND v_owner <> NEW.user_id THEN
        INSERT INTO public.notifications (recipient_user_id, actor_user_id, notification_type, target_id, payload)
        VALUES (v_owner, NEW.user_id, 'like', NEW.entity_id,
                jsonb_build_object('entity_type', NEW.entity_type, 'entity_id', NEW.entity_id));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- comment -> entity owner
CREATE OR REPLACE FUNCTION public.notify_on_comment()
RETURNS TRIGGER AS $$
DECLARE
    v_owner UUID := public.entity_owner_id(NEW.entity_type, NEW.entity_id);
BEGIN
    IF v_owner IS NOT NULL AND v_owner <> NEW.user_id THEN
        INSERT INTO public.notifications (recipient_user_id, actor_user_id, notification_type, target_id, payload)
        VALUES (v_owner, NEW.user_id, 'comment', NEW.entity_id,
                jsonb_build_object('entity_type', NEW.entity_type, 'entity_id', NEW.entity_id,
                                   'comment_id', NEW.id, 'preview', left(NEW.content, 80)));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- reaction -> comment author
CREATE OR REPLACE FUNCTION public.notify_on_comment_reaction()
RETURNS TRIGGER AS $$
DECLARE
    v_comment public.comments%ROWTYPE;
BEGIN
    SELECT * INTO v_comment FROM public.comments WHERE id = NEW.comment_id;
    IF FOUND AND v_comment.user_id <> NEW.user_id THEN
        INSERT INTO public.notifications (recipient_user_id, actor_user_id, notification_type, target_id, payload)
        VALUES (v_comment.user_id, NEW.user_id, 'reaction', v_comment.entity_id,
                jsonb_build_object('entity_type', v_comment.entity_type, 'entity_id', v_comment.entity_id,
                                   'comment_id', NEW.comment_id, 'emoji', NEW.emoji));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_comment_updated
    BEFORE UPDATE ON public.comments
    FOR EACH ROW EXECUTE FUNCTION public.touch_comment();

CREATE TRIGGER on_like_created
    AFTER INSERT ON public.likes
    FOR EACH ROW EXECUTE FUNCTION public.notify_on_like();

CREATE TRIGGER on_comment_created
    AFTER INSERT ON public.comments
    FOR EACH ROW EXECUTE FUNCTION public.notify_on_comment();

CREATE TRIGGER on_comment_reaction_created
    AFTER INSERT ON public.comment_reactions
    FOR EACH ROW EXECUTE FUNCTION public.notify_on_comment_reaction();
```

### Device tokens (push registration)

Clients call `register_device_token` instead of upserting: a token that already belongs to another user (shared
device) is re-assigned to the caller. Clients delete their own row directly on logout.

```sql
-- Registers (or re-assigns) a push token for the calling user. A token that belongs to another user
-- (shared device) is moved to auth.uid(); the client calls this RPC instead of upserting.
CREATE OR REPLACE FUNCTION public.register_device_token(p_token TEXT, p_platform TEXT)
RETURNS VOID AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;
    INSERT INTO public.device_tokens (user_id, token, platform)
    VALUES (auth.uid(), p_token, p_platform)
    ON CONFLICT (token) DO UPDATE
        SET user_id = EXCLUDED.user_id, platform = EXCLUDED.platform, updated_at = NOW();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

REVOKE ALL ON FUNCTION public.register_device_token(TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.register_device_token(TEXT, TEXT) TO authenticated;
```

### Push notifications

Every `INSERT` into `notifications` triggers a Database Webhook that calls the Edge Function
[`supabase/functions/send-push`](supabase/functions/send-push/index.ts). It loads the recipient's `device_tokens`,
fetches the actor's `display_name`, and sends via FCM HTTP v1 (Android) and APNs HTTP/2 (iOS). Message text is a generic
English fallback; `title_key`, `body_key` and `actor_name` are also sent in `data` for client-side localization,
together with `notification_id`, `type`, `entity_type`, `entity_id` (and `comment_id`, `preview`, `emoji`).
Tokens reported invalid (FCM `UNREGISTERED`, APNs `410` / `BadDeviceToken`) are deleted.

```bash
supabase functions deploy send-push --no-verify-jwt
supabase secrets set WEBHOOK_SECRET=<WEBHOOK_SECRET> FIREBASE_SERVICE_ACCOUNT="$(cat <service-account-key>.json)" \
  APNS_KEY_P8="$(cat AuthKey_XXXX.p8)" APNS_KEY_ID=... APNS_TEAM_ID=... \
  APNS_BUNDLE_ID=com.mediabeam.fitness APNS_HOST=api.sandbox.push.apple.com
```

| Secret | Meaning |
|---|---|
| `WEBHOOK_SECRET` | Shared secret, must match the `x-webhook-secret` header of the webhook |
| `FIREBASE_SERVICE_ACCOUNT` | Full service account JSON (FCM HTTP v1) |
| `APNS_KEY_P8`, `APNS_KEY_ID`, `APNS_TEAM_ID` | APNs token auth key (.p8 contents), key id, team id |
| `APNS_BUNDLE_ID` | `com.mediabeam.fitness` |
| `APNS_HOST` | `api.sandbox.push.apple.com` (debug) or `api.push.apple.com` (production) |

Webhook: Dashboard > Database > Webhooks > table `public.notifications`, event `Insert`, type Supabase Edge Function
`send-push` (POST), HTTP header `x-webhook-secret: <WEBHOOK_SECRET>`. If the dashboard reports `schema "supabase_functions" does not exist`, enable webhooks first or use the SQL trigger alternative. Details: `supabase/functions/send-push/README.md`.

### Challenge ranking & activity cleanup

```sql
-- The ranking of the members inside a challenge. Individual challenge: all participants, visible to participants and
-- the creator. Group challenge: only the members of the caller's own team(s); other teams only appear as aggregates
-- (challenge_team_standings), never as single members.
CREATE OR REPLACE FUNCTION public.challenge_member_ranking(p_challenge_id UUID)
RETURNS TABLE (
    user_id UUID,
    display_name TEXT,
    avatar_url TEXT,
    progress_value DOUBLE PRECISION,
    is_completed BOOLEAN
) AS $$
    SELECT p.user_id, pr.display_name, pr.avatar_url, p.progress_value, p.is_completed
    FROM public.challenges c
    JOIN public.challenge_participants p ON p.challenge_id = c.id
    JOIN public.profiles pr ON pr.id = p.user_id
    WHERE c.id = p_challenge_id
      AND (
        (c.scope = 'individual' AND (
            c.created_by = auth.uid() OR
            EXISTS (SELECT 1 FROM public.challenge_participants me WHERE me.challenge_id = c.id AND me.user_id = auth.uid())
        ))
        OR
        (c.scope = 'group' AND p.team_id IS NOT NULL AND public.is_team_member(p.team_id, auth.uid()))
      )
    ORDER BY p.progress_value DESC, pr.display_name;
$$ LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public;

REVOKE ALL ON FUNCTION public.challenge_member_ranking(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.challenge_member_ranking(UUID) TO authenticated;

-- Deleting an activity removes what hangs on it. Likes and comments point at their activity by a type + id (no
-- foreign key, they also exist for challenges), so a trigger cleans them up; reactions go with their comments, the
-- route with the activity (foreign keys), and the challenge progress is recalculated by on_activity_changed.
CREATE OR REPLACE FUNCTION public.delete_activity_social()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM public.comments WHERE entity_type = 'activity' AND entity_id = OLD.id;
    DELETE FROM public.likes WHERE entity_type = 'activity' AND entity_id = OLD.id;
    DELETE FROM public.notifications WHERE target_id = OLD.id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_activity_deleted ON public.activities;
CREATE TRIGGER on_activity_deleted
    AFTER DELETE ON public.activities
    FOR EACH ROW EXECUTE FUNCTION public.delete_activity_social();
```

### Activity routes, map privacy & social counts

- `activities.show_map` (default `TRUE`) is the owner's privacy setting. The route is stored in `activity_routes`
  (Encoded Polyline, precision 5), one row per activity, upserted by the client right after the activity. Other users
  can read a route only if `show_map` is `TRUE` and they are members of the activity's team; the owner always can.
- `social_counts(p_type, p_ids)` returns like count, comment count and `liked_by_me` for a whole feed page in ONE
  round trip. It is `SECURITY INVOKER`, so RLS applies and only visible likes/comments are counted. It uses
  `idx_likes_entity` / `idx_comments_entity_created`. The client sends at most 100 ids per call (not enforced
  server-side; keep pages small).

```sql
CREATE OR REPLACE FUNCTION public.social_counts(p_type social_entity_type, p_ids UUID[])
RETURNS TABLE (entity_id UUID, like_count INTEGER, comment_count INTEGER, liked_by_me BOOLEAN)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
    SELECT
        i.id,
        (SELECT COUNT(*)::int FROM public.likes l
          WHERE l.entity_type = p_type AND l.entity_id = i.id),
        (SELECT COUNT(*)::int FROM public.comments c
          WHERE c.entity_type = p_type AND c.entity_id = i.id),
        EXISTS (SELECT 1 FROM public.likes l2
                 WHERE l2.entity_type = p_type AND l2.entity_id = i.id AND l2.user_id = auth.uid())
    FROM unnest(p_ids) AS i(id);
$$;

REVOKE ALL ON FUNCTION public.social_counts(social_entity_type, UUID[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.social_counts(social_entity_type, UUID[]) TO authenticated;
```

---

### Realtime

`comments`, `likes` and `notifications` are added to the `supabase_realtime` publication (SQL in the migration block
above; or Dashboard > Database > Replication). Realtime `postgres_changes` honors the RLS policies for INSERT/UPDATE;
DELETE events are not filtered by RLS and carry only the primary key.

---

## 6. KMP Client Setup

In your KMP shared module (`shared/build.gradle.kts`), configure the Supabase Kotlin SDK dependencies:

```kotlin
// shared/build.gradle.kts (Version catalog libs reference)
implementation(libs.supabase.postgrest)
implementation(libs.supabase.gotrue)
implementation(libs.supabase.storage)
```

### Environment Configuration & API Key Migration
Configure your project URL and API key in `SupabaseClientFactory.kt`.

> [!IMPORTANT]
> **Base URL Format**: Pass `https://<YOUR_PROJECT_REF>.supabase.co` without `/rest/v1/` trailing subpaths because the Supabase SDK automatically appends API routes for Postgrest (`/rest/v1`), Auth (`/auth/v1`), and Storage (`/storage/v1`).
> **API Key Migration**: Supabase is transitioning to modern publishable API keys (`sbp_...`). Both legacy JWT `anon` keys and new publishable keys are fully supported in `createSupabaseClient`. See [Supabase API Keys Migration Guide](https://github.com/orgs/supabase/discussions/29260).

```kotlin
object SupabaseConfig {
    const val URL = "https://urhisbyqjygjqrfarply.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVyaGlzYnlxanlnanFyZmFycGx5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA4NDI0MzQsImV4cCI6MjEwNjQxODQzNH0.FC1UjgqhbFTY9EBeROBTmGbLgV5KmqpPNvH8_vwYbP0"
}
```

### Authentication Operations (Email & Password Only)

Using `io.github.jan.supabase.gotrue.auth` with `Email` provider in Supabase Kotlin 2.5.0:

```kotlin
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

class AuthRepositoryImpl(private val supabase: SupabaseClient) {

    // Sign Up with Email and Password
    suspend fun signUp(emailInput: String, passwordInput: String) {
        supabase.auth.signUpWith(Email) {
            email = emailInput
            password = passwordInput
        }
    }

    // Sign In with Email and Password
    suspend fun signIn(emailInput: String, passwordInput: String) {
        supabase.auth.signInWith(Email) {
            email = emailInput
            password = passwordInput
        }
    }

    // Sign Out
    suspend fun signOut() {
        supabase.auth.signOut()
    }
}
```

