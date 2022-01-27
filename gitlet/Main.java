package gitlet;

import java.io.IOException;


/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Edgar Navarro
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) throws IOException {
        Repository repository = new Repository();
        String firstArg;
        if(args.length > 0) {
            firstArg = args[0];
        }else{
            firstArg = "ERROR";
        }
        if(!firstArg.equals("init") && !repository.GITLET_DIR.exists()){
            System.out.println("Not in an initialized Gitlet directory.");
        }else {
            switch (firstArg) {
                case "init":
                    repository.init();
                    break;
                case "add":
                    repository.add(args[1]);
                    break;
                case "commit":
                    if (args.length > 1)
                        repository.commit(args[1]);
                    else
                        repository.commit("");
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
                    if (args.length == 3 && args[1].equals("--"))
                        repository.checkout(args[2]);
                    else if (args.length == 4 && args[2].equals("--"))
                        repository.checkout(args[1], args[3]);
                    else if (args.length == 2)
                        repository.checkout(args[1], false);
                    else
                        System.out.println("Incorrect operands.");
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
                    if (firstArg.equals("ERROR"))
                        System.out.println("Please enter a command.");

                    else
                        System.out.println("Not a command");
                    break;
            }
        }
        System.exit(0);
    }
}
