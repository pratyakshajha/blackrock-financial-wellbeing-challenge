# AutoSave Engine: Financial Well-being API

## Overview

The AutoSave Engine is a Spring Boot-based RESTful API designed to help users analyze their spending habits, validate financial transactions, and project potential investment returns. It provides a suite of endpoints for parsing expenses, applying complex temporal validation rules, and calculating the future value of savings based on different investment vehicles (NPS and Index Funds).

The application is fully containerized with Docker, making it easy to build, run, and deploy in any environment.

## Features

- **Expense Parsing**: Converts raw expense data into structured transactions, calculating savings potential by rounding up expenses to the nearest hundred.
- **Transaction Validation**: Identifies duplicate, invalid (e.g., negative amount), and valid transactions based on a user-defined investment cap (wage).
- **Temporal Constraint Filtering**: Applies advanced rules based on user-defined time periods (`p`, `q`, `k` periods) to adjust savings amounts dynamically.
- **Investment Return Projection**: Calculates the future value of investments for both NPS (7.11% interest) and Index Fund (14.49% interest) options, including inflation adjustments and tax benefit analysis for NPS.
- **System Performance Monitoring**: Exposes an endpoint to monitor application uptime, memory usage, and thread count using Spring Boot Actuator metrics.
- **Dockerized**: Fully containerized for easy deployment and scalability.

## Technologies Used

- **Language**: Java 17+
- **Framework**: Spring Boot 3.2.6
- **Build Tool**: Gradle
- **Libraries**:
  - Spring Web (for REST APIs)
  - Spring Boot Actuator (for performance metrics)
  - Spring Security (for future security enhancements)
  - Lombok (to reduce boilerplate code)
  - MapStruct (for efficient DTO-entity mapping)
  - JUnit 5 & Mockito (for comprehensive testing)
- **Containerization**: Docker

## Prerequisites

To build and run this project locally, you will need:

- **JDK 17** or higher
- **Docker Desktop** (for containerization)

## Getting Started

Follow these steps to get the application up and running on your local machine.

### 1. Clone the Repository

```sh
git clone <your-repository-url>
cd blackrock-financial-wellbeing-challenge
```

### 2. Build the Project

Use the Gradle wrapper to build the project. This will compile the code, run all tests, and create an executable JAR file.

```sh
./gradlew build
```

The executable JAR will be located in `build/libs/`.

### 3. Run the Application

You can run the application directly using the generated JAR file.

```sh
java -jar build/libs/autosave-engine-0.0.1-SNAPSHOT.jar
```

The application will start on port `5477` and will be accessible at `http://localhost:5477`. The base path for all API endpoints is `/blackrock/challenge/v1`.

## API Endpoints

All endpoints are relative to the base URL: `http://localhost:5477/blackrock/challenge/v1`

---

### 1. Parse Expenses

- **Endpoint**: `POST /transactions:parse`
- **Description**: Takes a list of raw expenses and returns a list of structured transactions with `ceiling` and `remanent` (potential savings) calculated.

**Example Request:**
```json
[
  {
    "amount": 250,
    "date": "2023-10-12 20:15:30"
  }
]
```

**Example Response:**
```json
[
  {
    "amount": 250.00,
    "date": "2023-10-12 20:15:30",
    "ceiling": 300.00,
    "remanent": 50.00
  }
]
```

---

### 2. Validate Transactions

- **Endpoint**: `POST /transactions:validator`
- **Description**: Validates a list of transactions against an investment cap (`wage`). It identifies valid transactions, duplicates, and transactions with negative amounts.

**Example Request:**
```json
{
  "transactions": [
    { "amount": 250, "date": "2023-10-12 20:15:30", "remanent": 50 },
    { "amount": -100, "date": "2023-10-13 10:00:00", "remanent": 0 }
  ],
  "wage": 1000
}
```

**Example Response:**
```json
{
  "valid": [
    { "amount": 250.00, "date": "2023-10-12 20:15:30", "remanent": 50.00 }
  ],
  "invalid": [
    { "amount": -100.00, "date": "2023-10-13 10:00:00", "remanent": 0.00, "message": "Negative amounts are not allowed" }
  ]
}
```

---

### 3. Filter Transactions (Temporal Constraints)

