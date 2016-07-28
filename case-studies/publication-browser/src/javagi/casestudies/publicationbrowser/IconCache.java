package javagi.casestudies.publicationbrowser;

import javax.swing.*;
import java.util.*;
import java.net.URL;

public class IconCache {
    private static Map<String, Icon> cache = new HashMap<String, Icon>();

    public static Icon get(String s) {
        if (cache.containsKey(s)) {
            return cache.get(s);
        } else {
            String name = "/icons/" + s;
            URL iconURL =  IconCache.class.getResource(name);
            Icon icon = null;
            if (iconURL == null) {
                System.err.println("Icon " + name + " not found");
            } else {
                icon = new ImageIcon(iconURL);
            }
            cache.put(s, icon);
            return icon;
        }
    }
}