package ColorblindMessageEncrypter;

import lombok.Data;
import java.util.ArrayList;

public @Data
class IshiharaParams {
    public String text;
    public int requestedWidth = 1920;
    public int requestedHeight = 1080;
    public int colorSetting = 0;
    public ArrayList<String> insideColors;
    public ArrayList<String> outsideColors;
}
