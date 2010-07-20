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
if RUBY_PLATFORM.downcase =~ /mswin(?!ce)|mingw|cygwin|bccwin/  # if windows
  HOME_DIR = ENV['USERPROFILE']
else
  HOME_DIR = ENV['HOME']
end
COOKIE_FILE = HOME_DIR + "/.groovy/groovyserv/cookie"
GROOVYSERVER_CMD = ENV.fetch("GROOVYSERV_HOME", File.dirname($0)+"/..") + "/bin/groovyserver"

#-------------------------------------------
# Functions
#-------------------------------------------

def start_server()
  puts "starting server..."
  unless FileTest.executable? GROOVYSERVER_CMD
    puts "ERROR: Command not found: #{GROOVYSERVER_CMD}"
    exit 1
  end
  system GROOVYSERVER_CMD
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
  socket.puts "Cwd: #{current_dir}"
  ARGV.each do |it|
    socket.puts "Arg: #{it}"
  end
  File.open(COOKIE_FILE) { |f|
    socket.puts "Cookie: #{f.read}"
  }
  socket.puts ""
end

def current_dir()
  pwd = Dir::pwd
  if pwd =~ /cygdrive/ # for cygwin
    pwd.gsub("/cygdrive/", "").gsub(/^([a-zA-Z])/) { "#{$1}:" }
  else
    pwd
  end
end

def handle_socket(socket)
  headers = read_headers(socket)
  if headers['Status']
    exit headers['Status'].to_i
  end
  data = socket.read(headers['Size'].to_i)
  return unless data

  if headers['Channel'] == 'out'
    $stdout.print data
  elsif headers['Channel'] == 'err'
    $stderr.print data
  end
end

def handle_stdin(socket)
  begin
    data = $stdin.read_nonblock(512)
  rescue EOFError
    send_interrupt(socket)
  else
    socket.write "Size: #{data.length}\n\n"
    socket.write data
  end
end

def send_interrupt(socket)
    socket.write "Size: -1\n\n"
end

def read_headers(socket)
  headers = {}
  while (line = socket.gets) != nil && line != "\n" do
    line.chomp!
    /([a-zA-Z]+): (.+)/ =~ line
    headers[$1] = $2
  end
  headers
end

#-------------------------------------------
# Main
#-------------------------------------------

# a mode to confirm server status
need_starting_server = ARGV.delete("--without-invoking-server")

failCount = 0
begin
  TCPSocket.open(DESTHOST, DESTPORT) { |socket|
    Signal.trap(:INT) {
      send_interrupt(socket)
      socket.close()
      exit 9
    }
    session(socket)
  }
rescue Errno::ECONNREFUSED
  if need_starting_server
    puts "ERROR: groovyserver isn't running"
    exit 1
  end
  if failCount >= 3
    puts "ERROR: Failed to start up groovyserver: #{GROOVYSERVER_CMD}"
    exit 1
  end
  start_server
  sleep 3
  failCount += 1
  retry
rescue => e
  puts "ERROR: #{e.message}"
  puts e.backtrace
  exit 1
end

