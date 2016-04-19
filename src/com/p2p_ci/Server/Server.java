package com.p2p_ci.Server;

/**
 * Created by zhmuch on 2016/3/5.
 */

import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
    
    //Record Info of active peers and their indexes of RFCs.
    private static LinkedList<activePeer> activePeers;
    private static LinkedList<peerRFC> peerRFCs;

    //Parameters of Server
    public final static String serverAddr = "192.168.1.3";
    public final static int serverPort = 7734;
    public final static String version = "P2P-CI/1.0";

    public static ServerSocket serverMain;

    public Server(){
    }

    public static void main(String[] args) throws IOException {

        //  Initialization;
        serverMain = new ServerSocket(serverPort);

//        System.out.println("ServerMain: Server is running.");
        System.out.println("ServerMain: Version: " + Server.version + "   Port: " + Server.serverPort + "  Listening...");

        //  Keep track of client and its files;
        activePeers = new LinkedList();
        peerRFCs = new LinkedList();

        while(true){

            //  Block until new client come in;
            Socket curr  = serverMain.accept();

            //  Create a new thread to handle with the new client;
            try {

                Thread t = new Thread(new ServerToClient(curr));
                t.start();

            }
            catch(Exception e){
                System.out.println(e);
            }
        }

    }

    /**
     * This function will add a new comming client to the active Peer List;
     * @param currPeerAddr
     * @param currPeerPort
     */
    static void addNewPeer(String currPeerAddr, int currPeerPort) {

//        System.out.println("ServerMain: New Active Peers:\n" +
//                currPeerAddr + " Upload Port#: " + currPeerPort);

        String[] tmp = currPeerAddr.split(":");
        String host = tmp[0];

        //  Check if the client is already in the list;
        boolean exist = false;
        for(activePeer i:activePeers){
            if(i.gethostName().equals(host) && i.getportNumber() == currPeerPort){
                System.out.println("ServerMain: activePeer Exist!");
                exist = true;
            }
        }

        //  If not, then add this client info into the active Peer List;
        if(!exist)
            activePeers.add(new activePeer(host, currPeerPort));

//        System.out.println("ServerMain: Now Active Peers: ");
//        for(activePeer i:activePeers)
//            System.out.println(i.gethostName()+" "+i.getportNumber());

    }

    /**
     * This function will add a new comming client`s RFC files to the Peer RFC list;
     * @param rfc
     */
    static void addPeerRFCs (peerRFC rfc) {

        // Check if this RFC file with this client info is already in the list;
        boolean exist = false;

        for(peerRFC i:peerRFCs)
            if (i.gettitle().equals(rfc.gettitle()) && i.numRFC() == rfc.numRFC() && i.hostName().equals(rfc.hostName())){
                exist = true;
                System.out.println("ServerMain: peerRFC <RFC " + rfc.numRFC() + " " + rfc.gettitle() + "> Exist!");
                break;
            }

        //  If not, then add it to the list;
        if(!exist)
            peerRFCs.add(rfc);

//        System.out.println("ServerMain: Successfully added!");
//        System.out.println("Server Main: Now peerRFCs has: ");
//        for(peerRFC i:peerRFCs){
//            System.out.println(i.gettitle());
//            System.out.println(i.hostName());
//            System.out.println(i.numRFC());
//        }

    }

    /**
     * This function will lookup a certain RFC num file with a certain name(option);
     * @param numrfc
     * @param title
     * @return
     */
    static String lookupRFCs (int numrfc, String title) {

        // System.out.println("Server Trying to Find: "+"="+numrfc+"="+title+"=");
        String res = version + " 200 OK\n";
        boolean found = false;

        for(peerRFC i:peerRFCs){
            if(i.numRFC() == numrfc || i.gettitle().equals(title)){             //Decide numrfc match or title match or both;
                for(activePeer j:activePeers)
                    if(j.gethostName().equals(i.hostName())) {
                        found = true;
                        res = res + "RFC" + " " + i.numRFC() + " " + i.gettitle() + " " + i.hostName() + " " + j.getportNumber() + "\n";
                    }
            }
        }

        if(found)
            return res;
        else
            return version + " 404 Not Found";

    }

    /**
     * This function will list all rfc records kept in the Server;
     * @param param can be modified list for a certain file or a certain client;
     * @return
     */
    static String list (String param) {

        // System.out.println("Server Trying to ListAll");
        String res = version + " 200 OK\n";
        boolean found = false;

        switch (param) {
            case "ALL":
//                System.out.println("Server Trying to List All");
                for(activePeer i:activePeers)
                    for(peerRFC j:peerRFCs)
                        if(i.gethostName().equals(j.hostName())){
                            found = true;
                            res = res + "RFC" + " " + j.numRFC() + " " + j.gettitle() + " " + j.hostName() + " " + i.getportNumber() + "\n";
                        }
                break;
            default:
                break;
        }

        if(found)
            return res;
        else
            return version + " 404 Not Found";

    }

    /**
     * This function will get executed when a client trying to leave this P2P server,
     * it will delete all the infomation about this client and its rfc files;
     * @param reHost
     */
    static void remove (String reHost) {
//        System.out.println("Host: " + reHost + " is going to Leave");

        //  Remove RFC file record;
        Iterator i;
        i = peerRFCs.listIterator();
        while(i.hasNext()) {
            peerRFC tmp = (peerRFC)i.next();
            if(tmp.hostName().equals(reHost))
                i.remove();
        }

        //  Remove active Client record;
        Iterator j;
        j = activePeers.listIterator();
        while(j.hasNext()) {
            activePeer tmp = (activePeer)j.next();
            if(tmp.gethostName().equals(reHost))
                j.remove();
        }

//        System.out.println("After Leave:\n" +
//                "activePeers:");
//        for(activePeer k:activePeers)
//            System.out.println(k.gethostName() + " " + k.getportNumber());
//        System.out.println("peerRFCs Records: ");
//        for(peerRFC k:peerRFCs)
//            System.out.println(k.hostName() + " " + k.gettitle());

    }

}

class activePeer {
    private String hostName;
    private int portNumber;

    public activePeer(String hostName, int portNumber) {
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

    public String gethostName() {
        return hostName;
    }

    public int getportNumber() {
        return portNumber;
    }

}

class peerRFC {
    private int numRFC;
    private String title;
    private String hostName;

    public peerRFC (int numRFC, String title, String hostName) {
        this.numRFC = numRFC;
        this.title = title;
        this.hostName = hostName;
    }

    public int numRFC() {
        return numRFC;
    }

    public String gettitle() {
        return title;
    }

    public String hostName() {
        return hostName;
    }

}
