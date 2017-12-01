<!-- {gen_warning} -->
<!-- File auto-generated on {gen_date}. See docs/hashmethods/hash_algorithms.tpl.md -->

## Hash Algorithms
AuthMe supports the following hash algorithms for storing your passwords safely.


Algorithm | Recommendation | Hash length | ASCII |     | Salt type | Length | Separate?
--------- | -------------- | ----------- | ----- | --- | --------- | ------ | ---------
[#algorithms]
{name} | {recommendation} | {hash_length} | {ascii_restricted} | | {salt_type} | {salt_length} | {separate_salt}
[/#algorithms]CUSTOM |  |  |  |  |  |  |  |

<!-- {gen_warning} -->

### Columns
#### Algorithm
The algorithm is the hashing algorithm used to store passwords with. Default is SHA256 and is recommended.
You can change the hashing algorithm in the config.yml: under `security`, locate `passwordHash`.

#### Recommendation
The recommendation lists our usage recommendation in terms of how secure it is (not how _well_ the algorithm works!).
- Recommended: The hash algorithm appears to be cryptographically secure and is one we recommend.
- Acceptable: There are safer algorithms that can be chosen but using the algorithm is generally OK.
- Deprecated: Cannot be used anymore actively or in the near future.
- Do not use: Hash algorithm isn't sufficiently secure. Use only if required to hook into another system.
- Does not work: The algorithm does not work properly; do not use.

#### Hash Length
The length of the hashes the algorithm produces. Note that the hash length is not (primarily) indicative of
whether an algorithm is secure or not.

#### ASCII
If denoted with a **y**, means that the algorithm is restricted to ASCII characters only, i.e. it will simply ignore
"special characters" such as `ÿ` or `Â`. Note that we do not recommend the use of "special characters" in passwords.

#### Salt Columns
Before hashing, a _salt_ may be appended to the password to make the hash more secure. The following columns describe
the salt the algorithm uses.
<!-- {gen_warning} -->

##### Salt Type
We do not recommend the usage
of any algorithm that doesn't use a randomly generated text as salt. This "salt type" column indicates what type of
salt the algorithm uses:
- Text: randomly generated text (see also the following column, "Length")
- Username: the salt is constructed from the username (bad)
- None: the algorithm uses no salt (bad)

##### Length
If applicable (salt type is "Text"), indicates the length of the generated salt. The longer the better.
If this column is empty when the salt type is "Text", it typically means the salt length can be defined in config.yml.

##### Separate
If denoted with a **y**, it means that the salt is stored in a separate column in the database. This is neither good
or bad.

{gen_footer}
