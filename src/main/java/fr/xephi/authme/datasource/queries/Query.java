package fr.xephi.authme.datasource.queries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fr.xephi.authme.datasource.DataSource;

public class Query {

	private String selector = null;
	private String from = null;
	private HashMap<String, String> where = new HashMap<String, String>();
	private List<String> into = new ArrayList<String>();
	private List<String> values = new ArrayList<String>();
	private List<String> updateSet = new ArrayList<String>();
	private QueryType type;
	private String buildQuery = "";

	/**
	 *
	 * @param source
	 */
	public Query()
	{
	}

	/**
	 *
	 * @param selector
	 * @return Query instance
	 */
	public Query select(String selector)
	{
		this.selector = selector;
		type = QueryType.SELECT;
		return this;
	}

	/**
	 *
	 * @return Query instance
	 */
	public Query update()
	{
		type = QueryType.UPDATE;
		return this;
	}

	/**
	 *
	 * @return Query instance
	 */
	public Query delete()
	{
		type = QueryType.DELETE;
		return this;
	}

	/**
	 *
	 * @param selector
	 * @return Query instance
	 */
	public Query insert()
	{
		type = QueryType.INSERT;
		return this;
	}

	/**
	 *
	 * @param column
	 * @return
	 */
	public Query addInsertInto(String column)
	{
		into.add(column);
		return this;
	}

	/**
	 *
	 * @param value
	 * @return
	 */
	public Query addInsertValue(String value)
	{
		values.add(value);
		return this;
	}

	/**
	 *
	 * @param set
	 * @return
	 */
	public Query addUpdateSet(String set)
	{
		updateSet.add(set);
		return this;
	}

	/**
	 *
	 * @param from
	 * @return Query instance
	 */
	public Query from(String from)
	{
		this.from = from;
		return this;
	}

	/**
	 *
	 * @param where
	 * @param String and/or/null
	 * @return Query instance
	 */
	public Query addWhere(String where, String logic)
	{
		this.where.put(where, logic);
		return this;
	}

	public Query build(){
		StringBuilder str = new StringBuilder();
		switch (type) {
			case SELECT:
			{
				str.append("SELECT ").append(selector).append(" FROM ").append(from);
			}
			case DELETE:
			{
				str.append("DELETE FROM ").append(from);
			}
			case UPDATE:
			{
				str.append("UPDATE ").append(from).append(" SET ");
				Iterator<String> iter = updateSet.iterator();
				while (iter.hasNext())
				{
					String s = iter.next();
					str.append(s);
					if (iter.hasNext())
						str.append(", ");
				}
			}
			case INSERT:
			{
				str.append("INSERT INTO ").append(from).append(" ('");
				Iterator<String> iter = into.iterator();
				while (iter.hasNext())
				{
					String s = iter.next();
					str.append(s);
					if (iter.hasNext())
						str.append("', '");
					else
						str.append("')");
				}
				str.append(" VALUES ('");
				iter = values.iterator();
				while (iter.hasNext())
				{
					String s = iter.next();
					str.append(s);
					if (iter.hasNext())
						str.append("', '");
					else
						str.append("')");
				}
			}
		}
		if (!where.isEmpty())
		{
			str.append(" WHERE");
			for (String key : where.keySet())
			{
				if (where.get(key) != null)
					str.append(" ").append(where.get(key));
				str.append(" ").append(key);
			}
		}
		str.append(";");
		this.buildQuery = str.toString();
		return this;
	}

	public String getQuery() {
		return this.buildQuery;
	}
}
