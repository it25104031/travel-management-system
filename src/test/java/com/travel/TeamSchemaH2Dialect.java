package com.travel;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

public class TeamSchemaH2Dialect extends H2Dialect {

    public TeamSchemaH2Dialect() { super(); }

    public TeamSchemaH2Dialect(DatabaseVersion version) { super(version); }

    public TeamSchemaH2Dialect(DialectResolutionInfo info) { super(info); }

    @Override
    public boolean supportsAlterColumnType() { return false; }
}
