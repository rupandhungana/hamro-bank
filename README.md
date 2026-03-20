# hamro-bank
## Project Overview 
This repository presents an academic implementation of a secure Banking System developed using Java 17 and JavaFX. The system focuses on the practical application of advanced Object-Oriented Programming (OOP) design patterns, including the Service and Repository patterns, to ensure a strict separation of concerns. Key technical features include a robust authentication system using BCrypt hashing, a layered architecture (Model-View-Controller), and an atomic transaction engine that manages fund transfers with database-level row locking to prevent race conditions and ensure data integrity.

## Requirements
- Java 17+
- Maven 3.8+
- MySQL 8+

## Setup
1. Create the database and tables:
   - Run `schema.sql` in MySQL.
2. Update DB credentials in `src/main/java/com/bankingsystem/util/DatabaseManager.java`.
3. Create an admin user:
   - Register a user via the UI, then run:
     `UPDATE users SET role='ADMIN' WHERE username='your_admin_username';`

## Run
`mvn -U -DskipTests package`
`mvn javafx:run`

to clean:
mvn clean

## Notes
- Passwords are stored with BCrypt.
- Transfers use SQL transactions with `FOR UPDATE` locking.
