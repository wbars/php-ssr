# php-ssr
```php
<?php
/**
 * @structuralSearchTemplate("Explicit magic methodCall")
 * @scope("open")
 * @variable(name = "b", minCount=1, maxCount=1, regExp="^__.+$")
 * @variable(name = "c", minCount=0, maxCount=inf)
 */
$_a->_b($_c);

/**
 * @structuralSearchTemplate("fopen call without binary safe modifier")
 * @scope("project")
 * @variable(name = "b", regExp=!"^.*b$")
 * @variable(name = "c", minCount=0, maxCount=1)
 * @variable(name = "d", minCount=0, maxCount=1)
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
