package place.server;

import place.PlaceTile;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * This class will be the initial point of contact a client makes with
 * the system when they want to join the game, the PlaceServer class spawns a
 * ClientServerThread to establish the ObjectObjectStream and ObjectInputStream then passes
 * it onto the NetworkServer class. NetworkServer should be the brains behind the networking operation,
 * the PlaceServer is just for the purpose of connecting.
 *
 * @author Dan
 */

public class PlaceServer {

    private ServerSocket server;
    private NetworkServer netServe;
    private statClass stats;


    public PlaceServer(int port){
        try {
            this.stats=new statClass();
            this.server = new ServerSocket(port);
            Thread netThread = new Thread( () -> this.run() );
            netThread.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void manage(int dim){

        System.out.println("Server test");
        try{
            this.netServe=new NetworkServer(dim,this,this.stats);
            while(true) {
                Socket socket = server.accept();
                ClientServerThread t1=new ClientServerThread(socket,this.netServe);
                t1.start();
            }
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * This thread continuously checks
     */
    public void run(){
        Scanner sc=new Scanner(System.in);
        while(true){
            String s=sc.nextLine();
            if(s.toLowerCase().substring(0,4).equals("stat")){
                stats.printStats();
            }
        }
    }
    public static void main(String[] args) {
        if(args.length != 2 || Integer.parseInt(args[1])<=1 ){
            System.out.println("Usage: java PlaceServer port DIM");
            System.exit(1);
        }else{
            PlaceServer server=new PlaceServer(Integer.parseInt(args[0]));
            server.manage(Integer.parseInt(args[1]));
        }
    }
}
