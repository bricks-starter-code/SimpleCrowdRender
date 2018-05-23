/**
 * Created by bricks on 5/8/2018.
 */
public class Entry {
    public int msec;
    public float x;
    public float y;

    public Entry(String[] splits) {
        msec = Integer.parseInt(splits[1]);
        x = Float.parseFloat(splits[2]);
        y = Float.parseFloat(splits[3]);
    }
}
