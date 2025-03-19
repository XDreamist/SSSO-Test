import javax.security.auth.login.*;
import javax.security.auth.Subject;
import com.sun.security.auth.module.Krb5LoginModule;
import java.util.Hashtable;
import javax.naming.*;
import javax.naming.directory.*;

public class ClientSideADAuth {

    // Configuration - adjust these to your AD setup
    private static final String LDAP_URL = "ldap://your.domain.controller:389";
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

    public static AuthResult authenticateUser() {
        try {
            // Step 1: Use JAAS to authenticate with the current user's Kerberos ticket
            LoginContext loginContext = new LoginContext("KerberosAuth", 
                new CallbackHandler() {
                    public void handle(javax.security.auth.callback.Callback[] callbacks) {
                        // No callbacks needed since we're using the system ticket
                    }
                });

            // Perform login using the system's Kerberos credentials
            loginContext.login();
            Subject subject = loginContext.getSubject();

            // Step 2: Use the authenticated subject to connect to LDAP
            return Subject.doAs(subject, new java.security.PrivilegedAction<AuthResult>() {
                @Override
                public AuthResult run() {
                    Hashtable<String, String> env = new Hashtable<>();
                    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                    env.put(Context.PROVIDER_URL, LDAP_URL);
                    env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI"); // Use Kerberos

                    DirContext ctx = null;
                    try {
                        // Create LDAP context with Kerberos authentication
                        ctx = new InitialDirContext(env);

                        // Retrieve the current user's details
                        SearchControls searchControls = new SearchControls();
                        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                        String[] attributes = {"sAMAccountName"};
                        searchControls.setReturningAttributes(attributes);

                        // Use the authenticated user's principal to find their sAMAccountName
                        NamingEnumeration<SearchResult> results = 
                            ctx.search(BASE_DN, "(objectClass=user)", searchControls);

                        if (results.hasMore()) {
                            SearchResult result = results.next();
                            Attributes attrs = result.getAttributes();
                            String username = (String) attrs.get("sAMAccountName").get();
                            return new AuthResult(true, username);
                        }
                        return new AuthResult(true, "Unknown User");

                    } catch (NamingException e) {
                        System.out.println("LDAP error: " + e.getMessage());
                        return new AuthResult(false, null);
                    } finally {
                        if (ctx != null) {
                            try {
                                ctx.close();
                            [trails off here]
