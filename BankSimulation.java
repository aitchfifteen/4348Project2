import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class BankSimulation {
    // Settings for the simulation
    private static final int NUM_TELLERS = 3;
    private static final int NUM_CUSTOMERS = 50;
    private static final int BANK_CAPACITY = 2; // Max number of customers allowed inside the bank at once

    public static void main(String[] args) throws InterruptedException {
        // Semaphore to limit concurrent access to the bank safe
        Semaphore safeSemaphore = new Semaphore(2);

        // Semaphore to limit the number of customers inside the bank
        Semaphore doorSemaphore = new Semaphore(BANK_CAPACITY);

        // BlockingQueue used as a waiting line for customers (FIFO)
        BlockingQueue<Customer> customerQueue = new LinkedBlockingQueue<>();

        // Create and start teller threads
        List<Thread> tellerThreads = new ArrayList<>();
        for (int i = 0; i < NUM_TELLERS; i++) {
            Teller teller = new Teller(safeSemaphore, customerQueue);
            Thread t = new Thread(teller, "Teller-" + i);
            t.start();
            tellerThreads.add(t);
        }

        // Create and start customer threads
        List<Thread> customerThreads = new ArrayList<>();
        for (int i = 0; i < NUM_CUSTOMERS; i++) {
            Customer customer = new Customer(doorSemaphore, customerQueue, i);
            Thread t = new Thread(customer, "Customer-" + i);
            t.start();
            customerThreads.add(t);
            // Simulate a random delay between customer arrivals
            Thread.sleep((int)(Math.random() * 40) + 10);
        }

        // Wait for all customer threads to finish their entrance into the queue
        for (Thread t : customerThreads) {
            t.join();
        }

        // Once all customers are added to the queue, send "poison pills" so each teller knows to stop
        for (int i = 0; i < NUM_TELLERS; i++) {
            customerQueue.put(new Customer(true));
        }

        // Wait for all teller threads to finish processing
        for (Thread t : tellerThreads) {
            t.join();
        }

        System.out.println("Bank simulation finished.");
    }

    /**
     * Teller simulates a bank teller that continuously takes customers from the queue,
     * processes them (with safe access if needed), and stops when a poison pill is encountered.
     */
    static class Teller implements Runnable {
        private final Semaphore safeSemaphore;
        private final BlockingQueue<Customer> customerQueue;

        public Teller(Semaphore safeSemaphore, BlockingQueue<Customer> customerQueue) {
            this.safeSemaphore = safeSemaphore;
            this.customerQueue = customerQueue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // Wait (blocking) for a customer to arrive in the queue.
                    Customer customer = customerQueue.take();
                    
                    // If this is a poison pill, break the loop to stop the teller.
                    if (customer.isPoisonPill()) {
                        break;
                    }
                    serveCustomer(customer);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().getName() + " finished.");
        }

        // Simulate serving a customer. Tellers acquire safeSemaphore when needing safe access.
        private void serveCustomer(Customer customer) throws InterruptedException {
            System.out.println(Thread.currentThread().getName() + " is serving Customer-" + customer.getId());
            // Safely access the safe
            safeSemaphore.acquire();
            try {
                // Simulate working with the bank safe
                Thread.sleep((int)(Math.random() * 100) + 50);
            } finally {
                safeSemaphore.release();
            }
            // Simulate additional service time
            Thread.sleep((int)(Math.random() * 50) + 50);
            System.out.println(Thread.currentThread().getName() + " finished serving Customer-" + customer.getId());
        }
    }

    /**
     * Customer simulates a bank customer who must first enter the bank (acquiring a door permit)
     * and then join the waiting queue for service.
     * There is also a special "poison pill" customer used to signal tellers to stop.
     */
    static class Customer implements Runnable {
        private final Semaphore doorSemaphore;
        private final BlockingQueue<Customer> customerQueue;
        private final int id;
        private final boolean poisonPill; // True if this is a poison pill

        // Constructor for regular customers
        public Customer(Semaphore doorSemaphore, BlockingQueue<Customer> customerQueue, int id) {
            this.doorSemaphore = doorSemaphore;
            this.customerQueue = customerQueue;
            this.id = id;
            this.poisonPill = false;
        }

        // Constructor for poison pills
        public Customer(boolean poisonPill) {
            this.doorSemaphore = null;
            this.customerQueue = null;
            this.id = -1;
            this.poisonPill = poisonPill;
        }

        public int getId() {
            return id;
        }

        public boolean isPoisonPill() {
            return poisonPill;
        }

        @Override
        public void run() {
            // Regular customers go through the bank door and join the queue
            if (!poisonPill) {
                try {
                    // Acquire permit to enter the bank
                    doorSemaphore.acquire();
                    System.out.println("Customer-" + id + " has entered the bank.");
                    
                    // Add self to the customer queue so a teller can serve them
                    customerQueue.put(this);
                    
                    // Once in the queue, the customer leaves the bank area (freeing up space)
                    doorSemaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
