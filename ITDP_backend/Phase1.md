# 🚀 Spring Boot Learning Guide: Train Delay Prediction App
## Build a Full-Stack Application with Java Spring Boot

---

## 📌 PROJECT OVERVIEW

Your application will have 3 main components:
1. **Spring Boot Backend** (Java) - REST API, Business Logic
2. **Python FastAPI ML Service** (Already exists) - Predictions
3. **Database** (PostgreSQL) - Train & station data

**Data Flow:**
```
User Input (Origin, Destination) 
    ↓
Spring Boot API (Search trains)
    ↓
Database Query (Get train list)
    ↓
User selects train
    ↓
Spring Boot calls FastAPI for prediction
    ↓
FastAPI ML Model processes → returns delay percentage
    ↓
Spring Boot formats response to user
```

---

## 📂 PROJECT STRUCTURE

```
train-delay-prediction-backend/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/traindelay/
│   │   │       ├── controller/          # REST endpoints
│   │   │       │   └── TrainController.java
│   │   │       ├── service/             # Business logic
│   │   │       │   ├── TrainService.java
│   │   │       │   └── PredictionService.java
│   │   │       ├── repository/          # Database queries (JPA)
│   │   │       │   └── TrainRepository.java
│   │   │       ├── model/               # Entity classes (DB models)
│   │   │       │   ├── Train.java
│   │   │       │   └── Station.java
│   │   │       ├── dto/                 # Data Transfer Objects (API request/response)
│   │   │       │   ├── TrainSearchRequest.java
│   │   │       │   ├── TrainResponse.java
│   │   │       │   └── PredictionResponse.java
│   │   │       ├── client/              # External API clients
│   │   │       │   └── FastAPIClient.java
│   │   │       ├── config/              # Configuration classes
│   │   │       │   └── WebClientConfig.java
│   │   │       ├── exception/           # Custom exceptions
│   │   │       │   └── TrainNotFoundException.java
│   │   │       └── TrainDelayPredictionApplication.java  # Main class
│   │   │
│   │   └── resources/
│   │       ├── application.properties   # Configuration
│   │       └── data.sql                 # Initial data
│   │
│   └── test/                            # Unit & integration tests
│
├── pom.xml                              # Maven configuration
├── docker-compose.yml                   # Services orchestration
└── README.md
```

---

## 🎓 SPRING BOOT CORE CONCEPTS YOU'LL LEARN

### 1️⃣ **Dependency Injection (DI)**
- Spring automatically manages object creation and dependencies
- Use `@Autowired`, `@Component`, `@Service`, `@Repository`
- No need to manually `new` objects

**Example:** Instead of:
```java
TrainService service = new TrainService(new TrainRepository());
```

You write:
```java
@Autowired
private TrainService service;  // Spring creates it automatically
```

### 2️⃣ **MVC Architecture**
- **Model:** Data (Entity classes, DTOs)
- **View:** REST JSON responses
- **Controller:** Handles HTTP requests, calls services

### 3️⃣ **JPA (Java Persistence API)**
- Converts Java objects to database records automatically
- Write queries in methods, Spring does SQL
- `@Entity`, `@Repository`, `CrudRepository`

### 4️⃣ **REST APIs**
- `@RestController` - Returns JSON instead of HTML views
- `@GetMapping`, `@PostMapping` - HTTP methods
- Status codes (200, 201, 404, 500)

### 5️⃣ **External Service Integration**
- `RestTemplate` or `WebClient` to call FastAPI
- Handle async operations
- Error handling & retries

### 6️⃣ **Exception Handling**
- `@ExceptionHandler`, `@ControllerAdvice`
- Global error responses with proper HTTP status

---

## 🛠️ PREREQUISITES

Before we start coding, ensure you have:

| Tool | Version | Why |
|------|---------|-----|
| **Java JDK** | 17+ | Spring Boot 3.3+ requires this |
| **Maven** | 3.8+ | Build tool & dependency management |
| **PostgreSQL** | 12+ | Database |
| **Git** | Latest | Version control |
| **IDE** | IntelliJ/VS Code | Code editor |
| **Docker** | Latest | Optional, for containerization |

### 📥 Installation Checklist

```bash
# Verify installations
java -version          # Should show Java 17+
mvn -version          # Should show Maven 3.8+
psql --version        # Should show PostgreSQL 12+
git --version         # Should show Git version
```

---

## ⚙️ PHASE 1: PROJECT SETUP & CONFIGURATION

### Step 1: Create Spring Boot Project

**Option A: Using Spring Initializr (Recommended for beginners)**
1. Go to https://start.spring.io/
2. Select:
    - **Project:** Maven
    - **Language:** Java
    - **Spring Boot:** 3.3.x
    - **Group:** com.traindelay
    - **Artifact:** train-delay-prediction-backend
    - **Packaging:** Jar
    - **Java:** 17

3. **Dependencies to add:**
    - Spring Boot DevTools (Auto-reload during development)
    - Spring Web (REST APIs)
    - Spring Data JPA (Database ORM)
    - PostgreSQL Driver (Database)
    - Lombok (Reduce boilerplate code)
    - Validation (Input validation)
    - Spring Boot Actuator (Monitoring)

4. Click "Generate" → Download → Unzip

**Option B: Using Maven command**
```bash
mvn archetype:generate \
  -DgroupId=com.traindelay \
  -DartifactId=train-delay-prediction-backend \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

### Step 2: Project Structure After Generation

```
train-delay-prediction-backend/
├── src/
│   ├── main/java/com/traindelay/
│   ├── main/resources/
│   └── test/
├── pom.xml
└── .gitignore
```

### Step 3: Update `pom.xml`

Your `pom.xml` will have dependencies. Key sections:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
  </parent>
  
  <groupId>com.traindelay</groupId>
  <artifactId>train-delay-prediction-backend</artifactId>
  <version>1.0.0</version>

  <dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>42.7.1</version>
      <scope>runtime</scope>
    </dependency>

    <!-- Lombok (reduces code) -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <version>1.18.30</version>
      <scope>provided</scope>
    </dependency>

    <!-- Validation -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## 📝 KEY LEARNING POINTS

- **pom.xml** defines all dependencies
- **Maven** automatically downloads & manages them
- **Spring Boot Starters** (spring-boot-starter-*) bundle related dependencies
- **Scope:** runtime/provided/test control when dependency is used

---

## 🎯 NEXT STEPS

Once setup is complete, you'll have:
1. ✅ Maven project structure
2. ✅ Spring Boot configured
3. ✅ All dependencies ready
4. ✅ IDE integration working

**In the next phase**, we'll create:
- Database models (Train, Station entities)
- Database configuration
- Repository layer for queries
- Service layer for business logic

---

## 📚 RESOURCES
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Guide](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL JDBC Documentation](https://jdbc.postgresql.org/)