# Amar Bank - GUI-Based Banking System

MITM 311 Advanced Object-Oriented Programming - OOP Lab Assessment.

A Java Swing desktop application for "Amar Bank" that manages two kinds of
accounts (Savings and Loan), processes their unique financial transactions, and
keeps a permanent local database in a **CSV file**.

## Build & Run

```bash
mvn clean package
java -jar target/AmarBank-1.0-SNAPSHOT.jar
```

Requires JDK 17+. No network or database server is needed; data is stored in
`data/accounts.csv`, created automatically on first run.

### Optional: SQLite storage

A SQLite-backed store is provided as an optional improvement and is fully
interchangeable with the CSV store. With the `org.xerial:sqlite-jdbc` driver on
the classpath, run with either flag to use it:

```bash
java -Damarbank.store=sqlite -jar target/AmarBank-1.0-SNAPSHOT.jar
# or
java -jar target/AmarBank-1.0-SNAPSHOT.jar --sqlite
```

If the driver is absent the application silently falls back to the required CSV
store.

## Verifying the logic

Two dependency-free harnesses live in `src/test/java`:

```bash
# Compile main + tests, then run them
SQLITE=$(find ~/.m2 -name 'sqlite-jdbc-*.jar' | head -1)
javac -d target/test-classes -cp "target/classes:$SQLITE" src/test/java/com/amarbank/*.java

java -cp "target/classes:target/test-classes:$SQLITE" com.amarbank.SelfTest      # 65 logic/regex/CSV/SQLite checks
java -cp "target/classes:target/test-classes:$SQLITE" com.amarbank.GuiSmokeTest  # builds all tabs + every theme
```

## How the code maps to the assignment

### 1. Object-Oriented Architecture (strict encapsulation)

| Class | Role |
|-------|------|
| `Account` (abstract) | Private fields `accountNumber`, `accountHolderName`, `branch`, `phone`, `balance` with public getters/setters. Abstract `deposit`/`withdraw`; common `transferFunds`, `updatePhone`, `shiftBranch`. |
| `SavingsAccount` | Standard `deposit` increases balance; `withdraw` decreases balance only with sufficient funds. |
| `LoanAccount` | Adds `loanLimit` and `amountDue`. `deposit` = repayment (reduces `amountDue` and balance); `withdraw` = borrowing (increases `amountDue` and balance, allowed only while `amountDue + amount <= loanLimit`). |
| `BankManagement` (controller) | Holds an `ArrayList<Account>` of all active accounts and contains the logic for reading/writing permanent storage (CSV by default). |

The storage format sits behind the `AccountStore` interface
(`storage/CsvAccountStore`, `storage/SqliteAccountStore`) so the controller's
logic is independent of the file format.

### 2. Regular-Expression Input Validation (`Validators`)

Built on `java.util.regex.Pattern` / `Matcher`; failures raise a `JOptionPane`
alert before any submission is processed.

| Field | Pattern |
|-------|---------|
| Phone number | `^01[3-9]\d{8}$` (valid 11-digit number) |
| Account number | `^AMB-\d{5}$` (e.g. `AMB-55412`) |
| Account holder name | `^[A-Z ]{2,50}$` (upper-case letters + spaces, 2-50 chars) |
| Branch name | `^[A-Za-z ]+$` (letters and spaces only) |

### 3. Graphical User Interface (Java Swing)

A tabbed (`JTabbedPane`) interface:

- **Account Management** - name / phone / branch / amount fields, a
  `JComboBox` to choose Savings vs Loan, and **Open Account**, **Update Phone
  Number** and **Shift Branch** buttons.
- **Transactions** - source / destination / amount fields with **Deposit**,
  **Withdraw** and **Transfer** buttons.
- **Accounts** - a searchable `JTable` roster plus a `JTextArea` summary of the
  selected account.

The window keeps a themed, fault-tolerant Look-and-Feel system (custom title
bar, switchable themes) that gracefully recovers from any unsupported theme.
