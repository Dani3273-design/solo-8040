# Testcontainers Demo

这是一个用于演示Testcontainers使用的Spring Boot 3项目。

## 项目简介

本项目展示了如何在不同环境中使用不同的数据库：
- **本地开发环境**：使用Testcontainers自动启动Docker运行MySQL服务
- **生产环境/测试环境**：使用真实的MySQL服务

## 技术栈

- **Spring Boot 3.2.5**
- **JDK 21**
- **Maven**
- **MySQL 8.0**
- **Spring JDBC Template** (数据库操作)
- **Testcontainers 1.19.7**

## 项目结构

```
src/main/
├── java/com/example/testcontainersdemo/
│   ├── TestcontainersDemoApplication.java  # 主应用类
│   ├── config/
│   │   └── TestcontainersInitializer.java   # Testcontainers初始化器
│   ├── controller/
│   │   └── OrderController.java             # 订单REST API控制器
│   ├── entity/
│   │   └── Order.java                        # 订单实体类
│   └── repository/
│       └── OrderRepository.java              # 订单数据访问层
└── resources/
    ├── application.properties                 # 主配置文件
    ├── application-dev.properties             # 开发环境配置
    ├── application-prod.properties            # 生产环境配置
    └── schema.sql                              # 数据库DDL脚本
```

## 环境配置

### 1. 安装JDK 21

确保本地已安装JDK 21：
```bash
java -version
```

### 2. 安装Maven

确保本地已安装Maven：
```bash
mvn -version
```

### 3. 安装Docker (使用Testcontainers必需)

Testcontainers需要Docker环境来运行容器。

#### macOS

1. 下载并安装 [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop)
2. 启动Docker Desktop
3. 验证安装：
```bash
docker --version
docker ps
```

#### Windows

1. 下载并安装 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
2. 启动Docker Desktop
3. 确保WSL 2已启用（Docker Desktop会提示）
4. 验证安装：
```bash
docker --version
docker ps
```

#### Linux

1. 安装Docker Engine：
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install docker.io docker-compose

# CentOS/RHEL
sudo yum install docker
sudo systemctl start docker
sudo systemctl enable docker
```

2. 将当前用户添加到docker组（避免每次使用sudo）：
```bash
sudo usermod -aG docker $USER
```

3. 注销并重新登录，或执行：
```bash
newgrp docker
```

4. 验证安装：
```bash
docker --version
docker ps
```

## 配置说明

### 主配置文件 (application.properties)

```properties
spring.application.name=testcontainers-demo
spring.profiles.active=dev

# Testcontainers开关 - 设置为true启用Testcontainers
app.testcontainers.enabled=true

# 默认MySQL配置（当Testcontainers禁用时使用）
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/orders_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=password

# 自动执行DDL脚本
spring.sql.init.mode=always
spring.sql.init.platform=mysql
```

### 开发环境配置 (application-dev.properties)

```properties
# 开发环境默认启用Testcontainers
app.testcontainers.enabled=true
```

### 生产环境配置 (application-prod.properties)

```properties
# 生产环境禁用Testcontainers，使用真实MySQL
app.testcontainers.enabled=false

# 生产环境数据库配置（使用环境变量覆盖）
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:orders_db}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:password}
```

## 运行项目

### 方式1：使用Testcontainers（本地开发环境）

确保Docker已启动，然后运行：

```bash
cd main
mvn spring-boot:run
```

默认情况下，`app.testcontainers.enabled=true`，应用会自动启动MySQL容器。

### 方式2：使用生产环境配置

```bash
cd main
mvn spring-boot:run -Dspring.profiles.active=prod
```

### 方式3：打包后运行

```bash
cd main
mvn clean package
java -jar target/testcontainers-demo-0.0.1-SNAPSHOT.jar
```

## API接口说明

应用启动后，可以通过以下接口进行操作：

### 获取所有订单
```http
GET /api/orders
```

### 根据ID获取订单
```http
GET /api/orders/{id}
```

### 根据订单号获取订单
```http
GET /api/orders/order-no/{orderNo}
```

### 创建订单
```http
POST /api/orders
Content-Type: application/json

