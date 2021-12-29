package gitlet;

public class Blob {
    private String fileName;
    private String contents;
    public Blob(String fileName, String contents){
        this.fileName = fileName;
        this.contents = contents;
    }
}
