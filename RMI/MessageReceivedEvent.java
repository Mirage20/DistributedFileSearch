/**
 * Created by Mirage on 2017-01-08.
 */
public interface MessageReceivedEvent {
    boolean onJoin(NodeData nodeData);
    boolean onLeave(NodeData nodeData);
    SearchResult onSearch(String query, NodeData queryNode, NodeData receivedNode);
    void onSearchSuccess(SearchResult result);
}
