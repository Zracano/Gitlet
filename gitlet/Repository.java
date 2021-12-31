package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
            clearStagingArea();
        }
    }
    public void commit(String message, String parent2) throws IOException {
        // Creates the commit
        Commit commit = new Commit(message, this, parent2);
        // Makes the Current Commit be our most recent commit SHA-1
        writeObject(new File(readContentsAsString(join(Branches,"head.txt"))),commit.hash());
        // Clears the Staging Area
        clearStagingArea();
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
    public void log(){
        // Gets the sha of our head
        String sha = readContentsAsString(join(Branches,"head.txt"));
        Commit current;
        // iterates through all commits in commit directory from head to initial by using parent Sha within each Commit
        do {
            current = readObject(join(Commits, sha), Commit.class);
            if (current.getParent2() == null) {
                System.out.println("===\ncommit " + current.hash() + "\nDate: " + current.getTime()
                        + "\n" + current.getMessage() + "\n");
            }else{
                System.out.println("===\ncommit " + current.hash() + "\nMerge: "+ current.getParent().substring(0,7)+
                        " " + current.getParent2().substring(0,7) + "\nDate: " + current.getTime()
                        + "\n" + current.getMessage() + "\n");
            }
            sha = current.getParent();
        }while(sha != null);
    }
    public void globalLog(){
        List<String> list = plainFilenamesIn(Commits);
        File commitSha;
        Commit current;
        // iterates through all commits in current directory using their file name(Sha) to retrieve the Commit object
        for(String fileName: list){
            commitSha = join(Commits, fileName);
            current = readObject(commitSha, Commit.class);
            if(current.getParent2() == null) {
                System.out.println("===\ncurrent " + current.hash() + "\nDate: " + current.getTime()
                        + "\n" + current.getMessage() + "\n");
            }else{
                System.out.println("===\ncurrent " + current.hash() + "\nMerge: "+ current.getParent().substring(0,7)+
                        " " + current.getParent2().substring(0,7) + "\nDate: " + current.getTime()
                        + "\n" + current.getMessage() + "\n");
            }
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
        //Makes list of files modified and not staged
        List<String> workingFiles = plainFilenamesIn(CWD);
        List<String> unstagedModifications = new ArrayList<>();
        for(String file: workingFiles){
            for(String stagedFile: stagedAddition) {
                if (file.equals(stagedFile) &&
                        !readContentsAsString(join(CWD, file)).equals(readContentsAsString(join(StagedAddition, stagedFile)))){
                    unstagedModifications.add(file);
                }
            }
        }
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
        for(String modifiedFiles: unstagedModifications){
            System.out.println(modifiedFiles);
        }
        System.out.println("\n=== Untracked Files");
        for(String untrackedFiles : untrackedFileList()){
            System.out.println(untrackedFiles);
        }
    }
    public void checkout(String fileName){
        // makes path of file to be replaced
        File path = join(CWD, fileName);
        Blob blob = latestCommit().blobTrackingFile(fileName);
        if(blob != null){
            writeObject(join(CWD, fileName), blob.getContents());
        }else
            System.out.println("File does not exist in that commit.");
    }
    public void checkout(String commitId, String fileName){
        Commit commit = readObject(join(Commits, commitId), Commit.class);
        if(commit != null){
            Blob blob = commit.blobTrackingFile(fileName);
            if(blob.getFileName().equals(fileName)){
                writeObject(join(CWD, fileName), blob.getContents());
            }else
                System.out.println("File does not exist in that commit.");
        }else
            System.out.println("No commit with that id exists.");
    }
    public void checkout(String branchName, boolean hyphen){
        if(isInside(branchName, Branches)){
            if(readContentsAsString(join(Branches,"head.txt")).equals(branchName)){
                System.out.println("No need to checkout the current branch.");
            }else if(!untrackedFileList().isEmpty()){
                System.out.println("There is an untracked file in the way; delete itm or add and commit it first.");
            }else{
                // gets latest commit fo given branch
                Commit commit = readObject(join(Branches, branchName), Commit.class);
                // Makes list of blobs tracked by commit
                List<Blob> blobs = new ArrayList<>(commit.blobList());
                // replaces tracked files in CWD
                for(Blob blob: blobs){
                    writeObject(join(CWD,blob.getFileName()), blob.getContents());
                }
                // Clear Staging area
                clearStagingArea();
                // makes given branch the head
                writeObject(join(Branches, "head.txt"),branchName);
            }
        }
            System.out.println("No such branch exists.");
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
        if(isInside(commitId, Commits)){
            if(untrackedFileList().isEmpty()) {
                // Checks out all files tracked in commit
                Commit commit = readObject(join(Commits, commitId), Commit.class);
                List<Blob> blobs = new ArrayList<>(commit.blobList());
                for (Blob blob : blobs) {
                    checkout(commitId, blob.getFileName());
                }
                // moves current branch head to that commit
                writeObject(join(Branches, readContentsAsString(join(Branches, "head.txt"))), commit.hash());
                // clears staging area
                clearStagingArea();
            }else
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
        }else
            System.out.println("No commit with that id exists.");

    }
    public void merge(String branchName) throws IOException {
        final String headBranchName = readContentsAsString(join(Branches, "head.txt"));
        String givenBranchCommitSha = readContentsAsString(join(Branches, branchName));
        String headBranchCommitSha = readContentsAsString(join(Branches, headBranchName));
        String latestCommonAncestorSha = null;
        boolean conflict = false;
        final Commit branchCommit = readObject(join(Commits, givenBranchCommitSha), Commit.class);
        final Commit currentCommit = readObject(join(Commits, headBranchCommitSha), Commit.class);
        if(!untrackedFileList().isEmpty()){
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first");
        }else if(!isInside(branchName, Branches)) {
            System.out.println("A branch with that name does not exist.");

        }else if(!plainFilenamesIn(StagedAddition).isEmpty() || !plainFilenamesIn(StagedRemoval).isEmpty()) {
            System.out.println("You have uncommitted changes.");
        }else {
            // makes list of all commits starting from commit head
            Commit current;
            List<Commit> commits = new ArrayList<>();
            do{
                current = readObject(join(Commits, headBranchCommitSha), Commit.class);
                commits.add(current);
                headBranchCommitSha = current.getParent();
            }while(headBranchCommitSha != null);
            // Checks all branch commit ancestors to see where the head and given branches split
            Commit branchCurrent = readObject(join(Commits,givenBranchCommitSha), Commit.class);
            String branchParentSha;
            while(!commits.contains(branchCurrent.hash())){
                branchParentSha = branchCommit.getParent();
                branchCurrent = readObject(join(Commits, branchParentSha), Commit.class);
            }
            final Commit latestCommonAncestorCommit = branchCommit;
            // Checks if the branch is fast-forwarded or if given branch is ancestor of current
            if(latestCommonAncestorCommit.hash().equals(currentCommit.hash())) {
                checkout(branchName);
                System.out.println("Current branch fast-forwarded.");
            }else if(commits.contains(branchCommit.hash()))
                System.out.println("Given branch is an ancestor of the current branch.");
            // get list of blobs tracked by each commit
            List<Blob> splitBlobs = latestCommonAncestorCommit.blobList();
            List<Blob> currentBlobs = currentCommit.blobList();
            List<Blob> branchBlobs = branchCommit.blobList();
            List<String> splitFiles = new ArrayList<>(latestCommonAncestorCommit.trackedFiles());
            List<String> currentFiles = new ArrayList<>(currentCommit.trackedFiles());
            List<String> branchFiles = new ArrayList<>(branchCommit.trackedFiles());
            // make lists of added files in both branches since split point
            List<Blob> addedCurrentBlobs = new ArrayList<>();
            List<Blob> addedBranchBlobs = new ArrayList<>();
            for(String file : currentFiles){
                if(!splitFiles.contains(file)){
                    addedCurrentBlobs.add(currentCommit.blobTrackingFile(file));
                }
            }
            for(String file : branchFiles){
                if(!splitFiles.contains(file)){
                    addedBranchBlobs.add(branchCommit.blobTrackingFile(file));
                }
            }
            //make lists of removed files in both branches since split point
            List<Blob> removedCurrentBlobs = new ArrayList<>();
            List<Blob> removedBranchBlobs = new ArrayList<>();
            for(String file: splitFiles){
                if(!currentFiles.contains(file)){
                    removedCurrentBlobs.add(currentCommit.blobTrackingFile(file));
                }
                if(!branchFiles.contains(file)){
                    removedBranchBlobs.add(branchCommit.blobTrackingFile(file));
                }
            }
            // make lists of modified files in both branches since split point
            List<Blob> currentModifiedBlobs = new ArrayList<>();
            List<Blob> branchModifiedBlobs = new ArrayList<>();
            String fileName;
            String content;
            for(Blob blob: splitBlobs){
                fileName = blob.getFileName();
                content = blob.getContents();
                if(currentCommit.blobTrackingFile(fileName) != null &&
                        !currentCommit.blobTrackingFile(fileName).getContents().equals(content)){
                    currentModifiedBlobs.add(currentCommit.blobTrackingFile(fileName));
                }
                if(branchCommit.blobTrackingFile(fileName) != null &&
                        !branchCommit.blobTrackingFile(fileName).getContents().equals(content)){
                    branchModifiedBlobs.add(branchCommit.blobTrackingFile(fileName));
                }
            }
            HashMap<String, String> currentConflicting = new HashMap<>();
            HashMap<String, String> branchConflicting = new HashMap<>();
            // used for logic in loop
            boolean temp = false;
            // Merge branches
            if(!branchModifiedBlobs.isEmpty()){
                for(Blob blob: branchModifiedBlobs){
                    // Checks for conflicting files
                    for(Blob currentBlob: currentModifiedBlobs){
                        if(currentBlob.getFileName().equals(blob.getFileName()) &&
                                !currentBlob.getContents().equals(blob.getContents())){
                            conflict = true;
                            temp = true;
                            branchConflicting.put(blob.getFileName(), blob.getContents());
                            currentConflicting.put(currentBlob.getFileName(), currentBlob.getContents());
                        }
                    }
                    if(!temp){
                        checkout(branchCommit.hash(), blob.getFileName());
                        add(blob.getFileName());
                    }
                    temp = false;
                }
            }
            // Checks for conflicting files
            if(!currentModifiedBlobs.isEmpty()){
                for(Blob blob: currentModifiedBlobs){
                    for(Blob branchBlob: branchModifiedBlobs){
                        if(branchBlob.getFileName().equals(blob.getFileName()) &&
                                !branchBlob.getContents().equals(blob.getContents())){
                            conflict = true;
                            currentConflicting.put(blob.getFileName(), blob.getContents());
                            branchConflicting.put(branchBlob.getFileName(), branchBlob.getContents());
                        }
                    }
                }
            }
            if(!addedCurrentBlobs.isEmpty()){
                for(Blob blob: addedCurrentBlobs){
                    for(Blob branchBlob: addedBranchBlobs){
                        if(branchBlob.getFileName().equals(blob.getFileName()) &&
                                !branchBlob.getContents().equals(blob.getContents())){
                            conflict = true;
                            currentConflicting.put(blob.getFileName(), blob.getContents());
                            branchConflicting.put(branchBlob.getFileName(), branchBlob.getContents());
                        }
                    }
                }
            }
            // Checks for removed files
            if(!removedBranchBlobs.isEmpty()){
                for(Blob blob: removedBranchBlobs){
                    for(String currentUnmodified: currentFiles){
                        if(currentUnmodified.equals(blob.getFileName())){
                            rm(currentUnmodified);
                        }
                    }
                }
            }
            if(conflict){
                String branchContent;
                String currentContent;
                for (String file: currentConflicting.keySet()) {
                    branchContent = branchConflicting.get(file);
                    currentContent = currentConflicting.get(file);
                    writeContents(join(CWD, file), "<<<<<<<< HEAD\n" +
                            currentContent + "\n=======\n" + branchContent + "\n>>>>>>>");
                }
                System.out.println("Encountered a merge conflict.");
            }else
                commit("Merged " + branchName + " into " + headBranchName + ".", branchCommit.hash());
        }
    }
    // Helper method to get the latest commit from head branch
    public Commit latestCommit(){
        String commitSha = readContentsAsString(join(Branches,readContentsAsString(join(Branches, "head.txt"))));
        return readObject(join(Commits, commitSha), Commit.class);
    }
    // Helper method for finding a list of untracked files
    public List<String> untrackedFileList(){
        // makes list of currently tracked fileNames
        List<String> filesTracked = new ArrayList<>();
        latestCommit().trackedBlobs.forEach(blobSha -> filesTracked.add(readObject(join(Blobs, blobSha), Blob.class).getFileName()));
        // makes list of fileNames in CWD
        List<String> fileNames = plainFilenamesIn(CWD);
        fileNames.removeAll(filesTracked);
        return fileNames;
    }
    // Helper method for clearing staging area
    public void clearStagingArea(){
        for(String file: plainFilenamesIn(StagedAddition)){
            restrictedDelete(join(StagedAddition, file));
        }
        for(String file: plainFilenamesIn(StagedRemoval)){
            restrictedDelete(join(StagedRemoval, file));
        }
    }
}
