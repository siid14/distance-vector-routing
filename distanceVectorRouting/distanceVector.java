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
               // checkNeighborTimeout();
            }
        }, 0, updateInterval * 1000); // Convert seconds to milliseconds
    }
   
    // Send updates to all neighbors
    private void sendDistanceVectorUpdates() {
        try {
            byte[] updateMessage = createUpdateMessage(); // create message once
            
            // send same message to each neighbor
            for (Map.Entry<Integer, Integer> neighbor : neighbors.entrySet()) {
                 // create and send UDP packet to this neighbor
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

            
            // create buffer and packet for receiving messages (data)
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            
            while (true) { // continuously listen for messages
                // * 1 LISTEN - WAIT FOR AND RECEIVE INCOMING MESSAGES
                // wait until a packet is received
                serverSocket.receive(receivePacket);
                
                // count rceived packets for 'packets' command
                packetsReceived.incrementAndGet();

                // * 2 GET MESSAGE DATA - CONVERRT DATA TO READABLE FORMAT
                // convert received data into ByteBuffer for easier parsing
                ByteBuffer buffer = ByteBuffer.wrap(receivePacket.getData(), 0, receivePacket.getLength());
                
                try {
                    // * 3 READ MESSAGE HEADER - GET SENDER INFO
                    // parse message header according to specific format

                    // first 2 bytes: number of updates i this msg
                    short numUpdates = buffer.getShort();

                    // next 2bytes: sender port number
                    short senderPort = buffer.getShort();

                    // next 4 bytes: sender IP address
                    byte[] ipBytes = new byte[4];
                    buffer.get(ipBytes);
                    InetAddress senderIP = InetAddress.getByAddress(ipBytes);
                    
                    // * 4 IDENTIFY SENDER - COMPARE WITH KNOWN SERVERS
                    // find which server sent this message by matching IP and port
                    int senderId = -1;
                    for (Map.Entry<Integer, ServerInfo> entry : serverInfo.entrySet()) {

                        // check if this server's port and IP match the sender's
                        if (entry.getValue().port == senderPort && // compare ports
                            entry.getValue().ip.equals(senderIP.getHostAddress())) { // compare IP addresses
                            
                            // if we found a match, store the server ID and exit loop
                            senderId = entry.getKey();  // get the ID of the sending server
                            break; // no need to check other servers
                        }
                    }

                    // * 5 PROCESS MESSAGE - IF SERVER FOUND -> PROCESS MESSAGE UDPATE
                    if (senderId != -1) {  // If we found the sender
                        // print required message
                        System.out.println("RECEIVED A MESSAGE FROM SERVER " + senderId);
                        
                        // update timestamp for neighbor timeout detection
                        lastUpdateTime.put(senderId, System.currentTimeMillis());
                        
                        // * 6 READ UPDATES MSG - PROCESS EACH UPDATE IN THE MESSAGE
                        // process each update in the message
                        for (int i = 0; i < numUpdates; i++) {
                            // read destination server information:
                            buffer.get(ipBytes);           // IP address (4 bytes)
                            short destPort = buffer.getShort();  // port (2 bytes)
                            buffer.getShort();             // skip padding (2 bytes)
                            short destId = buffer.getShort();    // server ID (2 bytes)
                            short cost = buffer.getShort();      // cost (2 bytes)


                            // * 7 APPLY BELLMAN-FORD EQUATION - UPDATE ROUTING TABLE
                            updateRoutingTable(senderId, destId, cost);
                             
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing message: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error receiving message: " + e.getMessage());
                }
    }

                // * 8 BELLMAN-FORD: Update routing if better path found
                private void updateRoutingTable(int viaNode, int destNode, int receivedCost) {
                    // get the cost to reach the neighbor (viaNode) from our routing table
                    // viaNode: the server that sent us the update
                    Integer costToVia = neighbors.get(viaNode);
                    
                    // if this node isn't our neighbor, ignore the update
                    // this prevents updates from non-neighboring nodes
                    if (costToVia == null) return;
                    
                    // calculate total cost to reach destNode through viaNode
                    // total cost = (our cost to reach neighbor) + (neighbor's cost to reach destination)
                    int newCost = costToVia + receivedCost;  
                    
                    // get our current known cost to reach destNode
                    Integer currentCost = routingTable.get(destNode);
                    
                    // if we never seen this destination before, set current cost to infinity
                    // this ensures first path to new destination is always accepted
                    if (currentCost == null) currentCost = Integer.MAX_VALUE;
                    
                    // Bellman-Ford - if new path is cheaper than current path
                    if (newCost < currentCost) {
                        // update routing table with new, lower cost to destination
                        routingTable.put(destNode, newCost);
                        
                        // update next hop table to route through viaNode to reach destNode
                        // this records that to reach destNode, we should forward to viaNode
                        nextHopTable.put(destNode, viaNode);
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
                            displayRoutingTable();
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

    private void displayRoutingTable() {
        System.out.println("Routing Table:");
        System.out.println("Destination | Next Hop | Cost");
        routingTable.keySet().stream()
            .sorted()
            .forEach(dest -> {
                int nextHop = nextHopTable.get(dest);
                int cost = routingTable.get(dest);
                System.out.printf("%11d | %8d | %4d%n", dest, nextHop, cost);
            });
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