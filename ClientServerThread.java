package place.server;

import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * This thread class is used by the server in order to establish a
 * connection with the client and then pass that info onto NetworkServer
 * in order to actually interface with the client. This class is responsible
 * for managing the long term IO with the client and takes the requests from
 * NetworkClient and calls the appropriate method in NetworkServer
 *
 * @author Dan
 */

public class ClientServerThread extends Thread{

    private String username; //Username of the client

    /**
     * Socket connection we will use to establish input
     * and output streams
     */
    private Socket socket;

    /**
     * Each thread has it's own reference to the original NetworkServer
     */
    private NetworkServer netServe;

    /**
     * Input and output streams for the socket of the client this thread
     * is associated with.
     */
    private ObjectInputStream in;
    private ObjectOutputStream out;

    /**
     * Time in milliseconds since last update. Measured in Unix time.
     * I initialized it to 0 so that I didn't have to worry about the first
     * tile placement
     */
    long lastTileChange=0;

    public ClientServerThread(Socket socket, NetworkServer netServe){
        this.socket=socket;
        this.netServe=netServe;
    }

    /**
     * This method will take care of the long term IO with the client.
     * This is where we will pass on each login request and each tile change
     * request. This only passes things on into the methods in NetworkServer,
     * it never received an output from NetworkServer
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void manage() throws IOException,ClassNotFoundException{
        try {
            boolean quit = false;
            while (quit == false) {
                PlaceRequest pr = (PlaceRequest) this.in.readUnshared();
                if (pr.getType().equals(PlaceRequest.RequestType.LOGIN)) {
                    this.username = (String) pr.getData();
                    boolean success = this.netServe.login(username, this.out);
                    if (success){
                        System.out.println(this.username + " @ " + this.socket.getRemoteSocketAddress().toString() + " logging in...");
                    }else{
                        System.out.println(this.username + " @ " + this.socket.getRemoteSocketAddress().toString() + " log in rejected");
                    }
                }
                if (pr.getType().equals(PlaceRequest.RequestType.CHANGE_TILE)) {
                    if(this.lastTileChange==0 || this.lastTileChange+500 <= System.currentTimeMillis()) {
                        this.netServe.changeTile((PlaceTile) pr.getData());
                        this.lastTileChange= System.currentTimeMillis();
                    }else{

                    }
                }
            }
        }catch(EOFException | SocketException eof){
            System.out.println(this.username + " @ " + this.socket.getRemoteSocketAddress().toString() + " logging off...");
            netServe.logoff(this.username);
            try{
                in.close();
                out.close();
            }catch(IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    public void run(){
        try{
            this.out=new ObjectOutputStream(socket.getOutputStream());
            this.in=new ObjectInputStream(socket.getInputStream());
            this.manage();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        catch(ClassNotFoundException ce){
            ce.printStackTrace();
        }
    }
}
