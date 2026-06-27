public class Main {
    public static void main(String[] args) {
        PasswordManager pwd = new PasswordManager("passwords");

        switch (args[0].toLowerCase()) {
            case "init":
                pwd.init(args[1]);
                break;
            case "put":
                pwd.put(args[1], args[2], args[3]);
                break;
            case "get":
                pwd.get(args[1], args[2]);
                break;
        }
    }
}