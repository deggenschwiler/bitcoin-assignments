import java.util.ArrayList;

public class TxHandler {
    UTXOPool uPool;
    public TxHandler(UTXOPool utxoPool) {
        UTXOPool uPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        //get the inputs
        ArrayList<Transaction.Input> allinputs = tx.getInputs();

        //for each input, check the outout is in the utxo
        for (Transaction.Input i: allinputs) {
            UTXO outputToCheck = new UTXO(i.prevTxHash, i.outputIndex);
            uPool.contains(outputToCheck);
        }



    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    }

}
