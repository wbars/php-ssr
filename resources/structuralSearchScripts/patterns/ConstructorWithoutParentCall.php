<?php

/**
 * @structuralSearchTemplate("Class with constructor without parent non-empty constructor")
 * @variable(name="arguments", minCount="0", maxCount="inf")
 * @variable(name="code", minCount="0", maxCount="inf", regExp=!"parent::__construct\(.*")
 * @variable(name="C", referenceName="Class with constructor")
 * @variable(name="a", minCount="0", maxCount="inf")
 * @ignoredVariables("_construct")
 */
class _A extends _C {

    public function __construct($_a) {
        _code
    }
}


/**
 * @structuralSearchTemplate("Class with constructor")
 * @ignoredVariables("_construct")
 * @variable(name="a", minCount="0", maxCount="inf")
 * @variable(name="code", minCount="1")
 * @severity("off")
 */
class _B {
    public function __construct($_a) {
        _code
    }
}