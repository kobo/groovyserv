# Contrib Notes

Here you will find unofficial rpmbuild scripts.

## Distro support

This version of scripts has so far only been tested on Fedora. (probably, work with Red Hat Enterprise Linux 7)

- Fedora 25 x86_64

## Compiled Package

- You can find prebuilt rpm binary from here
    - [FedoraCopr khara/groovyserv Copr](https://copr.fedoraproject.org/coprs/khara/groovyserv/)

```bash
$ sudo dnf copr enable khara/groovyserv
$ sudo dnf install -y groovyserv
```

## Building rpm Package (example)

settin up:

```bash
$ sudo dnf install -y dnf-plugins-core rpmdevtools make
```

git clone and make:

```bash
$ git clone https://github.com/kobo/groovyserv.git
$ cd groovyserv/contrib/rpm/
$ sudo dnf builddep -y ./groovyserv.spec
$ make rpm
```

install package:

```bash
$ sudo dnf install ./dist/RPMS/x86_64/groovyserv-*.x86_64.rpm
```
