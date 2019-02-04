package ColorblindMessageEncrypter;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public @Data
class Circle
{
    private double radius;
    private int x, y;

    //Method to calculate the max size the circle can be in relation to another circle
    //This method returns 0 if the generated circle is inside another circle
    public double maxRadius(Circle c1)
    {
        double dist = Math.sqrt(Math.pow((x-c1.x), 2)+Math.pow((y-c1.y), 2));
        return Math.max(0, dist - c1.radius);
    }
}