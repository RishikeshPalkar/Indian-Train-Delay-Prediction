# 🗄️ PHASE 2: DATABASE MODELS & CONFIGURATION

## 🎯 WHAT WE'LL BUILD

In this phase you'll learn:
1. **JPA Entity classes** - Java objects that map to database tables
2. **Database relationships** - How tables connect
3. **Database configuration** - Connecting Spring Boot to PostgreSQL
4. **Lombok** - Reducing boilerplate code

---

## 📊 DATABASE SCHEMA

```sql
-- Stations Table
CREATE TABLE stations (
    id SERIAL PRIMARY KEY,
    station_code VARCHAR(10) UNIQUE NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    region VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trains Table
CREATE TABLE trains (
    id SERIAL PRIMARY KEY,
    train_number VARCHAR(10) UNIQUE NOT NULL,
    train_name VARCHAR(100) NOT NULL,
    origin_station_id INTEGER NOT NULL REFERENCES stations(id),
    destination_station_id INTEGER NOT NULL REFERENCES stations(id),
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    train_type VARCHAR(20),
    total_seats INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Prediction History (optional - track predictions)
CREATE TABLE predictions (
    id SERIAL PRIMARY KEY,
    train_id INTEGER NOT NULL REFERENCES trains(id),
    predicted_delay_minutes INTEGER,
    confidence_score DOUBLE,
    prediction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 💾 JPA ENTITY CLASSES

### Concept: What are Entities?

**Entity** = Java class that represents a database table
- Each instance = one row in table
- Each field = one column in table
- JPA automatically generates SQL

### File: `Station.java`

```java
package com.traindelay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Station Entity
 * Represents railway stations
 * JPA Annotations explained:
 * @Entity - tells Spring this class maps to a DB table
 * @Table - specifies table name
 * @Id - primary key
 * @GeneratedValue - auto-increment
 */
@Entity
@Table(name = "stations")
@Data                           // Lombok: generates getters, setters, toString, equals
@NoArgsConstructor              // Lombok: generates no-arg constructor (required by JPA)
@AllArgsConstructor             // Lombok: generates constructor with all fields
@JsonIgnoreProperties(ignoreUnknown = true)  // Ignore extra JSON fields during deserialization
public class Station {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10)
    private String stationCode;
    
    @Column(nullable = false, length = 100)
    private String stationName;
    
    @Column(length = 50)
    private String region;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // One station can have many trains departing from it
    @OneToMany(mappedBy = "originStation", cascade = CascadeType.ALL)
    private List<Train> departingTrains;
    
    // One station can have many trains arriving at it
    @OneToMany(mappedBy = "destinationStation", cascade = CascadeType.ALL)
    private List<Train> arrivingTrains;
}
```

**Key Concepts Explained:**

| Annotation | Meaning | Example |
|-----------|---------|---------|
| `@Entity` | This class maps to a DB table | |
| `@Table(name="stations")` | Specify table name (auto: class name lowercase) | `stations` table |
| `@Id` | Primary key field | id |
| `@GeneratedValue(strategy=GenerationType.IDENTITY)` | Auto-increment (database generates value) | id: 1,2,3... |
| `@Column` | Define column properties | nullable, unique, length |
| `@OneToMany` | One station → Many trains | 1 station has N departing trains |
| `mappedBy` | Which field owns the relationship | `originStation` in Train class |

**Lombok Annotations:**
- `@Data` → getters + setters + toString + equals + hashCode
- `@NoArgsConstructor` → `public Station() {}`
- `@AllArgsConstructor` → `public Station(Long id, String code, ...)`

---

### File: `Train.java`

```java
package com.traindelay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Train Entity
 * Represents trains running between stations
 * @ManyToOne: Many trains can originate from one station
 * @Builder: Allows convenient object creation: 
 *           Train.builder().trainNumber("12345").trainName("Express").build()
 */
