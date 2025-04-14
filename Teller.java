// Teller.java
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Teller extends Thread {
    private static int nextId = 0;
    private static final Lock idAssignLock = new ReentrantLock();

    private final Semaphore safeSem;              // Controls access to the safe (max 2 tellers)
    private final Event noMoreCustomersFlag;        // Signifies when customer arrivals have ended
    private final Lock sharedLock;                  // Shared lock for synchronizing teller availability
    private final Condition tellerAvailCondition;   // Condition that signals free tellers
    private final List<Teller> tellerPool;          // Shared list of available tellers

    // Condition used for customer-to-teller communication
    private final Condition customerSyncCondition;  

    private Customer activeCustomer = null;         // Currently assigned customer
    private Transaction pendingOperation = null;      // Operation received from the customer
    private final Event operationComplete = new Event(); // Signals completion of a transaction

    private final int tellerId;                     // Unique identifier

    public Teller(Semaphore safeSem, Event noMoreCustomersFlag, Lock sharedLock,
                  Condition tellerAvailCondition, List<Teller> tellerPool) {
        idAssignLock.lock();
        try {
            this.tellerId = nextId++;
        } finally {
            idAssignLock.unlock();
        }
        this.safeSem = safeSem;
        this.noMoreCustomersFlag = noMoreCustomersFlag;
        this.sharedLock = sharedLock;
        this.tellerAvailCondition = tellerAvailCondition;
        this.tellerPool = tellerPool;
        this.customerSyncCondition = sharedLock.newCondition();
    }
    
    // Renamed the getter to getTellerId() to avoid conflict with Thread's getId()
    public int getTellerId() {
        return tellerId;
    }
    
    // Allow access to the customer synchronization condition.
    public Condition getCustomerSyncCondition() {
        return customerSyncCondition;
    }
    
    // Called by a customer to assign themselves to this teller.
    public void assignCustomer(Customer cust) {
        this.activeCustomer = cust;
    }
    
    // Called by a customer to send the transaction information.
    public void setPendingOperation(Transaction txn) {
        this.pendingOperation = txn;
    }
    
    // Provides the event used by a customer to wait for the transaction completion.
    public Event getOperationComplete() {
        return operationComplete;
    }
    
    // Helper to simulate delays while printing before and after messages.
    private void simulateDelay(String stage, int minMillis, int maxMillis) {
        int delay = minMillis + new Random().nextInt(maxMillis - minMillis + 1);
        System.out.printf("Teller-%d (%s): Starting delay (%d ms).%n", tellerId, stage, delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("Teller-%d (%s): Completed delay (%d ms).%n", tellerId, stage, delay);
    }
    
    @Override
    public void run() {
        while (!noMoreCustomersFlag.isSet() || hasActiveCustomer()) {
            markSelfAvailable();
            Customer cust = waitForCustomerAssignment();
            if (cust == null) continue;
            requestTransaction();
            Transaction txn = waitForTransactionInfo();
            System.out.printf("Teller-%d [Customer-%d]: Processing %s of $%.2f.%n",
                    tellerId, cust.getCustomerId(), txn.getType(), txn.getAmount());
            if ("Withdraw".equals(txn.getType())) {
                System.out.printf("Teller-%d: Requires manager approval for withdrawal.%n", tellerId);
                simulateDelay("Manager Approval", 5, 30);
            }
            System.out.printf("Teller-%d: Attempting safe access.%n", tellerId);
            safeSem.acquireUninterruptibly();
            System.out.printf("Teller-%d: Entered safe.%n", tellerId);
            simulateDelay("Transaction Processing", 10, 50);
            System.out.printf("Teller-%d: Exiting safe, transaction finalized.%n", tellerId);
            safeSem.release();
            System.out.printf("Teller-%d [Customer-%d]: Transaction complete.%n", tellerId, cust.getCustomerId());
            operationComplete.set();
            resetState();
        }
        System.out.printf("Teller-%d: No remaining customers. Shutting down.%n", tellerId);
    }
    
    // Adds this teller to the available pool and signals waiting customers.
    private void markSelfAvailable() {
        sharedLock.lock();
        try {
            tellerPool.add(this);
            System.out.printf("Teller-%d: Marked available.%n", tellerId);
            tellerAvailCondition.signalAll();
        } finally {
            sharedLock.unlock();
        }
    }
    
    // Waits until a customer assigns themselves to this teller.
    private Customer waitForCustomerAssignment() {
        sharedLock.lock();
        try {
            while (activeCustomer == null && !noMoreCustomersFlag.isSet()) {
                customerSyncCondition.await();
            }
            return activeCustomer;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            sharedLock.unlock();
        }
    }
    
    // Prompts the customer for the transaction details.
    private void requestTransaction() {
        System.out.printf("Teller-%d [Customer-%d]: Requesting transaction details.%n",
                tellerId, activeCustomer.getCustomerId());
    }
    
    // Waits for the transaction information from the customer.
    private Transaction waitForTransactionInfo() {
        sharedLock.lock();
        try {
            while (pendingOperation == null) {
                customerSyncCondition.await();
            }
            return pendingOperation;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            sharedLock.unlock();
        }
    }
    
    // Resets the teller's state for the next customer.
    private void resetState() {
        sharedLock.lock();
        try {
            activeCustomer = null;
            pendingOperation = null;
        } finally {
            sharedLock.unlock();
        }
    }
    
    // Checks if a customer is still assigned.
    private boolean hasActiveCustomer() {
        sharedLock.lock();
        try {
            return activeCustomer != null;
        } finally {
            sharedLock.unlock();
        }
    }
}
