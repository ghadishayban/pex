# pex, a parsing library

### Fundamentals

String and chars literals match... literally `"foo"`

Ordered Choice is the most important operation in a PEG. `B` will only be attempted only if `A` fails:
```clj
(/ A B)
```

Vectors denote sequencing rules together.  If you want `A` `B` & `C` to succeed sequentially:
```clj
[A B C]
```

There are many special forms besides `/` ordered choice:

`class` refers symbolically to a Matcher for a character class

```clj
["42" (class alpha)]
```

There are several helpers that build up character classes.  Each character class must be passed into `pex/compile` as a matcher. `TODO` Elaborate

`capture` places the region matched by the rule on the Value Stack

```clj
(capture integer (? fractional) (? exponent))
```

`EOI` means end of input. This only matches when input is exhausted, not when you're done parsing.

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
(pattern (* separator pattern))
```

`action` refers to a parse action, immediately invoking it.
```clj
(action make-integer)
```
Actions can manipulate the Value Stack by reducing over items captured,
updating the last item captured, or pushing a value.

There are also a few prebuilt actions that access an efficient StringBuffer for mutation while building up Strings.

### Rule Macros

User supplied macros can expand rules to remove boilerplate.
`TODO` example


# Rationale 

PEGs (Parsing Expression Grammars) are more powerful than regexes, compose better, and are expressible using PODS (Plain Ol' Data Structures)

Combinators compose, but need special care not to blow the stack in a language without TCO.  Let's implement the parser using a virtual machine [ref].

Deterministic parsers are a simpler model than those that produce ambiguity.

# Examples


