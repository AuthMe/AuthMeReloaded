#!/bin/sh
#
# That script sort the content of all files in the current directory
#
ls | grep -v .sort | while read file; do sort "$file" > "$file".sort; done
for file in *.sort; do mv "$file" "${file%%.sort}"; done
