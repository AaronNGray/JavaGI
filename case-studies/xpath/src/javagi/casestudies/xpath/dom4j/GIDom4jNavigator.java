package javagi.casestudies.xpath.dom4j;

import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Comment;
import org.dom4j.Text;
import org.dom4j.Namespace;
import org.dom4j.ProcessingInstruction;

import javagi.casestudies.xpath.GINavigator;

public class GIDom4jNavigator extends GINavigator<Document> {

    public static final GIDom4jNavigator theInstance = new GIDom4jNavigator();
    private GIDom4jNavigator() {}
}