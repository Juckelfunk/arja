package us.msu.cse.repair.ec.representation;

import us.msu.cse.repair.core.AbstractRepairProblem;
import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.ec.problems.ArjaEProblem;
import us.msu.cse.repair.ec.problems.ArjaProblem;
import us.msu.cse.repair.core.parser.ExtendedModificationPoint;

import org.eclipse.jdt.core.dom.Statement;

import java.io.Serializable;
import java.util.*;

public final class ArjaSolutionSummary implements Serializable {

    private static final long serialVersionUID = -5315751785850608455L;

    private final Set<ManipulationSummary> manipulations;

    public ArjaSolutionSummary(BitSet bits, int[] array, AbstractRepairProblem problem) {
        List<List<String>> availableManipulations = problem.getAvailableManipulations();

        manipulations = new LinkedHashSet<>();

        if (problem.getClass() == ArjaProblem.class) {
            List<ModificationPoint> modificationPoints = problem.getModificationPoints();
            int size = modificationPoints.size();
            for (int i = 0; i < size; i++) {
                if (bits.get(i)) {
                    ModificationPoint mp = modificationPoints.get(i);
                    manipulations.add(
                            new ManipulationSummary(
                                    mp.getStatement(),
                                    availableManipulations.get(i).get(array[i]),
                                    mp.getIngredients().get(array[size + i]))
                    );
                }
            }
        } else if (problem.getClass() == ArjaEProblem.class) {
            List<ExtendedModificationPoint> modificationPoints = ((ArjaEProblem) problem).getExtendedModificationPoints();
            int size = modificationPoints.size();
            for (int i = 0; i < size; i++) {
                if (bits.get(i)) {
                    ExtendedModificationPoint mp = (ExtendedModificationPoint) modificationPoints.get(i);
                    String manipName = availableManipulations.get(i).get(array[i]);

                    Statement ingredStatement = null;
                    if (manipName.equalsIgnoreCase("Replace"))
                        ingredStatement = mp.getReplaceIngredients().get(array[i + size]);
                    else if (manipName.equalsIgnoreCase("InsertBefore"))
                        ingredStatement = mp.getInsertIngredients().get(array[i + 2 * size]);

                    manipulations.add(new ManipulationSummary(mp.getStatement(), manipName, ingredStatement));
                }
            }
        }
    }

    public Set<ManipulationSummary> getManipulations() {
        return manipulations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArjaSolutionSummary that = (ArjaSolutionSummary) o;
        return getManipulations().equals(that.getManipulations());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getManipulations());
    }

    @Override
    public String toString() {
        return manipulations.toString();
    }
}
