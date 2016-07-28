package javagi.casestudies.gadt.list;

public class ListMain {
    static <T> List<T> mkList(T... elems) {
        List<T> l = new Nil<T>();
        for (int i = elems.length-1; i >= 0; i--) {
            l = new Cons<T>(elems[i], l);
        }
        return l;
    }
    public static void main(String[] args) {
        List<Integer> l1 = mkList(1,2,3);
        List<Integer> l2 = mkList(4,5,6);
        List<Integer> l3 = mkList(7);
        List<List<Integer>> ll = mkList(l1,l2,l3);
        List<Integer> l4 = ll.<Integer>flatten();
        System.out.println(l4);
    }
}