import java.util.Scanner;

public class ClientSideAuth {

    private static BaseAuth authentication;

    public static void main(String[] args) {
        String pwd = System.getProperty("user.dir");

        System.out.println("Which auth do you want to run: \n 1. Jaas\n 2. Krb5");
        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                authentication = new JaasAuth(pwd + "/jaas.conf");
                break;

            case "2":
            // We can make it "GSSAPI" or "simple". With simple, more additional auth might me be needed.
                authentication = new Krb5Auth(pwd + "/krb5.conf", "GSSAPI");
                authentication.debug = true;
                break;

            default:
                System.out.println("Invalid choice");
                break;
        }

        scanner.close();

        System.out.println(authentication.authenticateUser());
    }
}

// Ensure the system has a valid Kerberos ticket (run kinit if testing manually)
// Verify the LDAP server is reachable at 17.20.50.55:389
// Check that the DNS resolves the domain controller correctly
// Ensure the system time is synchronized with the AD server (Kerberos is time-sensitive)
// Match the realm name (EXAMPLE.COM) with your AD domain in uppercase