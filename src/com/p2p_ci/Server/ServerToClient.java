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


    public ServerToClient(Socket curr) throws IOException {
        this.currSocket = curr;
        this.in = new DataInputStream(currSocket.getInputStream());
        this.out = new DataOutputStream(currSocket.getOutputStream());
        this.currPeerAddr = ("" + currSocket.getRemoteSocketAddress()).substring(1);
    }

    public void run() {
        System.out.println("S2C: New Thread!");

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

        if(currPeerPort != -1)
            Server.addNewPeer(currPeerAddr, currPeerPort);


//            catch (IOException e)
//            {
//                System.out.println(e);
//            }
//            catch (SocketTimeoutException s)
//            {
//                System.out.println("Client: "+currPeerAddr+" Timeout!");
//            }

        try {
            waitRequest();

            currSocket.close();
            System.out.println("S2C: Connection Terminated.");
        }
        catch (IOException e){
            System.out.println("catch!");
            System.out.println(e);
        }
        catch (MessageException m){
            System.out.println("catch!");
            System.out.println(m);
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
            boolean leave = false;

//            String msg = in.readUTF();
            String msg = "";
             while(true){
                 String m = in.readUTF();
                 if(m.equals("EndOfMsg"))
                     break;
                 msg = msg + m;
             }
//            System.out.println("newMsg:\n" + msg);

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
            System.out.println("2");
            throw new MessageException();
        }

        peerRFC rfc = new peerRFC(Integer.parseInt(methods[2]), titles[1], hosts[1]);
        Server.addPeerRFCs(rfc);

        return Integer.parseInt(ports[1]);
    }

}

class MessageException extends Exception {}
