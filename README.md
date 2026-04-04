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

## Viva Questions and Answers: Mobile Banking App using JavaFX and MySQL

1. **What is encapsulation in this project?**  
   Encapsulation means keeping data and behavior together in one class.  
   Example: `Account` keeps balance as a field and updates it through methods like `deposit()` and `withdraw()`.

2. **Give a simple Java example of encapsulation.**
   ```java
   class User {
       private String username;
       public String getUsername() { return username; }
       public void setUsername(String username) { this.username = username; }
   }
   ```

3. **How is inheritance used in Java?**  
   Inheritance lets one class reuse another class’s properties and methods.  
   It reduces duplicate code and supports extension.

4. **Give a simple inheritance example.**
   ```java
   class Account { void showType(){ System.out.println("General"); } }
   class SavingsAccount extends Account { @Override void showType(){ System.out.println("Savings"); } }
   ```

5. **What is polymorphism in your project context?**  
   Polymorphism means one interface/reference can represent many forms.  
   For example, method overriding lets different account types define behavior differently.

6. **What is abstraction?**  
   Abstraction hides implementation details and shows only required operations.  
   Example: service classes expose methods like `login()` or `transfer()` without showing SQL details to controllers.

7. **What is FXML in JavaFX?**  
   FXML is an XML-based file to design UI layout separately from logic.  
   It improves readability and keeps controller code clean.

8. **What is the role of a JavaFX controller?**  
   The controller handles user events and binds UI actions to business logic.  
   Example: `LoginController` reads input fields and calls `AuthService.login(...)`.

9. **How do you load an FXML screen in JavaFX?**
   ```java
   FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bankingsystem/view/Login.fxml"));
   Parent root = loader.load();
   ```

10. **How is MySQL connected from Java?**  
    Using JDBC with `DriverManager.getConnection(url, user, pass)`.  
    In this project, `DatabaseManager` centralizes connection logic.

11. **Why use PreparedStatement instead of Statement?**  
    `PreparedStatement` prevents SQL injection and handles parameters safely.  
    It also improves readability and query reuse.

12. **Show a simple PreparedStatement example.**
   ```java
   String sql = "SELECT * FROM users WHERE username = ? AND status = ?";
   PreparedStatement ps = conn.prepareStatement(sql);
   ps.setString(1, username);
   ps.setString(2, "ACTIVE");
   ```

13. **What is a transaction in banking operations?**  
    A transaction is a group of SQL operations treated as one unit.  
    Either all changes happen (`commit`) or none happen (`rollback`).

14. **Why are commit and rollback important in fund transfer?**  
    Transfer updates sender and receiver balances together.  
    If one update fails, rollback prevents inconsistent money state.

15. **How does your app reduce SQL injection risk?**  
    It uses parameterized queries (`PreparedStatement`) instead of string-concatenated SQL.  
    User input is never directly appended into raw query text.

16. **How are passwords handled securely?**  
    Passwords are hashed with BCrypt before storing.  
    During login, entered password is verified against hash, not plain text.

17. **What is the login flow in this project?**  
    UI controller collects credentials, `AuthService` validates user status, then verifies BCrypt hash.  
    On success, session/user context is set and dashboard is opened.

18. **What checks are done before transfer?**  
    Verify sender account, receiver account, account status, and sufficient balance.  
    Then execute debit/credit inside one DB transaction.

19. **How is transaction history shown?**  
    Transactions are fetched from DB using account/user criteria and shown in JavaFX tables.  
    This gives users a clear audit trail of deposits, withdrawals, and transfers.

20. **Cross-question: Why did you choose JavaFX for this project?**  
    JavaFX gives a modern desktop UI with FXML + controller pattern.  
    It integrates well with Java business logic and JDBC backend.

21. **Cross-question: Why MySQL for banking records?**  
    MySQL is reliable, relational, and supports ACID transactions.  
    It is suitable for structured entities like users, accounts, and transactions.

22. **Cross-question: What is one future improvement you can add?**  
    Add OTP-based 2FA for login/transfer confirmation.  
    It improves security beyond just username and password.
