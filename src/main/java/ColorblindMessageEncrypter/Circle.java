package ColorblindMessageEncrypter;

//Object creation class
public class Circle
{
    //Declare variables for the radius and coordinates of the circle object
    private double radius;
    private int x, y;
    
    //Method to return the radius of the circle
    public double getRadius()
    {
        return radius;
    }
    
    //Method to set the radius of the circle passed to it
    public void setRadius(double radius)
    {
        this.radius = radius;
    }
    
    //Method to get the X coordinate of the circle
    public int getX()
    {
        return x;
    }
    
    //Method to set the X coordinate of the circle passed to it
    public void setX(int x)
    {
        this.x = x;
    }
    
    //Method to get the Y coordinate of the circle
    public int getY()
    {
        return y;
    }
    
    //Method to set the Y coordinate of the circle passed to it
    public void setY(int y)
    {
        this.y = y;
    }
    
    //Method to create the circle
    public Circle(double radius, int x, int y)
    {
        this.radius = radius;
        this.x = x;
        this.y = y;
    }
    
    //Method to calculate the max size the circle can be in relation to another circle
    //This method returns 0 if the generated circle is inside another circle
    public double maxRadius(Circle c1)
    {
        double dist = Math.sqrt(Math.pow((getX()-c1.getX()), 2)+Math.pow((getY()-c1.getY()), 2));
        return Math.max(0, dist - c1.getRadius());
    }
}