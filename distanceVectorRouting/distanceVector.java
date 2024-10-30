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

 
   
    // constructor
    public distanceVector(String topologyFile, int updateInterval) {
        
        //TODO: implement server initialization
        initializeServer();
        
        //TODO: implement topology file parsing
        loadTopology(topologyFile);
    }
    
    
    private void initializeServer() {
       //TODO: implement server socket initialization
    }
    
  
    private void loadTopology(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            //TODO: implement topology file parsing
        } catch (IOException e) {
            System.err.println("Error loading topology: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void listenForMessages() {
       //TODO: Implement message listening functionality
    }
    

    private void start() {
        
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