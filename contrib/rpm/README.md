# Contrib Notes

Here you will find unofficial rpmbuild scripts for Red Hat Enterprise Linux and Fedora.

## Distro support

This version of scripts has so far only been tested on RHEL7, CentOS7 and Fedora20. (probably, work with Oracle Linux7)

- RHEL7/CentOS7 x86_64
    - `groovy-2.3.7` package is seen as not provides in Official and 3rd Party repository(EPEL)... Therefore, you'll need to build rpm package of `groovy-2.3.7` before the build of `groovyserv`.
        - [It might be your: groovy.spec](https://gist.github.com/kazuhisya/1064519)


    - `golang` package is available through [EPEL](http://fedoraproject.org/wiki/EPEL) repository, and you need enable this repository.
- Fedora20,21 x86_6 
    - `groovy-2.3.7` package is seen as not provides in Official and 3rd Party repository(EPEL). Therefore, you'll need to build rpm package of `groovy-2.3.7` before the build of `groovyserv`.
        - [It might be your: groovy.spec](https://gist.github.com/kazuhisya/1064519)

## Building rpm Package (example)

settin up:

```bash
$ sudo yum install -y yum-utils rpmdevtools make
```

git clone and make:

```bash
$ git clone https://github.com/kobo/groovyserv.git
$ cd groovyserv/contrib/rpm/
$ sudo yum-builddep ./groovyserv.spec
$ make rpm
```

install package:

```bash
$ sudo yum install ./dist/RPMS/x86_64/groovyserv-*.el7.x86_64.rpm
```
