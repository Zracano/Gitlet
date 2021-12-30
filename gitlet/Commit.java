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
    // array of Sha1 of Blob objects
    public ArrayList<String> trackedBlobs;
    public Commit(){
        time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm a"));
        message = "Initial Commit";
        parent = null;
        trackedBlobs = null;
    }

    public Commit(String message, Repository r) throws IOException {
        time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm a"));
        this.message = message;
        // Sets the parent Sha to be the Sha of the most recent commit in the head branch
        parent = readContentsAsString(join(r.Branches, readContentsAsString(join(r.Branches,"head.txt"))));
        // adds all tracked blobs from parent commit to current commits tracked blobs
        trackedBlobs = new ArrayList<>(r.latestCommit().trackedBlobs);
        // Determines if we need to add and remove with this commit
        if(!(plainFilenamesIn(r.StagedAddition).size() == 0)){
            List<String> list = plainFilenamesIn(r.StagedAddition);
            Blob blob;
            File path;
            // adds blobs to tracked blobs
            for(String fileName: list){
                blob = new Blob(fileName, readContentsAsString(join(r.StagedAddition, fileName)));
                path = join(r.Blobs, sha1(blob));
                path.createNewFile();
                // Checks if our commit is already tracking this blob
                if(!trackedBlobs.contains(sha1(blob)))
                    trackedBlobs.add(fileName);
            }
        }
        // Removes Blob from tracked blobs
        if(!(plainFilenamesIn(r.StagedRemoval).size() == 0)){
            List<String> list = plainFilenamesIn(r.StagedRemoval);
            Blob blob;
            for(String fileName: list){
                blob = new Blob(fileName, readContentsAsString(join(r.StagedRemoval, fileName)));
                if(trackedBlobs.contains(sha1(blob)))
                    trackedBlobs.remove(sha1(blob));
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
