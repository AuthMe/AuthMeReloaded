package fr.xephi.authme.settings.custom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.CustomConfiguration;
import fr.xephi.authme.settings.custom.annotations.Comment;
import fr.xephi.authme.settings.custom.annotations.Type;

public class CustomSetting extends CustomConfiguration {

	private File configFile;
	public boolean isFirstLaunch = false;

	public CustomSetting(File file) {
		super(file);
		this.configFile = file;
		try {
			if (!configFile.exists())
			{
				isFirstLaunch = true;
				configFile.createNewFile();
			}
			else
			{
				load();
				loadValues();
			}
			save();
		} catch (IOException e)
		{
			ConsoleLogger.writeStackTrace(e);
		}
	}

	@Override
	public boolean reLoad()
	{
        boolean out = true;
        if (!configFile.exists()) {
            try {
				configFile.createNewFile();
	            save();
			} catch (IOException e) {
				out = false;
			}
        }
        if (out)
        {
            load();
            loadValues();
        }
        return out;
	}

	public void loadValues()
	{
		for (Field f : this.getClass().getDeclaredFields())
		{
			if (!f.isAnnotationPresent(Type.class))
				continue;
			f.setAccessible(true);
			try {
				switch (f.getAnnotation(Type.class).value())
				{
				case Boolean:
					f.setBoolean(this, this.getBoolean(f.getName()));
					break;
				case Double:
					f.setDouble(this, this.getDouble(f.getName()));
					break;
				case Int:
					f.setInt(this, this.getInt(f.getName()));
					break;
				case Long:
					f.setLong(this, this.getLong(f.getName()));
					break;
				case String:
					f.set(this, this.getString(f.getName()));
					break;
				case StringList:
					f.set(this, this.getStringList(f.getName()));
					break;
				default:
					break;
				}
			} catch (Exception e)
			{
				ConsoleLogger.writeStackTrace(e);
			}
		}
	}

	@Override
	public void save()
	{
		FileWriter writer = null;
		try {
			writer = new FileWriter(configFile);
			writer.write("");
			for (Field f : this.getClass().getDeclaredFields())
			{
				if (!f.isAnnotationPresent(Comment.class))
					continue;
				if (!f.isAnnotationPresent(Type.class))
					continue;
				for (String s : f.getAnnotation(Comment.class).value())
				{
					writer.append("# " + s + "\n");
				}
				writer.append(f.getName() + ": ");
				switch (f.getAnnotation(Type.class).value())
				{
				case Boolean:
					writer.append(f.getBoolean(this) ? "true" : "false");
					break;
				case Double:
					writer.append("" + f.getDouble(this));
					break;
				case Int:
					writer.append("" + f.getInt(this));
					break;
				case String:
					writer.append("'" + f.get(this).toString() + "'");
					break;
				case StringList:
					@SuppressWarnings("unchecked")
					List<String> list = (List<String>) f.get(this);
					writer.append("\n");
					if (list.isEmpty())
						writer.write("[]");
					else
						for (String s : list)
							writer.append("    - '" + s + "'\n");
					break;
				case Long:
					writer.append("" + f.getLong(this));
					break;
				default:
					break;

				}
				writer.append("\n");
				writer.flush();
			}
			writer.close();
		} catch (Exception e) {
			ConsoleLogger.writeStackTrace(e);
		}
	}

}
