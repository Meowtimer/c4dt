#!/bin/bash
BRANCH=$(hg log --template '{branch}' -r $HG_NODE)

if [ "$BRANCH" == "default" ] ; then
	:
elif [ "$BRANCH" == "statictyping" ] ; then
	:
fi