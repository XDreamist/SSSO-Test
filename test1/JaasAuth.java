import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class JaasAuth extends BaseAuth {

    public JaasAuth(String jaasConfFile) {
        super(jaasConfFile);

        this.authName = "JaasAuth";
        this.authType = "KerberosAuth";
    }

    public JaasAuth(String jaasConfFile, String authType) {
        super(jaasConfFile, authType);

        this.authName = "JaasAuth";
    }

    protected void setSystemProperties() {
        System.setProperty("java.security.auth.login.config", confFile);
        if (debug) System.setProperty("java.security.debug", "logincontext");
    }

    private static class ConfigurationCallbackHandler implements javax.security.auth.callback.CallbackHandler {
        @Override
        public void handle(javax.security.auth.callback.Callback[] callbacks) {
            // Add username/password handling if required by your JAAS config
            // For pure Kerberos, this might not be needed if using ticket cache
        }
    }

    @Override
    public AuthResponse authenticateUser() {
        try {
            super.authenticateUser();

            LoginContext loginContext = new LoginContext(authType, new ConfigurationCallbackHandler());
            loginContext.login();

            Subject subject = loginContext.getSubject();
            String username = subject.getPrincipals().stream()
                .findFirst()
                .map(principal -> principal.getName())
                .orElse("Unknown User");

            loginContext.logout();

            return new AuthResponse(true, null, username);
        }
        catch(LoginException e) {
            switch (authType) {
                case "KerberosAuth":
                    return new AuthResponse(
                        false, 
                        "(Kerberos error) " + e.getMessage()
                        );
            
                default:
                    return new AuthResponse(false, e.getMessage());
            }
        }
    }
}
