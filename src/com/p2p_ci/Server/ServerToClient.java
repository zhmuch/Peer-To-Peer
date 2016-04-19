package com.p2p_ci.Server;

/**
 * Created by zhmuch on 2016/3/5.
 */
import java.net.*;
import java.io.*;


public class ServerToClient implements Runnable {

    private Socket currSocket;
    private DataInputStream in;
    private DataOutputStream out;

    private String currPeerAddr;
    private int currPeerPort = -1;

    /**
     * Constructor;
     * @param curr
     * @throws IOException
     */
    public ServerToClient(Socket curr) throws IOException {
        this.currSocket = curr;
        this.in = new DataInputStream(currSocket.getInputStream());
        this.out = new DataOutputStream(currSocket.getOutputStream());
        this.currPeerAddr = ("" + currSocket.getRemoteSocketAddress()).substring(1);
    }

    public void run() {

        System.out.println("ServerToClient: New Thread! At: " + currPeerAddr);

        currPeerAddr = (currPeerAddr.split(":"))[0];

        //Reading Reg Msg from New Peer
        boolean regEnd = false;
        try{
            String regInfo = "";

            while(true) {
                while (true) {
                    String str = in.readUTF();
                    if (str.equals("EndOfMsg"))
                        break;

                    if (str.equals("EndOfReg")) {
                        regEnd = true;
                        break;
                    }

                    regInfo = regInfo + str;
                }

                if(regEnd)
                    break;

                currPeerPort = addPeerRFC(regInfo);
                regInfo = "";
            }
        }
        catch (Exception e){
            System.out.println(e);
        }

        //  Add this new Peer to the Peer List;
        if(currPeerPort != -1)
            Server.addNewPeer(currPeerAddr, currPeerPort);

        //  Waiting for the command from this Client;
        try {

            waitRequest();

            currSocket.close();
            System.out.println("ServerToClient: Connection Terminated. At: " + currPeerAddr);

        }
        catch (IOException e){
            e.printStackTrace();
        }
        catch (MessageException m){
            m.printStackTrace();
        }
        finally {
            return;
        }
    }

    private void waitRequest() throws IOException, MessageException {
        /**Keep Listening Requests from Peer
         * If Peer closed without Notifying Client, may throws EOFException.
         */

        //Greeting Message
        out.writeUTF("Server is waiting for requests...");
        out.writeUTF("EndOfMsg");

        while(true){
            //  Flag for leaving the System;
            boolean leave = false;

            String msg = "";
             while(true){
                 String m = in.readUTF();
                 if(m.equals("EndOfMsg"))
                     break;
                 msg = msg + m;
             }
//            System.out.println("newMsg:\n" + msg);

            //  Reform the command message;
            String[] lines = msg.split("\n");
            String[] firstLine = lines[0].split(" ");
            int firstLineSize = firstLine.length;
            String version = firstLine[firstLineSize - 1];
            String method = firstLine[0];

            if(!version.equals(Server.version)) {
//                System.out.println("Server Version" + "=" + Server.version + "=");
//                System.out.println("Client Version" + "=" + version + "=");

                out.writeUTF(Server.version + " 505 P2P-CI Version Not Supported from Server");
                out.writeUTF("EndOfMsg");
            }
            else{
//                System.out.println("newMsg Method: " + method);

                String hostAddr, title;
                int numrfc, portNum;

                switch (method) {
                    case "ADD":
//                        System.out.println("Peer want to ADD RFC:\n" + msg);

                        addPeerRFC(msg);

                        out.writeUTF("RFC " + firstLine[2] + " has been Added !");
                        out.writeUTF("EndOfMsg");
                        break;
                    case "LOOKUP":
                        // System.out.println("Peer want to LOOKUP: "+msg);
                        
                        numrfc = Integer.parseInt(firstLine[2]);
//                        hostAddr = lines[1];
//                        portNum =  Integer.parseInt((lines[2].split(": "))[1]);
                        title = (lines[3].split(": "))[1];

                        String lookupRes = Server.lookupRFCs(numrfc, title);
                        out.writeUTF(lookupRes);
                        out.writeUTF("EndOfMsg");
                        break;
                    case "LIST":
//                        System.out.println("Peer want to LIST ALL: ");

                        String param = firstLine[1];
                        String listRes = Server.list(param);

                        out.writeUTF(listRes);
                        out.writeUTF("EndOfMsg");
                        break;
                    case "EXIT":
//                        System.out.println("Peer want to LEAVE: ");
                        Server.remove(currPeerAddr);
                        leave = true;
                        break;
                    default:
                        out.writeUTF(Server.version + " 400 Bad Request");
                        out.writeUTF("EndOfMsg");
                        break;
                }
            }

            if(leave)
                break;
        }
//        System.out.println("S2C: Server Stop Listening");
    }

    /**
     * This function will add a new RFC file to the Server`s RFC list;
     * @param newReg
     * @return
     * @throws MessageException
     */
    public int addPeerRFC(String newReg) throws MessageException{

        String[] tmp = newReg.split("\n");
        int l = tmp.length;
        if(l != 4){
            System.out.println("l length: " + l);
            System.out.println(newReg);
            throw new MessageException();
        }

        String[] methods = tmp[0].split(" ");
        String[] hosts = tmp[1].split(": ");
        String[] ports = tmp[2].split(": ");
        String[] titles = tmp[3].split(": ");

        if(methods.length != 4 || hosts.length != 2 || ports.length != 2 || titles.length != 2 || !methods[0].equals("ADD")) {
            System.out.println("regInfo format Error!");
            throw new MessageException();
        }

        peerRFC rfc = new peerRFC(Integer.parseInt(methods[2]), titles[1], hosts[1]);
        Server.addPeerRFCs(rfc);

        return Integer.parseInt(ports[1]);
    }

}

//  Message Exception, can be modified;
class MessageException extends Exception {}
