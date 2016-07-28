import cj.util.*;

public class Use {

    private static void readTest(Collection<String,?> col) {
        Iterator<String,?> it = col.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    private static void setTest(List<String,? extends Modifiable> list) {
        list.set(0, "Stefan");
        if (! "Stefan".equals(list.get(0))) {
            throw new RuntimeException("set failed: " + list.get(0));
        }
    }

    private static void deleteTest(Collection<String,? extends Shrinkable> col) {
        col.remove("blub");
    }

    private static void addTest(Collection<String,? extends Resizable> col) {
        col.add("foo");
    }


    private static void testList() {
        System.out.println("testList");
        List<String, Resizable> list = new ArrayList<String,Resizable>();
        list.add("egg");
        addTest(list);
        list.add("blub");
        deleteTest(list);
        setTest(list);
        readTest(list);
        List<String, Shrinkable> list2 = (List) list;
        deleteTest(list2);
        setTest(list2);
        readTest(list2);
        List<String, Modifiable> list3 = (List) list;
        setTest(list3);
        readTest(list3);
    }

    private static void testSet() {
        System.out.println("testSet");
        Set<String, Resizable> set = new HashSet<String>();
        set.add("egg");
        addTest(set);
        set.add("blub");
        deleteTest(set);
        readTest(set);
        Set<String, Shrinkable> set2 = (Set) set;
        deleteTest(set2);
        readTest(set2);
        Set<String, Modifiable> set3 = (Set) set;
        readTest(set3);
    }

    private static void testMap() {
        System.out.println("testMap");
        Map<String,Integer,Resizable> map = new HashMap<String,Integer>();
        map.put("Stefan", 13);
        map.put("JavaGI", 100);
        map.remove("JavaGI");
        map.put("Stefan", 13);
        Iterator<Map.Entry<String,Integer,Resizable>, Modifiable> it = 
            map.modifiableEntrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,Integer,Resizable> entry = it.next();
            System.out.println(entry);
            entry.setValue(42);
            System.out.println(entry);
        }
    }

    public static void main(String... args) {
        testList();
        testSet();
        testMap();
    }
}