import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import java.util.HashSet;

public class MaxFeeTxHandler {
    private UTXOPool uPool;
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.uPool = new UTXOPool(utxoPool);
    }

    public boolean isValidTx(Transaction tx) {
        double intotal = 0.0d;
        double outotal = 0.0d;
        UTXOPool tPool = new UTXOPool();
        //for each input
        for (int x = 0; x < tx.numInputs(); x++){
            Transaction.Input i = tx.getInput(x);
            UTXO uToCheck = new UTXO(i.prevTxHash, i.outputIndex);
            Transaction.Output o = uPool.getTxOutput(uToCheck);
            //check the outputs are in the uPool
            if (!uPool.contains(uToCheck)) {
                return false;
            }
            // verify sig
            if (!Crypto.verifySignature(o.address, tx.getRawDataToSign(x), i.signature)) return false;
            if (tPool.contains(uToCheck)) {
                return false;
            }
            tPool.addUTXO(uToCheck, uPool.getTxOutput(uToCheck));
            // add in value to total
            intotal += o.value;
        }

        // for each output
        for (Transaction.Output o : tx.getOutputs()) {
            if (o.value < 0.0d) {
                return false;
            }
            outotal += o.value;
        }
        // more in than out or same?
        return intotal >= outotal;
    }

    public double feeValue(Transaction tx) {
        UTXOPool tPool = new UTXOPool();
        double intxtotal = 0.0d;
        double outtxtotal = 0.0d;
        double fee;
        for (Transaction.Input i : tx.getInputs()) {
            UTXO uToCheck = new UTXO(i.prevTxHash, i.outputIndex);
            if (!uPool.contains(uToCheck)) continue;
            // if there's a conflict, try both routes
            if (tPool.contains(uToCheck)) {
                //make a new set of transactions.
            }

            double y = uPool.getTxOutput(new UTXO(tx.getInput(0).prevTxHash, tx.getInput(0).outputIndex)).value;

            Transaction.Output o = uPool.getTxOutput(uToCheck);
            intxtotal += o.value;

            tPool.addUTXO(uToCheck, o);
        }
        for  (Transaction.Output o : tx.getOutputs()){
            outtxtotal += o.value;
        }
        fee = intxtotal - outtxtotal;
        return fee;
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Set<Transaction> validTxs = new HashSet<>();
        //order transactions by fee, highest first.
        Set<Transaction> txHighFeesFirst = new TreeSet<>((ta, tb) -> {
            double feea = feeValue(ta);
            double feeb = feeValue(tb);
            return Double.valueOf(feeb).compareTo(feea);
        });
        Collections.addAll(txHighFeesFirst, possibleTxs);
        for (Transaction txiq : txHighFeesFirst){
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