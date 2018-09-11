package fr.xephi.authme.message.updater;

import ch.jalu.configme.configurationdata.ConfigurationDataImpl;
import ch.jalu.configme.properties.Property;
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
        getAllMessageProperties().stream()
            .filter(prop -> prop.isPresent(reader))
            .forEach(prop -> setValue(prop, prop.determineValue(reader)));
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
