import java.io.IOException;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by Mirage on 2017-01-08.
 */

/**
 * Java RMI implementation of the communication.
 */
public class RMIMessageService implements MessageService {


    private DatagramSocket socket = null;
    private RemoteMethodServer remoteMethodServer = null;
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
                System.out.println("Binding UDP socket for Bootstrap Server communication");
                socket = new DatagramSocket();
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

        try {
            Registry registry = LocateRegistry.getRegistry(neighbor.getIP(), neighbor.getPort());
            RemoteMethod stub = (RemoteMethod) registry.lookup("RemoteMethod");
            boolean response = stub.join(nodeData.getIP(), nodeData.getPort());
            if (response) {
                System.out.println("Joined with neighbour " + neighbor.getIP() + ":" + neighbor.getPort());
            } else {
                System.out.println("Failed to Join with neighbour " + neighbor.getIP() + ":" + neighbor.getPort());
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
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
        try {
            Registry registry = LocateRegistry.getRegistry(neighbor.getIP(), neighbor.getPort());
            RemoteMethod stub = (RemoteMethod) registry.lookup("RemoteMethod");
            boolean response = stub.leave(nodeData.getIP(), nodeData.getPort());
            if (response) {
                System.out.println("Leave successful with neighbour " + neighbor.toString());
            } else {
                System.out.println("Leave failed with neighbour " + neighbor.toString());
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
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
            Registry registry = LocateRegistry.getRegistry(neighbor.getIP(), neighbor.getPort());
            RemoteMethod stub = (RemoteMethod) registry.lookup("RemoteMethod");
            System.out.println("Sending search request \"" + filename + "\" to Neighbor at " + neighbor.toString());
            stub.search(filename, nodeData.getIP(), nodeData.getPort(), nodeData.getIP(), nodeData.getPort(), Configuration.HOPS_MAX);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void startListening(MessageReceivedEvent receivedEvent) {

        System.out.println("Starting RMI Server...");
        remoteMethodServer = new RemoteMethodServer(receivedEvent);
        remoteMethodServer.bind();

    }

    public void stopListening() {
        System.out.println("Stopping listener thread...");
        remoteMethodServer.unbind();

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
     * Remote server implementation.
     */
    private class RemoteMethodServer implements RemoteMethod {

        private MessageReceivedEvent receivedEvent;
        private Registry registry = null;

        RemoteMethodServer(MessageReceivedEvent receivedEvent) {
            this.receivedEvent = receivedEvent;
        }

        // Binds remote object to the registry
        void bind() {

            try {
                RemoteMethod skeleton = (RemoteMethod) UnicastRemoteObject.exportObject(this, 0);
                registry = LocateRegistry.createRegistry(Configuration.NODE_PORT);
                registry.bind("RemoteMethod", skeleton);
                System.out.println("Remote object bind successful");
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (AlreadyBoundException e) {
                e.printStackTrace();
            }
        }

        // Unbind the registry entry for remote object
        void unbind() {
            try {
                registry.unbind("RemoteMethod");
                System.out.println("Remote object unbind successful");
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }

        // Remote call for join a neighbour
        public boolean join(String neighborIP, int neighbourPort) {
            NodeData neighbor = new NodeData(neighborIP, neighbourPort, null);
            System.out.println("Join called from neighbour " + neighbor.toString());
            return receivedEvent.onJoin(neighbor);
        }

        // Remote call for leave a neighbour
        public boolean leave(String neighborIP, int neighbourPort) {
            NodeData neighbor = new NodeData(neighborIP, neighbourPort, null);
            System.out.println("Leave called from neighbour " + neighbor.toString());
            return receivedEvent.onLeave(neighbor);
        }

        // Remote call for search
        public void search(String filename, String searchNodeIP, int searchNodePort, String neighborIP, int neighbourPort, int hops) {

            NodeData searchNode = new NodeData(searchNodeIP, searchNodePort, null);
            NodeData neighbor = new NodeData(neighborIP, neighbourPort, null);

            System.out.println("Search called from neighbor " + neighbor.toString() + ". Searching node " + searchNode.toString());
            SearchResult searchResult = receivedEvent.onSearch(filename, searchNode, neighbor);
            receivedQueries++;
            if (searchResult.isSuccess()) {
                System.out.println("Search success on this node. Hops:" + hops);
                try {
                    Registry registry = LocateRegistry.getRegistry(searchNode.getIP(), searchNode.getPort());
                    RemoteMethod stub = (RemoteMethod) registry.lookup("RemoteMethod");
                    NodeData owner = searchResult.getOwnerNode();
                    stub.searchSuccess(owner.getIP(), owner.getPort(), searchResult.getMatchingFileNames(), hops);
                    answeredQueries++;
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                System.out.println("Search failed on this node");
                try {
                    if (hops > 0) {
                        hops--;
                        List<NodeData> selectedNeighbours = searchResult.getSelectedNeighbours();

                        for (NodeData selectedNeighbour : selectedNeighbours) {
                            System.out.println("Calling search on neighbour " + selectedNeighbour.toString());
                            Registry registry = LocateRegistry.getRegistry(selectedNeighbour.getIP(), selectedNeighbour.getPort());
                            RemoteMethod stub = (RemoteMethod) registry.lookup("RemoteMethod");
                            NodeData searchedNode = searchResult.getOwnerNode();
                            stub.search(filename, searchNode.getIP(), searchNode.getPort(), searchedNode.getIP(), searchedNode.getPort(), hops);
                            forwardedQueries++;
                        }

                    } else {
                        System.out.println("Hop is 0, dropping the request. Node: " + searchResult.getOwnerNode().toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Remote call when search is success
        public void searchSuccess(String ownerIP, int ownerPort, List<String> matchingFileNames, int hops) {

            SearchResult result = new SearchResult();
            result.setOwnerNode(new NodeData(ownerIP, ownerPort, null));
            result.setMatchingFileNames(matchingFileNames);
            result.setHopCount(Configuration.HOPS_MAX - hops);
            System.out.println("Search success. Owner node " + result.getOwnerNode() + ". Hop count: " + (Configuration.HOPS_MAX - hops));
            receivedEvent.onSearchSuccess(result);
        }
    }
}
