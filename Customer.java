// Customer.java
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Customer extends Thread {
    private static int nextCustomerId = 1;
    private static final Lock idLock = new ReentrantLock();
    private final int customerId;
    private final Semaphore entrySemaphore;   // Limits bank entry (max 2 customers)
    private final Lock sharedLock;            // Shared lock with tellers
    private final Condition tellerAvailCond;  // Condition signaling free tellers
    private final List<Teller> tellerPool;    // Shared pool of available tellers

    private Teller chosenTeller;
    private final Transaction desiredOperation; // The transaction this customer will perform

    public Customer(Semaphore entrySemaphore, Lock sharedLock, Condition tellerAvailCond, List<Teller> tellerPool) {
        idLock.lock();
        try {
            this.customerId = nextCustomerId++;
        } finally {
            idLock.unlock();
        }
        this.entrySemaphore = entrySemaphore;
        this.sharedLock = sharedLock;
        this.tellerAvailCond = tellerAvailCond;
        this.tellerPool = tellerPool;
        this.desiredOperation = selectOperation();
    }
    
    // Decides randomly whether to deposit or withdraw.
    private Transaction selectOperation() {
        String op = new Random().nextBoolean() ? "Deposit" : "Withdraw";
        double amount = 100 + new Random().nextDouble() * 900;
        System.out.printf("Customer-%d: Selected operation %s for $%.2f.%n", customerId, op, amount);
        return new Transaction(op, amount);
    }
    
    @Override
    public void run() {
        System.out.printf("Customer-%d: Arriving at bank.%n", customerId);
        enterBank();
        chooseTeller();
        greetTeller();
        pauseForPrompt();
        provideOperation();
        awaitCompletion();
        leaveBank();
        System.out.printf("Customer-%d: Leaving bank.%n", customerId);
    }
    
    public int getCustomerId() {
        return customerId;
    }
    
    private void enterBank() {
        System.out.printf("Customer-%d: Waiting for entry.%n", customerId);
        try {
            entrySemaphore.acquire();
            System.out.printf("Customer-%d: Entered bank.%n", customerId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Select an available teller.
    private void chooseTeller() {
        sharedLock.lock();
        try {
            while (tellerPool.isEmpty()) {
                System.out.printf("Customer-%d: No teller available, waiting.%n", customerId);
                tellerAvailCond.await();
            }
            chosenTeller = tellerPool.remove(0);
            // Inform the teller that this customer is now assigned.
            synchronized (chosenTeller.getCustomerSyncCondition()) {
                chosenTeller.assignCustomer(this);
                chosenTeller.getCustomerSyncCondition().signal();
            }
            // Use getTellerId() here.
            System.out.printf("Customer-%d: Chose Teller-%d.%n", customerId, chosenTeller.getTellerId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            sharedLock.unlock();
        }
    }
    
    private void greetTeller() {
        System.out.printf("Customer-%d [Teller-%d]: Greeting the teller.%n", customerId, chosenTeller.getTellerId());
    }
    
    // Simulate waiting before the teller requests the transaction.
    private void pauseForPrompt() {
        try {
            int pauseDuration = 5 + new Random().nextInt(16); // 5 to 20 ms
            System.out.printf("Customer-%d [Teller-%d]: Waiting for prompt (%d ms).%n", customerId, chosenTeller.getTellerId(), pauseDuration);
            Thread.sleep(pauseDuration);
            System.out.printf("Customer-%d [Teller-%d]: Prompt received.%n", customerId, chosenTeller.getTellerId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Provide transaction details to the teller.
    private void provideOperation() {
        System.out.printf("Customer-%d [Teller-%d]: Sending operation info: %s for $%.2f.%n",
                customerId, chosenTeller.getTellerId(), desiredOperation.getType(), desiredOperation.getAmount());
        synchronized (chosenTeller.getCustomerSyncCondition()) {
            chosenTeller.setPendingOperation(desiredOperation);
            chosenTeller.getCustomerSyncCondition().signal();
        }
    }
    
    // Wait until the teller signals that the operation has been completed.
    private void awaitCompletion() {
        chosenTeller.getOperationComplete().waitUntilSet();
        System.out.printf("Customer-%d [Teller-%d]: Notified that transaction is complete.%n",
                customerId, chosenTeller.getTellerId());
    }
    
    private void leaveBank() {
        entrySemaphore.release();
    }
}
