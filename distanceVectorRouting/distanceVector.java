package distanceVectorRouting;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;


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
    private Timer updateTimer;
    private AtomicInteger packetsReceived = new AtomicInteger(0);
    private Map<Integer, Long> lastUpdateTime = new HashMap<>(); // track last update time from each neighbor
    private Map<Integer, Integer> routingTable = new HashMap<>(); // destination -> cost
    private Map<Integer, Integer> nextHopTable = new HashMap<>(); // destination -> next Hop

    // store server information
    public static class ServerInfo{
        String ip;
        int port;

        ServerInfo(String ip, int port){
            this.ip = ip;
            this.port = port;
        }
    }

    // initialize periodic updates in constructor after loading topology
    private void initializePeriodicUpdates() {
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendDistanceVectorUpdates();
                checkNeighborTimeout();
            }
        }, 0, updateInterval * 1000); // Convert seconds to milliseconds
    }
   
    // Send updates to all neighbors
    private void sendDistanceVectorUpdates() {
        try {
            byte[] updateMessage = createUpdateMessage();
            
            // Send to each neighbor
            for (Map.Entry<Integer, Integer> neighbor : neighbors.entrySet()) {
                int neighborId = neighbor.getKey();
                ServerInfo neighborInfo = serverInfo.get(neighborId);
                
                if (neighborInfo != null) {
                    DatagramPacket packet = new DatagramPacket(
                        updateMessage,
                        updateMessage.length,
                        InetAddress.getByName(neighborInfo.ip),
                        neighborInfo.port
                    );
                    serverSocket.send(packet);
                }
            }
        } catch (IOException e) {
            System.err.println("Error sending updates: " + e.getMessage());
        }
    }

     // create the update message in specified format
    private byte[] createUpdateMessage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // number of update fields (including self)
        dos.writeShort(routingTable.size() + 1);
        
        // server port
        dos.writeShort(serverPort);
        
        // server IP (4 bytes)
        byte[] ipBytes = InetAddress.getByName(serverIp).getAddress();
        dos.write(ipBytes);
        
        // add entry for self
        dos.write(ipBytes);  // server IP
        dos.writeShort(serverPort);  // server port
        dos.writeShort(0);  // padding
        dos.writeShort(serverId);  // sserver ID
        dos.writeShort(0);  // cost to self = 0
        
        // add entries for all destinations
        for (Map.Entry<Integer, Integer> entry : routingTable.entrySet()) {
            int destId = entry.getKey();
            if (destId != serverId) { // skip self as it's already added
                ServerInfo destInfo = serverInfo.get(destId);
                if (destInfo != null) {
                    dos.write(InetAddress.getByName(destInfo.ip).getAddress());
                    dos.writeShort(destInfo.port);
                    dos.writeShort(0);  // padding
                    dos.writeShort(destId);
                    dos.writeShort(entry.getValue());
                }
            }
        }
        
        return baos.toByteArray();

    }

    // constructor
    public distanceVector(String topologyFile, int updateInterval) {
        this.updateInterval = updateInterval;
        this.serverInfo = new HashMap<>();
        this.neighbors = new HashMap<>();

        // load topology first to get server informations
        loadTopology(topologyFile);

        initializeServer();
    }

    // converts IPs for generateUpdatePacket
    private int convertIPtoNumber(String ip) {
        try {
            InetAddress i = InetAddress.getByName(ip);
            int intIP = ByteBuffer.wrap(i.getAddress()).getInt();
            return intIP;
        } catch(UnknownHostException e) {
            System.out.println("There is no host ip.");
            return 0;
        }
    }
    
    // message format
    private DatagramPacket generateUpdatePacket(InetAddress address, int port) {
        ByteBuffer buffer = ByteBuffer.allocate(20*numServers);
        buffer.putShort((short)numServers);
        buffer.putShort((short)serverPort);
        buffer.putInt(convertIPtoNumber(serverIp));
        for (Map.Entry<Integer,ServerInfo> bufEntry : serverInfo.entrySet()) {
            int serverID = bufEntry.getKey();
            ServerInfo serverInfo = bufEntry.getValue();

            buffer.putInt(convertIPtoNumber(serverInfo.ip));
            buffer.putShort((short)serverInfo.port);
            buffer.putShort((short)serverID);
            int cost = neighbors.get(serverID);
            buffer.putInt(cost);
        }
        byte[] data = buffer.array();
        return new DatagramPacket(data, data.length, address, port);
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