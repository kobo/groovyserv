NAME := groovyserv
VERSION := $(shell grep Version: $(NAME).spec | tr -s " "| cut -d " " -f 2)

rpm:
	spectool -g  $(NAME).spec
	mkdir -p dist/{BUILD,RPMS,SPECS,SOURCES,SRPMS,install}
	mv v$(VERSION).tar.gz dist/SOURCES/
	rpmbuild -ba \
		--define "_topdir $(PWD)/dist" \
		--define "buildroot $(PWD)/dist/install" \
		--clean \
		$(NAME).spec
