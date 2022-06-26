
package fxstorage;

public class ImageDescription {
    
    private String description;
    private final String id;

    public ImageDescription(String description, String id) {
        this.description = description;
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getID() {
        return id;
    }

}
