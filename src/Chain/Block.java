package Chain;

import java.util.Date;
import java.util.ArrayList;

/**
 * This will be the concrete object class for Blocks.
 */

public class Block {

    public String hash;
    public String previousHash;
    private String merkleRoot;
    public ArrayList<Transaction> transactions = new ArrayList<>(); //our data will be a simple message.
    private long timeStamp;
    private int nonce;

    Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash(); // Making sure we do this after we set the other values
    }

    public String calculateHash() {
        return StringUtil.applySha256(
                previousHash +
                        Long.toString(timeStamp) +
                        Integer.toString(nonce) +
                        merkleRoot
        );
    }

    public void mineBlock(int difficulty){
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = StringUtil.getDifficultyString(difficulty); //Create a string with difficulty * "0"
        while(!hash.substring(0, difficulty).equals(target)){
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + hash);

    }

    //Add transactions to this block
    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if(transaction == null) return false;
        if((!previousHash.equals("0"))) {
            if((!transaction.processTransaction())) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }
        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }

}
