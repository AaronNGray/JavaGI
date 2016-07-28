import cj.util.*;

public class MisUse {

    public static void main(String... args) {
        List<String,Shrinkable> list1 = new ArrayList<String,Shrinkable>();
        list1.add("Stefan");
        List<String,Modifiable> list2 = new ArrayList<String,Modifiable>();
        list2.remove("Stefan");
        List<String,Object> list3 = new ArrayList<String,Object>();
        list3.set(0, "Stefan");
    }
}