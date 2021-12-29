package gitlet;

import java.io.File;
import java.io.IOException;
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
            if(isInside(fileName, StagedAddition)) {
                // make a blob to check blob from recent commit

                // If the current working version of the file is identical
                // to the version in the current commit,
                // do not stage it to be added, and remove it from the
                // staging area if it is already there
            }
            // obtains the blob of the file
            Blob blob = new Blob(fileName,readContentsAsString(file));
            // makes a clone of the file
            File copy = file;
            // writes the clone of the file to the Staged for addition directory
            File path = join(StagedAddition, fileName);
            path.createNewFile();
            writeObject(path,copy);
        }else{
            System.out.println("File does not exist.");
        }
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
            // Does this overwrite what is in the Branch file or simple add to it?
            writeObject(new File(readContentsAsString(join(Branches,"head.txt"))),commit.hash());
        }
    }
    public void rm(String fileName){

    }
    //Todo fix merge commits
    public void log(){
        // Gets the sha of our head
        String sha = readContentsAsString(join(Branches,"head.txt"));
        Commit current;
        // iterates through all commits in commit directory from head to initial
        do {
            current = readObject(join(Commits, sha), Commit.class);
            System.out.println("===\ncommit " + current.hash() + "\nDate: " + current.getTime()
                    + "\n" + current.getMessage() + "\n");
            sha = current.getParent();
        }while(sha != null);
    }
    public void globalLog(){
        List list = plainFilenamesIn(Commits);

    }
    public void find(String message){

    }
    public void status(){

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
}
