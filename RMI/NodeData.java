/**
 * Created by Mirage on 2017-01-08.
 */
public class NodeData {
    private String ip;
    private int port;
    private String username;

    public NodeData(String ip, int port, String username) {
        this.ip = ip;
        this.port = port;
        this.username = username;
    }

    public String getIP() {
        return this.ip;
    }

    public String getUsername() {
        return this.username;
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!NodeData.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final NodeData otherNode = (NodeData) obj;

        if (this.ip.equals(otherNode.getIP()) && this.port == otherNode.getPort()) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.ip + ":" + this.port;
    }
}
