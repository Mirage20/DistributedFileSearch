import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Created by Mirage on 2017-01-08.
 */
public final class Configuration {
    public static final String BOOTSTRAP_IP;
    public static final int BOOTSTRAP_PORT;
    public static final int HOPS_MAX;
    public static final String FILE_NAMES_PATH;
    public static final String QUERIES_PATH;
    public static final String NODE_IP;
    public static final int NODE_PORT;
    public static final String NODE_USERNAME;
    public static final int LISTENER_TIMEOUT;
    public static final long BENCHMARK_TIMEOUT;

    static {

        Properties properties = new Properties();

        try {

            FileInputStream fileInputStream = new FileInputStream(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getSchemeSpecificPart() + "config.properties");
            properties.load(fileInputStream);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        BOOTSTRAP_IP = properties.getProperty("bootstrap.ip", "127.0.0.1");
        BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bootstrap.port", "55555"));
        HOPS_MAX = Integer.parseInt(properties.getProperty("hops.max", "2"));
        FILE_NAMES_PATH = properties.getProperty("file.list");
        QUERIES_PATH = properties.getProperty("query.list");

        NODE_IP = properties.getProperty("node.ip", "127.0.0.1");
        NODE_PORT = Integer.parseInt(properties.getProperty("node.port", Integer.toString((int) (100 * Math.random() + 8000))));
        NODE_USERNAME = properties.getProperty("node.username", "mirage");
        LISTENER_TIMEOUT = Integer.parseInt(properties.getProperty("listener.timeout", "5000"));
        BENCHMARK_TIMEOUT = Integer.parseInt(properties.getProperty("benchmark.timeout", "1000"));

    }
}
