#!/bin/bash
BRANCH=$(hg log --template '{branch}' -r $HG_NODE)

if [ "$BRANCH" == "default" ] ; then
	:
elif [ "$BRANCH" == "statictyping" ] ; then
	hg push -r statictyping bbst
fi