<?php
class StructuralSearchConstraints {
    /**
     * Name of the variable.
     * <br/>
     * Variables with same name will be referred to the same variable constraint, so
     * it's convenient to have at most one variable definition for each variable name
     */
    const NAME = "name";
    /**
     * Regular expression which variable text should satisfy.
     * <br/>
     * You can put ! before the regular expression to make constraint work in reverse way.
     */
    const REGEXP = "regExp";
    /**
     * Minimum occurrences of the variable.
     */
    const MIN_COUNT = "minCount";
    /**
     * Maximum occurrences of the variable.
     * <br/>
     * Set info to search the patterns with any occurrences count, 0 if no occurrences expected, positive integer otherwise
     */
    const MAX_COUNT = "maxCount";
    /**
     * |-separated types of the variable.
     * <br/>
     * FQN of the class can be specified as type as well as std types such as primitives or arrays
     * Variable with type that equals any of defined type will be matched.
     */
    const TYPE = "type";
    /**
     * Name of the pattern for resolved value of the reference
     */
    const CONSTRAINT_NAME = "referenceName";
    /**
     * Pattern for resolved value of the reference
     */
    const CONSTRAINT = "reference";
    /**
     * Is the variable target of the search.
     * <br/>
     * Only one target can be set for each pattern. In case of absence complete match will be computed.
     */
    const TARGET = "target";
}