- **Endpoint**: `POST /transactions:filter`
- **Description**: Applies advanced temporal rules (`p`, `q`, `k` periods) to a list of transactions to adjust their `remanent` values and determine their validity.

**Example Request:**
```json
{
  "transactions": [
    { "amount": 250, "date": "2023-10-12 20:15:30", "remanent": 50 }
  ],
  "wage": 1000,
  "p": [
    { "start": "2023-10-01 00:00:00", "end": "2023-10-31 23:59:59", "extra": 25 }
  ],
  "q": [],
  "k": [
    { "start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59" }
  ]
}
```

**Example Response:**
```json
{
  "valid": [
    {
      "amount": 250.00,
      "date": "2023-10-12 20:15:30",
      "remanent": 75.00,
      "inKPeriod": true
    }
  ],
  "invalid": []
}
```

---

### 4. Calculate Investment Returns

- **Endpoints**:
  - `POST /returns:nps` (for National Pension Scheme)
  - `POST /returns:index` (for NIFTY 50 Index Fund)
- **Description**: Calculates the projected returns on investments for each `k` period. It takes into account the user's age, inflation, and all temporal constraints.

**Example Request (for both endpoints):**
```json
{
  "age": 29,
  "inflation": 5.5,
  "wage": 600000,
  "transactions": [
    { "amount": 250, "date": "2023-10-12 20:15:30" }
  ],
  "p": [
    { "start": "2023-10-01 00:00:00", "end": "2023-10-31 23:59:59", "extra": 25 }
  ],
  "q": [],
  "k": [
    { "start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59" }
  ]
}
```

**Example Response (`/returns:nps`):**
```json
{
  "transactionsTotalAmount": 250.00,
  "transactionsTotalCeiling": 300.00,
  "savingsByDates": [
    {
      "start": "2023-01-01 00:00:00",
      "end": "2023-12-31 23:59:59",
      "amount": 75.00,
      "profits": 44.96,
      "taxBenefit": 0.00
    }
  ]
}
```

---

### 5. Get Performance Metrics

- **Endpoint**: `GET /performance`
- **Description**: Reports system execution metrics such as application uptime, used heap memory, and live thread count.

**Example Response:**
```json
{
  "time": "00:11:52.345",
  "memory": "25.11 MB",
  "threads": 16
}
```

## Dockerization

The project includes a `Dockerfile` for easy containerization.

### 1. Build the Docker Image

Ensure you have built the project first (`./gradlew build`). Then, run the following command to build the Docker image.

```sh
docker build -t your-dockerhub-username/blk-hacking-ind-{name-lastname} .
```
*(Replace `your-dockerhub-username` with your actual Docker Hub username.)*

### 2. Run the Docker Container

Run the container locally, mapping the application port to your host machine.

```sh
docker run -p 5477:5477 your-dockerhub-username/blk-hacking-ind-{name-lastname}
```

Note: my particular image name is: `blk-hacking-ind-pratyaksha-jha:latest`
This can be run by: `docker run -p 5477:5477 pratyakshajha/blk-hacking-ind-pratyaksha-jha:latest`

The application will be accessible at `http://localhost:5477/blackrock/challenge/v1/`.

## Deployment

To deploy the application publicly, you can use a container registry and a cloud server.

1.  **Push to a Registry**: After building and tagging your image, push it to a public registry like Docker Hub.
    ```sh
    docker login
    docker push your-dockerhub-username/blk-hacking-ind-{name-lastname}
    ```

2.  **Run on a Server**:
    - Provision a virtual machine from a cloud provider (e.g., AWS EC2, DigitalOcean Droplet).
    - Install Docker on the server.
    - Ensure the server's firewall allows incoming traffic on port `5477`.
    - Pull and run your container on the server:
      ```sh
      docker pull your-dockerhub-username/blk-hacking-ind-{name-lastname}
      docker run -d -p 5477:5477 your-dockerhub-username/blk-hacking-ind-{name-lastname}
      ```

Your application will now be publicly accessible at `http://<your_server_public_ip>:5477/blackrock/challenge/v1/`.

## Testing

The project includes a comprehensive suite of unit and integration tests. To run all tests, execute the following command:

```sh
./gradlew test
```

A detailed test report will be generated in `build/reports/tests/test/index.html`.