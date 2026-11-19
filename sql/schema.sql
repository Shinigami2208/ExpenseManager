IF DB_ID('ExpenseDB') IS NULL
    CREATE DATABASE ExpenseDB;
GO

USE ExpenseDB;
GO

IF OBJECT_ID('categories') IS NULL
CREATE TABLE categories (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE
);
GO

IF OBJECT_ID('expenses') IS NULL
CREATE TABLE expenses (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    category_id  INT            NOT NULL,
    amount       DECIMAL(18, 2) NOT NULL CHECK (amount > 0),
    note         NVARCHAR(255)  NULL,
    expense_date DATE           NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    created_at   DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories(id)
);
GO

IF NOT EXISTS (SELECT 1 FROM categories)
BEGIN
    INSERT INTO categories (name) VALUES
        (N'Ăn uống'), (N'Đi lại'), (N'Nhà ở'), (N'Giải trí'), (N'Học tập');

    INSERT INTO expenses (category_id, amount, note, expense_date) VALUES
        (1, 45000,   N'Cơm trưa',          '2026-10-01'),
        (2, 20000,   N'Xăng xe',           '2026-10-02'),
        (3, 3000000, N'Tiền phòng tháng 10','2026-10-05'),
        (4, 120000,  N'Vé xem phim',       '2026-10-10'),
        (5, 350000,  N'Mua sách Java',     '2026-10-12');
END
GO

IF OBJECT_ID('wallets') IS NULL
CREATE TABLE wallets (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    name    NVARCHAR(100)  NOT NULL UNIQUE,
    balance DECIMAL(18, 2) NOT NULL DEFAULT 0,
    CONSTRAINT ck_wallets_balance CHECK (balance >= 0)
);
GO

IF COL_LENGTH('expenses', 'wallet_id') IS NULL
    ALTER TABLE expenses ADD wallet_id INT NULL
        CONSTRAINT fk_expenses_wallet FOREIGN KEY REFERENCES wallets(id);
GO

IF OBJECT_ID('transfers') IS NULL
CREATE TABLE transfers (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    from_wallet_id INT            NOT NULL,
    to_wallet_id   INT            NOT NULL,
    amount         DECIMAL(18, 2) NOT NULL CHECK (amount > 0),
    note           NVARCHAR(255)  NULL,
    created_at     DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_transfers_from FOREIGN KEY (from_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_transfers_to   FOREIGN KEY (to_wallet_id)   REFERENCES wallets(id),
    CONSTRAINT ck_transfers_diff CHECK (from_wallet_id <> to_wallet_id)
);
GO

IF NOT EXISTS (SELECT 1 FROM wallets)
    INSERT INTO wallets (name, balance) VALUES
        (N'Tiền mặt', 2000000), (N'Ngân hàng', 15000000);
GO
