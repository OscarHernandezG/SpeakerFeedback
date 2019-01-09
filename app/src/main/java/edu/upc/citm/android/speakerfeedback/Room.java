package edu.upc.citm.android.speakerfeedback;

public class Room
{
    private String name;
    private String id;
    private boolean open;
    private String password;

    public Room() {
        name = "default";
        id = "default";
        open = false;
        password = "";
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

