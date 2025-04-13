// Event.java
class Event {
    private boolean flag = false; // Indicates whether the event is set

    public synchronized boolean isSet() {
        return flag;
    }

    public synchronized void set() {
        flag = true;
        notifyAll(); // Notify any waiting threads
    }

    public synchronized void waitUntilSet() {
        while (!flag) {
            try {
                wait(); // Wait until the event is set
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
