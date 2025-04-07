package us.msu.cse.repair.ec.representation;

import org.eclipse.jdt.core.dom.Statement;

import java.io.Serializable;
import java.util.Objects;

public final class ManipulationSummary implements Serializable {

    private static final long serialVersionUID = 4308667595043855938L;

    private final StatementSummary locationSummary;

    private final String manipName;

    private final StatementSummary ingredientSummary;

    public ManipulationSummary(Statement location, String manipName, Statement ingredient) {
        this.locationSummary = new StatementSummary(location);
        this.manipName = manipName;
        this.ingredientSummary = new StatementSummary(ingredient);
    }

    public StatementSummary getLocationSummary() {
        return locationSummary;
    }

    public String getManipName() {
        return manipName;
    }

    public StatementSummary getIngredientSummary() {
        return ingredientSummary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManipulationSummary that = (ManipulationSummary) o;
        if (getManipName().equalsIgnoreCase("delete")) {
            return getLocationSummary().equals(that.getLocationSummary());
        } else {
            return getLocationSummary().equals(that.getLocationSummary())
                && getManipName().equals(that.getManipName())
                && getIngredientSummary().equals(that.getIngredientSummary());
        }
    }

    @Override
    public int hashCode() {
        if (getManipName().equalsIgnoreCase("delete")) {
            return Objects.hash(getLocationSummary());
        } else {
            return Objects.hash(getLocationSummary(), getManipName(), getIngredientSummary());
        }
    }

    @Override
    public String toString() {
        return String.format("ManipulationSummary{%s, %s, %s}", locationSummary, manipName, ingredientSummary);
    }
}
