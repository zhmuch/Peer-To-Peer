package com.p2p_ci.Client;

import sun.misc.Cleaner;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by zhmuch on 2016/3/7.
 */
public class PeerToPeer implements Runnable {

    private static String uploadHost;
    private static int uploadPort;
    private static ServerSocket peerUpload;

    private static Socket curr;
    private static DataInputStream in;
    private static DataOutputStream out;

    public PeerToPeer(String host, int port, ServerSocket peerU) {
        uploadHost = host;
        uploadPort = port;
        this.peerUpload = peerU;
    }

    public void run() {
        try
        {
            //Peer Upload Process Start Listening
//            System.out.println("Peer Upload Server Running!");
//            System.out.println("Address: " + uploadHost + "  Port Number: " + uploadPort);

            while(true){

                //  Block until a Peer want to download a file from here;
                curr = peerUpload.accept();
                in = new DataInputStream(curr.getInputStream());
                out = new DataOutputStream(curr.getOutputStream());

                //  Receiving request message;
                String request = "";
                while(true) {
                    String tmp = in.readUTF();
                    if(tmp.equals("EndOfMsg"))
                        break;
                    request = request + tmp;
                }

                //  Check version;
                String[] requests = request.split("\n");
                String[] firstLine = requests[0].split(" ");
                if( !firstLine[3].equals(Client.version) ) {
//                    System.out.println("Client.version" + "=" + Client.version + "=");
//                    System.out.println("firstLine.version" + "=" + firstLine[3] + "=");
                    out.writeUTF(Client.version + " 505 P2P-CI Version Not Supported");
                    out.writeUTF("EndOfMsg");
                }
                else{
                    //  Get method;
                    String method = firstLine[0];
                    int rfcNum = Integer.parseInt(firstLine[2]);
                    String downloadDir = "";
                    String data = "";

                    switch (method) {
                        case "GET":
                            downloadDir = Client.find(rfcNum);
                            if(!downloadDir.equals("")){

                                Date date = new Date();
                                DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
                                String currDate = format.format(date);

                                Properties props = System.getProperties();

                                File file = new File(downloadDir);

                                //  Construct file header;
                                String header = "";
                                header = header + Client.version + " 200 OK\n" +
                                        "Date: " + currDate + "\n" +
                                        "OS: " + props.getProperty("os.name") + " " + props.getProperty("os.version") + "\n" +
                                        "Last-Modified: " + format.format(file.lastModified()) + "\n" +
                                        "Content-Length: " + file.length() + "\n" +
                                        "Content-Type: text/text\n";
//                                System.out.println("The File Header:\n" + header);

                                //  Buffering download content;
                                BufferedReader br = new BufferedReader(new FileReader(downloadDir));
                                String line = "";
                                StringBuffer buffer = new StringBuffer();
                                while((line = br.readLine()) != null){
                                    buffer.append(line + "\n");
                                }
                                String fileContent = buffer.toString();
//                                System.out.println("File Contents:\n" +
//                                        fileContent);

                                //  Sending response message;
                                out.writeUTF(header);
                                out.writeUTF("EndOfMsg");

//                                data = data + fileContent;
                                data = fileContent;

                                //  Sending file data;
                                out.writeUTF(data);
                            }
                            else{
                                out.writeUTF(Client.version + " 404 Not Found");
                            }
                            out.writeUTF("EndOfMsg");
                            break;
                        default:
                            out.writeUTF(Client.version + " 400 Bad Request");
                            out.writeUTF("EndOfMsg");
                            break;
                    }
                }
            }
        }
        catch (SocketException e) {
            System.out.println("PeerToPeer Thread Closed !");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

}
