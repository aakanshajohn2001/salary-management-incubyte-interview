-- Core schema for the ACME salary management system.
-- Written in ANSI-standard SQL (GENERATED ... AS IDENTITY, no vendor-specific
-- extensions) so it runs unchanged on both H2 (dev/test) and PostgreSQL (prod).

CREATE TABLE currency (
    code            VARCHAR(3) PRIMARY KEY,
    fx_to_usd       NUMERIC(18, 6) NOT NULL
);

CREATE TABLE country (
    code            VARCHAR(2) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    currency_code   VARCHAR(3) NOT NULL REFERENCES currency (code)
);

CREATE TABLE department (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE app_user (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL
);

CREATE TABLE employee (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(200) NOT NULL UNIQUE,
    department_id   BIGINT NOT NULL REFERENCES department (id),
    country_code    VARCHAR(2) NOT NULL REFERENCES country (code),
    job_band        VARCHAR(10) NOT NULL CHECK (job_band IN ('L1', 'L2', 'L3', 'L4', 'L5', 'L6')),
    hire_date       DATE NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'TERMINATED')),
    created_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_employee_department ON employee (department_id);
CREATE INDEX idx_employee_country ON employee (country_code);
CREATE INDEX idx_employee_job_band ON employee (job_band);
CREATE INDEX idx_employee_status ON employee (status);

CREATE TABLE salary_record (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employee (id),
    amount          NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    currency_code   VARCHAR(3) NOT NULL REFERENCES currency (code),
    effective_date  DATE NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL
);

-- Supports "latest salary as of date" lookups and full history-by-employee reads.
CREATE INDEX idx_salary_record_employee_effective ON salary_record (employee_id, effective_date DESC);
