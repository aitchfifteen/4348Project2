// BankSimulation.java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BankSimulation {
    public static void main(String[] args) throws InterruptedException {
        // Semaphores to restrict bank entry and safe access.
        Semaphore bankDoorSem = new Semaphore(2);
        Semaphore safeSem = new Semaphore(2);
        
        // Shared flag to indicate that no more customers will arrive.
        Event noMoreCustomers = new Event();
        
        // Shared lock and condition for teller availability.
        Lock sharedLock = new ReentrantLock();
        Condition tellerAvailCond = sharedLock.newCondition();
        List<Teller> tellerPool = new ArrayList<>();
        
        // Create and start 3 teller threads.
        List<Teller> tellers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Teller teller = new Teller(safeSem, noMoreCustomers, sharedLock, tellerAvailCond, tellerPool);
            teller.start();
            tellers.add(teller);
        }
        
        // Create and start 50 customer threads.
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Customer cust = new Customer(bankDoorSem, sharedLock, tellerAvailCond, tellerPool);
            cust.start();
            customers.add(cust);
            // Pause between customer arrivals.
            Thread.sleep(10 + (int)(Math.random() * 40));
        }
        
        // Wait for all customers to finish their transactions.
        for (Customer cust : customers) {
            cust.join();
        }
        
        // Notify tellers that there are no more customers.
        noMoreCustomers.set();
        
        // Wake up any waiting teller threads.
        for (Teller teller : tellers) {
            synchronized (teller.getCustomerSyncCondition()) {
                teller.getCustomerSyncCondition().signalAll();
            }
        }
        
        // Wait for all teller threads to complete.
        for (Teller teller : tellers) {
            teller.join();
        }
        
        System.out.println("BankSimulation: All transactions complete. Bank closing.");
    }
}
