package ColorblindMessageEncrypter;

import lombok.NonNull;
import lombok.val;

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.FontMetrics;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

class IshiharaGenerator
{
	//Color hex code arrays
	private String[] outsideC = {"#cf5f47", "#cf5f47", "#fd9500", "#ffd500", "#ee8568", "#ee8568", "#eebd7a"}; //Red, Red, Orange, Yellow, Light Red, Light Red, Tan
	private String[] insideC = {"#5a8a50", "#a2ab5a", "#c9cc7d"};                                              //Dark Green, Green, Light Green

	private FontMetrics metrics;
	private Random rand = new Random();

	public BufferedImage CreateImage(@NonNull String msg, @NonNull Rectangle r, Boolean reverse, int scaling)
	{
		int maxSize = 6, minSize = 2;
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

		Rectangle canvasSize = new Rectangle( maxWidth/scaling, textHeightScaled*(line+1)/scaling);
		if (canvasSize.width > w)
			canvasSize.width = w;
		if (canvasSize.height > h)
			canvasSize.height = h;

		BufferedImage img = new BufferedImage(canvasSize.width * scaling, canvasSize.height * scaling, BufferedImage.TYPE_INT_ARGB);
		Graphics2D upscaledGraphics2d = img.createGraphics();

		//Creating Circle object array uncolored for output
		val circles = makeCircles(canvasSize, maxSize, (double)minSize);

		//Anti Aliasing
		upscaledGraphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		//Colors the circles based on whether or not their center intersects with string on the other image
		if (reverse)
		reverseColors();
		for (val circle : circles)
		{
			int color = text.getRGB(circle.x*scaling, circle.y*scaling);
			String[] colors = (color == 0 ? outsideC : insideC);
			upscaledGraphics2d.setColor(Color.decode(colors[rand.nextInt(colors.length)]));
			if (circle.value > minSize)
			upscaledGraphics2d.fillOval(scaling*(int)Math.round(circle.x-circle.value),
					scaling*(int)Math.round(circle.y-circle.value),
					scaling*(int)Math.round(2*circle.value),
					scaling*(int)Math.round(2*circle.value));
		}
		upscaledGraphics2d.dispose();

		BufferedImage resizedImage = new BufferedImage(canvasSize.width, canvasSize.height, TYPE_INT_RGB);
		Graphics2D g = resizedImage.createGraphics();
		g.setPaint(Color.white);
		g.fillRect(0, 0, canvasSize.width, canvasSize.height);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, canvasSize.width, canvasSize.height, null);
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

	private ArrayList<PointQuadTree<Double>.PointQuadTreeNode> makeCircles(Rectangle canvasSize, int maxSize, double minSize)
	{
		val rand = new Random();
		val circles = new PointQuadTree<Double>();
		double area = 0.0;
		//Create circles with random coordinates and radii with size based on its distance from the closest circle
		while (area < (.55*canvasSize.width*canvasSize.height))
		{
			int x = rand.nextInt(canvasSize.width);
			int y = rand.nextInt(canvasSize.height);

			double radius = shrink(x, y, circles, maxSize);

			if (radius > 0)
			{
				double multiplier = 1-rand.nextDouble()*.25;
				radius *= multiplier;
			}
			if (radius < minSize)
				continue;

			area += Math.PI*radius*radius;
			circles.insert(x, y, radius);
		}
		return circles.toList();
	}


	/**
	 *  Shrink the proposed radius to the maximum size without intersections based on nearby circles.
	 * @param x The x coordinate of the proposed circle.
	 * @param y The y cooordinate of the proposed circle
	 * @param circles QuadTree containing all of our circles
	 * @param maxSize The maximum/initial radius of a circle.
	 * @return The maximum legal size for a circle at the proposed points.
	 */
	private double shrink(int x, int y, PointQuadTree<Double> circles, int maxSize)
	{
		double radius = maxSize;
		// All possible nearby nodes that could intersect with this new node.
		val nearbyNodes = circles.query2D(new Rectangle(x - 2*maxSize, y - 2*maxSize, 4*maxSize, 4*maxSize));
		for (val node : nearbyNodes) {
			double dist = Math.sqrt(Math.pow(x-node.x, 2) + Math.pow(y-node.y, 2));
			radius = Math.min(radius, dist-node.value);

			if (radius < 0.0) return 0.0;
		}
		return radius;
	}
}