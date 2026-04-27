package com.example.testcontainersdemo.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TestcontainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    private static final String TESTCONTAINERS_ENABLED_PROPERTY = "app.testcontainers.enabled";
    private static final String MYSQL_IMAGE = "mysql:8.0";
    private static final int MYSQL_PORT = 3306;
    private static final String MYSQL_DATABASE = "orders_db";
    private static final String MYSQL_ROOT_PASSWORD = "root";
    
    private static GenericContainer<?> mysqlContainer;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        String testcontainersEnabled = environment.getProperty(TESTCONTAINERS_ENABLED_PROPERTY, "false");
        
        if ("true".equalsIgnoreCase(testcontainersEnabled)) {
            startMySQLContainer();
            overrideDataSourceProperties(environment);
        }
    }
    
    private void startMySQLContainer() {
        if (mysqlContainer == null) {
            mysqlContainer = new GenericContainer<>(DockerImageName.parse(MYSQL_IMAGE))
                    .withExposedPorts(MYSQL_PORT)
                    .withEnv("MYSQL_ROOT_PASSWORD", MYSQL_ROOT_PASSWORD)
                    .withEnv("MYSQL_DATABASE", MYSQL_DATABASE)
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
                    .withStartupTimeout(Duration.ofMinutes(5));
            
            mysqlContainer.start();
            
            try {
                Thread.sleep(Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (mysqlContainer != null && mysqlContainer.isRunning()) {
                    mysqlContainer.stop();
                }
            }));
        }
    }
    
    private void overrideDataSourceProperties(ConfigurableEnvironment environment) {
        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                mysqlContainer.getHost(),
                mysqlContainer.getMappedPort(MYSQL_PORT),
                MYSQL_DATABASE
        );
        
        Map<String, Object> testcontainersProperties = new HashMap<>();
        
        testcontainersProperties.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        testcontainersProperties.put("spring.datasource.url", jdbcUrl);
        testcontainersProperties.put("spring.datasource.username", "root");
        testcontainersProperties.put("spring.datasource.password", MYSQL_ROOT_PASSWORD);
        testcontainersProperties.put("spring.sql.init.platform", "mysql");
        
        environment.getPropertySources().addFirst(
                new MapPropertySource("testcontainers", testcontainersProperties)
        );
    }
    
    public static GenericContainer<?> getMySQLContainer() {
        return mysqlContainer;
    }
}
