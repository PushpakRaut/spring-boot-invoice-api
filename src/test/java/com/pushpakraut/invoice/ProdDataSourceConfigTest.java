package com.pushpakraut.invoice;

import com.pushpakraut.invoice.config.ProdDataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProdDataSourceConfigTest {
    @Test
    void parsesNeonStyleUrl() throws Exception {
        Method m = ProdDataSourceConfig.class.getDeclaredMethod("dataSource", String.class);
        m.setAccessible(true);
        var ds = (HikariDataSource) m.invoke(new ProdDataSourceConfig(),
                "postgresql://neondb_owner:p%40ss@ep-cool-1.us-west-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require");
        assertEquals("jdbc:postgresql://ep-cool-1.us-west-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require", ds.getJdbcUrl());
        assertEquals("neondb_owner", ds.getUsername());
        assertEquals("p@ss", ds.getPassword());
    }
}
