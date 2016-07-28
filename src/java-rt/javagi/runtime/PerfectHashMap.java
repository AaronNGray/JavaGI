package javagi.runtime;

public class PerfectHashMap {

    /* if we need an array of length (loadFactor * number_of_elements) then fall back to the regular hash map */
    private static int loadFactor = 3; 
    private ImplementationList[] values;
    private int remainder;

    private PerfectHashMap(ImplementationList[] valArray, int remainder) {
        this.values = valArray;
        this.remainder = remainder;
    }

    static PerfectHashMap newPerfectHashMap(Object[] keys, ImplementationList[] values) {
        if (keys.length != values.length) {
            throw new JavaGIRuntimeBug("Cannot construct PerfectHashMap");
        }
        int[] hashCodes = new int[keys.length];
        for (int i = 0; i < hashCodes.length; i++) {
            int h = keys[i].hashCode();
            for (int j = 0; j < i; j++) {
                if (hashCodes[j] == h) return null;
            }
            hashCodes[i] = h;
        }
        int remainder = hashCodes.length - 1;
        increment: while (true) {
            remainder++;
            if (remainder > loadFactor * keys.length) {
                return null;
            }
            boolean[] alreadyUsed = new boolean[remainder];
            for (int i = 0; i < hashCodes.length; i++) {
                int k = hashCodes[i] % remainder;
                if (alreadyUsed[k]) continue increment;
                alreadyUsed[k] = true;
            }
            break increment;
        }
        ImplementationList[] valArray = new ImplementationList[remainder];
        for (int i = 0; i < hashCodes.length; i++) {
            int k = hashCodes[i] % remainder;
            valArray[k] = values[i];
        }
        return new PerfectHashMap(valArray, remainder);
    }
    
    public ImplementationList get(Object key) {
        int k = key.hashCode() % remainder;
        return values[k];
    }
}
