package place.server;

import place.PlaceTile;

import java.util.*;

/**
 * This is the class I will use to generate statistics. It will
 * recalculate the statistics each time a new tile is added, this will
 * ensure the statistics are both real time and also quickly printed when
 * requested by the server.
 *
 * It will use mostly maps in order to gather and properly order the data.
 * This will save all the cost of sorting the data once the statistics are desired,
 * making the code faster and more efficient. It also means that all of the statistics
 * will be as close to real time as possible.
 *
 * It also uses a timer it has running since the beginning to determine the total time
 * elapsed then divides that by the total number of tiles changed to get average tile
 * changes per second.
 *
 * @author Dan
 */
public class statClass {
    private int totalTiles;
    private SortedMap<String, Integer> colors = new TreeMap<>(); //Used to find most popular colors
    private SortedMap<String, Integer> owners = new TreeMap<>(); //Used to find most active owners
    private List<Index> indices=new LinkedList(); //Used to find most contentious indices
    String[] colorNames=new String[] {"black","gray","silver","white","maroon","red","olive","yellow","green","lime",
        "teal","aqua","navy","blue","purple","fuchsia"}; //Used to get the actual name of the color not the hexNumber
    private long startTime;

    private class Index implements Comparable{
        private int col;
        private int row;
        private int count;

        public Index(int row, int col){
            this.row=row;
            this.col=col;
            this.count=1;
        }

        public int getCol(){
            return this.col;
        }

        public int getRow(){
            return this.row;
        }

        public int getCount(){
            return this.count;
        }
        public void increaseCount(){
            this.count+=1;
        }

        @Override
        public String toString() {
            return "(" + Integer.toString(this.row) + "," + Integer.toString(this.col) + ") Tile Changes= (" + Integer.toString(this.count) + ")";
        }

        @Override
        public int compareTo(Object o) {
            Index i=(Index)o;
            if(this.count < i.getCount()){
                return 1;
            }
            if(this.count == i.getCount()){
                if(this.row<i.getRow() || this.row==i.getRow()){
                    return 1;
                }else{
                    return -1;
                }
            }
            else{
                return -1;
            }
        }
    }

    public statClass(){
        this.totalTiles=0;
        startTime=System.currentTimeMillis();
    }

    public void addTile(PlaceTile tile){
        if(colors.containsKey(colorNames[tile.getColor().getNumber()])){
            int i=colors.get(colorNames[tile.getColor().getNumber()]);
            i+=1;
            colors.put(colorNames[tile.getColor().getNumber()],i);
        }
        if(!colors.containsKey(colorNames[tile.getColor().getNumber()])){
            colors.put(colorNames[tile.getColor().getNumber()],1);
        }
        if(owners.containsKey(tile.getOwner())){
            int i=owners.get(tile.getOwner());
            i++;
            owners.put(tile.getOwner(),i);
        }
        if(!owners.containsKey(tile.getOwner())){
            owners.put(tile.getOwner(),1);
        }
        Index i=indexExists(tile.getRow(),tile.getCol());
        if(i==null){
            this.indices.add(new Index(tile.getRow(),tile.getCol()));
        }
        if(i!=null){
            i.increaseCount();
        }
        this.totalTiles+=1;
    }

    public Index indexExists(int row, int col){
        ListIterator it=this.indices.listIterator();
        while(it.hasNext()){
            Index i=(Index) it.next();
            if(i.getRow()==row && i.getCol()==col){
                return i;
            }
        }
        return null;
    }

    /**
     * I will use this method to generate the String that will print out all of the statistics.
     * Because the object for this class is held in PlaceServer, I can just call System.out to
     * print instead of having to pass a string to the PlaceServer class, making it slightly more
     * efficient.
     *
     * It basically just takes the global maps I have and then creates a sorted set out of their
     * entry sets, sorted by reverse natural ordering of the values/ natural ordering of keys if
     * values are equal, and then prints out the sorted sets toString()s.
     */
    public void printStats(){
        long currentTime=System.currentTimeMillis();
        long elapsedTimeSeconds=(currentTime-startTime)/1000;
        double tilesRate=(double)totalTiles/(double)elapsedTimeSeconds;
        SortedSet<Map.Entry<String, Integer>> sortedColors = new TreeSet<>(
                new Comparator<Map.Entry<String, Integer>>() {
                    @Override
                    public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                        if(e1.getValue()<e2.getValue()){
                            return 1;
                        }
                        if(e1.getValue().equals(e2.getValue())){
                            return e1.getKey().compareTo(e2.getKey());
                        }else{
                            return -1;
                        }
                    }
                });
        sortedColors.addAll(colors.entrySet());
        System.out.println("Colors: " + sortedColors.toString());
        SortedSet<Map.Entry<String, Integer>> sortedOwners = new TreeSet<>(
                new Comparator<Map.Entry<String, Integer>>() {
                    @Override
                    public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                        if(e1.getValue()<e2.getValue()){
                            return 1;
                        }
                        if(e1.getValue().equals(e2.getValue())){
                            return e1.getKey().compareTo(e2.getKey());
                        }else{
                            return -1;
                        }
                    }
                });
        sortedOwners.addAll(owners.entrySet());
        System.out.println("Clients: " + sortedOwners.toString());
        Collections.sort(this.indices);
        System.out.println("Average Tile Changes per Second: " + tilesRate + " tiles");
        System.out.println("Most Contentious Indices: " + indices.toString());
    }

}