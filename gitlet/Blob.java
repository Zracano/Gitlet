package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {
    private String fileName;
    private String contents;
    public Blob(String fileName, String contents){
        this.fileName = fileName;
        this.contents = contents;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContents() {
        return contents;
    }
}
