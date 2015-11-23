package fr.xephi.authme.command;

import fr.xephi.authme.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class CommandParts {

    /**
     * The list of parts for this command.
     */
    private List<String> parts = new ArrayList<>();

    /**
     * Constructor.
     */
    public CommandParts() {
    }

    /**
     * Constructor.
     *
     * @param part The part to add.
     */
    public CommandParts(String part) {
        this.parts.add(part);
    }

    /**
     * Constructor.
     *
     * @param commandParts The command parts instance.
     */
    public CommandParts(CommandParts commandParts) {
        this.parts.addAll(commandParts.getList());
    }

    /**
     * Constructor.
     *
     * @param parts The list of parts.
     */
    public CommandParts(List<String> parts) {
        this.parts.addAll(parts);
    }

    /**
     * Constructor.
     *
     * @param base  The base part.
     * @param parts The list of additional parts.
     */
    public CommandParts(String base, List<String> parts) {
        this.parts.add(base);
        this.parts.addAll(parts);
    }

    /**
     * Get the command parts.
     *
     * @return Command parts.
     */
    public List<String> getList() {
        return this.parts;
    }

    /**
     * Add a part.
     *
     * @param part The part to add.
     *
     * @return The result.
     */
    public boolean add(String part) {
        return this.parts.add(part);
    }

    /**
     * Add some parts.
     *
     * @param parts The parts to add.
     *
     * @return The result.
     */
    public boolean add(List<String> parts) {
        return this.parts.addAll(parts);
    }

    /**
     * Add some parts.
     *
     * @param parts The parts to add.
     *
     * @return The result.
     */
    public boolean add(String[] parts) {
        for (String entry : parts)
            add(entry);
        return true;
    }

    /**
     * Get the number of parts.
     *
     * @return Part count.
     */
    public int getCount() {
        return this.parts.size();
    }

    /**
     * Get a part by it's index.
     *
     * @param i Part index.
     *
     * @return The part.
     */
    public String get(int i) {
        // Make sure the index is in-bound
        if (i < 0 || i >= getCount())
            return null;

        // Get and return the argument
        return this.parts.get(i);
    }

    /**
     * Get a range of the parts starting at the specified index up to the end of the range.
     *
     * @param start The starting index.
     *
     * @return The parts range. Arguments that were out of bound are not included.
     */
    public List<String> getRange(int start) {
        return getRange(start, getCount() - start);
    }

    /**
     * Get a range of the parts.
     *
     * @param start The starting index.
     * @param count The number of parts to get.
     *
     * @return The parts range. Parts that were out of bound are not included.
     */
    public List<String> getRange(int start, int count) {
        // Create a new list to put the range into
        List<String> elements = new ArrayList<>();

        // Get the range
        for (int i = start; i < start + count; i++) {
            // Get the part and add it if it's valid
            String element = get(i);
            if (element != null)
                elements.add(element);
        }

        // Return the list of parts
        return elements;
    }

    /**
     * Get the difference value between two references. This won't do a full compare, just the last reference parts instead.
     *
     * @param other The other reference.
     *
     * @return The result from zero to above. A negative number will be returned on error.
     */
    public double getDifference(CommandParts other) {
        return getDifference(other, false);
    }

    /**
     * Get the difference value between two references.
     *
     * @param other       The other reference.
     * @param fullCompare True to compare the full references as far as the range reaches.
     *
     * @return The result from zero to above. A negative number will be returned on error.
     */
    public double getDifference(CommandParts other, boolean fullCompare) {
        // Make sure the other reference is correct
        if (other == null)
            return -1;

        // Get the range to use
        int range = Math.min(this.getCount(), other.getCount());

        // Get and the difference
        if (fullCompare)
            return StringUtils.getDifference(this.toString(), other.toString());
        return StringUtils.getDifference(this.getRange(range - 1, 1).toString(), other.getRange(range - 1, 1).toString());
    }

    /**
     * Convert the parts to a string.
     *
     * @return The part as a string.
     */
    @Override
    public String toString() {
        return StringUtils.join(" ", this.parts);
    }
}
