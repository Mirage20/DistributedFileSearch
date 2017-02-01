import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by Mirage on 2017-01-08.
 */

/**
 * UDP implementation of the communication.
 */
public class UDPMessageService implements MessageService {


    private DatagramSocket socket = null;
    private Thread listenerThread = null;
    private int receivedQueries = 0;
    private int forwardedQueries = 0;
    private int answeredQueries = 0;

    /**
     * Register with bootstrap server.
     *
     * @param nodeData node details that request registering
     * @return list of neighbours given from the bootstrap server.
     */
    public List<NodeData> register(NodeData nodeData) {

        if (socket == null) {
            try {
                System.out.println("Binding UDP socket to " + nodeData.getIP() + ":" + nodeData.getPort());
                socket = new DatagramSocket(nodeData.getPort());
                System.out.println("Binding Successful");
            } catch (SocketException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to bind UDP socket port " + nodeData.getPort(), e);
            }
        }

        String bootstrapMessage = sendMessageBootstrap("REG " + nodeData.getIP() + " " + nodeData.getPort() + " " + nodeData.getUsername());

        StringTokenizer st = new StringTokenizer(bootstrapMessage, " ");
        String length = st.nextToken();
        String command = st.nextToken();
        int nodeCount = Integer.parseInt(st.nextToken());

        List<NodeData> randNeighbours = new ArrayList<NodeData>();
        if (command.equals("REGOK")) {
            if (nodeCount < 5) {
                for (int i = 0; i < nodeCount; i++) {
                    String ip = st.nextToken();
                    int port = Integer.parseInt(st.nextToken());
                    randNeighbours.add(new NodeData(ip, port, null));
                }
            } else {
                System.out.println("Error registering to Bootstrap server: " + nodeCount);
            }
        }

        return randNeighbours;

    }

    /**
     * Unregister with bootstrap server.
     *
     * @param nodeData node details that request unregistering
     * @return true if unregistration was successful.
     */
    public boolean unregister(NodeData nodeData) {

        if (socket != null) {

            String bootstrapMessage = sendMessageBootstrap("UNREG " + nodeData.getIP() + " " + nodeData.getPort() + " " + nodeData.getUsername());

            StringTokenizer st = new StringTokenizer(bootstrapMessage, " ");

            String length = st.nextToken();
            String command = st.nextToken();
            int value = Integer.parseInt(st.nextToken());

            if (command.equals("UNROK")) {
                if (value < 1) {
                    return true;
                } else {
                    System.out.println("Error unregistering from Bootstrap server: " + value);
                }
            }

            socket.close();
        }
        return false;
    }

    /**
     * Join with a neighbour in the network.
     *
     * @param nodeData node details that request joining.
     * @param neighbor neighbour node details.
     * @return true if join was successful.
     */
    public boolean join(NodeData nodeData, NodeData neighbor) {
        if (socket != null) {

            String nodeMessage = sendMessage("JOIN " + nodeData.getIP() + " " + nodeData.getPort(), neighbor.getIP(), neighbor.getPort());

            StringTokenizer st = new StringTokenizer(nodeMessage, " ");

            String length = st.nextToken();
            String command = st.nextToken();
            int value = Integer.parseInt(st.nextToken());


            if (command.equals("JOINOK")) {
                if (value < 1) {
                    return true;
                } else {
                    System.out.println("Error joining to node " + value);
                }
            }
        }
        return false;
    }

    /**
     * Leave from a neighbour in the network.
     *
     * @param nodeData node details that request leaving.
     * @param neighbor neighbour node details.
     * @return true if leave was successful.
     */
    public boolean leave(NodeData nodeData, NodeData neighbor) {
        if (socket != null) {

            String nodeMessage = sendMessage("LEAVE " + nodeData.getIP() + " " + nodeData.getPort(), neighbor.getIP(), neighbor.getPort());
            StringTokenizer st = new StringTokenizer(nodeMessage, " ");

            String length = st.nextToken();
            String command = st.nextToken();

            if (command.equals("LEAVEOK")) {
                int value = Integer.parseInt(st.nextToken());
                if (value < 1) {
                    return true;
                } else {
                    System.out.println("Error leaving from node " + value);
                }
            }

        }
        return false;
    }

