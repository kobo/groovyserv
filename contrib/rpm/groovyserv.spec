Summary: GroovyServ, a process server for Groovy
Name: groovyserv
Version: 0.10
Release: 0%{?dist}
License: Apache License, Version 2.0
Group: Development/Languages
Provides: groovyserv
Requires: java-1.6.0-openjdk
Requires: groovy >= 1.8.5
Source0: groovyserv-%{version}-src.zip
BuildArch: x86_64
BuildRoot: %{_tmppath}/%{name}-%{version}-root
Packager: Kazuhisa Hara <kazuhisya@gmail.com>
BuildRequires: groovy >= 1.8.5
BuildRequires: unzip
BuildRequires: java-1.6.0-openjdk

%description
Provides the GroovyServ mechanism for faster groovy execution.

%package ruby
Summary: GroovyServ Ruby Client
License: Apache License, Version 2.0
Group: Development/Languages
Requires: groovyserv >= %{version}-%{release}
Requires: ruby
BuildRequires: ruby

%description ruby
Provides the GroovyServ mechanism for faster groovy execution.

%prep
rm %{_sourcedir}/groovyserv-%{version}*bin.zip -rf
rm %{_sourcedir}/groovyserv-%{version} -rf

%build
cd %{_sourcedir}
unzip groovyserv-%{version}-src.zip
cd groovyserv-%{version}
export _JAVA_OPTIONS=-Dfile.encoding=UTF-8
./gradlew
cd %{_sourcedir}
mv groovyserv-%{version}/build/distributions/groovyserv-%{version}-linux-amd64-bin.zip .
rm groovyserv-%{version} -rf
unzip groovyserv-%{version}-linux-amd64-bin.zip

%install
mkdir -p $RPM_BUILD_ROOT/opt $RPM_BUILD_ROOT/usr/local/bin
cp -Rp %{_sourcedir}/groovyserv-%{version} $RPM_BUILD_ROOT/opt/groovyserv
for file in groovyserver groovyclient ; do
  ln -s /opt/groovyserv/bin/$file $RPM_BUILD_ROOT/usr/local/bin
done
ln -s /opt/groovyserv/bin/groovyclient.rb $RPM_BUILD_ROOT/usr/local/bin

#we're not interested in the bat file
rm $RPM_BUILD_ROOT/opt/groovyserv/bin/groovyserver.bat


#required since our sources are in svn
find $RPM_BUILD_ROOT -name .svn -type d | while read svndir ; do rm -rf $svndir ; done

%clean
rm -Rf $RPM_BUILD_ROOT

%post

%files
%defattr(-,root,root)

/opt/groovyserv/LICENSE.txt
/opt/groovyserv/NOTICE.txt
/opt/groovyserv/README.txt
/opt/groovyserv/bin/groovyserver
/opt/groovyserv/bin/groovyclient
/opt/groovyserv/lib
/usr/local/bin/groovyserver
/usr/local/bin/groovyclient

%files ruby
%defattr(-,root,root)
/opt/groovyserv/bin/groovyclient.rb
/usr/local/bin/groovyclient.rb

%changelog
* Sat Mar 31 2012 Kazuhisa Hara <kazuhisya@gmail.com>
- Updated to 0.10-release
- GroovyServ requires Groovy1.8.5+ (https://github.com/kobo/groovyserv/issues/38)
- Added BuildRequires Ruby

* Mon Aug  8 2011 Kazuhisa Hara <kazuhisya@gmail.com>
- Added a workaround for some tests fail

* Fri Aug  5 2011 Kazuhisa Hara <kazuhisya@gmail.com>
- Updated to 0.9-release
- build GroovyServ by Gradle

* Sun Jun 26 2011 Kazuhisa Hara <kazuhisya@gmail.com>
- Fix for the build section could be constructed automatically from the src.zip
- Divide ruby client package

* Tue Jun 21 2011 Kazuhisa Hara <kazuhisya@gmail.com>
- Added rpmfile distribution name
- Excluding Maven from installation dependency

* Mon Jun 20 2011 Kazuhisa Hara <kazuhisya@gmail.com>
- Updated to 0.8-release, and required packages has changed

* Thu May 19 2011 - osoell@austin.utexas.edu
- Updated to 0.8-SNAPSHOT to include malloc/free fix

* Mon May 2 2011 - osoell@austin.utexas.edu
- Initial version
