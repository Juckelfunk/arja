package us.msu.cse.repair.ec.representation;

import java.util.Arrays;
import java.util.BitSet;

public class ArjaDecisionVariable {

    private final BitSet bits;

    private final int[] array;

    public ArjaDecisionVariable(BitSet bits, int[] array) {
        this.bits = bits;
        this.array = array;
    }

    public BitSet getBits() {
        return (BitSet) bits.clone();
    }

    public int[] getArray() {
        return Arrays.copyOf(array, array.length);
    }
}
