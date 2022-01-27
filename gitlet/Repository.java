package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gitlet.Utils.*;

public class Repository implements Serializable {
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
        if(GITLET_DIR.exists()){
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
            File Master = join(Branches, "master");
            Master.createNewFile();
            writeContents(Master, initialCommit.hash());
            // Make Head pointer
            File Head = join(Branches, "head");
            Head.createNewFile();
            writeContents(Head, "master");
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
            if(!currentCommit.trackedBlobs.isEmpty() && currentCommit.trackedBlobs.contains(sha1(Utils.serialize(blob)))){
                if(plainFilenamesIn(StagedAddition).contains(fileName))
                    Delete(join(StagedAddition, fileName));
                if(plainFilenamesIn(StagedRemoval).contains(fileName))
                    Delete(join(StagedRemoval, fileName));
            }else {
                // makes a clone of the file
                File copy = file;
                // writes the clone of the file to the Staged for addition directory
                File path = join(StagedAddition, fileName);
                path.createNewFile();
                writeContents(path, readContentsAsString(copy));
                // Adds blob to be in Blob directory
                writeObject(join(Blobs, sha1(Utils.serialize(blob))), blob);

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
            File commitFile = join(Commits, commit.hash());
            commitFile.createNewFile();
            writeObject(commitFile, commit);
            // Makes the Current Commit be our most recent commit SHA-1
            writeContents(join(Branches, readContentsAsString(join(Branches,"head"))),commit.hash());
            // Clears the Staging Area
            clearStagingArea();
        }
    }
    public void commit(String message, String parent2) throws IOException {
        // Creates the commit
        Commit commit = new Commit(message, this, parent2);
        File commitFile = join(Commits, commit.hash());
        commitFile.createNewFile();
        writeObject(commitFile, commit);
        // Makes the Current Commit be our most recent commit SHA-1
        writeContents(join(Branches, readContentsAsString(join(Branches,"head"))),commit.hash());
        // Clears the Staging Area
        clearStagingArea();
    }
    public void rm(String fileName) throws IOException {
        if(!isInside(fileName, StagedAddition) && !latestCommit().containing(fileName)){
            System.out.println("No reason to remove the file.");
            return;
        }
        if(isInside(fileName, StagedAddition)){
            Delete(join(StagedAddition, fileName));
        }
        if(latestCommit().containing(fileName)){
            // writes the file in CWD to a file in Staged removal (stages file for removal)
            File file = join(StagedRemoval,fileName);
            file.createNewFile();
            if(join(CWD, fileName).exists())
                writeContents(file, readContentsAsString(join(CWD,fileName)));
            // removes file from CWD if not already done
            if(join(CWD,fileName).exists())
                restrictedDelete(join(CWD, fileName));
        }

    }
    public void log(){
        // Gets the sha of our head
        Commit current = latestCommit();
        String sha = current.hash();
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
                System.out.println("===\ncommit " + current.hash() + "\nDate: " + current.getTime()
                        + "\n" + current.getMessage() + "\n");
            }else{
                System.out.println("===\ncommit " + current.hash() + "\nMerge: "+ current.getParent().substring(0,7)+
                        " " + current.getParent2().substring(0,7) + "\nDate: " + current.getTime()
                        + "\n" + current.getMessage() + "\n");
            }
        }
    }
    public void find(String message){
        List<String> shas = plainFilenamesIn(Commits);
        Commit commit;
        boolean found = false;
        for(String sha: shas){
            commit = readObject(join(Commits, sha), Commit.class);
            if(commit.getMessage().equals(message)) {
                System.out.println(sha);
                found = true;
            }
        }
        if(!found)
            System.out.println("Found no commit with that message.");
    }
    public void status(){
        String head = readContentsAsString(join(Branches, "head"));
        List<String> branches = plainFilenamesIn(Branches);
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
                System.out.println("*"+ removeEnd(branchName));
            }else if(!branchName.equals("head"))
                System.out.println(removeEnd(branchName));
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
        System.out.println("\n=== Untracked Files ===\n");
        /*for(String untrackedFiles : untrackedFileList()){
            System.out.println(untrackedFiles);
        }*/
    }
    public void checkout(String fileName){
        // makes path of file to be replaced
        File path = join(CWD, fileName);
        Blob blob = latestCommit().blobTrackingFile(fileName);
        if(blob != null){
            writeContents(join(CWD, fileName), blob.getContents());
        }else
            System.out.println("File does not exist in that commit.");
    }
    public void checkout(String commitId, String fileName){
        if(plainFilenamesIn(Commits).contains(commitId)){
            Commit commit = readObject(join(Commits, commitId), Commit.class);
            Blob blob = commit.blobTrackingFile(fileName);
            if(blob != null && blob.getFileName().equals(fileName)){
                writeContents(join(CWD, fileName), blob.getContents());
            }else
                System.out.println("File does not exist in that commit.");
        }else
            System.out.println("No commit with that id exists.");
    }
    public void checkout(String branchName, boolean hyphen){
        if(isInside(branchName, Branches)){
            if(readContentsAsString(join(Branches,"head")).equals(branchName)){
                System.out.println("No need to checkout the current branch.");
            }else if(!untrackedFileList().isEmpty()){
                System.out.println("There is an untracked file in the way; delete itm or add and commit it first.");
            }else{
                // gets latest commit fo given branch
                Commit commit = latestCommit(branchName);
                // Makes list of blobs tracked by commit
                List<Blob> blobs = new ArrayList<>(commit.blobList());
                // replaces tracked files in CWD
                for(Blob blob: blobs){
                    writeContents(join(CWD,blob.getFileName()), blob.getContents());
                }
                for(String files: untrackedFileList(latestCommit(branchName))){
                    restrictedDelete(join(CWD, files));
                }
                // Clear Staging area
                clearStagingArea();
                // makes given branch the head
                writeContents(join(Branches, "head"),branchName);
            }
        }else
            System.out.println("No such branch exists.");
    }
    public void branch(String branchName) throws IOException {
        if(!isInside(branchName,Branches)){
            File branch = join(Branches, branchName);
            branch.createNewFile();
            writeContents(branch, latestCommit().hash());
        }else
            System.out.println("A branch with that name already exists");

    }
    public void rmBranch(String branchName){
        if(isInside(branchName,Branches)){
            if(!readContentsAsString(join(Branches,"head")).equals(branchName)){
                Delete(join(Branches, branchName));
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
                writeContents(join(Branches, readContentsAsString(join(Branches, "head"))), commit.hash());
                // clears staging area
                clearStagingArea();
            }else
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
        }else
            System.out.println("No commit with that id exists.");

    }
    public void merge(String branchName) throws IOException {
        final String headBranchName = readContentsAsString(join(Branches, "head"));

        if(branchName.equals(headBranchName)){
            System.out.println("Cannot merge a branch with itself.");
        }else if(!untrackedFileList().isEmpty()){
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first");
        }else if(!isInside(branchName, Branches)) {
            System.out.println("A branch with that name does not exist.");

        }else if(!plainFilenamesIn(StagedAddition).isEmpty() || !plainFilenamesIn(StagedRemoval).isEmpty()) {
            System.out.println("You have uncommitted changes.");
        }else {
            String givenBranchCommitSha = readContentsAsString(join(Branches, branchName));
            String headBranchCommitSha = readContentsAsString(join(Branches, headBranchName));
            String latestCommonAncestorSha = null;
            boolean conflict = false;
            final Commit branchCommit = readObject(join(Commits, givenBranchCommitSha), Commit.class);
            final Commit currentCommit = readObject(join(Commits, headBranchCommitSha), Commit.class);
            // makes list of all commits starting from commit head
            Commit current;
            List<Commit> commits = new ArrayList<>();
            do{
                current = readObject(join(Commits, headBranchCommitSha), Commit.class);
                commits.add(current);
                headBranchCommitSha = current.getParent();
            }while(headBranchCommitSha != null);
            List<String> commitShas = new ArrayList<>();
            commits.forEach(commit -> commitShas.add(commit.hash()));
            // Checks all branch commit ancestors to see where the head and given branches split
            Commit branchCurrent = branchCommit;
            String branchParentSha;
            while(!commitShas.contains(branchCurrent.hash())){
                branchParentSha = branchCurrent.getParent();
                branchCurrent = readObject(join(Commits, branchParentSha), Commit.class);
            }
            final Commit latestCommonAncestorCommit = branchCurrent;
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
                    removedCurrentBlobs.add(latestCommonAncestorCommit.blobTrackingFile(file));
                }
                if(!branchFiles.contains(file)){
                    removedBranchBlobs.add(latestCommonAncestorCommit.blobTrackingFile(file));
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
                        }else{
                            checkout(currentCommit.hash(), blob.getFileName());
                            add(blob.getFileName());
                            checkout(branchCommit.hash(), branchBlob.getFileName());
                            add(branchBlob.getFileName());
                        }
                    }
                }
            }
            // Checks for removed files
            List<Blob> currentUnmodified = currentBlobs;
            currentUnmodified.removeAll(currentModifiedBlobs);
            if(!removedBranchBlobs.isEmpty()){
                for(Blob blob: removedBranchBlobs){
                    for(Blob currentModified: currentModifiedBlobs){
                        if(currentModified.getFileName().equals(blob.getFileName())){
                            conflict = true;
                            currentConflicting.put(blob.getFileName(), blob.getContents());
                            branchConflicting.put(blob.getFileName(), "File was removed.");
                        }
                    }
                    for(Blob currentUnmodifiedBlob : currentUnmodified){
                        if(currentUnmodifiedBlob.getFileName().equals(blob.getFileName()))
                            rm(blob.getFileName());
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
        String commitSha = readContentsAsString(join(Branches,readContentsAsString(join(Branches, "head"))));
        return readObject(join(Commits, commitSha), Commit.class);
    }
    public Commit latestCommit(String branch){
        String commitSha = readContentsAsString(join(Branches,branch));
        return readObject(join(Commits, commitSha), Commit.class);
    }
    // Helper method for finding a list of untracked files
    public List<String> untrackedFileList(){
        // makes list of currently tracked fileNames
        List<String> filesTracked = new ArrayList<>();
        latestCommit().trackedBlobs.forEach(blobSha -> filesTracked.add(readObject(join(Blobs, blobSha), Blob.class).getFileName()));
        // makes list of fileNames in CWD
        List<String> fileNames = new ArrayList<>(plainFilenamesIn(CWD));
        fileNames.removeAll(filesTracked);
        return fileNames;
    }
    public List<String> untrackedFileList(Commit commit){
        // makes list of currently tracked fileNames
        List<String> filesTracked = new ArrayList<>();
        commit.trackedBlobs.forEach(blobSha -> filesTracked.add(readObject(join(Blobs, blobSha), Blob.class).getFileName()));
        // makes list of fileNames in CWD
        List<String> fileNames = new ArrayList<>(plainFilenamesIn(CWD));
        fileNames.removeAll(filesTracked);
        return fileNames;
    }
    // Helper method for clearing staging area
    public void clearStagingArea(){
        for(String file: plainFilenamesIn(StagedAddition)){
            Delete(join(StagedAddition, file));
        }
        for(String file: plainFilenamesIn(StagedRemoval)){
            Delete(join(StagedRemoval, file));
        }
    }
    // Helper method for removing  from files
    public String removeEnd(String fileName){
        return fileName.split("\\.")[0];
    }
}