@Entity
@Table(name = "trains")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder                        // Lombok: builder pattern for clean object creation
@JsonIgnoreProperties(ignoreUnknown = true)
public class Train {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10)
    private String trainNumber;
    
    @Column(nullable = false, length = 100)
    private String trainName;
    
    @Column(nullable = false, length = 20)
    private String trainType;  // Express, Local, Superfast, etc.
    
    // Many trains departing FROM one station
    @ManyToOne(fetch = FetchType.LAZY)  // LAZY = load only when accessed (better performance)
    @JoinColumn(name = "origin_station_id", nullable = false)
    private Station originStation;
    
    // Many trains arriving AT one station
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_station_id", nullable = false)
    private Station destinationStation;
    
    @Column(nullable = false)
    private LocalTime departureTime;
    
    @Column(nullable = false)
    private LocalTime arrivalTime;
    
    @Column(nullable = false)
    private Integer totalSeats;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prediction> predictions;
}
```

**New Concepts:**

| Annotation | Meaning | Why |
|-----------|---------|-----|
| `@ManyToOne` | This entity references another entity | Train.originStation → Station |
| `@JoinColumn` | Foreign key column in DB | `origin_station_id` column |
| `fetch = FetchType.LAZY` | Load data only when accessed | Improves query performance |
| `@Builder` | Fluent object creation syntax | `new Train()` → `Train.builder().trainNumber().trainName().build()` |

---

### File: `Prediction.java`

```java
package com.traindelay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Prediction Entity
 * Stores ML model predictions for audit & analytics
 */
@Entity
@Table(name = "predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;
    
    @Column(nullable = false)
    private Integer predictedDelayMinutes;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;  // 0.0 to 1.0 (0% to 100%)
    
    @Column(name = "prediction_date", nullable = false, updatable = false)
    private LocalDateTime predictionDate = LocalDateTime.now();
}
```

---

## ⚙️ DATABASE CONFIGURATION

### File: `application.properties`

**Location:** `src/main/resources/application.properties`

```properties
# ============================================
# Server Configuration
# ============================================
server.port=8080
server.servlet.context-path=/api

# ============================================
# Database Configuration (PostgreSQL)
# ============================================
# Connection URL
spring.datasource.url=jdbc:postgresql://localhost:5432/train_delay_db
# Database username
spring.datasource.username=postgres
# Database password
spring.datasource.password=your_password_here
# Driver class
spring.datasource.driver-class-name=org.postgresql.Driver

# ============================================
# JPA/Hibernate Configuration
# ============================================
# Hibernate dialect (PostgreSQL specific SQL generation)
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# DDL (Data Definition Language) Strategy
# update: Create tables if not exist, update if exist
# create: Drop & recreate tables (use only for development)
# validate: Check if tables match entities (for production)
spring.jpa.hibernate.ddl-auto=update

# Show generated SQL in console (helpful for debugging)
spring.jpa.show-sql=true

# Format SQL for readability
spring.jpa.properties.hibernate.format_sql=true

# Show binding parameters in SQL
spring.jpa.properties.hibernate.use_sql_comments=true

# ============================================
# Logging Configuration
# ============================================
logging.level.root=INFO
logging.level.com.traindelay=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# ============================================
# Application Name & Version
# ============================================
spring.application.name=train-delay-prediction-backend
spring.application.version=1.0.0
```

**What Each Config Does:**

| Property | Value | Explanation |
|----------|-------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/train_delay_db` | Where PostgreSQL is running + database name |
| `spring.datasource.username` | `postgres` | Default PostgreSQL user |
| `spring.datasource.password` | Your password | |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-create tables from entities |
| `spring.jpa.show-sql` | `true` | Print SQL queries to console |

---

## 📝 APPLICATION PROPERTIES (YAML Alternative)

If you prefer YAML format (`application.yml`):

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/train_delay_db
    username: postgres
    password: your_password_here
    driver-class-name: org.postgresql.Driver
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

logging:
  level:
    root: INFO
    com.traindelay: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## 🗄️ CREATE POSTGRESQL DATABASE

