-- V6 was intentionally skipped. V5 transitions directly to V7.
-- Seed data for local development
-- Run with: psql -U nexus -d nexus_os -f seed.sql

-- Admin user (password: Admin123!)
INSERT INTO users (id, email, password_hash, full_name)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'admin@nexusos.dev',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Admin User'
) ON CONFLICT (email) DO NOTHING;

-- Demo user (password: Demo123!)
INSERT INTO users (id, email, password_hash, full_name)
VALUES (
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'demo@nexusos.dev',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Demo User'
) ON CONFLICT (email) DO NOTHING;

-- Organization
INSERT INTO organizations (id, name, slug)
VALUES (
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'Nexus Corp',
    'nexus-corp'
) ON CONFLICT (slug) DO NOTHING;

-- Workspace
INSERT INTO workspaces (id, organization_id, name, slug)
VALUES (
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'Engineering',
    'engineering'
) ON CONFLICT (organization_id, slug) DO NOTHING;

-- Memberships
INSERT INTO memberships (user_id, workspace_id, role)
VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'd4e5f6a7-b8c9-0123-defa-234567890123', 'OWNER'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'd4e5f6a7-b8c9-0123-defa-234567890123', 'MEMBER')
ON CONFLICT (user_id, workspace_id) DO NOTHING;

-- Sample project
INSERT INTO projects (id, workspace_id, name, description)
VALUES (
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'Q3 Product Launch',
    'Planning and execution for the Q3 product launch'
) ON CONFLICT DO NOTHING;

-- Sample tasks
INSERT INTO tasks (id, project_id, title, description, status, position)
VALUES
    (uuid_generate_v4(), 'e5f6a7b8-c9d0-1234-efab-345678901234', 'Design mockups', 'Create initial design mockups', 'TODO', 0),
    (uuid_generate_v4(), 'e5f6a7b8-c9d0-1234-efab-345678901234', 'API integration', 'Integrate with backend APIs', 'IN_PROGRESS', 1),
    (uuid_generate_v4(), 'e5f6a7b8-c9d0-1234-efab-345678901234', 'User testing', 'Conduct user testing sessions', 'TODO', 2)
ON CONFLICT DO NOTHING;
