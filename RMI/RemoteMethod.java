import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Mirage on 2017-01-21.
 */
public interface RemoteMethod extends Remote {
    boolean join(String neighborIP, int neighbourPort) throws RemoteException;;
    boolean leave(String neighborIP, int neighbourPort) throws RemoteException;;
    void search(String filename, String searchNodeIP, int searchNodePort, String neighborIP, int neighbourPort, int hops) throws RemoteException;;
    void searchSuccess(String ownerIP, int ownerPort,List<String> matchingFileNames, int hops) throws RemoteException;

}
