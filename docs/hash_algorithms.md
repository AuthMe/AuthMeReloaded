<!-- AUTO-GENERATED FILE! Do not edit this directly -->
<!-- File auto-generated on Sat Apr 29 18:27:40 CEST 2017. See docs/hashmethods/hash_algorithms.tpl.md -->

## Hash Algorithms
AuthMe supports the following hash algorithms for storing your passwords safely.


Algorithm | Recommendation | Hash length | ASCII |     | Salt type | Length | Separate?
--------- | -------------- | ----------- | ----- | --- | --------- | ------ | ---------
ARGON2 | Recommended | 96 |  | | Text | 16 | 
BCRYPT | Recommended | 60 |  | | Text |  | 
BCRYPT2Y | Recommended | 60 |  | | Text | 22 | 
CRAZYCRYPT1 | Do not use | 128 |  | | Username |  | 
DOUBLEMD5 | Deprecated | 32 |  | | None |  | 
IPB3 | Acceptable | 32 |  | | Text | 5 | Y
IPB4 | Does not work | 60 |  | | Text | 22 | Y
JOOMLA | Acceptable | 65 |  | | Text | 32 | 
MD5 | Deprecated | 32 |  | | None |  | 
MD5VB | Acceptable | 56 |  | | Text | 16 | 
MYBB | Acceptable | 32 |  | | Text | 8 | Y
PBKDF2 | Recommended | 165 |  | | Text | 16 | 
PBKDF2DJANGO | Acceptable | 77 | Y | | Text | 12 | 
PHPBB | Acceptable | 34 |  | | Text | 16 | 
PHPFUSION | Do not use | 64 | Y | |  |  | Y
ROYALAUTH | Do not use | 128 |  | | None |  | 
SALTED2MD5 | Acceptable | 32 |  | | Text |  | Y
SALTEDSHA512 | Recommended | 128 |  | |  |  | Y
SHA1 | Deprecated | 40 |  | | None |  | 
SHA256 | Recommended | 86 |  | | Text | 16 | 
SHA512 | Deprecated | 128 |  | | None |  | 
SMF | Do not use | 40 |  | | Username |  | 
TWO_FACTOR | Does not work | 16 |  | | None |  | 
WBB3 | Acceptable | 40 |  | | Text | 40 | Y
WBB4 | Recommended | 60 |  | | Text | 8 | 
WHIRLPOOL | Deprecated | 128 |  | | None |  | 
WORDPRESS | Acceptable | 34 |  | | Text | 9 | 
XAUTH | Recommended | 140 |  | | Text | 12 | 
XFBCRYPT |  | 60 |  | |  |  | 
CUSTOM |  |  |  |  |  |  |  |

<!-- AUTO-GENERATED FILE! Do not edit this directly -->

### Columns
#### Algorithm
The algorithm is the hashing algorithm used to store passwords with. Default is SHA256 and is recommended.
You can change the hashing algorithm in the config.yml: under `security`, locate `passwordHash`.

#### Recommendation
The recommendation lists our usage recommendation in terms of how secure it is (not how _well_ the algorithm works!).
- Recommended: The hash algorithm appears to be cryptographically secure and is one we recommend.
- Acceptable: There are safer algorithms that can be chosen but using the algorithm is generally OK.
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
<!-- AUTO-GENERATED FILE! Do not edit this directly -->

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

---

This page was automatically generated on the [AuthMe/AuthMeReloaded repository](https://github.com/AuthMe/AuthMeReloaded/tree/master/docs/) on Sat Apr 29 18:27:40 CEST 2017
