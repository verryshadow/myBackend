psql postgresql://codex-postgres:codex-password@localhost:5432/codex_ui << EOF
INSERT INTO site VALUES (1, 'site1');
INSERT INTO site VALUES (2, 'site2');
EOF
