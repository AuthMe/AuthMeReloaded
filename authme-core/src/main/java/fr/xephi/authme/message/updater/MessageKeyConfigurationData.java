package fr.xephi.authme.message.updater;

import ch.jalu.configme.configurationdata.ConfigurationDataImpl;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.convertresult.PropertyValue;
import ch.jalu.configme.resource.PropertyReader;
import fr.xephi.authme.message.MessageKey;

import java.util.List;
import java.util.Map;

public class MessageKeyConfigurationData extends ConfigurationDataImpl {

    /**
     * Constructor.
     *
     * @param propertyListBuilder property list builder for message key properties
     * @param allComments registered comments
     */
    public MessageKeyConfigurationData(MessageUpdater.MessageKeyPropertyListBuilder propertyListBuilder,
                                       Map<String, List<String>> allComments) {
        super(propertyListBuilder.getAllProperties(), allComments);
    }

    @Override
    public void initializeValues(PropertyReader reader) {
        for (Property<String> property : getAllMessageProperties()) {
            PropertyValue<String> value = property.determineValue(reader);
            if (value.isValidInResource()) {
                setValue(property, value.getValue());
            }
        }
    }

    @Override
    public <T> T getValue(Property<T> property) {
        // Override to silently return null if property is unknown
        return (T) getValues().get(property.getPath());
    }

    @SuppressWarnings("unchecked")
    public List<Property<String>> getAllMessageProperties() {
        return (List) getProperties();
    }

    public String getMessage(MessageKey messageKey) {
        return getValue(new MessageUpdater.MessageKeyProperty(messageKey));
    }

    public void setMessage(MessageKey messageKey, String message) {
        setValue(new MessageUpdater.MessageKeyProperty(messageKey), message);
    }
}
