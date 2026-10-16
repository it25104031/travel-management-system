package com.travel.common;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

public class TeamSchemaMySQLDialect extends MySQLDialect {

    public TeamSchemaMySQLDialect() { super(); }

    public TeamSchemaMySQLDialect(DatabaseVersion version) { super(version); }

    public TeamSchemaMySQLDialect(DialectResolutionInfo info) { super(info); }

    @Override
    public boolean supportsAlterColumnType() { return false; }

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
