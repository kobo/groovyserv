Summary: GroovyServ, a process server for Groovy
Name: groovyserv
Version: 0.8
Release: 0%{?dist}
License: Apache License, Version 2.0
Group: Development/Languages
Provides: groovyserv
Requires: java-1.6.0-openjdk
Requires: groovy >= 1.7.0
Source0: groovyserv-0.8
BuildArch: x86_64
BuildRoot: %{_tmppath}/%{name}-%{version}-root
Packager: Kazuhisa Hara <kazuhisya@gmail.com>

%description
Provides the GroovyServ mechanism for faster groovy execution.

%prep

%build

%install
mkdir -p $RPM_BUILD_ROOT/opt $RPM_BUILD_ROOT/usr/local/bin
cp -Rp %{SOURCE0} $RPM_BUILD_ROOT/opt/groovyserv
for file in groovyserver groovyclient ; do
  ln -s /opt/groovyserv/bin/$file $RPM_BUILD_ROOT/usr/local/bin
done
#we're not interested in the ruby client
rm $RPM_BUILD_ROOT/opt/groovyserv/bin/groovyclient.rb


#required since our sources are in svn
find $RPM_BUILD_ROOT -name .svn -type d | while read svndir ; do rm -rf $svndir ; done

%clean
rm -Rf $RPM_BUILD_ROOT

%post

%files
%defattr(-,root,root)

/opt/groovyserv
/usr/local/bin/groovyserver
/usr/local/bin/groovyclient

%changelog
* Tue Jun 21 2011 Kazuhisa Hara <kazuhisya@gmail.com>
- Added rpmfile distribution name
- Excluding Maven from installation dependency

* Mon Jun 20 2011 Kazuhisa Hara <kazuhisya@gmail.com>
- Updated to 0.8-release, and required packages has changed

* Thu May 19 2011 - osoell@austin.utexas.edu
- Updated to 0.8-SNAPSHOT to include malloc/free fix

* Mon May 2 2011 - osoell@austin.utexas.edu
- Initial version
