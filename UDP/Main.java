import java.util.Scanner;

/**
 * Created by Mirage on 2017-01-08.
 */
public class Main {
    public static void main(String[] args) {

        System.out.println("==============================================================");
        System.out.println("Enter #PN to print neighbour table and #PF to print file list.");
        System.out.println("==============================================================");

        Node node = new Node(Configuration.NODE_IP, Configuration.NODE_PORT, Configuration.NODE_USERNAME);
        node.connect();

        String command;
        Scanner scanner = new Scanner(System.in);

        // Handles the user commands.
        while (!(command = scanner.nextLine()).equals("#X")) {
            if (command.startsWith("#")) {
                if (command.equals("#PN")) {
                    node.printNeighbors();
                } else if (command.equals("#PF")) {
                    node.printFileNames();
                } else if(command.equals("#BENCH")){
                    node.runBenchmark();
                }else if(command.equals("#STAT")){
                    node.printQueryStatistics();
                }
            } else {
                node.search(command);
            }
        }

        node.disconnect();
    }
}
