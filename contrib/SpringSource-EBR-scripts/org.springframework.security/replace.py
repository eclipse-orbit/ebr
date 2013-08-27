#!/usr/bin/env python

import re
import sys
from glob import glob

def replace(filename, pattern, replacement):
	try:
		with open(filename) as input:
			lines = input.readlines()
			with open(filename + "", "w") as output:
				for line in lines:
					if pattern in line:
						output.write(re.sub(pattern, replacement, line))
					else:
						output.write(line)
	except IOError, e:
		print("Unable to process %s due to %s" % (filename, e))

if __name__ == "__main__":

	if len(sys.argv) < 2:
		raise Exception("Insufficient arguments: Must provide [SOURCE TAG] and [TARGET TAG]")

	source_tag = sys.argv[1]
	target_tag = sys.argv[2]

	paths = [
		"org.springframework.security.acls", 
		"org.springframework.security.aspects",
		"org.springframework.security.cas",
		"org.springframework.security.config",
		"org.springframework.security.core",
		"org.springframework.security.crypto",
		"org.springframework.security.ldap",
		"org.springframework.security.openid",
		"org.springframework.security.remoting",
		"org.springframework.security.taglibs",
		"org.springframework.security.web"]

	for path in paths:
		replace("%s/%s/build.xml" % (path, target_tag), source_tag, target_tag)
		replace("%s/%s/ivy.xml" % (path, target_tag), source_tag, target_tag)
		[replace(manifest_file, source_tag, target_tag) for manifest_file in glob("%s/%s/*.mf" % (path, target_tag))]
	

