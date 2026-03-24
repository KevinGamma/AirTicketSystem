package com.airticket.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(ChatBiDataSourceProperties.class)
public class ChatBiDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mainDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(@Qualifier("mainDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }

    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "chatBiDataSource")
    public DataSource chatBiDataSource(ChatBiDataSourceProperties properties,
                                       @Value("${spring.datasource.username:}") String mainDatasourceUsername) {
        validate(properties, mainDatasourceUsername);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("ChatBiReadOnlyPool");
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        dataSource.setMinimumIdle(properties.getMinimumIdle());
        dataSource.setReadOnly(true);
        dataSource.setAutoCommit(true);
        dataSource.setConnectionInitSql("SET SESSION TRANSACTION READ ONLY");
        return dataSource;
    }

    @Bean(name = "chatBiJdbcTemplate")
    public JdbcTemplate chatBiJdbcTemplate(@Qualifier("chatBiDataSource") DataSource chatBiDataSource) {
        return new JdbcTemplate(chatBiDataSource);
    }

    private void validate(ChatBiDataSourceProperties properties, String mainDatasourceUsername) {
        if (!StringUtils.hasText(properties.getUrl())) {
            throw new IllegalStateException("Missing chatbi.datasource.url for ChatBI read-only datasource");
        }
        if (!StringUtils.hasText(properties.getUsername())) {
            throw new IllegalStateException("Missing chatbi.datasource.username for ChatBI read-only datasource");
        }
        if (properties.getPassword() == null) {
            throw new IllegalStateException("Missing chatbi.datasource.password for ChatBI read-only datasource");
        }
        if (!StringUtils.hasText(properties.getDriverClassName())) {
            throw new IllegalStateException("Missing chatbi.datasource.driver-class-name for ChatBI read-only datasource");
        }
        if (StringUtils.hasText(mainDatasourceUsername)
            && properties.getUsername().trim().equals(mainDatasourceUsername.trim())) {
            throw new IllegalStateException("chatbi.datasource.username must differ from spring.datasource.username to enforce account isolation");
        }
    }
}
