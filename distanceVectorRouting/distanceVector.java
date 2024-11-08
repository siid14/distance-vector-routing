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
                    String command = scanner.nextLine();
                    //TODO: implement command processing (update, step, display, etc.)
                    
                }
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    public static void main(String[] args) {
        // * Validate command line arguments:
        // - must have exactly 4 arguments
        // - first argument must be "-t" (topology file flag)
        // - third argument must be "-i" (interval flag)

        // check if we have the correct number of arguments
        // we need exactly 4 args: -t, topology filename, -i, and update interval
        if (args.length != 4) {
            System.err.println("Error: Incorrect number of arguments");
            printUsage();
            System.exit(1);
        }
    
        // validate the topology file flag (-t)
        // first argument must be "-t" to specify the topology file
        if (!args[0].equals("-t")) {
            System.err.println("Error: First argument must be -t");
            printUsage();
            System.exit(1);
        }
    
        // validate the update interval flag (-i)
        // third argument must be "-i" to specify the update interval
        if (!args[2].equals("-i")) {
            System.err.println("Error: Third argument must be -i");
            printUsage();
            System.exit(1);
        }
    
        // get the topology file path from second argument
        String topologyFile = args[1];
        int updateInterval;
    
        // parse and validate the update interval
        // must be a positive integer representing seconds between updates
        try {
            updateInterval = Integer.parseInt(args[3]);
            // ensure the interval is positive
            if (updateInterval <= 0) {
                throw new NumberFormatException("Update interval must be positive");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid update interval - must be a positive integer");
            printUsage();
            System.exit(1);
            return;
        }
    
        // initialize and start the server
        try {
            distanceVector server = new distanceVector(topologyFile, updateInterval);
            server.start();
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: server -t <topology-file-name> -i <routing-update-interval>");
        System.out.println("  -t : topology file flag");
        System.out.println("  <topology-file-name> : name of the file containing network topology");
        System.out.println("  -i : update interval flag");
        System.out.println("  <routing-update-interval> : time between routing table updates in seconds");
    }
}