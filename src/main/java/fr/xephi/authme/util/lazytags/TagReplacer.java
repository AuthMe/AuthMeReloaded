package fr.xephi.authme.util.lazytags;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Replaces tags lazily by first determining which tags are being used
 * and only applying those replacements afterwards.
 *
 * @param <A> the argument type
 */
public final class TagReplacer<A> {

    @NotNull
    private final List<Tag<A>> tags;
    @NotNull
    private final Collection<String> messages;

    /**
     * Private constructor. Use {@link #newReplacer(Collection, Collection)}.
     *
     * @param tags the tags that are being used in the messages
     * @param messages the messages
     */
    private TagReplacer(@NotNull List<Tag<A>> tags, @NotNull Collection<String> messages) {
        this.tags = tags;
        this.messages = messages;
    }

    /**
     * Creates a new instance of this class, which will provide the given
     * messages adapted with the provided tags.
     *
     * @param allTags all available tags
     * @param messages the messages to use
     * @param <A> the argument type
     * @return new tag replacer instance
     */
    @NotNull
    public static <A> TagReplacer<A> newReplacer(@NotNull Collection<Tag<A>> allTags,
                                                 @NotNull Collection<String> messages) {
        List<Tag<A>> usedTags = determineUsedTags(allTags, messages);
        return new TagReplacer<>(usedTags, messages);
    }

    /**
     * Returns the messages with the tags applied for the given argument.
     *
     * @param argument the argument to get the messages for
     * @return the adapted messages
     */
    @NotNull
    public List<String> getAdaptedMessages(@NotNull A argument) {
        // Note ljacqu 20170121: Using a Map might seem more natural here but we avoid doing so for performance
        // Although the performance gain here is probably minimal...
        List<TagValue> tagValues = new LinkedList<>();
        for (Tag<A> tag : tags) {
            tagValues.add(new TagValue(tag.getName(), tag.getValue(argument)));
        }

        List<String> adaptedMessages = new LinkedList<>();
        for (String line : messages) {
            String adaptedLine = line;
            for (TagValue tagValue : tagValues) {
                adaptedLine = adaptedLine.replace(tagValue.tag, tagValue.value);
            }
            adaptedMessages.add(adaptedLine);
        }
        return adaptedMessages;
    }

    /**
     * Determines which tags are used somewhere in the given list of messages.
     *
     * @param allTags all available tags
     * @param messages the messages
     * @param <A> argument type
     * @return tags used at least once
     */
    @NotNull
    private static <A> List<Tag<A>> determineUsedTags(@NotNull Collection<Tag<A>> allTags,
                                                      @NotNull Collection<String> messages) {
        return allTags.stream()
            .filter(tag -> messages.stream().anyMatch(msg -> msg.contains(tag.getName())))
            .collect(Collectors.toList());
    }

    /** (Tag, value) pair. */
    private static final class TagValue {

        /** The tag to replace. */
        @NotNull
        private final String tag;
        /** The value to replace with. */
        @NotNull
        private final String value;

        TagValue(@NotNull String tag, @NotNull String value) {
            this.tag = tag;
            this.value = value;
        }

        @Override
        public String toString() {
            return "TagValue[tag='" + tag + "', value='" + value + "']";
        }
    }

}
