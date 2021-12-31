package gitlet;

import javax.swing.text.DateFormatter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;

import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        Repository repository = new Repository();
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                repository.init();
                break;
            case "add":
                repository.add(args[1]);
                break;
            case "commit":
                repository.commit(args[1]);
                break;
            case "rm":
                repository.rm(args[1]);
                break;
            case "log":
                repository.log();
                break;
            case "global-log":
                repository.globalLog();
                break;
            case "find":
                repository.find(args[1]);
                break;
            case "status":
                repository.status();
                break;
            case "checkout":
                if(args[1].equals("--"))
                    repository.checkout(args[2]);
                else if(args[2].equals("--"))
                    repository.checkout(args[1], args[3]);
                else
                    repository.checkout(args[1], false);
                break;
            case "branch":
                repository.branch(args[1]);
                break;
            case "rm-branch":
                repository.rmBranch(args[1]);
                break;
            case "reset":
                repository.reset(args[1]);
                break;
            case "merge":
                repository.merge(args[1]);
                break;
            default:
                if(firstArg !=null)
                    System.out.println("Not a command");
                else
                    System.out.println("Enter a command");
                break;
        }
        System.exit(0);
    }
}
