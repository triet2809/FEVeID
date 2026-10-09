ALTER TABLE accounts
ADD COLUMN role VARCHAR(50) NOT NULL  DEFAULT 'MEMBER';

CREATE INDEX idx_accounts_role ON accounts(role);