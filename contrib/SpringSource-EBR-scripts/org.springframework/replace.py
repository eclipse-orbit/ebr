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

def set_release_type(filename, source_tag, target_tag):
	try:
		with open(filename) as input:
			lines = input.readlines()
			with open(filename + "", "w") as output:
				for line in lines:
					if "release.type" in line:
						parts = line.split(" ")
						for index, part in enumerate(parts):
							if "milestone" in part and "RELEASE" in target_tag:
								parts[index] = 'value="release"/>\n'
							if "release" in part and "RC" in target_tag:
								parts[index] = 'value="milestone"/>\n'
							if "release" in part and "M" in target_tag.split(".")[-1]:
								parts[index] = 'value="milestone"/>\n'
						output.write(" ".join(parts))
						
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
		"org.springframework.aop", 
		"org.springframework.aspects",
		"org.springframework.beans",
		"org.springframework.context.support",
		"org.springframework.context",
		"org.springframework.core",
		"org.springframework.expression",
		"org.springframework.instrument.tomcat",
		"org.springframework.instrument",
		"org.springframework.jdbc",
		"org.springframework.jms",
		"org.springframework.orm",
		"org.springframework.oxm",
		"org.springframework.web.struts",
		"org.springframework.test",
		"org.springframework.transaction",
		"org.springframework.web.portlet",
		"org.springframework.web",
		"org.springframework.web.servlet"]

	for path in paths:
		replace("%s/%s/build.xml" % (path, target_tag), source_tag, target_tag)
		set_release_type("%s/%s/build.xml" % (path, target_tag), source_tag, target_tag)
		replace("%s/%s/ivy.xml" % (path, target_tag), source_tag, target_tag)
		[replace(manifest_file, source_tag, target_tag) for manifest_file in glob("%s/%s/*.mf" % (path, target_tag))]
	

