package edu.emory.cellbio.ijbat.pi;

/**
 *
 * @author Benjamin Nanes
 */
public class WekaClassifierFile {
    private String path;
    public WekaClassifierFile(String path) {
        this.path = path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getPath() {
        return path == null ? "" : path;
    }
}
