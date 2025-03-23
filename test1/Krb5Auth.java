import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class Krb5Auth extends BaseAuth {
    private static final String LDAP_URL = "ldap://17.20.50.55:389";
    private static final String BASE_DN = "DC=PFA,DC=ORDO,DC=IN";

    public Krb5Auth(String krb5ConfFile) {
        super(krb5ConfFile);

        this.authName = "Krb5Auth";
        this.authType = "GSSAPI";
    }

    public Krb5Auth(String krb5ConfFile, String authType) {
        super(krb5ConfFile, authType);

        this.authName = "Krb5Auth";
    }

    protected void setSystemProperties() {
        System.setProperty("java.security.krb5.conf", confFile);
        if (debug) System.setProperty("sun.security.krb5.debug", "true");

        //System.setProperty("java.security.krb5.realm", "PFA.ORDO.IN");
        //System.setProperty("java.security.krb5.kdc", "17.20.50.55");
    }

    private String getCurrentUserPrincipal() {
        String username = System.getProperty("user.name", "unknown");
        return username.contains("@") ? username : username + "@PFA.ORDO.IN";
    }

    @Override
    public AuthResponse authenticateUser() {
        super.authenticateUser();

        // Step 1: Use the authenticated subject to connect to LDAP
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        env.put(Context.SECURITY_AUTHENTICATION, authType);
        
        // Add Kerberos-specific properties
        env.put(Context.SECURITY_PRINCIPAL, getCurrentUserPrincipal());
        env.put("javax.security.sasl.qop", "auth-conf");
        env.put("javax.security.sasl.server.authentication", "true");
        env.put("javax.security.sasl.strength", "high");

        // Step 2: creating the context
        try {
            DirContext ctx = null;
            try {
                ctx = new InitialDirContext(env);

                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                String[] attributes = {"sAMAccountName", "userPrincipalName"};
                searchControls.setReturningAttributes(attributes);

                String userPrincipal = getCurrentUserPrincipal();
                String filter = "(&(objectClass=user)(userPrincipalName=" + userPrincipal + "))";

                NamingEnumeration<SearchResult> results = ctx.search(BASE_DN, filter, searchControls);
                
                if (results.hasMore()) {
                    SearchResult result = results.next();
                    Attributes attrs = result.getAttributes();
                    String username = (String) attrs.get("sAMAccountName").get();
                    return new AuthResponse(true, null, username);
                }
                return new AuthResponse(true, null, "Unknown User");
            } finally {
                if (ctx != null) {
                    ctx.close();
                }
            }
        } catch (NamingException e) {
            String errorMsg = e.getMessage();
            switch (authType.toLowerCase()) {
                case "gssapi":
                    return new AuthResponse(false, "(LDAP error) " + errorMsg);
                case "simple":
                    return new AuthResponse(false, "(Simple auth error) " + errorMsg);
                default:
                    return new AuthResponse(false, errorMsg);
            }
        }
    }
}

// The LDAP server is reachable at 17.20.50.55:389
// The Kerberos ticket is valid (kinit if needed)
// The system time is synchronized with the KDC
// The BASE_DN matches your AD structure