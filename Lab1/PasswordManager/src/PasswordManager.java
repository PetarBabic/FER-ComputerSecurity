import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HashMap;

public class PasswordManager {
    private final String fileLocation;
    private final int SALT_SIZE = 16;
    private byte[] salt = new byte[SALT_SIZE];
    private final int IV_SIZE = 12;
    private byte[] iv = new byte[IV_SIZE];
    private final MyFile file = new MyFile();

    public PasswordManager(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    // Function that either encrypts or decrypts the provided data and returns it as a byte array
    private byte[] AES(String password, byte[] data, boolean encrypt) {
        try {
            // Initialization of Cipher object using:
            //      AES cipher
            //      GCM mode of operation
            //      No padding scheme
            // Using this because it provides confidentiality and integrity features in one package
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            // Specific set of parameters required for GCM
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);

            if(encrypt)
                cipher.init(Cipher.ENCRYPT_MODE, generateSecretKey(password), gcmParameterSpec);
            else
                cipher.init(Cipher.DECRYPT_MODE, generateSecretKey(password), gcmParameterSpec);

            return cipher.doFinal(data);
        }
        // Catching AEADBadTagException in case of a wrong password or integrity check failed
        catch (AEADBadTagException e) {
            System.out.println("Master password incorrect or integrity check failed.");
            System.exit(1);
            throw new RuntimeException(e);
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    // Function that derives a secret key from the provided password using Hashed Message Authentication Code based on SHA256
    private SecretKey generateSecretKey(String password) {
        try {
            // Initialization of SecretKeyFactory object
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            // Initialization of KeySpec object using the: provided password, salt, iterationCount and the needed keyLength
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 100000, 128);
            // Computes the derived  key from the previously provided password and salt and returns raw bytes (.getEncoded())
            byte[] secret = secretKeyFactory.generateSecret(keySpec).getEncoded();
            // Converts the raw bytes into an AES key, so it can be used for encryption/decryption
            return new SecretKeySpec(secret, "AES");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Function that firstly encrypts the provided data, and then merges IV, salt and cipher so that it can be stored to the file
    private byte[] encrypt(String password, byte[] data) {
        try {
            initializeSalt();
            initializeIV();

            byte[] cipher = AES(password, data, true);
            // ByteBuffer is used because it's faster than copying arrays around
            ByteBuffer buffer = ByteBuffer.wrap(new byte[IV_SIZE + SALT_SIZE + cipher.length]);

            // Combining all the needed data
            buffer.put(iv);
            buffer.put(salt);
            buffer.put(cipher);

            return buffer.array();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Function that firstly splits the given data into IV, salt and a cipher, and then returns the decrypted cipher in a byte array
    private byte[] decrypt(String password, byte[] data) {
        try {
            byte[] cipher = new byte[data.length - IV_SIZE - SALT_SIZE];
            // ByteBuffer is used because it's faster than copying arrays around
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            // Splitting the provided data
            byteBuffer.get(iv);
            byteBuffer.get(salt);
            byteBuffer.get(cipher);

            return AES(password, cipher, false);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Function that initializes salt using SecureRandom
    private void initializeSalt() {
        try {
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            secureRandom.nextBytes(salt);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Function that initializes IV using SecureRandom
    private void initializeIV() {
        try {
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            secureRandom.nextBytes(iv);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Initialization of the password manager that generates an empty database
    public void init(String masterPassword) {
        // Firstly initialize the salt and IV
        initializeSalt();
        initializeIV();

        // Checks whether the database exists and creates it if it doesn't
        if(file.createFile(fileLocation))
            System.out.println("Password manager initialized");
        else {
            System.out.println("Password manager already exists");
        }

        // Some "empty" data to be stored into the database
        byte[] emptyData = encrypt(masterPassword, convertToByteArray(new HashMap<String, String>()));

        // Finally writing the data into the file
        file.write(fileLocation, emptyData);
    }

    // Function that converts a HashMap<String, String> into a byte array so that it can be easily encrypted
    public byte[] convertToByteArray(HashMap<String, String> hashMap) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(hashMap);

            return byteOut.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Function that converts a byte array back into a HashMap<String, String> so that the data can be read once it is decrypted
    @SuppressWarnings("unchecked")
    public HashMap<String, String> convertToHashMap(byte[] byteArray) {
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteArray);
            ObjectInputStream in = new ObjectInputStream(byteIn);

            return (HashMap<String, String>) in.readObject();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Function that saves the web-site address and it's password to the database
    public void put(String masterPassword, String address, String password) {
        // Firstly it reads the encrypted data from the file            ( file.read(fileLocation) )
        // Then it decrypts the given data                              ( decrypt(masterPassword, file.read(fileLocation)) )
        // And finally it converts the given byte array into a HashMaps ( convertToHashMap(decrypt(masterPassword, file.read(fileLocation))) )
        HashMap<String, String> data = new HashMap<>(convertToHashMap(decrypt(masterPassword, file.read(fileLocation))));

        // Storing the data into the database
        data.put(address, password);
        System.out.println("Stored password for: " + address);

        // Firstly converting the HashMap into a byte array ( convertToByteArray(data) )
        // Then encrypting the database                     ( encrypt(masterPassword, convertToByteArray(data)) )
        // And finally saving the byte array to a file
        file.write(fileLocation, encrypt(masterPassword, convertToByteArray(data)));
    }

    // Function that gets the password for the given web-site address
    public void get(String masterPassword, String address) {
        // Firstly it reads the encrypted data from the file            ( file.read(fileLocation) )
        // Then it decrypts the given data                              ( decrypt(masterPassword, file.read(fileLocation)) )
        // And finally it converts the given byte array into a HashMaps ( convertToHashMap(decrypt(masterPassword, file.read(fileLocation))) )
        HashMap<String, String> data = new HashMap<>(convertToHashMap(decrypt(masterPassword, file.read(fileLocation))));

        // Checking if the entry for the given web-site exists
        String password = data.get(address);
        if(password != null)
            System.out.println("Password for " + address + " is: " + password);
        else
            System.out.println("Master password incorrect or integrity check failed.");
    }
}
