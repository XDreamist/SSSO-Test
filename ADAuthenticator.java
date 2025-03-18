import javax.naming.*;
import javax.naming.directory.*;
import java.util.Hashtable;

public class ADAuthenticator {
    private String ldapUrl;
    private String domain;
    private String searchBase;

    public ADAuthenticator(String ldapUrl, String domain, String searchBase) {
        this.ldapUrl = ldapUrl;        // e.g., "ldap://your.ad.server:389"
        this.domain = domain;          // e.g., "yourdomain.com"
        this.searchBase = searchBase;  // e.g., "DC=yourdomain,DC=com"
    }

    public boolean authenticate(String username, String password) {
        // Set up the environment for creating the initial context
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        
        // Specify the security authentication mechanism
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        
        // Full domain username (e.g., user@domain.com or DOMAIN\\username)
        String principal = username + "@" + domain;
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);

        DirContext ctx = null;
        try {
            // Create the initial context - if this succeeds, authentication worked
            ctx = new InitialDirContext(env);
            
            // Optionally, you can search for user details
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            
            String[] attributes = {"cn", "mail", "displayName"};
            searchControls.setReturningAttributes(attributes);
            
            String filter = "(sAMAccountName=" + username + ")";
            NamingEnumeration<SearchResult> results = 
                ctx.search(searchBase, filter, searchControls);
            
            if (results.hasMore()) {
                SearchResult result = results.next();
                Attributes attrs = result.getAttributes();
                System.out.println("User found: " + attrs.get("cn"));
                return true;
            }
            return true;

        } catch (AuthenticationException e) {
            System.out.println("Authentication failed: " + e.getMessage());
            return false;
        } catch (NamingException e) {
            System.out.println("LDAP error: " + e.getMessage());
            return false;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    System.out.println("Error closing context: " + e.getMessage());
                }
            }
        }
    }

    // Example usage
    public static void main(String[] args) {
        // Configuration - replace with your AD settings
        String ldapUrl = "ldap://your.ad.server:389";
        String domain = "yourdomain.com";
        String searchBase = "DC=yourdomain,DC=com";

        ADAuthenticator authenticator = 
            new ADAuthenticator(ldapUrl, domain, searchBase);
        
        // Test authentication
        String username = "testuser";
        String password = "testpassword";
        
        boolean isAuthenticated = authenticator.authenticate(username, password);
        System.out.println("Authentication " + 
            (isAuthenticated ? "successful" : "failed"));
    }
}