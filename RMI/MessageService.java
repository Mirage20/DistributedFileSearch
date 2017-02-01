import java.util.List;

/**
 * Created by Mirage on 2017-01-08.
 */
public interface MessageService {
    List<NodeData> register(NodeData nodeData);
    boolean unregister(NodeData nodeData);
    boolean join(NodeData nodeData, NodeData neighbor);
    boolean leave(NodeData nodeData, NodeData neighbor);
    void search(String filename, NodeData nodeData, NodeData neighbor);

    void startListening(MessageReceivedEvent receivedEvent);
    void stopListening();

    int getReceivedQueries();
    int getForwardedQueries();
    int getAnsweredQueries();
    void resetStatistics();
}
