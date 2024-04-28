import java.net.*;
import java.io.*;

class client {
      public static void main(String args[]) throws Exception {
            // Establish a socket connection with the server
            Socket socket = new Socket("localhost", 50000);

            // Create output stream to send data to the server
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            // Create input stream reader to read data from the server
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String receivedMessage = "";
            String currentMessage = "";

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

            if (receivedMessage.equals("OK")) {
                  System.out.println("Authentication successful.");

                  // Send REDY message to request a job from the server
                  outputStream.write(("REDY\n").getBytes());
                  outputStream.flush();

                  // Receive job information from the server
                  receivedMessage = inputReader.readLine();

                  // Initialize variables to store job and server information
                  int jobID = 0;
                  int numOfRecords = 0;
                  int jobCores = 0;
                  int jobMemory = 0;
                  int jobDisk = 0;
                  int serverCores = 0;
                  int serverMemory = 0;
                  int serverDisk = 0;
                  int scheduledServerID = 0;
                  int waitingJobs = 0;
                  int firstServerID = 0;
                  String serverType = "";
                  String firstServerType = "";
                  String[] dataArray = null;

                  // Process jobs received from the server
                  while (!receivedMessage.equals("NONE")) {
                        boolean isFirst = true;
                        boolean isJobScheduled = false;

                        // Send REDY message to request another job from the server
                        outputStream.write(("REDY\n").getBytes());
                        currentMessage = inputReader.readLine();

                        // Check if the received message contains job information
                        if (currentMessage.contains("JOBN") || currentMessage.contains("JOBP")) {
                              // Parse the job information
                              dataArray = currentMessage.split(" ");
                              jobID = Integer.parseInt(dataArray[2]);
                              jobCores = Integer.parseInt(dataArray[4]);
                              jobMemory = Integer.parseInt(dataArray[5]);
                              jobDisk = Integer.parseInt(dataArray[6]);

                              // Send GETS message to request server capabilities from the server
                              outputStream.write(("GETS Capable " + jobCores + " " + jobMemory + " " + jobDisk + "\n")
                                          .getBytes());
                              outputStream.flush();

                              // Receive server capabilities from the server
                              receivedMessage = inputReader.readLine();

                              // Parse the server capabilities response
                              String[] dataArray2 = receivedMessage.split(" ");
                              numOfRecords = Integer.parseInt(dataArray2[1]);

                              // Send OK message to confirm receiving server capabilities
                              sendOK(outputStream);

                              // Process each server capability
                              for (int i = 0; i < numOfRecords; i++) {
                                    String[] dataArray3 = null;
                                    receivedMessage = inputReader.readLine();
                                    dataArray3 = receivedMessage.split(" ");

                                    if (isFirst) {
                                          firstServerType = dataArray3[0];
                                          firstServerID = Integer.parseInt(dataArray3[1]);
                                          isFirst = false;
                                    }

                                    serverCores = Integer.parseInt(dataArray3[4]);
                                    serverMemory = Integer.parseInt(dataArray3[5]);
                                    serverDisk = Integer.parseInt(dataArray3[6]);

                                    waitingJobs = Integer.parseInt(dataArray3[7]);

                                    // Check if there are no waiting jobs and the current server has enough
                                    // resources for the job
                                    if (waitingJobs == 0 && jobCores <= serverCores && jobMemory <= serverMemory
                                                && jobDisk <= serverDisk && !isJobScheduled) {
                                          serverType = dataArray3[0];
                                          scheduledServerID = Integer.parseInt(dataArray3[1]);
                                          isJobScheduled = true;
                                    }
                              }

                              // Send OK message to confirm receiving server capabilities
                              sendOK(outputStream);
                              receivedMessage = inputReader.readLine();

                              // If no suitable server found, assign the job to the first server in the list
                              if (!isJobScheduled) {
                                    serverType = firstServerType;
                                    scheduledServerID = firstServerID;
                              }

                              // Send SCHD message to schedule the job on a server
                              outputStream.write(("SCHD " + jobID + " " + serverType + " " + scheduledServerID + "\n")
                                          .getBytes());
                              outputStream.flush();

                              // Receive scheduling response from the server
                              receivedMessage = inputReader.readLine();
                        } else {
                              receivedMessage = currentMessage;
                        }
                  }

                  // No more jobs to schedule
                  System.out.println("No more jobs to schedule. Waiting jobs: " + waitingJobs);
            } else {
                  System.out.println("Authentication failed.");
            }

            // Send QUIT message to terminate the simulation gracefully
            sendQUIT(outputStream, inputReader);

            // Close the streams and socket
            outputStream.close();
            socket.close();
      }

      // Helper method to send OK message to the server
      static void sendOK(DataOutputStream dout) throws Exception {
            dout.write(("OK\n").getBytes());
            dout.flush();
      }

      // Helper method to send QUIT message to the server and receive termination
      // response
      static void sendQUIT(DataOutputStream outputStream, BufferedReader inputReader) throws Exception {
            outputStream.write(("QUIT\n").getBytes());
            outputStream.flush();

            // Receive termination response from the server
            String receivedMessage = inputReader.readLine();

            if (receivedMessage.equals("QUIT")) {
                  System.out.println("Simulation terminated gracefully.");
            }
      }
}
