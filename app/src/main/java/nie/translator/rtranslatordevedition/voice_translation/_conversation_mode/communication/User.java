package nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication;

public class User {
    private String username;
    private double __createdtime__;
    private double __updatedtime__;
    private int active;
    private String firstname;
    private String lastname;
    private int online;
    private String personal_language;
    private boolean skip;
    private String socket_id;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double get__createdtime__() {
        return __createdtime__;
    }

    public void set__createdtime__(double __createdtime__) {
        this.__createdtime__ = __createdtime__;
    }

    public double get__updatedtime__() {
        return __updatedtime__;
    }

    public void set__updatedtime__(double __updatedtime__) {
        this.__updatedtime__ = __updatedtime__;
    }

    public int isActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public int isOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public String getPersonalLanguage() {
        return personal_language;
    }

    public void setPersonalLanguage(String personal_language) {
        this.personal_language = personal_language;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String getSocketId() {
        return socket_id;
    }

    public void setSocketId(String socket_id) {
        this.socket_id = socket_id;
    }
}