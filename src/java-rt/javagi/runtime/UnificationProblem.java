package javagi.runtime;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

class UnificationProblem implements Cloneable {

    UnificationProblem() {
    }
    
    UnificationProblem(Type... ts) {
        if (ts.length % 2 != 0) {
            throw new IllegalArgumentException("Argument array is not of even length");
        }
        for(int i = 0; i < ts.length - 1; ) {
            Type t = ts[i++];
            Type u = ts[i++];
            enqueue(t, u);
        }
    }

    private HashSet<Pair<Type, Type>> set = new HashSet<Pair<Type, Type>>();
    
    public boolean isEmpty() {
        return set.isEmpty();
    }

    public Pair<Type, Type> dequeue() {
        Pair<Type, Type> first = set.iterator().next();
        set.remove(first);
        return first;
    }

    public void enqueue(Type t, Type u) {
        set.add(new Pair<Type,Type>(t,u));
    }
    
    public void enqueue(Pair<Type,Type> p) {
        set.add(p);
    }

    public void applySubst(Substitution s) {
        HashSet<Pair<Type, Type>> newList = new HashSet<Pair<Type, Type>>();
        for (Pair<Type, Type> p : set) {
            newList.add(new Pair<Type,Type>(
                    Types.applySubst(p.fst, s), 
                    Types.applySubst(p.snd, s)));
        }
        this.set = newList;
    }
    
    public List<UnificationProblem> allPossibilities() {
        List<Pair<Type, Type>> list = new ArrayList<Pair<Type, Type>>();
        list.addAll(this.set);
        return allPossibilities(list, 0);
    }
    
    private List<UnificationProblem> allPossibilities(List<Pair<Type, Type>> input, int i) {
        if (i >= input.size()) {
            List<UnificationProblem> ups = new ArrayList<UnificationProblem>();
            ups.add(new UnificationProblem());
            return ups;
        } else {
            Pair<Type,Type> p1 = input.get(i);
            Pair<Type,Type> p2 = p1.swap();
            List<UnificationProblem> ups = new ArrayList<UnificationProblem>();
            for (UnificationProblem up : allPossibilities(input, i+1)) {
                UnificationProblem clone = (UnificationProblem) up.clone();
                ups.add(up);
                ups.add(clone);
                up.enqueue(p1);
                clone.enqueue(p2);
            }
            return ups;
        }
    }
    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("UnificationProblem(");
        for (Pair<Type,Type> p : set) {
            sb.append(p.fst.toString());
            sb.append(" =? ");
            sb.append(p.snd.toString());
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof UnificationProblem)) {
            return false;
        }
        UnificationProblem up = (UnificationProblem) other;
        return this.set.equals(up.set);
    }
    
    @Override
    public int hashCode() {
        return 23*this.set.hashCode();   
    }
    
    @Override
    public UnificationProblem clone() {
        UnificationProblem up = new UnificationProblem();
        up.set = (HashSet<Pair<Type, Type>>) this.set.clone();
        return up;
    }
}