    /**
     * Sends a search request to a neighbour.
     *
     * @param filename search file name.
     * @param nodeData node details of the searching node
     * @param neighbor node details of the neighbour.
     */
    public void search(String filename, NodeData nodeData, NodeData neighbor) {

        try {
            String searchRequest = "SER " + nodeData.getIP() + " " + nodeData.getPort() + " \"" + filename + "\" " + Configuration.HOPS_MAX;
            searchRequest = String.format("%04d", searchRequest.length() + 5) + " " + searchRequest;

            DatagramPacket dpResponse = new DatagramPacket(searchRequest.getBytes(), searchRequest.getBytes().length,
                    InetAddress.getByName(neighbor.getIP()), neighbor.getPort());

            System.out.println("Sending (" + searchRequest + ") to Neighbor at " + neighbor.toString());
            socket.send(dpResponse);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void startListening(MessageReceivedEvent receivedEvent) {

        System.out.println("Starting listener thread...");
        listenerThread = new Thread(new Listener(receivedEvent));
        listenerThread.start();

    }

    public void stopListening() {

        System.out.println("Stopping listener thread...");
        listenerThread.interrupt();

        try {
            listenerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public int getReceivedQueries() {
        return receivedQueries;
    }

    public int getForwardedQueries() {
        return forwardedQueries;
    }

    public int getAnsweredQueries() {
        return answeredQueries;
    }

    // client mode communication with bootstrap
    private String sendMessageBootstrap(String message) {
        while (true) {
            try {

                String request = String.format("%04d", message.length() + 5) + " " + message;

                DatagramPacket dpRequest = new DatagramPacket(request.getBytes(), request.getBytes().length,
                        InetAddress.getByName(Configuration.BOOTSTRAP_IP), Configuration.BOOTSTRAP_PORT);

                System.out.println("Sending (" + request + ") to Bootstrap Server at " + Configuration.BOOTSTRAP_IP + ":"
                        + Configuration.BOOTSTRAP_PORT);
                socket.send(dpRequest);

                socket.setSoTimeout(Configuration.LISTENER_TIMEOUT);

                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);

                System.out.println("Waiting for Bootstrap Server response...");
                socket.receive(incoming);

                byte[] data = incoming.getData();

                String bootstrapResponse = new String(data, 0, incoming.getLength());
                System.out.println("Bootstrap Response successful (" + bootstrapResponse + ")");

                return bootstrapResponse;
            } catch (SocketTimeoutException ex) {
                System.out.println("Connection timeout. Re-connecting...");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // client mode communication with neighbour
    private String sendMessage(String message, String ip, int port) {

        while (true) {
            try {

                String request = String.format("%04d", message.length() + 5) + " " + message;

                DatagramPacket dpRequest = new DatagramPacket(request.getBytes(), request.getBytes().length,
                        InetAddress.getByName(ip), port);

                System.out.println("Sending (" + request + ") to Node at " + ip + ":" + port);
                socket.send(dpRequest);

                socket.setSoTimeout(Configuration.LISTENER_TIMEOUT);

                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);

                System.out.println("Waiting for Node response...");
                socket.receive(incoming);

                byte[] data = incoming.getData();

                String nodeResponse = new String(data, 0, incoming.getLength());
                System.out.println("Node Response successful (" + nodeResponse + ")");

                return nodeResponse;
            } catch (SocketTimeoutException ex) {
                System.out.println("Connection timeout. Re-connecting...");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Message receiving thread implantation.
     */
    private class Listener implements Runnable {

        private MessageReceivedEvent receivedEvent;

        Listener(MessageReceivedEvent receivedEvent) {
            this.receivedEvent = receivedEvent;
        }

        public void run() {
            System.out.println("Listener thread started.");

            boolean isTimeout = false;
            while (!Thread.currentThread().isInterrupted()) {
                try {

                    byte[] buffer = new byte[65536];
                    DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);

                    if (!isTimeout) {
                        System.out.println("Listening for Node request...");
                    }
                    socket.setSoTimeout(Configuration.LISTENER_TIMEOUT);
                    socket.receive(incoming);
                    isTimeout = false;
                    byte[] data = incoming.getData();
                    String nodeResponse = new String(data, 0, incoming.getLength());
                    System.out.println("Node request received (" + nodeResponse + ")");

                    StringTokenizer st = new StringTokenizer(nodeResponse, " ");

                    String length = st.nextToken();
                    String command = st.nextToken();


                    if (command.equals("JOIN")) {

                        String ip = st.nextToken();
                        int port = Integer.parseInt(st.nextToken());

                        boolean result = receivedEvent.onJoin(new NodeData(ip, port, null));

                        String response = "JOINOK " + (result ? 0 : 9999);
                        response = String.format("%04d", response.length() + 5) + " " + response;

                        DatagramPacket dpResponse = new DatagramPacket(response.getBytes(), response.getBytes().length,
                                InetAddress.getByName(ip), port);

                        System.out.println("Sending (" + response + ") to Node at " + ip + ":" + port);
                        socket.send(dpResponse);

                    } else if (command.equals("LEAVE")) {

                        String ip = st.nextToken();
                        int port = Integer.parseInt(st.nextToken());

                        boolean result = receivedEvent.onLeave(new NodeData(ip, port, null));

                        String response = "LEAVEOK " + (result ? 0 : 9999);
                        response = String.format("%04d", response.length() + 5) + " " + response;

                        DatagramPacket dpResponse = new DatagramPacket(response.getBytes(), response.getBytes().length,
                                InetAddress.getByName(ip), port);

                        System.out.println("Sending (" + response + ") to Node at " + ip + ":" + port);
                        socket.send(dpResponse);

                    } else if (command.equals("SER")) {
                        receivedQueries++;
                        String ip = st.nextToken();
                        int port = Integer.parseInt(st.nextToken());
                        String fileName = "";
                        String fileNamePart;
                        // Decode file names with spaces
                        while (st.hasMoreTokens()) {
                            fileNamePart = st.nextToken();

                            if (fileNamePart.startsWith("\"") && fileNamePart.endsWith("\"")) {
                                fileName = fileNamePart;
                                break;
                            }

                            if (fileNamePart.startsWith("\"")) {
                                fileName = fileNamePart;
                            } else if (fileNamePart.endsWith("\"")) {
                                fileName += " " + fileNamePart;
                                break;
                            } else {
                                fileName += " " + fileNamePart;
                            }
                        }
                        int hops = Integer.parseInt(st.nextToken());

                        SearchResult searchResult = receivedEvent.onSearch(fileName.substring(1, fileName.length() - 1), new NodeData(ip, port, null),
                                new NodeData(incoming.getAddress().getHostAddress(), incoming.getPort(), null));

                        // Send message according to the search result.
                        if (searchResult.isSuccess()) {
                            System.out.println("Search success Node: " + searchResult.getOwnerNode().toString());
                            String response = "SEROK " + searchResult.getMatchingFileNames().size() + " "
                                    + searchResult.getOwnerNode().getIP() + " " + searchResult.getOwnerNode().getPort() + " " + hops;

                            List<String> matchingFiles = searchResult.getMatchingFileNames();

                            for (String matchingFile : matchingFiles) {
                                response += " \"" + matchingFile + "\"";
                            }
                            response = String.format("%04d", response.length() + 5) + " " + response;

                            DatagramPacket dpResponse = new DatagramPacket(response.getBytes(), response.getBytes().length,
                                    InetAddress.getByName(ip), port);

                            System.out.println("Sending (" + response + ") to Node at " + ip + ":" + port);
                            socket.send(dpResponse);
                            answeredQueries++;
                        } else {
                            System.out.println("Search fail Node: " + searchResult.getOwnerNode().toString());
                            if (hops > 0) {
                                hops--;
                                List<NodeData> selectedNeighbours = searchResult.getSelectedNeighbours();

                                for (NodeData selectedNeighbour : selectedNeighbours) {
                                    String response = "SER " + ip + " " + port + " " + fileName + " " + hops;
                                    response = String.format("%04d", response.length() + 5) + " " + response;

                                    DatagramPacket dpResponse = new DatagramPacket(response.getBytes(), response.getBytes().length,
                                            InetAddress.getByName(selectedNeighbour.getIP()), selectedNeighbour.getPort());

                                    System.out.println("Sending (" + response + ") to Node at "
                                            + selectedNeighbour.getIP() + ":" + selectedNeighbour.getPort());
                                    socket.send(dpResponse);
                                    forwardedQueries++;
                                }
                            } else {
                                System.out.println("Hop is 0, dropping the request. Node: " + searchResult.getOwnerNode().toString());
                            }
                        }

                    } else if (command.equals("SEROK")) {
                        int fileCount = Integer.parseInt(st.nextToken());
                        String ip = st.nextToken();
                        int port = Integer.parseInt(st.nextToken());
                        int hops = Integer.parseInt(st.nextToken());
                        List<String> fileNames = new ArrayList<String>();

                        String fileName = "";
                        String fileNamePart;
                        while (st.hasMoreTokens()) {
                            fileNamePart = st.nextToken();

                            if (fileNamePart.startsWith("\"") && fileNamePart.endsWith("\"")) {
                                fileName = fileNamePart;
                                fileNames.add(fileName.substring(1, fileName.length() - 1));
                                fileName = "";
                            }

                            if (fileNamePart.startsWith("\"")) {
                                fileName = fileNamePart;
                            } else if (fileNamePart.endsWith("\"")) {
                                fileName += " " + fileNamePart;
                                fileNames.add(fileName.substring(1, fileName.length() - 1));
                                fileName = "";
                            } else {
                                fileName += " " + fileNamePart;
                            }
                        }

                        if (fileCount != fileNames.size()) {
                            System.out.println("Could not recover file names.");
                        }

                        SearchResult searchResult = new SearchResult();
                        searchResult.setSuccess(true);
                        searchResult.setMatchingFileNames(fileNames);
                        searchResult.setOwnerNode(new NodeData(ip, port, null));
                        searchResult.setHopCount(Configuration.HOPS_MAX - hops);
                        receivedEvent.onSearchSuccess(searchResult);

                    } else {
                        String ip = st.nextToken();
                        int port = Integer.parseInt(st.nextToken());

                        String response = "ERROR";
                        response = String.format("%04d", response.length() + 5) + " " + response;

                        DatagramPacket dpResponse = new DatagramPacket(response.getBytes(), response.getBytes().length,
                                InetAddress.getByName(ip), port);

                        System.out.println("Sending (" + response + ") to Node at " + ip + ":" + port);
                        socket.send(dpResponse);
                    }

                } catch (SocketTimeoutException ex) {
                    isTimeout = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Listener thread stopped.");
        }
    }
}