{
    "orderNo": "ORD2024004",
    "productName": "iPad Pro",
    "quantity": 1,
    "price": 6799.00,
    "status": "PENDING"
}
```

### 更新订单
```http
PUT /api/orders/{id}
Content-Type: application/json

{
    "productName": "iPad Pro 12.9",
    "quantity": 1,
    "price": 8999.00,
    "status": "PAID"
}
```

### 删除订单
```http
DELETE /api/orders/{id}
```

## Testcontainers使用细节

### 工作原理

1. **配置检查**：`TestcontainersInitializer`会在应用启动前检查`app.testcontainers.enabled`配置
2. **容器启动**：当配置为`true`时，自动启动MySQL 8.0容器
3. **配置覆盖**：自动覆盖Spring Boot的数据源配置，指向容器中的MySQL
4. **自动关闭**：应用停止时，自动关闭MySQL容器

### 核心配置类

`TestcontainersInitializer.java`是Testcontainers的核心配置类，实现了`ApplicationContextInitializer`接口：

```java
public class TestcontainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    private static final String TESTCONTAINERS_ENABLED_PROPERTY = "app.testcontainers.enabled";
    
    private static MySQLContainer<?> mysqlContainer;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        String testcontainersEnabled = environment.getProperty(TESTCONTAINERS_ENABLED_PROPERTY, "false");
        
        if ("true".equalsIgnoreCase(testcontainersEnabled)) {
            startMySQLContainer();
            overrideDataSourceProperties(environment);
        }
    }
    
    // 启动MySQL容器
    private void startMySQLContainer() {
        if (mysqlContainer == null) {
            mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("orders_db")
                    .withUsername("test")
                    .withPassword("test")
                    .withUrlParam("useSSL", "false")
                    .withUrlParam("serverTimezone", "UTC")
                    .withUrlParam("allowPublicKeyRetrieval", "true");
            
            mysqlContainer.start();
            
            // 注册关闭钩子，应用停止时关闭容器
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (mysqlContainer != null && mysqlContainer.isRunning()) {
                    mysqlContainer.stop();
                }
            }));
        }
    }
    
    // 覆盖数据源配置
    private void overrideDataSourceProperties(ConfigurableEnvironment environment) {
        Map<String, Object> testcontainersProperties = new HashMap<>();
        
        testcontainersProperties.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        testcontainersProperties.put("spring.datasource.url", mysqlContainer.getJdbcUrl());
        testcontainersProperties.put("spring.datasource.username", mysqlContainer.getUsername());
        testcontainersProperties.put("spring.datasource.password", mysqlContainer.getPassword());
        testcontainersProperties.put("spring.sql.init.platform", "mysql");
        
        // 添加到环境变量的最前面，确保优先级最高
        environment.getPropertySources().addFirst(
                new MapPropertySource("testcontainers", testcontainersProperties)
        );
    }
}
```

### 在主应用中注册

```java
@SpringBootApplication
public class TestcontainersDemoApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TestcontainersDemoApplication.class);
        // 注册Testcontainers初始化器
        application.addInitializers(new TestcontainersInitializer());
        application.run(args);
    }
}
```

### 常用Testcontainers配置

#### 1. 指定MySQL版本
```java
MySQLContainer<?> mysql = new MySQLContainer<>("mysql:5.7");
```

#### 2. 配置端口映射
```java
MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withExposedPorts(3306);
```

#### 3. 配置环境变量
```java
MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withEnv("MYSQL_ROOT_PASSWORD", "root")
        .withEnv("MYSQL_DATABASE", "mydb")
        .withEnv("MYSQL_USER", "user")
        .withEnv("MYSQL_PASSWORD", "password");
