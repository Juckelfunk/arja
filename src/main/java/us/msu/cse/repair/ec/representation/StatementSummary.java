package us.msu.cse.repair.ec.representation;

import org.eclipse.jdt.core.dom.Statement;

import java.io.Serializable;

public final class StatementSummary implements Serializable {

    private static final long serialVersionUID = 7295316777091788257L;

    private final String text;

    public StatementSummary(Statement statement) {
        text = statement != null ? statement.toString() : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementSummary that = (StatementSummary) o;
        return getText().equals(that.getText());
    }

    public String getText() {
        return text;
    }
}
