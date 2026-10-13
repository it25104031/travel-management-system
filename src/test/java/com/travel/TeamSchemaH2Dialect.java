package com.travel;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/** H2 counterpart of TeamSchemaMySQLDialect for the tests: existing team columns are never altered. */
public class TeamSchemaH2Dialect extends H2Dialect {

    public TeamSchemaH2Dialect() { super(); }

    public TeamSchemaH2Dialect(DatabaseVersion version) { super(version); }

    public TeamSchemaH2Dialect(DialectResolutionInfo info) { super(info); }

    @Override
    public boolean supportsAlterColumnType() { return false; }
}
