Files Included: BankSimulation.java - The main file that initializes and runs the bank simulation, managing teller and customer threads and controlling access to resources.

Teller.java - Contains the Teller class representing a bank teller thread. Each teller waits for a customer, processes transactions, and accesses the safe.

Customer.java - Contains the Customer class representing a customer thread. Each customer requests a transaction and waits for a teller.

Transaction.java - Contains the Transaction class, which holds transaction details (type and amount) for each customer.

Event.java - Implements a simple event class that allows signaling among threads.

To compile and run the project:

Compile: Run javac *.java in the project directory. This will compile all Java files.
Run: Execute java BankSimulation to start the bank simulation.

This project relies heavily on thread safety to avoid race conditions and deadlocks. The use of semaphores, locks, and condition variables helps control access to shared resources like the bank door and the safe, ensuring that only the allowed number of customers and tellers can access these resources at any time. Each customer and teller runs as an independent thread, and synchronization mechanisms allow them to interact without conflict. Simulation Flow: Tellers Announce Availability (Tellers initialize and announce their readiness) -> Customers Enter the Bank (Customers enter in sequence, wait for tellers, and then request transactions) -> Transaction Completion (Once all customers finish, the simulation stops, and tellers conclude their work)