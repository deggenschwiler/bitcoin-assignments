import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {
    UTXOPool uPool, tPool;
    public TxHandler(UTXOPool utxoPool) {
        uPool = new UTXOPool(utxoPool);
        tPool = new UTXOPool();
    }

    /**
     * @return true if:
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        int numin = tx.numInputs();
        int numout = tx.numOutputs();
        byte[] message;
        byte[] sig;
        PublicKey pubkey;
        //for each input,
        for (Transaction.Input i : tx.getInputs()) {
            UTXO outputToCheck = new UTXO(i.prevTxHash, i.outputIndex);

            //check the outputs are in the utxoPool
            if (!uPool.contains(outputToCheck)) {
                return false;
            } else {
                if (tPool.contains(outputToCheck)) {
                    return false;
                } else {
                    tPool.addUTXO(outputToCheck, uPool.getTxOutput(outputToCheck));
                }
            }

            // verify sig
            if (i.outputIndex < numin) {
                message = tx.getRawTx();
                sig = tx.getInput(i.outputIndex).signature;
                pubkey = uPool.getTxOutput(outputToCheck).address;
                if (!Crypto.verifySignature(pubkey, message, sig)) return false;
            }

        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>;
        for (Transaction txiq : possibleTxs){
            if(isValidTx(txiq)){
                validTxs.add(txiq);
            }
        }
        Transaction [] validTxar = new Transaction [validTxs.size()];
        validTxar = validTxs.toArray(validTxar);
        return validTxar;
    }

}