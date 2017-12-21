// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.
import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    //This is the hashmap of the chain of blocks. A unique identifier mapped to a list of BlockLinks
    private HashMap<ByteArrayWrapper, BlockLink> blockchain;
    // this is the blocklink we're going to work on now;
    private BlockLink blockToWorkOn;
    // This is the list of transactions we're trying to shove into a block.
    private TransactionPool txPool;

    private class BlockLink {
        public Block block;
        public BlockLink previous;
        public ArrayList<BlockLink> next;
        public int blockheight;
        private UTXOPool utxPool;

        // this is an inner class of objects which store unique utxoPools, and their position within the blockchain for a potential new block
        public BlockLink(Block block, BlockLink previous, UTXOPool utxPool) {
            this.block = block;
            this.previous = previous;
            next = new ArrayList<>();
            this.utxPool = utxPool;
            if (previous == null) {
                // if previous BlockFork doesn't exist then it must be gensis block so set height to 1
                this.blockheight = 1;
            } else {
                //otherwise this is a contender for the block of height one larger than previous.
                this.blockheight = previous.blockheight + 1;
                // also add this to next ArrayList of previous block.
                previous.next.add(this);
            }
        }

        // make that private utxoPool accessible in read only sense
        public UTXOPool copyUtxPool() {
            return new UTXOPool(utxPool);
        }
    }

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // make a Hashmap blockchain object
        blockchain = new HashMap<>();
        // make a utxo pool object for the geniss block
        UTXOPool gPool = new UTXOPool();
        //make a new transaction pool
        txPool = new TransactionPool();
        // Make a blocklink to start linking the blocks together, intitiate with gensis block, null previous, and genesis utxo pool
        BlockLink gensisBlockLink = new BlockLink(genesisBlock, null, gPool);
        // make a hashpointer for the first block
        ByteArrayWrapper hashpointer_genblock = new ByteArrayWrapper(genesisBlock.getHash());
        // add it to the blockchain
        blockchain.put(hashpointer_genblock, gensisBlockLink);
        // adjust to height of most recent block
        blockToWorkOn = gensisBlockLink;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return blockToWorkOn.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return blockToWorkOn.copyUtxPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        //get previous block link from blockchain's max height blocklink's prev block hash pointer
        // as that needs to be in the new block.
        ByteArrayWrapper hashpointer_prev = new ByteArrayWrapper(block.getPrevBlockHash());
        BlockLink previousBlockLink = blockchain.get(hashpointer_prev);
        // if genesis block attempted - don't include it
        if (block.getPrevBlockHash() == null) return false;
        if (previousBlockLink == null) return false;
        // reject older than cut off age blocks to keep memory usage low.
        if (previousBlockLink.blockheight <= (blockToWorkOn.blockheight - CUT_OFF_AGE)) return false;
        // Construct a TXHandler object, with the UTXOPool from the previous block link (their test suite will provide)
        TxHandler handler = new TxHandler(previousBlockLink.copyUtxPool());
        // make new transaction objects from the transactions in the block provided to this function.
        // I have no idea why the IDE suggested .toArray, but I guess it wanted to give it an array?
        Transaction[] transactions = block.getTransactions().toArray(new Transaction[0]);
        //make sure they're all valid
        Transaction[] validTxs = handler.handleTxs(transactions);
        // add the valid ones to the block
        for (Transaction tx : validTxs){
            block.addTransaction(tx);
        }
        //grab utxo pool from previous block via txhandler
        UTXOPool newPool = handler.getUTXOPool();
        // add a new transaction to the block and utxo pool
        addCoinbaseTransaction(block, newPool);
        // new blocklink object with defined block as this one we're adding, the previous blocklink, and the new utxpool
        BlockLink newBlockLink = new BlockLink(block, previousBlockLink, newPool);
        // the function below needs a byte array wrapper, so.. Make one from the block's hash, since we need to point at it.
        ByteArrayWrapper hashpointer_new = new ByteArrayWrapper(block.getHash());
        blockchain.put(hashpointer_new, newBlockLink);
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    //Coinbase transaction
    public void addCoinbaseTransaction(Block block, UTXOPool uPool){
        Transaction cb = block.getCoinbase();
        int i = 0;
        for (Transaction.Output txo : cb.getOutputs()){
            UTXO utxo = new UTXO(cb.getHash(), i);
            uPool.addUTXO(utxo, txo);
            i++;
        }
    }

}