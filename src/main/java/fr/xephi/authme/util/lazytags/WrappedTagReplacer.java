package fr.xephi.authme.util.lazytags;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Applies tags lazily to the String property of an item. This class wraps
 * a {@link TagReplacer} with the extraction of the String property and
 * the creation of new items with the adapted string property.
 *
 * @param <T> the item type
 * @param <A> the argument type to evaluate the replacements
 */
public class WrappedTagReplacer<T, A> {

    @NotNull
    private final Collection<T> items;
    @NotNull
    private final BiFunction<T, String, ? extends T> itemCreator;
    @NotNull
    private final TagReplacer<A> tagReplacer;

    /**
     * Constructor.
     *
     * @param allTags all available tags
     * @param items the items to apply the replacements on
     * @param stringGetter getter of the String property to adapt on the items
     * @param itemCreator a function taking (T, String): the original item and the adapted String, returning a new item
     */
    public WrappedTagReplacer(@NotNull Collection<Tag<A>> allTags,
                              @NotNull Collection<T> items,
                              @NotNull Function<? super T, String> stringGetter,
                              @NotNull BiFunction<T, String, ? extends T> itemCreator) {
        this.items = items;
        this.itemCreator = itemCreator;

        List<String> stringItems = items.stream().map(stringGetter).collect(Collectors.toList());
        tagReplacer = TagReplacer.newReplacer(allTags, stringItems);
    }

    /**
     * Creates adapted items for the given argument.
     *
     * @param argument the argument to adapt the items for
     * @return the adapted items
     */
    @NotNull
    public List<T> getAdaptedItems(@NotNull A argument) {
        List<String> adaptedStrings = tagReplacer.getAdaptedMessages(argument);
        List<T> adaptedItems = new LinkedList<>();

        Iterator<T> originalItemsIter = items.iterator();
        Iterator<String> newStringsIter = adaptedStrings.iterator();
        while (originalItemsIter.hasNext() && newStringsIter.hasNext()) {
            adaptedItems.add(itemCreator.apply(originalItemsIter.next(), newStringsIter.next()));
        }
        return adaptedItems;
    }

}
