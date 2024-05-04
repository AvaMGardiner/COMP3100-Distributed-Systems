import java.net.*;
import java.io.*;

class Client {
    public static void main(String args[]) throws Exception {
        // Establish a socket connection with the server
        Socket socket = new Socket("localhost", 50000);

        // Create output stream to send data to the server
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        // Create input stream reader to read data from the server
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String receivedMessage = "";

        // Send initial HELO message to the server
        outputStream.write(("HELO\n").getBytes());
        outputStream.flush();

        // Receive response from the server
        receivedMessage = inputReader.readLine();

        // Get the username of the current user
        String username = System.getProperty("user.name");

        // Send authentication message with username to the server
        outputStream.write(("AUTH " + username + "\n").getBytes());
        outputStream.flush();

        // Receive authentication response from the server
        receivedMessage = inputReader.readLine();

        // Initialize variables for job scheduling
        int jobID = 0;
        int jobCores = 0;
        int maxJobCores = 0;
        int numOfRecords = 0;
        int n = 0;
        String maxJobType = "";
        String serverResponse = "";
        String[] jobFields = null;
        boolean isFirst = true;

        // Continue until there are no more jobs
        while (!receivedMessage.equals("NONE")) {
            // Request a job from the server
            serverResponse = exchangeMessages(outputStream, inputReader, "REDY\n");

            // If this is the first time, get all the jobs and find the type with the most
            // cores
            if (isFirst) {
                receivedMessage = exchangeMessages(outputStream, inputReader, "GETS All\n");
                jobFields = receivedMessage.split(" ");
                numOfRecords = Integer.parseInt(jobFields[1]);
                send(outputStream, "OK\n");

                // Go through all the jobs and compare their cores
                for (int i = 0; i < numOfRecords; i++) {
                    String[] jobFields2 = null;
                    receivedMessage = receive(inputReader);
                    jobFields2 = receivedMessage.split(" ");
                    jobCores = Integer.parseInt(jobFields2[4]);
                    if (jobCores > maxJobCores) {
                        maxJobType = jobFields2[0];
                        maxJobCores = jobCores;
                    }
                    if (maxJobType.equals(jobFields2[0]))
                        n = Integer.parseInt(jobFields2[1]);
                }
                n++;
                receivedMessage = exchangeMessages(outputStream, inputReader, "OK\n");
                isFirst = false;
            }

            // If the server sent a new job, schedule it to the type with the most cores
            if (serverResponse.contains("JOBN")) {
                // Implementing LRR: Schedule each job to a server of the largest type in a
                // round-robin fashion
                receivedMessage = exchangeMessages(outputStream, inputReader,
                        "SCHD " + jobID + " " + maxJobType + " " + jobID % n + "\n");
                jobID++;
            } else
                receivedMessage = serverResponse;
        }

        // Terminate the connection by sending the QUIT message
        send(outputStream, "QUIT\n");

        // Close the output stream, input stream, and socket
        outputStream.close();
        inputReader.close();
        socket.close();
    }

    // Helper method to send a message to the server and receive a response
    public static String exchangeMessages(DataOutputStream outputStream, BufferedReader inputReader, String message)
            throws IOException {
        send(outputStream, message);
        return receive(inputReader);
    }

    // Helper method to send a message to the server
    public static void send(DataOutputStream outputStream, String message) throws IOException {
        outputStream.write(message.getBytes());
        outputStream.flush();
    }

    // Helper method to receive a message from the server
    public static String receive(BufferedReader inputReader) throws IOException {
        return inputReader.readLine();
    }
}
