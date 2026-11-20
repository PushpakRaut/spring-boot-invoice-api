package com.pushpakraut.invoice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Hosted (prod) database. Accepts the connection string exactly as Neon/Render/Heroku show it,
 * e.g. postgresql://user:password@host/db?sslmode=require, so it can be pasted without editing.
 * No fallback: the app refuses to start without DATABASE_URL.
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    @Bean
    DataSource dataSource(@Value("${DATABASE_URL}") String url) {
        HikariDataSource ds = new HikariDataSource();
        if (url.startsWith("jdbc:")) {
            ds.setJdbcUrl(url);
            return ds;
        }
        URI u = URI.create(url.replaceFirst("^postgres(ql)?://", "http://"));
        String[] creds = u.getRawUserInfo() == null ? new String[0] : u.getRawUserInfo().split(":", 2);
        ds.setJdbcUrl("jdbc:postgresql://" + u.getHost() + (u.getPort() > 0 ? ":" + u.getPort() : "")
                + u.getRawPath() + (u.getRawQuery() == null ? "?sslmode=require" : "?" + u.getRawQuery()));
        if (creds.length > 0) ds.setUsername(URLDecoder.decode(creds[0], StandardCharsets.UTF_8));
        if (creds.length > 1) ds.setPassword(URLDecoder.decode(creds[1], StandardCharsets.UTF_8));
        ds.setMaximumPoolSize(5); // free-tier databases allow few connections
        return ds;
    }
}
