package com.p2p_ci.Client;

/**
 * Created by zhmuch on 2016/3/5.
 */
import java.net.*;
import java.io.*;
import java.util.*;


public class Client {

    public static String serverAddress = "192.168.1.4";
    public static int serverPort = 7734;

    public final static String version = "P2P-CI/1.0";
    public final static String localDirectory = "D:\\rfc\\";
    public final static int localUploadPort = 6666;

    public static String localHost;
    public static int localPort;
    public static boolean flag = false;

    private static LinkedList<localRFC> localRFCs = new LinkedList<>();
    private static DataInputStream in;
    private static DataOutputStream out;

    public static void main(String[] args) throws IOException {

        File file = new File(localDirectory);
        String files[] = file.list();

        for (int i = 0; i < files.length; i++) {
            String[] tmp = files[i].split("_");
            if (tmp.length != 2)
                System.out.println("Local RFC File Name Error! " + files[i]);

            int rfcNum = Integer.parseInt((tmp[0].split(" "))[1]);
            String title = tmp[1];
            localRFCs.add(new localRFC(rfcNum, title));
        }
        //        System.out.println("Local RFCs Lists: ");
        //        for(localRFC i:localRFCs){
        //            System.out.println(i.numRFC());
        //            System.out.println(i.gettitle());
        //        }

        try {
            Socket clientMain = new Socket(serverAddress, serverPort);

            in = new DataInputStream(clientMain.getInputStream());
            out = new DataOutputStream(clientMain.getOutputStream());

            localHost = ("" + clientMain.getLocalAddress()).substring(1);
            localPort = Integer.parseInt("" + clientMain.getLocalPort());

            for (localRFC i : localRFCs) {
                String tmp = "ADD RFC " + i.numRFC() + " " + version + "\n" +
                        "Host: " + localHost + "\n" +
                        "Port: " + localUploadPort + "\n" +
                        "Title: " + i.gettitle() + "\n";
//                System.out.println("Reg Info:\n" + tmp);
                out.writeUTF(tmp);
                out.writeUTF("EndOfMsg");
            }
            out.writeUTF("EndOfReg");
//            System.out.println("regInfo: ");
//            System.out.println(regInfo);

            //Greeting Message
            String greetMsg = "";
            while (true) {
                String tmp = in.readUTF();
                if (tmp.equals("EndOfMsg"))
                    break;
                greetMsg = greetMsg + tmp;
            }
            System.out.println("Successfully Registered at " + clientMain.getInetAddress());
            System.out.println(greetMsg);

            //Run upload server process
            ServerSocket peerU = new ServerSocket(localUploadPort);
            Thread upload = new Thread(new PeerToPeer(localHost, localUploadPort, peerU));
            upload.start();

            waitRequest();

            clientMain.close();
            peerU.close();
            System.out.println("Successfully Disconnected!");
            flag = true;
        }
        catch (Exception t){
            System.out.println(t);
        }
    }

