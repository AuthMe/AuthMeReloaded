package fr.xephi.authme.datasource.queries;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;

public class Query {

	private DataSource source;
	private String selector = null;
	private String from = null;
	private HashMap<String, String> where = new HashMap<String, String>();
	private List<String> into = new ArrayList<String>();
	private List<String> values = new ArrayList<String>();
	private List<String> updateSet = new ArrayList<String>();
	private boolean isSelect = false;
	private boolean isDelete = false;
	private boolean isUpdate = false;
	private boolean isInsert = false;
	private String buildQuery = "";

	/**
	 *
	 * @param source
	 */
	public Query(DataSource source)
	{
		this.source = source;
	}

	/**
	 *
	 * @param selector
	 * @return Query instance
	 */
	public Query select(String selector)
	{
		this.selector = selector;
		isSelect = true;
		isDelete = false;
		isUpdate = false;
		isInsert = false;
		return this;
	}

	/**
	 *
	 * @return Query instance
	 */
	public Query update()
	{
		isSelect = false;
		isDelete = false;
		isUpdate = true;
		isInsert = false;
		return this;
	}

	/**
	 *
	 * @return Query instance
	 */
	public Query delete()
	{
		isSelect = false;
		isDelete = true;
		isUpdate = false;
		isInsert = false;
		return this;
	}

	/**
	 *
	 * @param selector
	 * @return Query instance
	 */
	public Query insert()
	{
		isSelect = false;
		isDelete = false;
		isUpdate = false;
		isInsert = true;
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
		if (isSelect)
		{
			str.append("SELECT ").append(selector).append(" FROM ").append(from);
		}
		else if (isDelete)
		{
			str.append("DELETE FROM ").append(from);
		}
		else if (isUpdate)
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
		else if (isInsert)
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
