#!/usr/bin/ruby
#
# Copyright 2009-2010 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
require 'socket'

#-------------------------------------------
# Constants
#-------------------------------------------

DESTHOST = "localhost" # for security
DESTPORT = 1961
DOT_DIR = ENV['HOME'] + "/.groovy/groovyserver"
LOG_FILE = DOT_DIR + "/groovyserver.log"
COOKIE_FILE = DOT_DIR + "/key"
GROOVYSERVER_CMD = ENV.fetch("GROOVYSERV_HOME", File.dirname($0)) + "/hoge"

#-------------------------------------------
# Functions
#-------------------------------------------

def start_server()
  puts "starting server..."
  unless FileTest.executable? GROOVYSERVER_CMD
    puts "ERROR: Command not found: #{GROOVYSERVER_CMD}"
    exit 1
  end
  system "#{GROOVYSERVER_CMD} >> #{LOG_FILE} 2>&1"
end

def session(socket)
  send_command(socket)
  while true do
    IO.select([socket, $stdin]).each do |ins|
      if ins[0] == socket
        handle_socket(socket)
      elsif ins[0] == $stdin
        handle_stdin(socket)
      end
    end
  end
end

def send_command(socket)
  socket.puts "Cwd: #{Dir::getwd}"
  ARGV.each do |it|
    socket.puts "Arg: #{it}"
  end
  File.open(COOKIE_FILE) { |f|
    socket.puts "Cookie: #{f.read}"
  }
  socket.puts ""
end

def handle_socket(socket)
  headers = read_headers(socket)
  if headers['Status']
    exit headers['Status'].to_i
  end
  data = socket.read(headers['Size'].to_i)
  return unless data

  if headers['Channel'] == 'o'
    $stdout.print data
  elsif headers['Channel'] == 'e'
    $stderr.print data
  end
end

def handle_stdin(socket)
  begin
    data = $stdin.read_nonblock(512)
  rescue EOFError
    socket.write "Size: 0\n\n"
  else
    socket.write "Size: #{data.length}\n\n"
    socket.write data
  end
end

def read_headers(socket)
  headers = {}
  while (line = socket.gets) != nil && line != "\n" do
    chomp line
    /([a-zA-Z]+): (.+)/ =~ line
    headers[$1] = $2
  end
  headers
end

#-------------------------------------------
# Main
#-------------------------------------------

Signal.trap(:INT) { exit 1 }
begin
  TCPSocket.open(DESTHOST, DESTPORT) { |socket|
    session(socket)
  }
rescue Errno::ECONNREFUSED
  start_server
  sleep 3
  retry
rescue => e
  puts "ERROR: #{e.message}"
  puts e.backtrace
  exit 1
end

