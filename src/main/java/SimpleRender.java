import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by bricks on 5/8/2018.
 */
public class SimpleRender {

    public static void main(String args[]) throws IOException {
        new SimpleRender();
    }

    public SimpleRender() throws IOException {
        //String csv = args[1];
        String csv = "AS.csv";
        String fileName = "./data/" + csv;

        HashMap<Integer, Agent> agents = new HashMap<>();

        //read file into stream, try-with-resources
        ///From https://www.mkyong.com/java8/java-8-stream-read-a-file-line-by-line/
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {

            stream.forEach(line->{
                String[] splits = line.split(",");
                int id = Integer.parseInt(splits[0]);
                if(!agents.containsKey(id))
                {
                    Agent agent = new Agent();
                    agent.id = id;
                    agents.put(id, agent);
                }
                agents.get(id).entries.add(new Entry(splits));
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        //At this point, we should have a list of agents

        //Get the extents of the simulation
        double minBoundaryX = agents.values().stream().map(a->a.entries).flatMap(List::stream).mapToDouble(e->e.x).min().getAsDouble();
        double minBoundaryY = agents.values().stream().map(a->a.entries).flatMap(List::stream).mapToDouble(e->e.y).min().getAsDouble();
        double maxBoundaryX = agents.values().stream().map(a -> a.entries).flatMap(List::stream).mapToDouble(e -> e.x).max().getAsDouble();
        double maxBoundaryY = agents.values().stream().map(a -> a.entries).flatMap(List::stream).mapToDouble(e -> e.y).max().getAsDouble();

        double centerX = (maxBoundaryX + minBoundaryX)/2;
        double centerY = (maxBoundaryY + minBoundaryY)/2;
        double width = (maxBoundaryX - minBoundaryX);
        double height = (maxBoundaryY - minBoundaryY);
        double minDimension = Math.min(width, height);

        int imageWidth = 512;


        //Get the last millisecond that we have information about
        int lastMSec = agents.values().stream().map(agent->agent.entries).flatMap(List::stream).mapToInt(entry->entry.msec).max().getAsInt();
        System.out.println(lastMSec);

        int fps = 25;
        int msecPerFrame = 1000/fps;


        lastMSec = Math.min(lastMSec, 25000);

        //Clean the old files
        Files.list(Paths.get("./images"))
                .filter(Files::isRegularFile)
                .forEach(file->file.toFile().delete());

        int count = 0;
        for(int i = 0; i <= lastMSec; i += msecPerFrame)
        {
            System.out.println("mSec = "  + i );

            BufferedImage bi = new BufferedImage(imageWidth, imageWidth, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = (Graphics2D) bi.getGraphics();



            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imageWidth, imageWidth);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            //
            g.scale(imageWidth/minDimension, imageWidth/minDimension);
            g.translate(-minBoundaryX, -minBoundaryY);

            for(Integer id : agents.keySet())
            {
                boolean shouldRender = false;
                float x = 0;
                float y = 0;

                Agent agent = agents.get(id);
                final int currentMSec = i;
                List<Entry> le = agent.entries.stream().filter(entry->entry.msec <= currentMSec).collect(Collectors.toList());
                List<Entry> greater = agent.entries.stream().filter(entry->entry.msec > currentMSec).collect(Collectors.toList());

                //Check to see if we should render at all
                if(le.size() > 0 && greater.size() > 0)
                {
                    Entry minEntry = le.get(le.size() - 1);
                    Entry maxEntry = greater.get(0);

                    int minMSec = minEntry.msec;
                    int maxMSec = maxEntry.msec;
                    float minX = minEntry.x;
                    float minY = minEntry.y;
                    float maxX = maxEntry.x;
                    float maxY = maxEntry.y;

                    float percent = (i - minMSec)/(float)(maxMSec - minMSec);

                    shouldRender = true;
                    x = percent * (maxX - minX) + minX;
                    y = percent * (maxY - minY) + minY;


                }
                else if(le.size() > 0)
                {
                    if(le.get(le.size() - 1).msec == i)
                    {
                        shouldRender = true;
                        x = le.get(le.size() - 1).x;
                        y = le.get(le.size() - 1).y;

                    }
                }
                else if (greater.size() > 0)
                {
                    if(greater.get(0).msec == i)
                    {
                        shouldRender = true;
                        x = greater.get(0).x;
                        y = greater.get(0).y;

                    }
                }

                if(shouldRender)
                {
                    g.setColor(Color.BLACK);
                    double d = .25;
                    Shape s = new Ellipse2D.Double(x - d, y - d, d * 2, d * 2);
                    g.fill(s);
                }

            }


            g.dispose();

            ImageIO.write(bi, "PNG", Paths.get("./images/a" + String.format("%06d", count++) + ".png").toFile());
        }

        Runtime.getRuntime().exec(new String[]{"ffmpeg.exe","-framerate", "25", "-i", "./images/a%06d.png", "-y","output" + csv + ".mpg"});


    }
}
