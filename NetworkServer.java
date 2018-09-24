package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used as the main controller of all the server side
 * operations. It holds a master HashMap of all the current clients
 * that are connected, as well as the master copy of the Board server
 * side. It has methods for logging in a user, logging out a user,
 * receiving a tile change and also sending a tile change.
 *
 * Clients must wait 500 milliseconds before submitting another tile
 * @author Dan
 */
public class NetworkServer {

    private ConcurrentHashMap<String, ObjectOutputStream> clientOutput = new ConcurrentHashMap();
    private PlaceBoard board;
    private PlaceServer server; //Will be used to handle statistics
    private statClass stats;

    public NetworkServer(int dim,PlaceServer server, statClass stats) {
        this.board = new PlaceBoard(dim);
        this.server=server;
        this.stats=stats;
    }

    /**
     * This methods logs a client into the system.
     *
     * @param username: represents the username of the client
     * @param out:      represents the output stream of the client, to be used to
     *                  send the board.
     */
    public synchronized boolean login(String username, ObjectOutputStream out) {
        boolean result=false;
        try {
            if (clientOutput.containsKey(username)) {
                out.writeUnshared(new PlaceRequest(PlaceRequest.RequestType.ERROR, "Username already taken"));
                result=false;
            } else {
                clientOutput.put(username, out);
                out.writeUnshared(new PlaceRequest(PlaceRequest.RequestType.LOGIN_SUCCESS, "Login Successful"));
                out.writeUnshared(new PlaceRequest(PlaceRequest.RequestType.BOARD, this.board));
                Thread.sleep(1000); //sleep for 1 second to allow new board to catch up
                result=true;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * This method takes in a PlaceTile object and first checks to see if the
     * move is valid, if the move is valid, it will then set that tile in the board
     * to the new tile
     *
     * @param tile: the tile that may be changed
     */
    public synchronized void changeTile(PlaceTile tile) {
        this.stats.addTile(tile);
        try {
            if (this.board.isValid(tile)) {
                this.board.setTile(tile);
                this.tileChanged(tile);
                Date now=new Date();
                System.out.println(tile.toString() + " was changed at " + now.toString());
            } else {
                clientOutput.get(tile.getOwner()).writeUnshared(new PlaceRequest(PlaceRequest.RequestType.ERROR, "Invalid Move"));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * This method is used to push out the updated board after the change
     * has been made. In order to do this, it creates a new iterator each time
     * it's called, because the entry set can and likely will change each time,
     * and then iterates over it, getting the ObjectOutputStream, and writing out
     * a new PlaceRequest Object containing the tile changed.
     */
    private synchronized void tileChanged(PlaceTile tile) {
        try {
            Iterator it = clientOutput.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ObjectOutputStream out = (ObjectOutputStream) entry.getValue();
                out.writeUnshared(new PlaceRequest(PlaceRequest.RequestType.TILE_CHANGED, tile));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Clients never actually indicate they are logging off, the connection just disappears.
     * This should be indicated by the connection being null.
     */
    public synchronized void logoff(String username) {
        clientOutput.remove(username);
    }

}
