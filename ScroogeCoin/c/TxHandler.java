import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {
    private UTXOPool uPool;
    public TxHandler(UTXOPool utxoPool) {
        this.uPool = new UTXOPool(utxoPool);
    }

    public boolean isValidTx(Transaction tx) {
        double intotal = 0.0d;
        double outotal = 0.0d;
        UTXOPool tPool = new UTXOPool();
        //for each input
        for (int x = 0; x < tx.numInputs(); x++){
            Transaction.Input i = tx.getInput(x);
            UTXO utxoToCheck = new UTXO(i.prevTxHash, i.outputIndex);
            Transaction.Output o = uPool.getTxOutput(utxoToCheck);
            //check the outputs are in the uPool
            if (!uPool.contains(utxoToCheck)) {
                return false;
            } else {
                if (tPool.contains(utxoToCheck)) {
                    return false;
                } else {
                    tPool.addUTXO(utxoToCheck, uPool.getTxOutput(utxoToCheck));
                }
            }
            // all values are positive
            if (o.value < 0.0d){
                return false;
            }
            else {
                // add in value to total
                intotal += o.value;
            }
            // verify sig
            if (!Crypto.verifySignature(o.address, tx.getRawDataToSign(x), i.signature)) return false;
        }
        // for each output
        for (Transaction.Output o : tx.getOutputs()){
            if(o.value < 0.0d){
                return false;
            }
            else {
                outotal += o.value;
            }
        }
        // more in than out or same?
        return intotal >= outotal;
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<>();
        for (Transaction txiq : possibleTxs){
            if(isValidTx(txiq)){
                validTxs.add(txiq);
                // remove the valid utxo from the pool
                for (Transaction.Input i : txiq.getInputs()) {
                    UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);
                    uPool.removeUTXO(utxo);
                }
                // add a new utxo with the new hash, so another tx can use it
                for (int o = 0; o < txiq.numOutputs(); o++) {
                    Transaction.Output out = txiq.getOutput(o);
                    UTXO utxo = new UTXO(txiq.getHash(), o);
                    uPool.addUTXO(utxo, out);
                }

            }
        }
        Transaction [] validTxar = new Transaction [validTxs.size()];
        validTxar = validTxs.toArray(validTxar);
        return validTxar;
    }

}