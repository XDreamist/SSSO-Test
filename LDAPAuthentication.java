import javax.naming.*;
import javax.naming.directory.*;
import java.util.Hashtable;

public class LDAPAuthentication {
    
    // Configuration parameters - modify these according to your AD setup
    private static final String LDAP_URL = "ldap://your.domain.controller:389";
    private static final String DOMAIN = "YOURDOMAIN";
    private static final String BASE_DN = "DC=yourdomain,DC=com";
    
    public static class AuthResult {
        private boolean isAuthenticated;
        private String username;
        
        public AuthResult(boolean isAuthenticated, String username) {
            this.isAuthenticated = isAuthenticated;
            this.username = username;
        }
        
        public boolean isAuthenticated() { return isAuthenticated; }
        public String getUsername() { return username; }
    }
    
    public static AuthResult authenticateUser(String username, String password) {
        // Prepare the environment for LDAP connection
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        
        // Specify security credentials
        String principal = username + "@" + DOMAIN;
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);
        
        DirContext ctx = null;
        try {
            // Attempt to create initial context - this validates credentials
            ctx = new InitialDirContext(env);
            
            // If successful, get additional user information
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            
            String[] attributes = {"sAMAccountName", "displayName"};
            searchControls.setReturningAttributes(attributes);
            
            String filter = "(&(objectClass=user)(sAMAccountName=" + username + "))";
            
            NamingEnumeration<SearchResult> results = 
                ctx.search(BASE_DN, filter, searchControls);
            
            if (results.hasMore()) {
                SearchResult result = results.next();
                Attributes attrs = result.getAttributes();
                String sAMAccountName = (String) attrs.get("sAMAccountName").get();
                return new AuthResult(true, sAMAccountName);
            }
            
            return new AuthResult(true, username);
            
        } catch (AuthenticationException e) {
            System.out.println("Authentication failed: " + e.getMessage());
            return new AuthResult(false, null);
        } catch (NamingException e) {
            System.out.println("LDAP error: " + e.getMessage());
            return new AuthResult(false, null);
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
    
    public static void main(String[] args) {
        System.out.println("Running!...");
        // Example usage
        String testUsername = "testuser";
        String testPassword = "testpassword";
        
        AuthResult result = authenticateUser(testUsername, testPassword);
        
        if (result.isAuthenticated()) {
            System.out.println("Authentication successful for user: " + 
                result.getUsername());
        } else {
            System.out.println("Authentication failed");
        }
    }
}