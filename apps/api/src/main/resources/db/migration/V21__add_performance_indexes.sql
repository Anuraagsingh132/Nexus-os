CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_projects_workspace_id ON projects(workspace_id);
CREATE INDEX IF NOT EXISTS idx_tasks_title_lower ON tasks(LOWER(title));
