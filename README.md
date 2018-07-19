# php-ssr
```php
<?php
/**
 * @structuralSearchTemplate("Explicit magic methodCall")
 * @scope("open")
 * @variable(name = "b", minCount=1, maxCount=1, regExp=!"^__.+$")
 */
$_a->_b($c);

/**
 * @structuralSearchTemplate("fopen call without binary safe modifier")
 * @scope("project")
 * @variable(name = "c", referenceConstraint="fopen($_a, \"_b\", $_c, $_d)")
 */
fopen($_a, "_b", $_c, $_d);

/**
 * @structuralSearchTemplate("Final method inside final class")
 * @variable(name="b", minCount=1, maxCount="inf")
 */
final class _a {
    final function _b()
}
```
