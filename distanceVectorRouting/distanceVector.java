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