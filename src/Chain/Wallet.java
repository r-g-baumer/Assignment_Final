package Chain;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;
    public String name;

    public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>(); //only UTXOs owned by this wallet.

    public Wallet(String name){
        this.name= name;
        generateKeyPair();
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(ecSpec, random);   //256 bytes provides an acceptable security level
            KeyPair keyPair = keyGen.generateKeyPair();
            // Set the public and private keys from the keyPair
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    //returns balance and stores the UTXO's owned by this wallet in this.UTXOs
    public float getBalance() {
        float total = 0;
        for (Map.Entry<String, TransactionOutput> item: NoobChain.UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            if(UTXO.isMine(publicKey)) { //if output belongs to me ( if coins belong to me )
                UTXOs.put(UTXO.id,UTXO); //add it to our list of unspent transactions.
                total += UTXO.value ;
            }
        }
        return total;
    }

    //Generates and returns a new transaction from this wallet.
    public Transaction sendFunds(PublicKey _recipient,float value ) {
        if(getBalance() < value) { //gather balance and check funds.
            System.out.println("#Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }
        //create array list of inputs
        ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();

        //We basically sum up as many values of Transaction outputs until we have a value equal to the valuie of transaction (or more)
        float total = 0;
        for (Map.Entry<String, TransactionOutput> item: UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            //It's okay if the sum of inputs is higher than the value
            //We stop adding the inputs to the total the moment the for loop's condition is met
            if(total > value) break;
        }

        Transaction newTransaction = new Transaction(publicKey, _recipient , value, inputs);
        newTransaction.generateSignature(privateKey);

        //We remove the TransactionOutputs corresponding to the transaction inputs we just used for the transaction
        for(TransactionInput input: inputs){
            //UXTOs is the unspent outputs and input is the iterable instance of the for loop that corresponds
            // to the value of an inputs' element value (reminder: inputs is the ArrayList of Transaction inputs)
            //  Here what we do is remove from the Outputs of the user's entire set of transactions the outputs
            //   that were used as inputs for this new transactions
            UTXOs.remove(input.transactionOutputId);
        }
        return newTransaction;
    }

}
