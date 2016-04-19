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
    public final static String serverAddr = "192.168.1.4";
    public final static int serverPort = 7734;
    public final static String version = "P2P-CI/1.0";

    public static ServerSocket serverMain;

    public Server(){
    }

    public static void main(String[] args) throws IOException {

        //Initialization
        serverMain = new ServerSocket(serverPort);
        System.out.println("ServerMain: Server is running.");
        System.out.println("ServerMain: Version: "+Server.version+"   Port: "+Server.serverPort);
        System.out.println("ServerMain: Listening...");
        activePeers = new LinkedList();
        peerRFCs = new LinkedList();

        while(true){
            Socket curr  = serverMain.accept();
            try
            {

                Thread t = new Thread(new ServerToClient(curr));
                t.start();
                // System.out.println("Get one..."+"Accept Client:  " + socket);
                // BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                // while(true) {
                //     String str = in.readLine();
                //     if(str.equals("BYE"))
                //     {
                //         break;
                //     }
                //     System.out.println(str);
                //     out.println("Hello");
                // }
            }
            catch(Exception e){
                System.out.println(e);
            }
        }
    }

    static void addNewPeer(String currPeerAddr, int currPeerPort) {
        System.out.println("ServerMain: New Active Peers:\n" +
                currPeerAddr + " Upload Port#: " + currPeerPort);

        String[] tmp = currPeerAddr.split(":");
        String host = tmp[0];

        boolean exist = false;
        for(activePeer i:activePeers){
            if(i.gethostName().equals(host) && i.getportNumber() == currPeerPort){
                System.out.println("ServerMain: activePeer Exist!");
                exist = true;
            }
        }

        if(!exist)
            activePeers.add(new activePeer(host, currPeerPort));

//        System.out.println("ServerMain: Now Active Peers: ");
//        for(activePeer i:activePeers)
//            System.out.println(i.gethostName()+" "+i.getportNumber());
    }

    static void addPeerRFCs (peerRFC rfc) {
        boolean exist = false;

        for(peerRFC i:peerRFCs)
            if (i.gettitle().equals(rfc.gettitle()) && i.numRFC() == rfc.numRFC() && i.hostName().equals(rfc.hostName())){
                exist = true;
                System.out.println("ServerMain: peerRFC <RFC " + rfc.numRFC() + " " + rfc.gettitle() + "> Exist!");
                break;
            }

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

    static String lookupRFCs (int numrfc, String title) {
        // System.out.println("Server Trying to Find: "+"="+numrfc+"="+title+"=");
        String res = version + " 200 OK\n";
        boolean found = false;

        for(peerRFC i:peerRFCs){
            if(i.numRFC() == numrfc || i.gettitle().equals(title)){
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

    static void remove (String reHost) {
//        System.out.println("Host: " + reHost + " is going to Leave");

        Iterator i;
        i = peerRFCs.listIterator();
        while(i.hasNext()) {
            peerRFC tmp = (peerRFC)i.next();
            if(tmp.hostName().equals(reHost))
                i.remove();
        }

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
