package fr.xephi.authme.util.datacolumns.sqlimplementation;

import fr.xephi.authme.util.datacolumns.Column;
import fr.xephi.authme.util.datacolumns.predicate.AlwaysTruePredicate;
import fr.xephi.authme.util.datacolumns.predicate.AndPredicate;
import fr.xephi.authme.util.datacolumns.predicate.ComparingPredicate;
import fr.xephi.authme.util.datacolumns.predicate.IsNotNullPredicate;
import fr.xephi.authme.util.datacolumns.predicate.IsNullPredicate;
import fr.xephi.authme.util.datacolumns.predicate.OrPredicate;
import fr.xephi.authme.util.datacolumns.predicate.Predicate;

import java.util.LinkedList;
import java.util.List;

public class PredicateSqlGenerator<C> {

    private final C context;

    public PredicateSqlGenerator(C context) {
        this.context = context;
    }

    public WhereClauseResult generateWhereClause(Predicate<C> predicate) {
        StringBuilder sqlResult = new StringBuilder();
        List<Object> bindings = new LinkedList<>();
        generateWhereClause(predicate, sqlResult, bindings);
        return new WhereClauseResult(sqlResult.toString(), bindings);
    }

    private void generateWhereClause(Predicate<C> predicate, StringBuilder sqlResult, List<Object> objects) {
        final Class<?> clazz = predicate.getClass();
        if (clazz == ComparingPredicate.class) {
            ComparingPredicate<?, C> eq = (ComparingPredicate<?, C>) predicate;
            processComparingClause(eq, sqlResult, objects);
        } else if (clazz == OrPredicate.class) {
            OrPredicate<C> or = (OrPredicate<C>) predicate;
            processCombiningClause(or.getLeft(), or.getRight(), "OR", sqlResult, objects);
        } else if (clazz == AndPredicate.class) {
            AndPredicate<C> and = (AndPredicate<C>) predicate;
            processCombiningClause(and.getLeft(), and.getRight(), "AND", sqlResult, objects);
        }  else if (clazz == IsNullPredicate.class) {
            IsNullPredicate<C> isNull = (IsNullPredicate<C>) predicate;
            processIsNullAndNotNullPredicate(false, isNull.getColumn(), sqlResult);
        } else if (clazz == IsNotNullPredicate.class) {
            IsNotNullPredicate<C> isNotNull = (IsNotNullPredicate<C>) predicate;
            processIsNullAndNotNullPredicate(true, isNotNull.getColumn(), sqlResult);
        } else if (clazz == AlwaysTruePredicate.class) {
            addAlwaysTruePredicate(sqlResult);
        } else {
            throw new IllegalStateException("Unhandled predicate '" + predicate + "'");
        }
    }

    private void addAlwaysTruePredicate(StringBuilder sqlResult) {
        sqlResult.append("1 = 1");
    }

    private void processComparingClause(ComparingPredicate<?, C> predicate, StringBuilder sqlResult,
                                        List<Object> objects) {
        if (predicate.getColumn().isColumnUsed(context)) {
            sqlResult.append(predicate.getColumn().resolveName(context))
                .append(convertComparingTypeToSqlOperator(predicate.getType()))
                .append("?");
            objects.add(predicate.getValue());
        } else {
            addAlwaysTruePredicate(sqlResult);
        }
    }

    private String convertComparingTypeToSqlOperator(ComparingPredicate.Type type) {
        switch (type) {
            case LESS:           return " < ";
            case LESS_EQUALS:    return " <= ";
            case EQUALS:         return " = ";
            case NOT_EQUALS:     return " <> ";
            case GREATER:        return " > ";
            case GREATER_EQUALS: return " >= ";
            default:
                throw new IllegalStateException("Unknown comparing predicate type '" + type + "'");
        }
    }

    private void processIsNullAndNotNullPredicate(boolean isNegated, Column<?, C> column, StringBuilder sqlResult) {
        if (column.isColumnUsed(context)) {
            final String condition = isNegated ? " IS NOT NULL" : " IS NULL";
            sqlResult.append(column.resolveName(context)).append(condition);
        } else {
            addAlwaysTruePredicate(sqlResult);
        }
    }

    private void processCombiningClause(Predicate<C> left, Predicate<C> right, String operator,
                                        StringBuilder sqlResult, List<Object> objects) {
        sqlResult.append("(");
        generateWhereClause(left, sqlResult, objects);
        sqlResult.append(") ").append(operator).append(" (");
        generateWhereClause(right, sqlResult, objects);
        sqlResult.append(")");
    }

    public static class WhereClauseResult {
        private final String generatedSql;
        private final List<Object> bindings;

        public WhereClauseResult(String generatedSql, List<Object> bindings) {
            this.generatedSql = generatedSql;
            this.bindings = bindings;
        }

        public String getGeneratedSql() {
            return generatedSql;
        }

        public List<Object> getBindings() {
            return bindings;
        }
    }
}
