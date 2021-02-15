# Find Regex

A GitHub action to find a regex in files or the output of commands.

Files will always be checked first, all commands are ran in the current working directory.

The first match is returned.

#### Inputs

Only `regex` is required, although if `files` and `commands` are both empty nowhere will be searched.

* `regex` - the regex to look for. Can use any syntax supported by Kotlin JS.

* `files` - files to check. This happens before commands are run. Comma separated list, will be un-quoted un-escaped unless
  enclosed in `'`.
* `commands` - commands to check the stdout of. Comma separated list, will be un-quoted un-escaped unless
  enclosed in `'`.
* `require-match` - whether to error if no match is found.  Defaults to true.
* `group` - the capture group to use as output.  Can be 0 to use the entire match.  Default is `1`.
* `ignore-case` - whether the regex should ignore case.  Defaults to true.
* `multiline` - whether the regex should be multi line.  Defaults to false.

#### Outputs
 * `match` - the match or match group, according to `group`.

### Example

To find the version of a gradle project:
