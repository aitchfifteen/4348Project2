// Transaction.java
// Represents an operation (Deposit or Withdraw) with a specific amount.
class Transaction {
    private final String txnType;   // "Deposit" or "Withdraw"
    private final double txnAmount; // The monetary amount for the transaction

    public Transaction(String txnType, double txnAmount) {
        this.txnType = txnType;
        this.txnAmount = txnAmount;
    }

    public String getType() {
        return txnType;
    }

    public double getAmount() {
        return txnAmount;
    }
}
