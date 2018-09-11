<?php

/**
 * @structuralSearchTemplate("Loop that immediately return")
 * @variable(name="before", minCount="0", maxCount="inf")
 * @variable(name="after", minCount="0", maxCount="inf")
 * @variable(name="code", minCount="0", maxCount="1")
 */
foreach ($_list as $_item) {
    _before
    return _code;
    _after
}