```

#### 4. 等待容器就绪
```java
MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2));
```

#### 5. 配置初始化脚本
```java
MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withInitScript("schema.sql");
```

### 性能优化

#### 1. 复用容器
在开发过程中，可以通过设置系统属性来复用容器：
```java
static {
    System.setProperty("testcontainers.reuse.enable", "true");
}
```

#### 2. 使用镜像加速
配置Docker镜像加速器可以加快容器启动速度。

## Testcontainers启动多个服务配置

如果需要通过Testcontainers同时启动多个服务（如MySQL、Redis），可以按照以下方式配置：

### 1. 添加依赖

在`pom.xml`中添加Redis容器依赖：

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>redis</artifactId>
    <version>${testcontainers.version}</version>
</dependency>
```

### 2. 创建多服务初始化器

创建一个新的初始化器类，同时启动MySQL和Redis：

```java
package com.example.testcontainersdemo.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class MultiContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    private static final String TESTCONTAINERS_ENABLED_PROPERTY = "app.testcontainers.enabled";
    
    private static MySQLContainer<?> mysqlContainer;
    private static GenericContainer<?> redisContainer;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        String testcontainersEnabled = environment.getProperty(TESTCONTAINERS_ENABLED_PROPERTY, "false");
        
        if ("true".equalsIgnoreCase(testcontainersEnabled)) {
            startAllContainers();
            overrideAllProperties(environment);
        }
    }
    
    private void startAllContainers() {
        // 启动MySQL容器
        if (mysqlContainer == null) {
            mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("orders_db")
                    .withUsername("test")
                    .withPassword("test")
                    .withUrlParam("useSSL", "false")
                    .withUrlParam("serverTimezone", "UTC")
                    .withUrlParam("allowPublicKeyRetrieval", "true");
            
            mysqlContainer.start();
        }
        
        // 启动Redis容器
        if (redisContainer == null) {
            redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
            
            redisContainer.start();
        }
        
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (mysqlContainer != null && mysqlContainer.isRunning()) {
                mysqlContainer.stop();
            }
            if (redisContainer != null && redisContainer.isRunning()) {
                redisContainer.stop();
            }
        }));
    }
    
    private void overrideAllProperties(ConfigurableEnvironment environment) {
        Map<String, Object> testcontainersProperties = new HashMap<>();
        
        // MySQL配置
        testcontainersProperties.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        testcontainersProperties.put("spring.datasource.url", mysqlContainer.getJdbcUrl());
        testcontainersProperties.put("spring.datasource.username", mysqlContainer.getUsername());
        testcontainersProperties.put("spring.datasource.password", mysqlContainer.getPassword());
        testcontainersProperties.put("spring.sql.init.platform", "mysql");
        
        // Redis配置
        testcontainersProperties.put("spring.data.redis.host", redisContainer.getHost());
        testcontainersProperties.put("spring.data.redis.port", redisContainer.getMappedPort(6379));
        
        environment.getPropertySources().addFirst(
                new MapPropertySource("testcontainers", testcontainersProperties)
        );
    }
    
    public static MySQLContainer<?> getMySQLContainer() {
        return mysqlContainer;
    }
    
    public static GenericContainer<?> getRedisContainer() {
        return redisContainer;
    }
}
```

### 3. 在主应用中注册

```java
@SpringBootApplication
public class TestcontainersDemoApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TestcontainersDemoApplication.class);
        application.addInitializers(new MultiContainersInitializer());
        application.run(args);
    }
}
```

### 4. 添加Redis配置类

```java
package com.example.testcontainersdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
```

### 5. 使用容器间网络

如果需要容器之间互相通信（如应用容器连接数据库容器），可以使用网络别名：

```java
// 创建自定义网络
Network network = Network.newNetwork();

// MySQL容器加入网络
MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withNetwork(network)
        .withNetworkAliases("mysql");

// Redis容器加入网络
GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withNetwork(network)
        .withNetworkAliases("redis")
        .withExposedPorts(6379);

// 应用容器（如果需要）可以通过别名访问其他容器
// mysql:3306, redis:6379
```

### 6. 使用Docker Compose

对于更复杂的多容器场景，可以使用Testcontainers的Docker Compose支持：

