package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static gitlet.Utils.*;

public class Commit implements Serializable{
    private String message;
    private String parent;
    private String time;
    private Repository r;
    private ArrayList<String> trackedBlobs;
    public Commit(){
        time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm a"));
        message = "Initial Commit";
        parent = null;
        trackedBlobs = null;
    }

    public Commit(String message, Repository r) throws IOException {
        this.r = r;
        time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm a"));
        this.message = message;
        parent = readContentsAsString(join(r.Branches, readContentsAsString(join(r.Branches,"head.txt"))));
        // adds all tracked blobs from parent commit to current commits tracked blobs
        trackedBlobs = new ArrayList<>(readObject(join(r.Commits, parent), Commit.class).trackedBlobs);
        // Determines if we need to add and remove with this commit
        if(!(plainFilenamesIn(r.StagedAddition).size() == 0)){
            List<String> list = plainFilenamesIn(r.StagedAddition);
            Blob blob;
            File path;
            for(String fileName: list){
                blob = new Blob(fileName, readContentsAsString(new File(fileName)));
                path = join(r.Blobs, sha1(blob));
                path.createNewFile();
                if(!trackedBlobs.contains(fileName))
                    trackedBlobs.add(fileName);
            }
        }
        if(!(plainFilenamesIn(r.StagedRemoval).size() == 0)){
            List<String> list = plainFilenamesIn(r.StagedRemoval);
            for(String fileName: list){
                if(trackedBlobs.contains(fileName))
                    trackedBlobs.remove(fileName);
            }
        }
    }

    public String hash(){
        return Utils.sha1(Utils.serialize(this));
    }

    public String getMessage() {
        return message;
    }
    public String getTime() {
        return time;
    }
    public String getParent() {
        return parent;
    }
}
// make a hashset that uses the commit sha-1 as key and value
// The commits should point to their parent sha-1
