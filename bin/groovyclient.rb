#!/usr/bin/ruby

require 'socket'

DESTPORT=1961

s = TCPSocket.open("localhost", DESTPORT)
s.puts("Cwd: .")
ARGV.each {|it|
  s.puts("Arg: "+it)
}
s.puts("")

def readHeaders(soc)
  headers={}
  while (line = soc.gets) != "\n"
    chomp line
    /([A-Za-z]*): (.*)/ =~ line
    headers[$1]=$2;
  end
  headers
end

headers = readHeaders(s)
data = s.read(headers['Size'].to_i)
print data

s.close