```java
package com.example.testcontainersdemo.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DockerComposeInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    private static final String TESTCONTAINERS_ENABLED_PROPERTY = "app.testcontainers.enabled";
    
    private static DockerComposeContainer<?> environment;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        
        String testcontainersEnabled = env.getProperty(TESTCONTAINERS_ENABLED_PROPERTY, "false");
        
        if ("true".equalsIgnoreCase(testcontainersEnabled)) {
            startDockerCompose();
            overrideProperties(env);
        }
    }
    
    private void startDockerCompose() {
        if (environment == null) {
            environment = new DockerComposeContainer<>(new File("docker-compose.yml"))
                    .withExposedService("mysql_1", 3306, Wait.forLogMessage(".*ready for connections.*", 2))
                    .withExposedService("redis_1", 6379, Wait.forListeningPort());
            
            environment.start();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (environment != null) {
                    environment.stop();
                }
            }));
        }
    }
    
    private void overrideProperties(ConfigurableEnvironment env) {
        Map<String, Object> properties = new HashMap<>();
        
        // MySQL配置
        String mysqlHost = environment.getServiceHost("mysql_1", 3306);
        Integer mysqlPort = environment.getServicePort("mysql_1", 3306);
        properties.put("spring.datasource.url", 
                String.format("jdbc:mysql://%s:%d/orders_db?useSSL=false&serverTimezone=UTC", 
                        mysqlHost, mysqlPort));
        properties.put("spring.datasource.username", "test");
        properties.put("spring.datasource.password", "test");
        properties.put("spring.sql.init.platform", "mysql");
        
        // Redis配置
        String redisHost = environment.getServiceHost("redis_1", 6379);
        Integer redisPort = environment.getServicePort("redis_1", 6379);
        properties.put("spring.data.redis.host", redisHost);
        properties.put("spring.data.redis.port", redisPort);
        
        env.getPropertySources().addFirst(
                new MapPropertySource("docker-compose", properties)
        );
    }
}
```

### docker-compose.yml示例

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: orders_db
      MYSQL_USER: test
      MYSQL_PASSWORD: test
    ports:
      - "3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

## 注意事项

1. **Docker必须运行**：使用Testcontainers时，确保Docker Desktop（Windows/macOS）或Docker服务（Linux）正在运行
2. **首次启动较慢**：首次运行时需要下载MySQL镜像，可能需要一些时间
3. **容器自动清理**：应用停止后，容器会自动停止并删除（除非配置了reuse）
4. **环境变量**：生产环境建议使用环境变量覆盖数据库配置，不要硬编码密码

## 测试说明

本项目包含的测试场景：

1. **Testcontainers模式**：自动启动Docker中的MySQL容器（`app.testcontainers.enabled=true`）
2. **生产模式**：连接真实的MySQL服务（`app.testcontainers.enabled=false`）

可以通过修改`app.testcontainers.enabled`配置和`spring.profiles.active`来切换不同模式。

## 常见问题

### Q1: Testcontainers无法连接Docker
**解决方案**：
- 确保Docker Desktop（Windows/macOS）或Docker服务（Linux）正在运行
- 在Linux上，确保当前用户在docker组中：`sudo usermod -aG docker $USER`

### Q2: MySQL镜像下载慢
**解决方案**：
- 配置Docker镜像加速器
- 使用国内镜像源，如阿里云、网易云等

### Q3: 容器启动后应用连接失败
**解决方案**：
- 检查容器是否完全启动（MySQL需要一些时间初始化）
- 确认端口映射是否正确
- 检查用户名密码是否匹配

### Q4: 如何在测试中使用Testcontainers
**解决方案**：
Testcontainers更常用于集成测试，示例如下：

```java
@SpringBootTest
@Testcontainers
class OrderRepositoryTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Test
    void testSaveOrder() {
        // 测试代码
    }
}
```

## 参考链接

- [Testcontainers官方文档](https://www.testcontainers.org/)
- [Spring Boot Testcontainers集成](https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1)
- [MySQL容器文档](https://www.testcontainers.org/modules/databases/mysql/)
- [Redis容器文档](https://www.testcontainers.org/modules/redis/)
- [Docker Compose支持](https://www.testcontainers.org/modules/docker_compose/)