Before running Spring Boot, create the database:

```bash
# Connect to PostgreSQL
psql -U postgres

# In PostgreSQL prompt, create database
CREATE DATABASE train_delay_db;

# Verify creation
\l

# Exit
\q
```

Or using SQL script file (`init-db.sql`):

```sql
-- Create database
CREATE DATABASE train_delay_db;

-- Connect to it
\c train_delay_db;

-- Create sequences for auto-increment
CREATE SEQUENCE stations_id_seq START 1;
CREATE SEQUENCE trains_id_seq START 1;
CREATE SEQUENCE predictions_id_seq START 1;

-- Create tables
CREATE TABLE stations (
    id BIGINT PRIMARY KEY DEFAULT nextval('stations_id_seq'),
    station_code VARCHAR(10) UNIQUE NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    region VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trains (
    id BIGINT PRIMARY KEY DEFAULT nextval('trains_id_seq'),
    train_number VARCHAR(10) UNIQUE NOT NULL,
    train_name VARCHAR(100) NOT NULL,
    origin_station_id BIGINT NOT NULL REFERENCES stations(id),
    destination_station_id BIGINT NOT NULL REFERENCES stations(id),
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    train_type VARCHAR(20),
    total_seats INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE predictions (
    id BIGINT PRIMARY KEY DEFAULT nextval('predictions_id_seq'),
    train_id BIGINT NOT NULL REFERENCES trains(id),
    predicted_delay_minutes INTEGER,
    confidence_score DOUBLE PRECISION,
    prediction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster queries
CREATE INDEX idx_trains_origin ON trains(origin_station_id);
CREATE INDEX idx_trains_destination ON trains(destination_station_id);
CREATE INDEX idx_predictions_train ON predictions(train_id);
```

Run the script:
```bash
psql -U postgres -f init-db.sql
```

---

## 📊 DATABASE RELATIONSHIPS EXPLAINED

```
Station (1) ─────────────── (Many) Trains
   ↑                              ↑
   │ originStation                │ origin
   └──────────────────────────────┘
   
   └──────────────────────────────┐
   │ destinationStation           │ destination
   ↓                              ↓
 (1) ─────────────── (Many)
```

**Example:**
```
Station: Mumbai Central (id=1)
    ├─ Departing Trains:
    │  ├─ 12345 Express (to Delhi)
    │  ├─ 12346 Local (to Pune)
    │  └─ 12347 Superfast (to Bangalore)
    │
    └─ Arriving Trains:
       ├─ 11111 Express (from Delhi)
       └─ 11112 Local (from Goa)
```

**In Java:**
```java
Station mumbai = stationRepository.findByStationCode("CSTM");

// Get all trains departing from Mumbai
List<Train> departingTrains = mumbai.getDepartingTrains();  // [12345, 12346, 12347]

// Get all trains arriving in Mumbai
List<Train> arrivingTrains = mumbai.getArrivingTrains();    // [11111, 11112]
```

---

## ✅ CHECKLIST FOR THIS PHASE

- [ ] Created Station.java entity
- [ ] Created Train.java entity
- [ ] Created Prediction.java entity
- [ ] Configured application.properties
- [ ] Created PostgreSQL database
- [ ] Verified database connection

---

## 🎓 KEY LEARNING POINTS

1. **@Entity** - Maps Java class to database table
2. **@ManyToOne, @OneToMany** - Define relationships between entities
3. **@JoinColumn** - Specifies foreign key column
4. **Lombok reduces boilerplate** - No need to write getters/setters
5. **JPA auto-generates SQL** - No manual SQL queries needed
6. **application.properties** - Centralized configuration
7. **fetch = FetchType.LAZY** - Performance optimization

---

## 🚀 NEXT PHASE

After verifying entities work with database:
1. Create **Repository** layer (Query builders)
2. Create **Service** layer (Business logic)
3. Create **Controller** layer (REST endpoints)
4. Implement **FastAPI integration**