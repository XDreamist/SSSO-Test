public class BaseAuth {
    protected String authName;
    protected String authType;
    protected String confFile;
    public boolean debug = false;

    public BaseAuth(String confFile) {
        this.confFile = confFile;
    }

    public BaseAuth(String confFile, String authType) {
        this.confFile = confFile;
        this.authType = authType;
    }

    @Override
    public String toString() {
        return authName;
    }

    protected void setSystemProperties() {}

    public AuthResponse authenticateUser() {
        setSystemProperties();

        return new AuthResponse(
            false, 
            "This is just a base class. Don't use this!"
        );
    }
}
