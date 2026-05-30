package com.skillnet.config;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

/**
 * El driver JDBC de PostgreSQL no entiende {@code classpath:} en {@code sslrootcert}.
 * Extrae {@code global-bundle.pem} del classpath a un archivo temporal con ruta absoluta.
 */
@Configuration
@Profile("prod")
@EnableConfigurationProperties(DataSourceProperties.class)
public class RdsSslDataSourceConfig {

    private static final String CLASSPATH_CERT = "classpath:global-bundle.pem";

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) throws IOException {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setJdbcUrl(resolveJdbcUrl(properties.getUrl()));
        return dataSource;
    }

    private String resolveJdbcUrl(String jdbcUrl) throws IOException {
        if (jdbcUrl == null || !jdbcUrl.contains(CLASSPATH_CERT)) {
            return jdbcUrl;
        }
        Path certPath = materializeCertFile();
        String absolutePath = certPath.toAbsolutePath().toString().replace('\\', '/');
        return jdbcUrl.replace(CLASSPATH_CERT, absolutePath);
    }

    private Path materializeCertFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("global-bundle.pem");
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "No se encontró global-bundle.pem en classpath. Descárgalo a src/main/resources/.");
        }
        Path target = Path.of(System.getProperty("java.io.tmpdir"), "skillnet-rds-global-bundle.pem");
        try (InputStream input = resource.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}
