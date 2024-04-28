import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Client {

    public static void main(String[] args) {

        HashMap<String, Integer> serverCores = new HashMap<>();
        HashMap<String, ArrayList<Job>> serverJobs = new HashMap<>();

        try {
            // Parse the XML configuration file (ds-system.xml) to populate the serverCores
            // map
            File configFile = new File("ds-system.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();

            NodeList serverList = doc.getElementsByTagName("server");
            for (int i = 0; i < serverList.getLength(); i++) {
                Node serverNode = serverList.item(i);
                if (serverNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element serverElement = (Element) serverNode;
                    String type = serverElement.getAttribute("type");
                    int cores = Integer.parseInt(serverElement.getAttribute("coreCount"));
                    serverCores.put(type, cores);
                }
                if (parts[0] != null && !parts[0].isEmpty() && parts[0].matches("\\d+")) {
                    int id = Integer.parseInt(parts[0]);
                }
                if (parts.length > 0 && !parts[0].isEmpty() && parts[0].matches("\\d+")) {
                    int id = Integer.parseInt(parts[0]);
                }

            }

            Socket s = new Socket("127.0.0.1", 50000);

            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            System.out.println("Target IP: " + s.getInetAddress() + " Target Port: " + s.getPort());
            System.out.println("Local IP: " + s.getLocalAddress() + " Local Port: " + s.getLocalPort());

            out.write(("HELO\n").getBytes());
            out.flush();
            System.out.println("SENT: HELO");

            String str = in.readLine();
            System.out.println("RCVD: " + str);

            out.write(("AUTH " + System.getProperty("user.name") + "\n").getBytes());
            out.flush();
            System.out.println("SENT: AUTH " + System.getProperty("user.name"));

            str = in.readLine();
            System.out.println("RCVD: " + str);

            out.write(("REDY\n").getBytes());
            out.flush();
            System.out.println("SENT: REDY");

            while (true) {
                str = in.readLine();
                System.out.println("RCVD: " + str);

                if (str.equals("NONE")) {
                    break;
                }

                String[] parts = str.split("\\s+");
                int id = Integer.parseInt(parts[0]);
                String serverType = parts[4];
                int estRuntime = Integer.parseInt(parts[3]);
                int cores = Integer.parseInt(parts[5]);
                int submitTime = Integer.parseInt(parts[1]);

                Job job = new Job(id, submitTime, estRuntime, cores, serverType);

                // Schedule job to the first server of the largest type
                int largestCores = 0;
                String largestType = "";
                for (Map.Entry<String, Integer> entry : serverCores.entrySet()) {
                    if (entry.getValue() > largestCores) {
                        largestCores = entry.getValue();
                        largestType = entry.getKey();
                    }
                }

                if (!serverJobs.containsKey(largestType)) {
                    serverJobs.put(largestType, new ArrayList<>());
                }
                serverJobs.put(largestType, serverJobs.get(largestType));
                serverJobs.get(largestType).add(job);
                out.write(("SCHD " + job.getId() + " " + largestType + " 0\n").getBytes());
                out.flush();
                System.out.println("SENT: SCHD " + job.getId() + " " + largestType + " 0");
            }

            out.write(("QUIT\n").getBytes());
            out.flush();
            System.out.println("SENT: QUIT");

            str = in.readLine();
            System.out.println("RCVD: " + str);

            in.close();
            out.close();
            s.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Job {
    private int id;
    private int submitTime;
    private int estRuntime;
    private int cores;
    private String serverType;

    public Job(int id, int submitTime, int estRuntime, int cores, String serverType) {
        this.id = id;
        this.submitTime = submitTime;
        this.estRuntime = estRuntime;
        this.cores = cores;
        this.serverType = serverType;
    }

    public int getId() {
        return id;
    }

    public int getSubmitTime() {
        return submitTime;
    }

    public int getEstRuntime() {
        return estRuntime;
    }

    public int getCores() {
        return cores;
    }

    public String getServerType() {
        return serverType;
    }
}
