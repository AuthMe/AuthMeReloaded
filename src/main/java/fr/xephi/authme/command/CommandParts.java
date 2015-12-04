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
    private final List<String> parts = new ArrayList<>();

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
     * @param parts The list of parts.
     */
    public CommandParts(List<String> parts) {
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
     * Get the number of parts.
     *
     * @return Part count.
     */
    public int getCount() {
        return this.parts.size();
    }

    /**
     * Get a part by its index.
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
     * Convert the parts to a string.
     *
     * @return The part as a string.
     */
    @Override
    public String toString() {
        return StringUtils.join(" ", this.parts);
    }
}
