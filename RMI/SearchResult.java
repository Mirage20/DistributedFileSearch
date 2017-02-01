import java.util.List;

/**
 * Created by Mirage on 2017-01-08.
 */
public class SearchResult {
    private boolean isSuccess;
    private List<String> matchingFileNames;
    private NodeData ownerNode;
    private List<NodeData> selectedNeighbours;
    private int hopCount = 0;

    public boolean isSuccess() {
        return isSuccess;
    }

    public List<NodeData> getSelectedNeighbours() {
        return selectedNeighbours;
    }

    public List<String> getMatchingFileNames() {
        return matchingFileNames;
    }

    public void setMatchingFileNames(List<String> matchingFileNames) {
        this.matchingFileNames = matchingFileNames;
    }

    public void setSelectedNeighbours(List<NodeData> selectedNeighbours) {
        this.selectedNeighbours = selectedNeighbours;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public NodeData getOwnerNode() {
        return ownerNode;
    }

    public void setOwnerNode(NodeData ownerNode) {
        this.ownerNode = ownerNode;
    }

    public int getHopCount() {
        return hopCount;
    }


    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }
}
