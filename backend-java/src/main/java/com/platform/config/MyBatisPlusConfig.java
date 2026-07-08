package com.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.platform.handler.JsonbTypeHandler;
import com.platform.handler.LocalDateTimeTypeHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * Register custom LocalDateTime TypeHandler globally to handle
     * PostgreSQL TIMESTAMPTZ → OffsetDateTime → LocalDateTime conversion.
     * Uses ApplicationReadyEvent to lazily resolve SqlSessionFactory,
     * avoiding circular dependency with MybatisPlusAutoConfiguration.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerTypeHandlers(ApplicationReadyEvent event) {
        LocalDateTimeTypeHandler localDateTimeHandler = new LocalDateTimeTypeHandler();
        JsonbTypeHandler jsonbHandler = new JsonbTypeHandler();
        Map<String, SqlSessionFactory> factories = event.getApplicationContext()
                .getBeansOfType(SqlSessionFactory.class);
        for (SqlSessionFactory factory : factories.values()) {
            factory.getConfiguration().getTypeHandlerRegistry()
                    .register(LocalDateTime.class, JdbcType.TIMESTAMP_WITH_TIMEZONE, localDateTimeHandler);
            factory.getConfiguration().getTypeHandlerRegistry()
                    .register(LocalDateTime.class, localDateTimeHandler);
            factory.getConfiguration().getTypeHandlerRegistry()
                    .register(String.class, JdbcType.OTHER, jsonbHandler);
        }
    }
}
