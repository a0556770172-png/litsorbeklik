-- Litsor BeKlik — initial schema
-- Run once via the Supabase MCP connector (or SQL editor). RLS is mandatory on every table:
-- each authenticated user may only see/modify rows they own.

create table if not exists profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  user_code text not null unique,
  created_at timestamptz not null default now()
);

create table if not exists user_secrets (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  secret_type text not null check (secret_type in ('ai_api_key', 'github_pat')),
  provider text,
  ciphertext text not null, -- "salt:iv:ciphertext" base64, AES-GCM, client-derived key (zero-knowledge)
  created_at timestamptz not null default now()
);

create table if not exists projects (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  package_name text not null,
  status text not null default 'draft',
  repo_url text,
  ai_engine text not null default 'CLOUD' check (ai_engine in ('CLOUD','LOCAL')),
  build_engine text not null default 'GITHUB' check (build_engine in ('GITHUB','LOCAL')),
  created_at timestamptz not null default now()
);

create table if not exists app_specs (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  version int not null default 1,
  goal text,
  screens jsonb not null default '[]',
  features jsonb not null default '[]',
  raw_text text,
  created_at timestamptz not null default now()
);

create table if not exists build_runs (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  engine text not null check (engine in ('GITHUB','LOCAL')),
  status text not null default 'pending',
  log_url text,
  apk_url text,
  created_at timestamptz not null default now()
);

create table if not exists app_versions (
  id uuid primary key default gen_random_uuid(),
  version_code int not null,
  version_name text not null,
  apk_storage_path text not null,
  changelog text,
  released_at timestamptz not null default now()
);

create table if not exists device_profiles (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  total_ram_mb bigint,
  has_supported_npu boolean not null default false,
  chosen_model_tier text,
  detected_at timestamptz not null default now()
);

-- Row Level Security
alter table profiles enable row level security;
alter table user_secrets enable row level security;
alter table projects enable row level security;
alter table app_specs enable row level security;
alter table build_runs enable row level security;
alter table app_versions enable row level security;
alter table device_profiles enable row level security;

create policy "profiles_self" on profiles for all using (auth.uid() = id) with check (auth.uid() = id);
create policy "user_secrets_owner" on user_secrets for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "projects_owner" on projects for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "device_profiles_owner" on device_profiles for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);

create policy "app_specs_via_project" on app_specs for all using (
  exists (select 1 from projects p where p.id = app_specs.project_id and p.owner_id = auth.uid())
) with check (
  exists (select 1 from projects p where p.id = app_specs.project_id and p.owner_id = auth.uid())
);

create policy "build_runs_via_project" on build_runs for all using (
  exists (select 1 from projects p where p.id = build_runs.project_id and p.owner_id = auth.uid())
) with check (
  exists (select 1 from projects p where p.id = build_runs.project_id and p.owner_id = auth.uid())
);

-- app_versions is read-only public metadata for the self-update check (no owner column)
create policy "app_versions_read_all" on app_versions for select using (true);
