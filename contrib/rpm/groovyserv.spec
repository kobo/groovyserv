%define debug_package %{nil}

Summary:       GroovyServ, a process server for Groovy
Name:          groovyserv
Version:       1.1.0
Release:       1%{?dist}
License:       Apache License, Version 2.0
Group:         Development/Languages
Requires:      java >= 1.8
Requires:      groovy >= 2.3.7
Source0:       https://github.com/kobo/%{name}/archive/v%{version}.tar.gz
BuildRoot:     %{_tmppath}/%{name}-%{version}-root
Packager:      Kazuhisa Hara <kazuhisya@gmail.com>
BuildRequires: groovy >= 2.3.7
BuildRequires: golang >= 1.3
BuildRequires: java >= 1.8
BuildRequires: java-devel >= 1.8

%description
Provides the GroovyServ mechanism for faster groovy execution.

%prep
%setup -q -n %{name}-%{version}

%build
export _JAVA_OPTIONS=-Dfile.encoding=UTF-8
./gradlew clean distLocalBin
mv build/distributions/%{name}-%{version}-bin-local.zip ../
cd ../ && rm -rf %{name}-%{version}
unzip %{name}-%{version}-bin-local.zip

%install
mkdir -p $RPM_BUILD_ROOT/opt $RPM_BUILD_ROOT/usr/bin
cp -Rp $RPM_BUILD_DIR/%{name}-%{version} $RPM_BUILD_ROOT/opt/%{name}
for file in groovyserver groovyclient ; do
  ln -s /opt/%{name}/bin/$file $RPM_BUILD_ROOT/usr/bin
done

%clean
rm -Rf $RPM_BUILD_ROOT

%post

%files
%defattr(-,root,root)
/opt/groovyserv
%defattr(755,root,root)
/usr/bin/groovyserver
/usr/bin/groovyclient

%changelog
* Thu Dec  8 2016 Kazuhisa Hara <kazuhisya@gmail.com> - 1.1.0-1
- Updated to 1.1.0-release
- Update source zip to tar.gz version
- Fixed to pass the test  for rpmlint
- Added prebuilt rpm link from Fedora Copr

* Thu Nov  6 2014 Kazuhisa Hara <kazuhisya@gmail.com> - 1.0.0-1
- Updated to 1.0.0-release
- Commands written in Ruby is removed
- [rpm] Added golang in BuildRequires
- [rpm] No longer depend on architecture
- [rpm] bin path is changed

* Tue Jul 30 2013 Kazuhisa Hara <kazuhisya@gmail.com>
- Updated to 0.13-release

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
