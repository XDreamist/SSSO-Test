public class AuthResponse {
    private boolean isAuthenticated;
    private String username = "Invalid User";
    private String response;

    // Constructors
    public AuthResponse(boolean isAuthenticated, String response, String username) {
        this.isAuthenticated = isAuthenticated;
        this.response = response;
        this.username = username;
    }

    public AuthResponse(boolean isAuthenticated, String response) {
        this.isAuthenticated = isAuthenticated;
        this.response = response;
    }

    // Native Methods
    @Override
    public String toString() {
        if (isAuthenticated) return "Authentication succesful: " + response;
        else return "Authentication failed: " + response;
    }

    // Class Specifics
    public boolean isSuccess() { 
        return isAuthenticated; 
    }
    
    public String getUsername() throws Exception {
        if (isAuthenticated) {
            return username;
        } else {
            throw new Exception("Tried to call the username from an unsuccessful authentication!");
        }
    }
}