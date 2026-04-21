CREATE TABLE accounts (
    id              UUID            NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    balance_pln     DECIMAL(19, 2)  NOT NULL,
    balance_usd     DECIMAL(19, 2)  NOT NULL,
    version         BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT ck_accounts_balance_pln_non_negative CHECK (balance_pln >= 0),
    CONSTRAINT ck_accounts_balance_usd_non_negative CHECK (balance_usd >= 0)
);
