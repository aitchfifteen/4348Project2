# Development Log

## 2025-04-12

- **3:00 PM**
  Devlog: Bank Simulation

- Simulate a bank operation with multiple teller threads serving customer threads while enforcing limits on the number of customers inside the bank and simultaneous access to the safe.

Key Components:

Semaphores:

- Door Semaphore: Limits the bank capacity (only 2 customers can be inside).

- Safe Semaphore: Ensures that at most 2 tellers access the safe concurrently.

BlockingQueue:

- Serves as a FIFO line for customers waiting to be served.

Poison Pill Pattern:

- Special customer objects are used to signal tellers to stop after all real customers have been processed.

Implementation Steps:

Setup Resources:
Create semaphores for door and safe access and a LinkedBlockingQueue for customers.

Teller Threads:
Each teller continuously takes customers from the queue. When a teller takes a poison pill, it stops processing.

Customer Threads:
Each customer acquires a door permit, enters the queue, and then releases the door permit.

Shutdown Coordination:
After all customers have been added, the main thread places one poison pill per teller into the queue and waits for all threads to finish.

Outcome:
The final design simplifies synchronization by leveraging concurrent constructs (semaphores, blocking queue) and the poison pill technique, ensuring a clean, maintainable simulation of bank operations

- **6:00 PM**
  Began designing the Transaction class to represent deposits and withdrawals with a specific amount.

Created the class structure in Transaction.java.

Declared private, immutable fields for transaction type (txnType) and amount (txnAmount).

Implemented the constructor to initialize these fields upon object creation.

Added getter methods (getType() and getAmount()) to allow read-only access to the transaction details.

Compiled and manually tested the file to ensure the class worked as intended.
