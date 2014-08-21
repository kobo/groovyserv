# Contrib Notes

Here you will find unofficial rpmbuild scripts for Red Hat Enterprise Linux and Fedora.

## Distro support

This version of scripts has so far only been tested on RHEL7, CentOS7 and Fedora20. (probably, work with Oracle Linux7)

- RHEL7 x86_64
    - `groovy` package is available through `Red Hat Enterprise Linux 7 Server - Optional` channel, and you need enable this channel.
    - [How to enable a repository using Red Hat Subscription Manager (RHSM)? - Red Hat Customer Portal](https://access.redhat.com/node/265523)
- CentOS7 x86_64
    - When you use CentOS7, there is no problem. `gorrovy` included in the Base Repository.
- Fedora20 x86_64


## Building rpm Package

setting up:

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
$ sudo yum install ./dist/RPMS/x86_64/groovyserv-*.rpm
```
