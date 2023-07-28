# Copyflix: A Scala & Akka Technical Demo

Copyflix is a technical demonstration of a movie management system built using Scala and Akka. This project serves as an introduction for developers looking to get started with Scala, Akka, and related technologies. It showcases the power of reactive programming and provides a hands-on example of building a robust system with these tools.

## Features

- **Reactive Programming**: Experience the power of reactive programming with Akka actors.
- **CRUD Operations**: Perform Create, Read, Update, and Delete operations on movies.
- **Search Capabilities**: Query movies based on various attributes like release year.
- **Akka Persistence**: Learn how to persistently store and manage state in Akka applications.
- **Akka HTTP**: Dive into building HTTP routes and handling requests using Akka HTTP.

## Technical Stack

- **Language**: Scala 2.13.10
- **Framework**: Akka
- **Database**: MongoDB (using the Reactive Streams driver)
- **HTTP**: Akka HTTP for building the RESTful API

## Getting Started

### Prerequisites

- Install Scala and SBT (Scala Build Tool).
- MongoDB instance running locally or provide connection details in the configuration.

### Running the Project

1. Clone the repository: `git clone https://github.com/epantke/copyflix.git`
2. Navigate to the project directory: `cd copyflix`
3. Start the application using SBT: `sbt run`
4. The server will start on `localhost:8080`.

## API Endpoints

- **Add Movie**: POST `/movies`
  - Payload: `{ "id": "1234", "name": "Movie Name", "year": 1997, "rating": 3 }`
- **Update Movie**: PUT `/movies/{movieId}`
  - Payload: `{ "id": "1234", "name": "Updated Movie Name", "year": 1997, "rating": 3 }`
- **Delete Movie**: DELETE `/movies/{movieId}`
- **Get Movie by ID**: GET `/movies/{movieId}`
- **List All Movies**: GET `/movies`

## Configuration

The application's configuration can be found in `src/main/resources/application.conf`. This file contains settings related to:

- Server details
- Akka actor systems and persistence
- MongoDB connection details
- Backoff strategies

## Contribution & Feedback

This project is a technical demo, and contributions are welcome! If you have suggestions, improvements, or want to discuss best practices, feel free to fork the repository, submit pull requests, or open issues.

## Acknowledgements

Special thanks to the Scala and Akka communities for their extensive documentation and support.

---