    private static void waitRequest() throws Exception {
        boolean exit = false;

        while(true){

//            System.out.println("Please Enter New Request: \n" +
//                    "ADD, to add a locally available RFC to the server`s index,\n" +
//                    "LOOKUP, to find peers that have the specified RFC,\n" +
//                    "LIST, to request the whole index of RFCs from the server,\n" +
//                    "DOWNLOAD, to download a RFC file from another peer,\n" +
//                    "EXIT, to disconnect from the P2P-CI server.");

            BufferedReader keybd = new BufferedReader(new InputStreamReader(System.in));
            String type = keybd.readLine();
//            System.out.println("Request Type: "+type);

            boolean waitForServer = true;

            switch (type){
                case "ADD":
                    System.out.println("Please Enter RFC Number: ");
                    int numRFCAdd = Integer.parseInt(keybd.readLine());

                    String addRFC = "";
                    boolean found = false;
                    for (localRFC i : localRFCs)
                        if(i.numRFC() == numRFCAdd){
                            addRFC = i.gettitle();
                            found = true;
                            break;
                        }

                    String addMsg;
                    if(found) {
                        addMsg = "ADD RFC " + numRFCAdd + " " + version + "\n" +
                                "Host: " + localHost + "\n" +
                                "Port: " + localUploadPort + "\n" +
                                "Title: " + addRFC;
                        out.writeUTF(addMsg);
                        out.writeUTF("EndOfMsg");
                    }
                    else {
                        waitForServer = false;
                        System.out.println("RFC " + numRFCAdd + " Not Found!");
                    }
                    break;
                case "LOOKUP":
                    System.out.println("Please Enter RFC Number: ");
                    int numRFCfind = Integer.parseInt(keybd.readLine());
                    System.out.println("Please Enter RFC Title: ");
                    String titleRFCfind = keybd.readLine();

                    String findMsg = "LOOKUP RFC " + numRFCfind + " " + version + "\n" +
                            "Host: " + localHost + "\n" +
                            "Port: " + localUploadPort + "\n" +
                            "Title: " + titleRFCfind;
                    out.writeUTF(findMsg);
                    out.writeUTF("EndOfMsg");
                    break;
                case "LIST":
                    System.out.println("Sending LIST Request: ");

                    String listMsg = "LIST ALL " + version + "\n" +
                            "Host: " + localHost + "\n" +
                            "Port: " + localUploadPort;
                    out.writeUTF(listMsg);
                    out.writeUTF("EndOfMsg");
                    break;
                case "DOWNLOAD":
                    waitForServer = false;

                    System.out.println("Please Enter Target Address: ");
                    String tarAddr = keybd.readLine();
                    System.out.println("Please Enter Target Port: ");
                    int tarPort = Integer.parseInt(keybd.readLine());
                    System.out.println("Please Enter RFC Number: ");
                    int numRFCtoDown = Integer.parseInt(keybd.readLine());
                    System.out.println("Please Enter File Name: ");
                    String rfcName = keybd.readLine();
                    rfcName = "RFC " + numRFCtoDown + "_" + rfcName;

                    Properties props = System.getProperties();
                    String req = "GET RFC " + numRFCtoDown + " " + version + "\n" +
                            "Host: " + localHost + "\n" +
                            "OS: " + props.getProperty("os.name") + " " + props.getProperty("os.version") + "\n";

                    if(download(tarAddr, tarPort, req, rfcName)) {
                        //Add a new RFC record to localRFCs.
                        localRFCs.add(new localRFC(numRFCtoDown, rfcName));

                        //Add a new RFC record to Server.
                        waitForServer = true;
                        String newADDMsg;
                        newADDMsg = "ADD RFC " + numRFCtoDown + " " + version + "\n" +
                                "Host: " + localHost + "\n" +
                                "Port: " + localUploadPort + "\n" +
                                "Title: " + rfcName;
                        out.writeUTF(newADDMsg);
                        out.writeUTF("EndOfMsg");

                        System.out.println("Download Successfully!");
                    }
                    break;
                case "EXIT":
                    System.out.println("Trying to Leave!");
                    waitForServer = false;
                    exit = true;
                    out.writeUTF("EXIT " + version);
                    out.writeUTF("EndOfMsg");
                    break;
                default:
                    out.writeUTF(type + " " + version);
                    out.writeUTF("EndOfMsg");
                    break;
            }

            if(waitForServer){
                String response = "";
                while(true){
                    String tmp = in.readUTF();
                    if(tmp.equals("EndOfMsg"))
                        break;
                    response = response + tmp;
                }
                System.out.println("Response for " + type + " is:\n" + response);
                System.out.println();
            }

            if(exit)
                break;
        }
    }

    public static String find (int rfcNum) {
        String res = "";

        String rfcTitle = "";
        boolean found = false;

        for(localRFC i:localRFCs){
            if(i.numRFC() == rfcNum){
                rfcTitle = i.gettitle();
                found = true;
                break;
            }
        }

        if(found)
            res = res + localDirectory + "RFC " + rfcNum + "_" + rfcTitle;
        return res;
    }

    private static boolean download (String tarAddr, int tarPort, String req, String rfcName) {
        boolean succ = false;
        try {
            Socket tempDownload = new Socket(tarAddr, tarPort);
            DataInputStream tempIn = new DataInputStream(tempDownload.getInputStream());
            DataOutputStream tempOut = new DataOutputStream(tempDownload.getOutputStream());

            tempOut.writeUTF(req);
            tempOut.writeUTF("EndOfMsg");

            String msg = "";
            while(true) {
                String t = tempIn.readUTF();
                if(t.equals("EndOfMsg"))
                    break;
                msg = msg + t;
            }

            System.out.println("Downloaded File Contents:\n" +
                    msg);
            File txt = new File(localDirectory + rfcName + "Newly Download");
            if( !txt.exists() ){
                txt.createNewFile();
            }
            byte bytes[];
            bytes = msg.getBytes(); //新加的
            int b = msg.length(); //改
            FileOutputStream fos = new FileOutputStream(txt);
            fos.write(bytes,0,b);
            fos.close();

            succ = true;

            tempDownload.close();
        }
        catch(IOException e){
            System.out.println(e);
        }
        finally {
            return succ;
        }
    }

}

class localRFC {
    private int numRFC;
    private String title;

    public localRFC (int numRFC, String title) {
        this.numRFC = numRFC;
        this.title = title;
    }

    public int numRFC() {
        return numRFC;
    }

    public String gettitle() {
        return title;
    }
}
