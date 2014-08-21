NAME := groovyserv
VERSION := $(shell grep Version: $(NAME).spec | cut -d " " -f 2)

rpm:
	spectool -g  $(NAME).spec
	mkdir -p dist/{BUILD,RPMS,SPECS,SOURCES,SRPMS,install}
	mv $(NAME)-*-src.zip dist/SOURCES/
	rpmbuild -ba \
		--define "_topdir $(PWD)/dist" \
		--define "buildroot $(PWD)/dist/install" \
		--clean \
		$(NAME).spec
