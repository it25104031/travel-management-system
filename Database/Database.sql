-- =====================================================
-- Travel Management System - database setup
-- Safe to run more than once.
-- (Hibernate ddl-auto=update can also create these tables;
--  this script is for creating the DB + sample data manually.)
-- =====================================================

CREATE DATABASE IF NOT EXISTS travel_management_db;
USE travel_management_db;

-- =========================
-- USER TABLE
-- =========================
CREATE TABLE IF NOT EXISTS app_user (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,   -- BCrypt hash
    full_name VARCHAR(100) NOT NULL,
    email     VARCHAR(100) NOT NULL,
    role      VARCHAR(20)  NOT NULL    -- ADMIN or USER
);

-- Default accounts are created automatically by DataInitializer (with hashed passwords)
-- when the app starts, so they are not inserted here.

-- =========================
-- INVOICE TABLE
-- =========================
CREATE TABLE IF NOT EXISTS invoice (
    invoice_id     INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(30)   NOT NULL UNIQUE,
    customer_name  VARCHAR(100)  NOT NULL,
    invoice_date   DATETIME      NOT NULL,
    total_amount   DECIMAL(10,2) NOT NULL CHECK (total_amount > 0),
    invoice_status VARCHAR(30)   NOT NULL   -- UNPAID / PARTIAL / PAID (calculated by the app)
);

-- =========================
-- PAYMENT TABLE
-- =========================
CREATE TABLE IF NOT EXISTS payment (
    payment_id     INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id     INT           NOT NULL,
    payment_date   DATETIME      NOT NULL,
    amount         DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(30)   NOT NULL,
    payment_status VARCHAR(30)   NOT NULL,  -- COMPLETED / PENDING / FAILED
    FOREIGN KEY (invoice_id) REFERENCES invoice(invoice_id)
);

-- =========================
-- PAYMENT SLIP TABLE
-- =========================
CREATE TABLE IF NOT EXISTS payment_slip (
    slip_id            INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id         INT           NOT NULL,
    user_id            INT           NOT NULL,
    amount_paid        DECIMAL(10,2) NOT NULL CHECK (amount_paid > 0),
    payment_date       DATETIME      NOT NULL,
    bank_reference     VARCHAR(100),
    original_file_name VARCHAR(255)  NOT NULL,
    stored_file_name   VARCHAR(255)  NOT NULL,
    content_type       VARCHAR(100)  NOT NULL,
    file_size          BIGINT        NOT NULL,
    status             VARCHAR(30)   NOT NULL DEFAULT 'PENDING',  -- PENDING / APPROVED / REJECTED
    admin_remark       TEXT,
    created_at         DATETIME      NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoice(invoice_id),
    FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

-- =========================
-- SAMPLE DATA (statuses match the payments)
-- =========================
INSERT IGNORE INTO invoice
(invoice_number, customer_name, invoice_date, total_amount, invoice_status)
VALUES
('INV-0001', 'Amal Perera',    '2026-09-24 10:00:00', 250000.00, 'UNPAID'),
('INV-0002', 'Nimal Silva',    '2026-09-24 11:30:00', 180000.00, 'PAID'),
('INV-0003', 'Kamal Fernando', '2026-09-24 14:00:00', 320000.00, 'PARTIAL');

INSERT INTO payment (invoice_id, payment_date, amount, payment_method, payment_status)
SELECT i.invoice_id, '2026-09-24 12:00:00', 180000.00, 'CARD', 'COMPLETED'
FROM invoice i
WHERE i.invoice_number = 'INV-0002'
  AND NOT EXISTS (SELECT 1 FROM payment p WHERE p.invoice_id = i.invoice_id);

INSERT INTO payment (invoice_id, payment_date, amount, payment_method, payment_status)
SELECT i.invoice_id, '2026-09-24 15:00:00', 100000.00, 'BANK_TRANSFER', 'COMPLETED'
FROM invoice i
WHERE i.invoice_number = 'INV-0003'
  AND NOT EXISTS (SELECT 1 FROM payment p WHERE p.invoice_id = i.invoice_id);
