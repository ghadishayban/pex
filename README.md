# pex, a parsing library

This is super-alpha.

### Rationale 

PEGs (Parsing Expression Grammars) are more powerful than regexes, compose better, and are expressible using PODS (Plain Ol' Data Structures)

Deterministic parsers are a simpler model than those that produce ambiguity.

`pex` is implemented with a Virtual Machine, just like its inspiration LPEG.

### Fundamentals

Grammars are input as a quoted datastructure, just like Datomic queries.

```clj
(def Number '{number [digits (? fractional) (? exponent)]
              fractional ["." digits]
              exponent   ["e" (? (/ "+" "-")) digits]
              digits     [(class num) (* (class num))]})
```

The left hand side of the map is the name of the rule, the right hand side is the definition.
Any bare symbol inside a definition is a call to that rule.  Calls are *not* applied with parentheses.
Parentheses denote some special behavior.

Grammars are then compiled, like a java.util.regex.Pattern.
The compiled grammar can then be run upon inputs.

### Rule Fundamentals

String and chars literals match... literally
```clj
"foo"
```

Ordered Choice is the most important operation in a PEG. Rule `B` will only be attempted only if `A` fails:
```clj
(/ A B)
```

NB that `A` cannot be a prefix of `B` if you want `B` to ever match:
```clj
;; invalid, foo will always win over foobar
(/ "foo" "foobar")
```

Vectors denote sequencing rules together.  If you want `A` `B` & `C` to succeed sequentially:
```clj
[A B C]
```

### The Value Stack

TODO

`capture` places the region matched by the rule on the Value Stack

```clj
(capture integer (? fractional) (? exponent))
```

### Char Matchers

`class` refers symbolically to a matcher for a particular character class.

```clj
["42" (class alpha)]
```

There are several helpers that build up character classes.  Each character class must be passed into `pex/compile` as a matcher. `TODO` Elaborate

### Optionality or Repetition

`?` is an optional rule:
```clj
(? b)
```

`*` is repetition, 0 or more times.
```clj
(* foo)
```

The typical way to match separator delimited things:
```clj
[pattern (* separator pattern)]
```
### Parse Actions

`action` refers to a parse action, immediately invoking it.
```clj
(action make-integer)
```
Actions can manipulate the Value Stack by reducing over items captured,
updating the last item captured, or push a value.

There are also a few pre-built actions that access an efficient StringBuffer for mutation while building up Strings.

`EOI` means end of input. This only matches when input is exhausted, not when you're done parsing.

### Rule Macros

User supplied macros can expand rules to remove boilerplate.
`TODO` example

# Examples

JSON parser

