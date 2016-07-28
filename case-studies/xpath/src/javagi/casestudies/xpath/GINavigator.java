package javagi.casestudies.xpath;

import java.util.Iterator;
import java.text.*;
import java.io.PrintStream;

import javagi.casestudies.xpath.jaxen.Navigator;
import javagi.casestudies.xpath.jaxen.UnsupportedAxisException;
import javagi.casestudies.xpath.jaxen.FunctionCallException;
import javagi.casestudies.xpath.jaxen.XPath;
import javagi.casestudies.xpath.jaxen.saxpath.SAXPathException;

import javagi.runtime.RT;

public class GINavigator<TDocument> implements Navigator where TDocument implements XDocument {

    @Override
    public XPath parseXPath(String xpath) throws SAXPathException {
        return XDocument[TDocument].parseXPath(xpath);
    }

    @Override
    public XDocument getDocument(String uri) throws FunctionCallException {
        return XDocument[TDocument].load(uri);
    }
}