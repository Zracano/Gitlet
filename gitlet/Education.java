package gitlet;

import java.io.File;
import java.io.IOException;

import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Education {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) throws IOException {
        // TODO: what if args is empty?
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                File currentDirectory = new File(System.getProperty("user.dir"));
                File GITLET_DIR = join(currentDirectory, ".gitlet");
                File saves = join(GITLET_DIR, "saves");
                File dog = join(saves, "dog.py");
                File cat = join(GITLET_DIR, "saves");
                File clap = join(saves, "clap");

                GITLET_DIR.mkdir();
                saves.mkdir();

                clap.createNewFile();
                loL jump = new loL();
                writeObject(clap, jump);
                loL comedy = readObject(clap,loL.class);
                System.out.println(comedy.laughs);
                String sit = readContentsAsString(join(saves,"LSvisualizer.py"));
                dog.createNewFile();
                writeContents(dog, sit);

                break;
            case "add":
                // TODO: handle the `add [filename]` command

                break;
            // TODO: FILL THE REST IN
        }
    }
}
