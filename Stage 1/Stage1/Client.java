import java.net.Socket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class Client {
    public static void main(String[] args) {
        try {
            Socket s = new Socket("127.0.0.1", 50000);

            int numServers = 10;
            int[] serverCoreCounts = new int[numServers];
            int[] serverTypes = new int[numServers];
            int[] serverIds = new int[numServers];

            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            System.out.println("Target IP: " + s.getInetAddress() + " Target Port: " + s.getPort());
            System.out.println("Local IP: " + s.getLocalAddress() + " Local Port: " + s.getLocalPort());

            // Step 1: Send HELO to server
            out.writeUTF("HELO\n");
            out.flush();
            System.out.println("SENT: HELO");

            // Step 2: Wait for OK response from server
            String str = in.readLine();
            if (str.startsWith("OK")) {
                System.out.println("RCVD: " + str);
            } else {
                throw new Exception("Invalid response from server: " + str);
            }

            // Step 3: Send AUTH with authentication information to server
            out.writeUTF("AUTH Ava Gardiner\n");
            out.flush();
            System.out.println("SENT: AUTH Ava Gardiner");

            // Step 4: Wait for OK response from server
            str = in.readLine();
            if (str.startsWith("OK")) {
                System.out.println("RCVD: " + str);
            } else {
                throw new Exception("Invalid response from server: " + str);
            }

            // Step 5: Send REDY to server
            out.writeUTF("REDY\n");
            out.flush();
            System.out.println("SENT: REDY");

            // Step 6: Wait for JOBN, JOBP, JCPL, RESF, RESR, CHKQ, or NONE message from
            // server
            str = in.readLine();
            System.out.println("RCVD: " + str);

            int largestServerType = -1; // initialize to an invalid value
            int largestServerId = -1;
            int largestServerCoreCount = -1;

            while (str.startsWith("JOBN")) {
                // Parse job information
                String[] jobFields = str.split("\\s+");
                int jobId = Integer.parseInt(jobFields[2]);
                int jobSubmitTime = Integer.parseInt(jobFields[3]);
                int jobEstRuntime = Integer.parseInt(jobFields[4]);
                int jobCore = Integer.parseInt(jobFields[5]);

                // Find the largest server that can accommodate the job
                for (int i = 0; i < numServers; i++) {
                    if (serverCoreCounts[i] >= jobCore) {
                        if (largestServerType == -1 || serverTypes[i] > largestServerType ||
                                (serverTypes[i] == largestServerType && serverCoreCounts[i] > largestServerCoreCount)) {
                            largestServerType = serverTypes[i];
                            largestServerId = serverIds[i];
                            largestServerCoreCount = serverCoreCounts[i];
                        }
                    }
                }

                // Send SCHD command to allocate the largest server
                out.writeUTF("SCHD " + jobId + " " + largestServerType + " " + largestServerId + "\n");
                out.flush();

                // Wait for OK response from server
                str = in.readLine();
                if (str.startsWith("OK")) {
                    System.out.println("RCVD: " + str);
                } else {
                    throw new Exception("Invalid response from server: " + str);
                }

                // Wait for another job or server status update
                str = in.readLine();
                System.out.println("RCVD: " + str);
            }

            // Step 7: Send a message to gracefully terminate the simulation
            out.writeUTF("QUIT\n");
            out.flush();
            System.out.println("SENT: QUIT");

            // Step 8: Wait for the server to finish processing remaining jobs and terminate
            str = in.readLine();
            System.out.println("RCVD: " + str);

            in.close();
            out.close();
            s.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }}

    

    
    

    

    

    
    

    

    

    

    

    

    

    

    

    
    
    
    
    
    

    

    
    
    
    
    
        
        
    

    

    

    

    

    
    
        
    
