package fr.xephi.authme.settings.properties;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class responsible for retrieving all {@link Property} fields
 * from {@link SettingsClass} implementations via reflection.
 */
public final class SettingsFieldRetriever {

    /** The classes to scan for properties. */
    private static final List<Class<? extends SettingsClass>> CONFIGURATION_CLASSES = Arrays.asList(
        DatabaseSettings.class,     ConverterSettings.class, PluginSettings.class,
        RestrictionSettings.class,  EmailSettings.class,     HooksSettings.class,
        ProtectionSettings.class,   PurgeSettings.class,     SecuritySettings.class,
        RegistrationSettings.class, BackupSettings.class);

    private SettingsFieldRetriever() {
    }

    /**
     * Scan all given classes for their properties and return the generated {@link PropertyMap}.
     *
     * @return PropertyMap containing all found properties and their associated comments
     * @see #CONFIGURATION_CLASSES
     */
    public static PropertyMap getAllPropertyFields() {
        PropertyMap properties = new PropertyMap();
        for (Class<?> clazz : CONFIGURATION_CLASSES) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                Property property = getPropertyField(field);
                if (property != null) {
                    properties.put(property, getCommentsForField(field));
                }
            }
        }
        return properties;
    }

    private static String[] getCommentsForField(Field field) {
        if (field.isAnnotationPresent(Comment.class)) {
            return field.getAnnotation(Comment.class).value();
        }
        return new String[0];
    }

    /**
     * Return the given field's value if it is a static {@link Property}.
     *
     * @param field The field's value to return
     * @return The property the field defines, or null if not applicable
     */
    private static Property<?> getPropertyField(Field field) {
        field.setAccessible(true);
        if (field.isAccessible() && Property.class.equals(field.getType()) && Modifier.isStatic(field.getModifiers())) {
            try {
                return (Property) field.get(null);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not fetch field '" + field.getName() + "' from class '"
                    + field.getDeclaringClass().getSimpleName() + "': " + StringUtils.formatException(e));
            }
        }
        return null;
    }

}
