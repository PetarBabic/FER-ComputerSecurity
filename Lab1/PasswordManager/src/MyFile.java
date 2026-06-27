import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class MyFile {
    public boolean createFile(String fileName) {
        try {
            File passwords = new File(fileName);
            return passwords.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(String fileName, byte[] data) {
        File file = new File(fileName);

        try(FileOutputStream outputStream = new FileOutputStream(file)){
            outputStream.write(data);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public byte[] read(String fileName) {
        try {
            return Files.readAllBytes( Path.of(fileName));
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
