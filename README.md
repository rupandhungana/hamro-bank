# Banking System (JavaFX + MySQL)

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