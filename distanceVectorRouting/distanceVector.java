package distanceVectorRouting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * main class for Distance Vector Routing implementation
 */
public class distanceVector {
    private int serverId;
    private String serverIp;
    private int serverPort;
    private int updateInterval;
    private DatagramSocket serverSocket;
    private Map<Integer, ServerInfo> serverInfo; // stores all server information
    private Map<Integer, Integer> neighbors;     // stores neighbor costs
    private int numServers;
    private int numNeighbors;

    // store server information
    public static class ServerInfo{
        String ip;
        int port;

        ServerInfo(String ip, int port){
            this.ip = ip;
            this.port = port;
    }}
   
    // constructor
    public distanceVector(String topologyFile, int updateInterval) {
        this.updateInterval = updateInterval;
        this.serverInfo = new HashMap<>();
        this.neighbors = new HashMap<>();

        // load topology first to get server informations
        loadTopology(topologyFile);

        initializeServer();
    }
    
    
    private void initializeServer() {
        try {
            // create UDP socket bound to this server's port
            serverSocket = new DatagramSocket(serverPort);
            System.out.println("Server " + serverId + " started on " + serverIp + ":" + serverPort);
        } catch (Exception e) {
            System.err.println("Failed to initialize server: " + e.getMessage());
            System.exit(1);
        }
    }
    
  
    private void loadTopology(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
        // read number of servers and neighbors
        numServers = Integer.parseInt(reader.readLine().trim());
        numNeighbors = Integer.parseInt(reader.readLine().trim());
        
        // read server information
        for (int i = 0; i < numServers; i++) {
            String[] parts = reader.readLine().trim().split(" ");
            int id = Integer.parseInt(parts[0]);
            String ip = parts[1];
            int port = Integer.parseInt(parts[2]);
            
            // store server information
            serverInfo.put(id, new ServerInfo(ip, port));
            
            // if this is our server information, save it
            if (i == 0) {  // Assuming this server info is first in the file
                serverId = id;
                serverIp = ip;
                serverPort = port;
            }
        }
        
        // read neighbor information
        for (int i = 0; i < numNeighbors; i++) {
            String[] parts = reader.readLine().trim().split(" ");
            int server1 = Integer.parseInt(parts[0]);
            int server2 = Integer.parseInt(parts[1]);
            int cost = Integer.parseInt(parts[2]);
            
            // only store if this server is involved
            if (server1 == serverId) {
                neighbors.put(server2, cost);
            }
        }
        
        System.out.println("Topology loaded successfully");
        
    } catch (IOException e) {
        System.err.println("Error loading topology: " + e.getMessage());
        System.exit(1);
    }
}
    
    
    private void listenForMessages() {
        try {
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            
            while (true) {
                // wait for and receive packet
                serverSocket.receive(receivePacket);
                
                // process received data
                String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("RECEIVED A MESSAGE FROM SERVER " + 
                    receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort());
                
                // TODO: implement proper message processing
            }
        } catch (IOException e) {
            System.err.println("Error receiving message: " + e.getMessage());
        }
    }
    

    private void start() {
        try {
            // start listening for messages in a separate thread
            Thread listenThread = new Thread(this::listenForMessages);
            listenThread.setDaemon(true);
            listenThread.start();
            
            try (// start command line interface
            Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("Enter command >> ");
                    String command = scanner.nextLine().trim();
                    String[] parts = command.split(" ");
                    String action = parts[0].toLowerCase();
                    switch (action) {
                        case "help":
                            displayHelp();
                            break;
                        case "update":
                            if (parts.length != 4) {
                                System.out.println("Usage: update <server ID 1> <server ID 2> <Link Cost>");
                                break;
                            }
                            int serverId1 = Integer.parseInt(parts[1]);
                            int serverId2 = Integer.parseInt(parts[2]);
                            int linkCost = Integer.parseInt(parts[3]);
                            break;
                        case "step":
                            break;
                        case "packets":
                            break;
                        case "display":
                            break;
                        case "disable":
                            if (parts.length != 2) {
                                System.out.println("Usage: disable <server ID>");
                                break;
                            }
                            int disableServerId = Integer.parseInt(parts[1]);
                            break;
                        case "crash":
                            break;
                        default:
                            System.out.println("Unknown command. Here's the list of commands");
                            displayHelp();
                            break;
                    }
                    
                }

            }
            
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
    private void displayHelp(){
        System.out.println("Information about built in commands: \n\n");
        System.out.println("\thelp: Displays information about the available user interface options or manual.\n");
        System.out.println("\tupdate <server-ID1> <server-ID2> <Link Cost>: server-ID1, server-ID2: The link for which the cost is being updated.\n" + //
                        "Link Cost: It specifies the new link cost between the source and the destination server. Note\n" + //
                        "that this command will be issued to both server-ID1 and server-ID2 and involve them to\n" + //
                        "update the cost and no other server.\n");
        System.out.println("\tstep: Send routing update to neighbors right away. Note that except this, routing updates only happen periodically.\n");
        System.out.println("\tpackets: Display the number of distance vector packets this server has received since the last invocation of this information\n");
        System.out.println("\tdisplay: Display the current routing table. And the table should be displayed in a sorted order from small ID to big.\n");
        System.out.println("\tdisable <server-ID>: Disable the link to a given server. Doing this “closes” the connection to a given server with server-ID. Here you need to check if the given server is its neighbor\n");
        System.out.println("\tcrash: Close” all connections. This is to simulate server crashes. Close all connections on all links. The neighboring servers must handle this close correctly and set the link cost to infinity.\n");
    }
    public static void main(String[] args) {
        // * validate command line arguments:
        // must have exactly 4 arguments
        // first argument must be "-t" (topology file flag)
        // third argument must be "-i" (interval flag)
        if (args.length != 4 || !args[0].equals("-t") || !args[2].equals("-i")) {
            System.out.println("Usage: java distanceVector -t <topology-file-name> -i <routing-update-interval>");
            System.exit(1);
        }
        
        String topologyFile = args[1];
        int updateInterval = Integer.parseInt(args[3]);
        
        distanceVector server = new distanceVector(topologyFile, updateInterval);
        server.start();
    }
}