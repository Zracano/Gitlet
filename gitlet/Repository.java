package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static gitlet.Utils.*;

public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File StagingArea = join(GITLET_DIR,"StagingArea");
    public static final File StagedAddition = join(StagingArea,"Staged for Addition");
    public static final File StagedRemoval = join(StagingArea, "Staged for Removal");
    public static final File Commits = join(GITLET_DIR, "Commits");
    public static final File Blobs = join(GITLET_DIR, "Blobs");
    public static final File Branches = join(GITLET_DIR, "Branches");


    public void init() throws IOException {
        if(GITLET_DIR.exists() == true){
            System.out.println("A Gitlet version-control system already exists in the current directory.");
        }else {
            // Makes Directories
            GITLET_DIR.mkdir();
            StagingArea.mkdir();
            StagedAddition.mkdir();
            StagedRemoval.mkdir();
            Commits.mkdir();
            Blobs.mkdir();
            Branches.mkdir();
            // Makes initial Commit
            Commit initialCommit = new Commit();
            File firstCommit = join(Commits, initialCommit.hash());
            firstCommit.createNewFile();
            writeObject(firstCommit, initialCommit);
            // Makes Branch 'Master'
            File Master = join(Branches, "master.txt");
            Master.createNewFile();
            writeObject(Master, initialCommit.hash());
            // Make Head pointer
            File Head = join(Branches, "head.txt");
            Head.createNewFile();
            writeObject(Head, "master");
        }
    }
    public void add(String fileName) throws IOException {
        File file = join(CWD, fileName);
        // Checks to see if the file exists
        if(file.exists()){
            // obtains the blob of the file
            Blob blob = new Blob(fileName,readContentsAsString(file));
            Commit currentCommit = latestCommit();
            // determines if
            // Unstages file if it is identical to current tracked blob
            if(currentCommit.trackedBlobs.contains(sha1(blob))){
                restrictedDelete(join(StagedAddition, fileName));
                restrictedDelete(join(StagedRemoval, fileName));
            }else {
                // makes a clone of the file
                File copy = file;
                // writes the clone of the file to the Staged for addition directory
                File path = join(StagedAddition, fileName);
                path.createNewFile();
                writeObject(path, copy);
            }
        }else
            System.out.println("File does not exist.");
    }
    public void commit(String message) throws IOException {
        // Checks for things to be Staged and a non-blank message
        if(plainFilenamesIn(StagedAddition).size() == 0 && plainFilenamesIn(StagedRemoval).size() == 0){
            System.out.println("No changes added to the commit.");
        } else if(message.isBlank()){
            System.out.println("Please enter a commit message.");
        }else{
            // Creates the commit
            Commit commit = new Commit(message, this);
            // Makes the Current Commit be our most recent commit SHA-1
            writeObject(new File(readContentsAsString(join(Branches,"head.txt"))),commit.hash());
            // Clears the Staging Area
            for(String file: plainFilenamesIn(StagedAddition)){
                restrictedDelete(join(StagedAddition, file));
            }
            for(String file: plainFilenamesIn(StagedRemoval)){
                restrictedDelete(join(StagedRemoval, file));
            }
        }
    }
    public void rm(String fileName) throws IOException {
        if(!isInside(fileName, StagedAddition) && !latestCommit().trackedBlobs.contains(fileName)){
            System.out.println("No reason to remove the file.");
            return;
        }
        if(isInside(fileName, StagedAddition)){
            restrictedDelete(join(StagedAddition, fileName));
        }
        if(latestCommit().trackedBlobs.contains(fileName)){
            // writes the file in CWD to a file in Staged removal (stages file for removal)
            File file = join(StagedRemoval,fileName);
            file.createNewFile();
            writeObject(file, join(CWD,fileName));
            // removes file from CWD if not already done
            if(join(CWD,fileName).exists())
                restrictedDelete(join(CWD, fileName));
        }

    }
    //Todo fix merge commits
    public void log(){
        // Gets the sha of our head
        String sha = readContentsAsString(join(Branches,"head.txt"));
        Commit current;
        // iterates through all commits in commit directory from head to initial by using parent Sha within each Commit
        do {
            current = readObject(join(Commits, sha), Commit.class);
            System.out.println("===\ncommit " + current.hash() + "\nDate: " + current.getTime()
                    + "\n" + current.getMessage() + "\n");
            sha = current.getParent();
        }while(sha != null);
    }
    //Todo fix merge commits
    public void globalLog(){
        List<String> list = plainFilenamesIn(Commits);
        File commitSha;
        Commit commit;
        // iterates through all commits in commit directory using their file name(Sha) to retrieve the Commit object
        for(String fileName: list){
            commitSha = join(Commits, fileName);
            commit = readObject(commitSha, Commit.class);
            System.out.println("===\ncommit " + commit.hash() + "\nDate: " + commit.getTime()
                    + "\n" + commit.getMessage() + "\n");
        }
    }
    public void find(String message){
        List<String> shas = plainFilenamesIn(Commits);
        Commit commit;
        for(String sha: shas){
            commit = readObject(join(Commits, sha), Commit.class);
            if(commit.getMessage().equals(message))
                System.out.println(sha);
        }
    }
    public void status(){
        String head = readContentsAsString(join(Branches, "head.txt"));
        List<String> branches = plainFilenamesIn(Branches);
        branches.remove("head.txt");
        List<String> stagedAddition = plainFilenamesIn(StagedAddition);
        List<String> stagedRemoval = plainFilenamesIn(StagedRemoval);
        System.out.println("=== Branches ===");
        for(String branchName: branches){
            if(branchName.equals(head)){
                System.out.println("*"+branchName);
            }else
                System.out.println(branchName);
        }
        System.out.println("\n=== Staged Files ===");
        for(String stagedFile: stagedAddition){
            System.out.println(stagedFile);
        }
        System.out.println("\n=== Removed Files ===");
        for(String removedFile: stagedRemoval){
            System.out.println(removedFile);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        // Todo Finish Modifications check
        System.out.println("\n=== Untracked Files");
        //Todo Finish Untracked files check
    }
    public void checkout(String fileName){

    }
    public void checkout(String commitId, String fileName){

    }
    public void checkout(String branchName, boolean hyphen){

    }
    public void branch(String branchName) throws IOException {
        if(!isInside(branchName,Branches)){
            File branch = join(Branches, branchName);
            branch.createNewFile();
            writeObject(branch, readContentsAsString(join(Branches,"head.txt")));
        }else
            System.out.println("A branch with that name already exists");

    }
    public void rmBranch(String branchName){
        if(!isInside(branchName,Branches)){
            if(readContentsAsString(join(Branches,"head.txt")) != branchName){
                if(!restrictedDelete(branchName))
                    System.out.println("Failed to delete branch");
            }else
                System.out.println("Cannot remove the current branch");
        }else
            System.out.println("A branch with that name does not exist.");
    }
    public void reset(String commitId){

    }
    public void merge(String branchName){

    }
    // Helper method to get the latest commit from head branch
    public Commit latestCommit(){
        String commitSha = readContentsAsString(join(Branches,readContentsAsString(join(Branches, "head.txt"))));
        return readObject(join(Commits, commitSha), Commit.class);
    }
}
