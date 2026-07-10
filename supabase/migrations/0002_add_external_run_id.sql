-- Adds the engine-specific run handle (e.g. GitHub Actions numeric run id) as its own column,
-- distinct from build_runs.id (the Supabase row's own uuid primary key).
alter table build_runs add column if not exists external_run_id text;
