ALTER TABLE tasks ADD COLUMN assignee_id UUID REFERENCES users(id);
