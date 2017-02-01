import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mirage on 2017-01-08.
 */
public class Node implements MessageReceivedEvent {

    private NodeData nodeData;
    private MessageService messageService;
    private List<NodeData> neighbors;
    private List<String> fileNames = new ArrayList<String>();
    long sendTime = 0;

    public Node(String ip, int port, String username) {
        nodeData = new NodeData(ip, port, username);
        messageService = new UDPMessageService();
        neighbors = new ArrayList<NodeData>();
        loadFileNames();
    }

    /**
     * Establish the connection to P2P network.
     */
    public void connect() {

        int receivedNeighborCount = 0;
        int failNeighborCount = 0;

        do {

            List<NodeData> randNeighbors = messageService.register(nodeData);
            receivedNeighborCount = randNeighbors.size();
            // Try to connect with neighbors given from the bootstrap sever.
            for (NodeData randNeighbor : randNeighbors) {
                if (messageService.join(nodeData, randNeighbor)) {
                    neighbors.add(randNeighbor);
                } else {
                    System.out.println("Could not join to the neighbor at " + randNeighbor.getIP() + ":" + randNeighbor.getPort());
                    failNeighborCount++;
                }
            }
            // Re-register with bootstrap server to get new neighbours.
            if (receivedNeighborCount > 0 && failNeighborCount >= receivedNeighborCount) {
                System.out.println("Failed to join any of the neighbors. Re-registering to request new neighbors.");
                messageService.unregister(nodeData);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        } while (true);
        messageService.startListening(this);
    }

    /**
     * Disconnect form the P2P network.
     */
    public void disconnect() {

        messageService.stopListening();
        for (NodeData neighbor : neighbors) {
            messageService.leave(nodeData, neighbor);
        }

        messageService.unregister(nodeData);

    }

    /**
     * Search the given file name within the network
     *
     * @param filename name of the file.
     */
    public void search(String filename) {
        sendTime = System.currentTimeMillis();
        List<NodeData> neighborsCopy = new ArrayList<NodeData>(neighbors);
        Collections.shuffle(neighborsCopy);
        List<NodeData> randNeighbors = neighborsCopy.subList(0, Math.min(2, neighbors.size()));
        for (NodeData randNeighbor : randNeighbors) {
            messageService.search(filename, nodeData, randNeighbor);
        }

    }

    public void printNeighbors() {
        for (NodeData neighbor : neighbors) {
            System.out.println("Neighbor Node " + neighbor.getIP() + " : " + neighbor.getPort());
        }
        System.out.println("Total neighbors = " + neighbors.size());
    }

    public void printFileNames() {
        for (String fileName : fileNames) {
            System.out.println(fileName);
        }
        System.out.println("Total files = " + fileNames.size());
    }

    /**
     * Callback when other node try to join.
     *
     * @param nodeData data of the joining node.
     * @return Must return true if addition to the neighbour table was successful.
     */
    public boolean onJoin(NodeData nodeData) {

        if (!neighbors.contains(nodeData)) {
            neighbors.add(nodeData);
            return true;
        }
        return false;
    }

    /**
     * Callback when other node try to leave.
     *
     * @param nodeData data of the joining node.
     * @return Must return true if deletion from the neighbour table was successful.
     */
    public boolean onLeave(NodeData nodeData) {
        return neighbors.remove(nodeData);
    }

    /**
     * Callback when search query is received.
     *
     * @param query        search query
     * @param queryNode    data of the searching node.
     * @param receivedNode data of the  node that the packet received..
     * @return Search result indicating the details.
     */
    public SearchResult onSearch(String query, NodeData queryNode, NodeData receivedNode) {

        System.out.println("Search request | Query node " + queryNode.toString()
                + " | Received node " + receivedNode.toString() + " | Filename \"" + query + "\"");

        SearchResult result = new SearchResult();
        result.setOwnerNode(nodeData);

        List<String> matchingFileNames = new ArrayList<String>();

        for (String fileName : fileNames) {
            if (fileName.toLowerCase().matches(".*\\b" + query.toLowerCase() + "\\b.*")) {
                matchingFileNames.add(fileName);
                System.out.println("File \"" + fileName + "\" matched with query \"" + query + "\"");
            }

        }

        if (matchingFileNames.size() > 0) {
            result.setSuccess(true);
            result.setMatchingFileNames(matchingFileNames);
            return result;
        } else {
            result.setSuccess(false);
            List<NodeData> neighborsCopy = new ArrayList<NodeData>(neighbors);
            neighborsCopy.remove(queryNode);
            neighborsCopy.remove(receivedNode);
            Collections.shuffle(neighborsCopy);
            result.setSelectedNeighbours(neighborsCopy.subList(0, Math.min(2, neighborsCopy.size())));

            return result;
        }

    }

    /**
     * Callback when a given search query is successful.
     *
     * @param result search result indicating node details of the owner and matching file names.
     */
    public void onSearchSuccess(SearchResult result) {
        long elapsed = System.currentTimeMillis() - sendTime;
        List<String> files = result.getMatchingFileNames();
        for (String fileName : files) {
            System.out.println(fileName);
        }
        System.out.println("Hop Count = " + result.getHopCount() + ". Latency = " + elapsed + " ms");
    }


    // loads the file names list from the file given in the configuration.
    private void loadFileNames() {
        try {
            FileInputStream fileInputStream = new FileInputStream(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getSchemeSpecificPart()
                    + Configuration.FILE_NAMES_PATH);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

            String line = bufferedReader.readLine();
            while (line != null) {
                fileNames.add(line);
                line = bufferedReader.readLine();
            }
            Collections.shuffle(fileNames);
            fileNames = fileNames.subList(0, Math.min((int) Math.round((2 * Math.random()) + 3), fileNames.size()));
            bufferedReader.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // run the test benchmark
    void runBenchmark() {
        try {
            System.out.println("==========================================================");
            System.out.println("Benchmark started.");
            FileInputStream fileInputStream = new FileInputStream(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getSchemeSpecificPart()
                    + Configuration.QUERIES_PATH);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

            String line = bufferedReader.readLine();
            List<String> queriesList = new ArrayList<String>();
            while (line != null) {
                queriesList.add(line);
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            fileInputStream.close();

            for (String query : queriesList) {
                this.search(query);
                Thread.sleep(Configuration.BENCHMARK_TIMEOUT);
            }
            System.out.println("Benchmark finished.");
            System.out.println("==========================================================");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    void printQueryStatistics() {
        System.out.println("Received Queries = " + messageService.getReceivedQueries());
        System.out.println("Forwarded Queries = " + messageService.getForwardedQueries());
        System.out.println("Answered Queries = " + messageService.getAnsweredQueries());
    }

}
