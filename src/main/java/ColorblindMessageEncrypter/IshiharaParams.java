package ColorblindMessageEncrypter;

import lombok.Data;
import java.util.ArrayList;

public @Data
class IshiharaParams {
	private String text = null;
	private int requestedWidth = 1920;
	private int requestedHeight = 1080;
	private int colorSetting = 0;
	private ArrayList<String> insideColors;
	private ArrayList<String> outsideColors;
}
