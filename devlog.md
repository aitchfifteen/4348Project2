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

## 2025-04-13

Overview:
Designed a lightweight synchronization utility to coordinate thread behavior. This class centers on a simple boolean flag with synchronized methods to both signal and await an event. The goal was to provide a straightforward mechanism for inter-thread communication while ensuring safety and responsiveness.

Brainstorming & Design:

Choice of Synchronization:

Chose synchronized methods (on isSet(), set(), and waitUntilSet()) to guarantee atomic state changes and visibility across threads.

Implemented a waiting loop inside waitUntilSet to handle spurious wake-ups while also preserving interruption handling.

Notification Mechanism:

Decided to use notifyAll() in the set() method so that any thread waiting for the event would resume promptly.

**09:00 AM**:

Outlined the API for Event.java including methods to query (isSet()), signal (set()), and wait (waitUntilSet()) for the event.

**09:30 AM**:

Completed the initial coding of the class.

Integrated the synchronized block with a boolean flag, ensuring that once set, all waiting threads are notified.

**10:00 AM**:

Ran initial tests in a multi-threaded scenario.

Verified that threads blocked in waitUntilSet() correctly resume their execution once set() is called.

Outcome:

Event.java now provides an efficient mechanism for thread signaling.

The design meets the requirement for simple inter-thread coordination with proper handling for spurious wake-ups and interrupts.

Brainstorming & Design:

Multithreading Setup:

Extended Thread to allow each Teller to operate concurrently.

Implemented a static counter with a dedicated lock (using ReentrantLock) to assign a unique ID to each Teller.

Concurrency Controls:

Semaphore (safeSemaphore):

Limited access to the safe by allowing only two tellers inside concurrently.

Lock and Conditions:

Used a shared ReentrantLock with associated Conditions to manage waiting for customer transactions and to signal when a teller is ready.

Inter-Process Communication:

Integrated an Event (transactionComplete) to signal that a transaction has been processed.

**11:00 AM**:

Built the goToManager() method, simulating the request for withdrawal permission with a random delay (mimicking real-world variability in response times).

**11:45 AM**:

Developed safe access methods:

The accessSafe() method acquires a semaphore permit, controlling entry to the safe.

The performTransaction() method simulates processing within the safe, complete with randomized processing delays.

**1:00 PM**:

Conducted extensive testing with multiple teller threads to ensure:

Only two tellers enter the safe at any given time.

The teller’s state transitions (waiting, requesting manager permission, processing, and releasing the safe) function correctly.

**3:00 PM**:

Finishing up and push to main. Taking a break
