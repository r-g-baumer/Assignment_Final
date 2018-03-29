package Chain;

import com.google.gson.GsonBuilder;
import java.security.Security;
import java.util.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;

public class NoobChain {

    private static int difficulty = 3;

    public static float minimumTransaction = 0.1f;

    private static ArrayList<Wallet> wallets = new ArrayList<>();
    private static Wallet walletA;
    private static Wallet walletB;
    public static Wallet coinbase;
    private static Transaction genesisTransaction;

    private static ArrayList<Block> blockchain = new ArrayList<>();

    //list of all unspent transactions.
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<>();


    private static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //compare registered hash and calculated hash:
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

            //loop thru blockchains transactions:
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).recipient != currentTransaction.recipient) {
                    System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                    return false;
                }
                if( currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                    return false;
                }

            }

        }
        System.out.println("Blockchain is valid");
        return true;
    }

    private static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }

    public static void Funds(Block block, Float amount, Wallet from, Wallet to){
        System.out.println("\n" +from.name+ " is attempting to send "+ Float.toString (amount) + " to " +  to.name +"...");
        block.addTransaction(from.sendFunds(to.publicKey, amount));
        addBlock(block);
        System.out.println("\n" + from.name + "'s balance is: " + from.getBalance());
        System.out.println("\n" + to.name + "'s balance is: " + to.getBalance());
    }


    public static float readAmount(java.util.Scanner input, String promptMessage){
        System.out.println(promptMessage);
        while ((!input.hasNextFloat())){
            input.next();
            System.out.println("Please answer with an amount expressed in numbers");
            System.out.println(promptMessage);
        }
        float type = input.nextFloat();
        return type;
    }

    public static String readYN(java.util.Scanner input,String promptMessage){
        System.out.println(promptMessage);
        while ((!input.hasNext())){
            input.next();
            System.out.println("Please answer with Y or N");
            System.out.println(promptMessage);
        }
        String type = input.next();
        return type;
    }

    public static Wallet readWallet(java.util.Scanner input,String promptMessage){
        Wallet walllet = null;
        System.out.println(promptMessage);
        String nameWallet = input.nextLine();

        if (nameWallet.equals(walletA.name)) {
            walllet = walletA;
        }else{
            if (nameWallet.equals(coinbase.name)){
                walllet = coinbase;
            }else{
                if (nameWallet.equals(walletB.name)){
                    walllet = walletB;
                }
            }
        }

        return walllet;

    }

    public static void checkMoney(Wallet wallet){
        System.out.println("The balance of " + wallet.name + " is: " + wallet.getBalance());
    }

    public static void main(String[] args){

        java.util.Scanner YN = new java.util.Scanner(System.in).useLocale(Locale.US);
        java.util.Scanner YN1 = new java.util.Scanner(System.in).useLocale(Locale.US);
        java.util.Scanner whatWallet = new java.util.Scanner(System.in).useLocale(Locale.US);
        java.util.Scanner whatWalletS = new java.util.Scanner(System.in).useLocale(Locale.US);
        java.util.Scanner whatWalletR = new java.util.Scanner(System.in).useLocale(Locale.US);
        java.util.Scanner whatAmount = new java.util.Scanner(System.in).useLocale(Locale.US);

        //Setup Bouncey castle as a Security Provider
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        //Create the new wallets
        walletA = new Wallet("Hamza's wallet");
        walletB = new Wallet("Slava's wallet");
        coinbase = new Wallet("The bank");

        wallets.add(coinbase);
        wallets.add(walletA);
        wallets.add(walletB);

        String presentation = "The wallets available are: ";
        for (int i=0; i<wallets.size();i++){
            presentation += ", " + wallets.get(i).name;
        }
        System.out.println(presentation);

        //create genesis transaction, which sends 100 NoobCoin to coinbase:
        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 1000f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction
        genesisTransaction.transactionId = "0"; //manually set the transaction id
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId)); //manually add the Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //its important to store our first transaction in the UTXOs list.

        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);

        String replyYN = readYN(YN, "Would you like to check the money inside a wallet?");

        if (replyYN.equals("Y")){
            Wallet replyWallet = readWallet(whatWallet, "Which wallet?");
            checkMoney(replyWallet);
        }else {
            if(replyYN.equals("N")){
                System.out.println("Okay.");
            }else {
                while((!replyYN.equals("Y")||(!replyYN.equals("N")))){
                    readYN(YN, "Would you like to check the money inside a wallet?");
                }
            }
        }

        String replyYN1 = readYN(YN1, "Would you like to send funds from a wallet to a wallet?");

        if (replyYN1.equals("Y")){
            if(blockchain.size()==1){
                Block transaction = new Block(genesis.hash);
                Wallet replyWalletSend = readWallet(whatWalletS, "From which wallet?");
                Wallet replyWalletReceive = readWallet(whatWalletR, "To which wallet?");
                Float replyAmount = readAmount(whatAmount, "How much?");
                Funds(transaction, replyAmount, replyWalletSend, replyWalletReceive);
            }else{
                Block transaction = new Block(blockchain.get(blockchain.size()).hash);
                Wallet replyWalletSend = readWallet(whatWalletS, "From which wallet?");
                Wallet replyWalletReceive = readWallet(whatWalletR, "To which wallet?");
                Float replyAmount = readAmount(whatAmount, "How much?");
                Funds(transaction, replyAmount, replyWalletSend, replyWalletReceive);
            }

        }else {
            if(replyYN1.equals("N")){
                System.out.println("Okay.");
            }else {
                while((!replyYN1.equals("Y")||(!replyYN.equals("N")))){
                    readYN(YN1, "Would you like to send funds from a wallet to a wallet?");
                }
            }
        }

        isChainValid();



        /*
        Block block1 = new Block(genesis.hash);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("\nWalletA is Attempting to send funds (40) to WalletB...");
        block1.addTransaction(walletA.sendFunds(walletB.publicKey, 40f));
        addBlock(block1);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block2 = new Block(block1.hash);
        System.out.println("\nWalletA Attempting to send more funds (1000) than it has...");
        block2.addTransaction(walletA.sendFunds(walletB.publicKey, 1000f));
        addBlock(block2);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block3 = new Block(block2.hash);
        System.out.println("\nWalletB is Attempting to send funds (20) to WalletA...");
        block3.addTransaction(walletB.sendFunds( walletA.publicKey, 20));
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());
        */



    }
}