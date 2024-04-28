import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class DsClient {

    private String serverAddress;
    private int serverPort;
    private String username;
    private Socket socket;
    private BufferedReader reader;
    private DataOutputStream writer;

    public DsClient(String serverAddress, int serverPort, String username) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.username = username;
    }

    public void run() throws Exception {
        // Create a socket and initialize input and output streams
        socket = new Socket(serverAddress, serverPort);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new DataOutputStream(socket.getOutputStream());

        // Send HELO and receive OK
        writer.writeBytes("HELO\n");
        reader.readLine();

        // Send AUTH and receive OK
        writer.writeBytes("AUTH " + username + "\n");
        reader.readLine();

        String message;
        while ((message = reader.readLine()) != null && !message.equals("NONE")) {
            if (message.equals("JOBN")) {
                // Identify the largest server type using GETS
                writer.writeBytes("GETS All\n");
                String response = reader.readLine();
                String[] parts = response.split("\\s+");
                int nRecs = Integer.parseInt(parts[1]);
                int recSize = Integer.parseInt(parts[2]);
                writer.writeBytes("OK\n");

                String largestServerType = null;
                int largestServerCount = -1;

                for (int i = 0; i < nRecs; i++) {
                    String record = reader.readLine();
                    String[] fields = record.split("\\s+");
                    String serverType = fields[0];
                    int serverCount = Integer.parseInt(fields[1]);

                    if (largestServerType == null || serverCount > largestServerCount) {
                        largestServerType = serverType;
                        largestServerCount = serverCount;
                    }
                }

                writer.writeBytes("OK\n");
                reader.readLine();

                // Schedule the job using SCHD
                String[] jobParts = reader.readLine().split("\\s+");
                String jobID = jobParts[2];
                writer.writeBytes("SCHD " + jobID + " " + largestServerType + " 0\n");

                // Add a new line character
                writer.writeBytes("\n");
            } else {
                writer.writeBytes("REDY\n");
            }
        }

        // Send QUIT and receive QUIT
        writer.writeBytes("QUIT\n");
        reader.readLine();

        // Close the socket
        socket.close();
    }

    public static void main(String[] args) throws Exception {
        DsClient client = new DsClient("127.0.0.1", 50000, "username");
        client.run();
    }
}