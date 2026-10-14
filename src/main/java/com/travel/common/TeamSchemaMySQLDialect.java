package com.travel.common;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * MySQL dialect that never changes the type or length of an existing column.
 *
 * With ddl-auto=update, Hibernate would otherwise "correct" the team's travel_db columns to its own defaults
 * (e.g. VARCHAR(10) to VARCHAR(255), or TEXT and VARCHAR(500) down to VARCHAR(255)). This way the agreed schema
 * stays exactly as written and Hibernate only adds the missing columns and tables.
 */
public class TeamSchemaMySQLDialect extends MySQLDialect {

    public TeamSchemaMySQLDialect() { super(); }

    public TeamSchemaMySQLDialect(DatabaseVersion version) { super(version); }

    public TeamSchemaMySQLDialect(DialectResolutionInfo info) { super(info); }

    @Override
    public boolean supportsAlterColumnType() { return false; }

    // ---- row locks: never use "FOR UPDATE OF <alias>" (not supported by MariaDB / XAMPP) ----

    @Override
    public String getWriteLockString(String aliases, int timeout) { return getWriteLockString(timeout); }

    @Override
    public String getWriteLockString(String aliases, jakarta.persistence.Timeout timeout) { return getWriteLockString(timeout); }

    @Override
    public String getReadLockString(String aliases, int timeout) { return getReadLockString(timeout); }

    @Override
    public String getReadLockString(String aliases, jakarta.persistence.Timeout timeout) { return getReadLockString(timeout); }

    @Override
    public String getForUpdateString(String aliases) { return getForUpdateString(); }

    @Override
    public String getForUpdateNowaitString(String aliases) { return getForUpdateNowaitString(); }

    @Override
    public String getForUpdateSkipLockedString(String aliases) { return getForUpdateSkipLockedString(); }
}
