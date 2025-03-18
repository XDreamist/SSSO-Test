import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

public class ApacheADAuthenticator {
    private String hostname;
    private int port;
    private String domain;
    private String searchBase;

    public ApacheADAuthenticator(String hostname, int port, String domain, String searchBase) {
        this.hostname = hostname;      // e.g., "your.ad.server"
        this.port = port;              // e.g., 389
        this.domain = domain;          // e.g., "yourdomain.com"
        this.searchBase = searchBase;  // e.g., "DC=yourdomain,DC=com"
    }

    public boolean authenticate(String username, String password) {
        LdapConnection connection = null;
        try {
            // Create connection
            connection = new LdapNetworkConnection(hostname, port);
            
            // Bind with user credentials
            String bindDn = username + "@" + domain;
            connection.bind(bindDn, password);

            // If bind succeeds, authentication worked
            System.out.println("Authentication successful");
            return true;

            // Optionally, search for user details
            // Dn searchDn = new Dn(searchBase);
            // Entry entry = connection.lookup(searchDn, "cn", "mail");
            // System.out.println("User CN: " + entry.get("cn"));

        } catch (Exception e) {
            System.out.println("Authentication failed: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    System.out.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        ApacheADAuthenticator authenticator = 
            new ApacheADAuthenticator("your.ad.server", 389, "yourdomain.com", "DC=yourdomain,DC=com");
        
        String username = "testuser";
        String password = "testpassword";
        
        boolean isAuthenticated = authenticator.authenticate(username, password);
        System.out.println("Authentication " + 
            (isAuthenticated ? "successful" : "failed"));
    }
}