// Teller.java
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Teller extends Thread {
    private static int idCounter = 0;
    private static final Lock idLock = new ReentrantLock();

    private final Semaphore safeSemaphore; // Only 2 tellers allowed in safe at a time
    private final Event noMoreCustomersEvent;
    private final Lock tellerAvailableLock;
    private final Condition tellerAvailableCond;
    private final List<Teller> readyTellers;
    private final Condition customerReadyCond;
    private Transaction transaction = null;
    private final Event transactionComplete = new Event();
    private final int id;

    public Teller(Semaphore safeSemaphore, Event noMoreCustomersEvent, Lock tellerAvailableLock,
                  Condition tellerAvailableCond, List<Teller> readyTellers) {
        idLock.lock();
        try {
            this.id = idCounter++;
        } finally {
            idLock.unlock();
        }
        this.safeSemaphore = safeSemaphore;
        this.noMoreCustomersEvent = noMoreCustomersEvent;
        this.tellerAvailableLock = tellerAvailableLock;
        this.tellerAvailableCond = tellerAvailableCond;
        this.readyTellers = readyTellers;
        this.customerReadyCond = tellerAvailableLock.newCondition();
    }

    // Provides the condition used for customer-teller synchronization.
    public Condition getCustomerReadyCond() {
        return customerReadyCond;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Event getTransactionComplete() {
        return transactionComplete;
    }


    // Announces that this teller is ready.
    private void announceReady() {
        tellerAvailableLock.lock();
        try {
            readyTellers.add(this);
            System.out.printf("Teller %d [Idle]: ready to serve.%n", id);
            tellerAvailableCond.signalAll();
        } finally {
            tellerAvailableLock.unlock();
        }
    }



    // Waits for the customer to provide the transaction.
    private Transaction waitForTransaction() {
        tellerAvailableLock.lock();
        try {
            while (transaction == null) {
                customerReadyCond.await();
            }
            return transaction;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            tellerAvailableLock.unlock();
        }
    }

    // Simulate going to the manager for withdrawal permission.
    private void goToManager() {
        System.out.printf("Teller %d [Manager]: requesting permission for withdrawal.%n", id);
        System.out.printf("Teller %d [Manager]: waiting for permission...%n", id);
        try {
            int delay = new Random().nextInt(26) + 5; // 5 to 30ms delay
            Thread.sleep(delay);
            System.out.printf("Teller %d [Manager]: received permission after %d ms.%n", id, delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Tries to access the safe (only 2 tellers allowed inside at once).
    private void accessSafe() {
        System.out.printf("Teller %d [Safe]: attempting to access safe.%n", id);
        safeSemaphore.acquireUninterruptibly();
        System.out.printf("Teller %d [Safe]: entered safe.%n", id);
    }

    // Simulates the transaction processing inside the safe.
    private void performTransaction() {
        System.out.printf("Teller %d [Safe]: starting transaction processing.%n", id);
        try {
            int processingTime = new Random().nextInt(41) + 10; // 10 to 50ms
            Thread.sleep(processingTime);
            System.out.printf("Teller %d [Safe]: finished processing transaction after %d ms.%n", id, processingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("Teller %d [Safe]: leaving safe.%n", id);
        safeSemaphore.release();
    }

 
}