package javagi.casestudies.xpath.jdom;

import org.jdom.*;
import javagi.casestudies.xpath.GINavigator;

public class GIJDomNavigator extends GINavigator<Document> {

    public static final GIJDomNavigator theInstance = new GIJDomNavigator();
    private GIJDomNavigator() {}
}