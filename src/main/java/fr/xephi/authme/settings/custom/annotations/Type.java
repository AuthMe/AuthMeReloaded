package fr.xephi.authme.settings.custom.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.xephi.authme.settings.custom.annotations.Type.SettingType;

/**
*
* Set the type of a field value
*
* @author xephi59
*
*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Type {

	public enum SettingType {
		String(0),
		Int(1),
		Boolean(2),
		Double(3),
		StringList(4),
		Long(5);

		private int type;

		SettingType(int type)
		{
			this.type = type;
		}

		public int getType()
		{
			return this.type;
		}
	}

	public SettingType value() default SettingType.String;
}
