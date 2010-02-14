#!/usr/bin/ruby

require 'socket'

DESTPORT=1961

s = TCPSocket.open("localhost", DESTPORT)
s.puts("Cwd: "+Dir::getwd)
ARGV.each {|it|
  s.puts("Arg: "+it)
}
s.puts("")

def readHeaders(soc)
  headers={}
  while (line = soc.gets) != nil && line != "\n"
    chomp line
    /([A-Za-z]*): (.*)/ =~ line
    headers[$1]=$2;
  end
  headers
end

begin
  headers = readHeaders(s)
  if (headers['Status'] != nil) then
    exit(headers['Status'].to_i)
  end
  data = s.read(headers['Size'].to_i)
  if (data != nil) then
    print data
  end
end while data != nil

s.close

