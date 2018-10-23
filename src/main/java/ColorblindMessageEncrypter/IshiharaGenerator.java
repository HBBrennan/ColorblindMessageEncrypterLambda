package ColorblindMessageEncrypter;

import lombok.NonNull;

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.FontMetrics;

 public class IshiharaGenerator
{
    //Color hex code arrays
    private String[] outsideC = {"#cf5f47", "#cf5f47", "#fd9500", "#ffd500", "#ee8568", "#ee8568", "#eebd7a"}; //Red, Red, Orange, Yellow, Light Red, Light Red, Tan
    private String[] insideC = {"#5a8a50", "#a2ab5a", "#c9cc7d"};                                              //Dark Green, Green, Light Green
    
    private FontMetrics metrics;
    private Random rand = new Random();
    
    public BufferedImage CreateImage(@NonNull String msg, @NonNull Rectangle r, Boolean reverse, int scaling)
    {  
        int maxSize = 6*scaling, minSize = 2*scaling;
        Font font = new Font("Roboto", Font.BOLD, 250*scaling);

        int h = r.height;
        int w = r.width;
        
        BufferedImage text = new BufferedImage(w*scaling, h*scaling, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gText = text.createGraphics();
        gText.setFont(font);
        metrics = gText.getFontMetrics(font);
        
        gText.setColor(Color.black);
        int textHeightScaled = metrics.getHeight();
        int textAscentScaled = metrics.getMaxAscent();
        int maxLines = h/(textHeightScaled/scaling);
        String[] lines = new String[maxLines];
        String[] words = msg.split(" ");
        int line = 0;
        int i = 0;
        
        while (line < maxLines && i < words.length)
        {
            if (metrics.stringWidth(lines[line]+words[i]) < w*scaling)
            {
                if (lines[line] == null)
                {
                    lines[line] = words[i];
                }
                else 
                {
                    lines[line] += (" " + words[i]);
                }
                i++;
            }
            else
            {
                line++;
            }
        }
        
        if (line == maxLines)
        line--;
        
        for (int l = 0; l <= line; l++)
        {
            if (lines[l] != null)
                gText.drawString(lines[l], 0, (textAscentScaled+(l*textHeightScaled)));
        }

        gText.dispose();
        
        int maxWidth = 0;
        for (String row : lines)
        {
            if (row != null && metrics.stringWidth(row) > maxWidth)
            maxWidth = metrics.stringWidth(row);
        }
        
        int canvasSizeX = maxWidth/scaling;
        if (canvasSizeX > w)
        canvasSizeX = w;
        int canvasSizeY = textHeightScaled*(line+1)/scaling;
        if (canvasSizeY > h)
        canvasSizeY = h;
        
        BufferedImage img = new BufferedImage(canvasSizeX*scaling, canvasSizeY*scaling, BufferedImage.TYPE_INT_ARGB);
        Graphics2D upscaledGraphics2d = img.createGraphics();
        
        //Creating Circle object array uncolored for output
        ArrayList<Circle> circles = makeCircles(canvasSizeX*scaling, canvasSizeY*scaling, maxSize, (double)minSize);
        
        //Anti Aliasing 
        upscaledGraphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        //Colors the circles based on whether or not their center intersects with string on the other image
        if (reverse)
        reverseColors();
        try{
        for (Circle circle : circles)
        {
            int color = text.getRGB(circle.getX(), circle.getY());
            String[] colors = null;
            if(color == 0)
            {
                colors = outsideC;
            }
            else
            {
                colors = insideC;
            }
            int num = rand.nextInt(colors.length);
            upscaledGraphics2d.setColor(Color.decode(colors[num]));
            if (circle.getRadius() > minSize)
            upscaledGraphics2d.fillOval((int)(circle.getX()-circle.getRadius()), (int)(circle.getY()-circle.getRadius()), (int)(2*circle.getRadius()), (int)(2*circle.getRadius()));
        }
        }
        catch (Exception e)
        {System.out.println (e);}
        upscaledGraphics2d.dispose();

        BufferedImage resizedImage = new BufferedImage(canvasSizeX, canvasSizeY, 1);
        Graphics2D g = resizedImage.createGraphics();
        g.setPaint(Color.white);
        g.fillRect(0, 0, canvasSizeX, canvasSizeY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, canvasSizeX, canvasSizeY, (ImageObserver)null);
        g.dispose();

        return resizedImage;
    }
        
    //Method to switch color scheme
    public void reverseColors()
    {
        String[] temp;
        temp = outsideC;
        outsideC = insideC;
        insideC = temp;
    }

    private ArrayList<Circle> makeCircles(int canvasSizex, int canvasSizey, int maxSize, double minSize)
    {
        //Declare random object to for randomized decrease of radius and randomized coordinates of each circle
        Random rand = new Random();

        //Create array of circle objects
        ArrayList<Circle> circles = new ArrayList<Circle>();
        double area = 0;
        //Create circles with random coordinates and radiuses based on its distance from the closest circle
        while (area < (.55*canvasSizex*canvasSizey))
        {
            int x = rand.nextInt(canvasSizex);
            int y = rand.nextInt(canvasSizey);
            Circle circle = new Circle(maxSize, x, y);

            double radius = shrink(circle, circles, maxSize);

            if (radius > 0)
            {
                double multiplier = 1-rand.nextDouble()*.25;
                radius *= multiplier;
            }
            if (radius < minSize)
                radius = 0    ;
            circle.setRadius(radius);
            if (radius > 0)
            {
                area += Math.PI*radius*radius;
                circles.add(circle);
            }
        }
        return circles;
    }


    private double shrink(Circle circle, ArrayList<Circle> circles, int maxSize)
    {
        double radius = maxSize;
        for (Circle circle2 : circles)
        {
            double newRadius = circle.maxRadius(circle2);
            if (newRadius < radius)
            {
                if (newRadius / circle2.getRadius() > .5)
                {
                    radius = newRadius;
                }
                else
                {
                    radius = 0;
                    break;
                }
            }
        }
        return radius;
    }
}