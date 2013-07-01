#!/usr/bin/ruby
#
# Copyright 2009-2013 the original author or authors.
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
require 'base64'

#-------------------------------------------
# Constants
#-------------------------------------------

DESTHOST = "127.0.0.1" # for security
DESTPORT = ENV.fetch("GROOVYSERVER_PORT", 1961)
IS_WINDOWS = RUBY_PLATFORM.downcase =~ /mswin(?!ce)|mingw|cygwin|bccwin/
HOME_DIR = IS_WINDOWS ? ENV['USERPROFILE'] : ENV['HOME']
AUTHTOKEN_FILE_BASE = HOME_DIR + "/.groovy/groovyserv/authtoken"
GROOVYSERVER_CMD = File.expand_path(ENV.fetch("GROOVYSERV_HOME", File.dirname($0)+"/..") + "/bin/groovyserver")
VERSION_MESSAGE = "GroovyServ Version: Client: @GROOVYSERV_VERSION@ (.ruby)"

ERROR_INVALID_AUTHTOKEN = 201
ERROR_CLIENT_NOT_ALLOWED = 202

#-------------------------------------------
# Classes
#-------------------------------------------

class Options
  attr_reader :client, :server
  def initialize
    @client = {
      :without_invoking_server => false,
      :host => DESTHOST,
      :port => DESTPORT,
      :authtoken => nil,
      :quiet => false,
      :env_all => false,
      :env_include_mask => [],
      :env_exclude_mask => [],
      :help => false,
      :version => false,
      :groovyserver_opt => [],
    }
    @server = {
      :help => false,
      :version => false,
      :args => [],
    }
  end
  def need_to_invoke_server?
    return false if @client[:without_invoking_server]
    @client[:groovyserver_opt].any? {|v| ['-k', '-r'].include?(v) }
  end
end

#-------------------------------------------
# Global variables
#-------------------------------------------

$options = nil    # FIXME shouldn't use global variables

#-------------------------------------------
# Functions
#-------------------------------------------

def usage()
  puts "\
usage: groovyclient.rb -C[option for groovyclient] [args/options for groovy]
options:
  -Ch,-Chelp                       show this usage
  -Cs,-Chost                       specify the host to connect to groovyserver
  -Cp,-Cport <port>                specify the port to connect to groovyserver
  -Ca,-Cauthtoken <authtoken>      specify the authtoken
  -Ck,-Ckill-server                kill the running groovyserver
  -Cr,-Crestart-server             restart the running groovyserver
  -Cq,-Cquiet                      suppress statring messages
  -Cenv <substr>                   pass environment variables of which a name
                                   includes specified substr
  -Cenv-all                        pass all environment variables
  -Cenv-exclude <substr>           don't pass environment variables of which a
                                   name includes specified substr
  -Cv,-Cversion                    display the GroovyServ version"
end

def start_server(args)
  unless FileTest.executable? GROOVYSERVER_CMD
    STDERR.puts "ERROR: Command not found: #{GROOVYSERVER_CMD}"
    exit 1
  end
  if $options.client[:quiet]
    args << "-q"
  else
    command_str = "'#{GROOVYSERVER_CMD}' -p #{$options.client[:port]} #{args.join(' ')}"
    STDERR.printf "Invoking server: %s\n", command_str
  end
  system(GROOVYSERVER_CMD, "-p", $options.client[:port].to_s, *args)
end

def session(socket, args)
  send_command(socket, args)
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

def send_envvars(socket)
  ENV.each{|key,value|
    if $options.client[:env_all] || $options.client[:env_include_mask].any?{|item| key.include?(item) }
      if !$options.client[:env_exclude_mask].any?{|item| key.include?(item) }
        socket.puts "Env: #{key}=#{value}"
      end
    end
  }
end

def send_command(socket, args)
  socket.puts "Cwd: #{current_dir}"
  args.each do |arg|
    # why using gsub? It's because Base64.encode64 happens to break lines automatically.
    # TODO using default encoding.
    socket.puts "Arg: #{Base64.encode64(arg).gsub(/\s/, '')}"
  end
  socket.puts "AuthToken: #{authtoken}"
  send_envvars(socket)
  if ENV['CLASSPATH']
    socket.puts "Cp: #{ENV['CLASSPATH']}"
  end
  socket.puts ""
end

def authtoken
  $options.client[:authtoken] || File.open(AUTHTOKEN_FILE_BASE + "-" + $options.client[:port].to_s) { |f| f.read }
end

def current_dir()
  if IS_WINDOWS
    # native path expression including a drive letter is needed.
    `cmd.exe /c cd`.strip # FIXME it's ugly...
  else
    Dir::pwd
  end
end

def handle_socket(socket)
  headers = read_headers(socket)
  if headers.empty?
    exit 1
  end
  if headers['Status']
    if $options.server[:help]
      puts "\n"
      usage()
    end
    if $options.server[:version]
      puts VERSION_MESSAGE
    end
    status_code = headers['Status'].to_i
    if status_code == ERROR_INVALID_AUTHTOKEN
      STDERR.puts "ERROR: rejected by groovyserv because of invalid authtoken"
    elsif status_code == ERROR_CLIENT_NOT_ALLOWED
      STDERR.puts "ERROR: rejected by groovyserv because of not allowed client address"
    end
    exit status_code
  end
  data = socket.read(headers['Size'].to_i)
  return unless data

  if headers['Channel'] == 'out'
    $stdout.print data
    $stdout.flush
  elsif headers['Channel'] == 'err'
    $stderr.print data
    $stderr.flush
  end
end

$closedStdin = false # FIXME
def handle_stdin(socket)
  return if $closedStdin
  begin
    data = $stdin.read_nonblock(512)
  rescue EOFError
    socket.write "Size: 0\n\n"
    $closedStdin = true
  else
    socket.write "Size: #{data.length}\n\n"
    socket.write data
  end
end

def send_interrupt(socket)
    socket.write "Size: -1\n\n"
    socket.close()
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

def parse_option(args)
  options = Options.new
  args.each_with_index do |arg, i|
    case arg
    when "-Cwithout-invoking-server"
      options.client[:without_invoking_server] = true
    when "-Ch", "-Chost"
      options.client[:host] = args.delete_at(i + 1)
    when "-Cp", "-Cport"
      port = args.delete_at(i + 1)
      unless port =~ /^[0-9]+$/
        raise "Invalid port number #{port} for #{arg}"
      end
      options.client[:port] = port
    when "-Ca", "-Cauthtoken"
      options.client[:authtoken] = args.delete_at(i + 1)
    when "-Ck" , "-Ckill-server"
      options.client[:groovyserver_opt] << "-k"
    when "-Cr", "-Crestart-server"
      options.client[:groovyserver_opt] << "-r"
    when "-Cq", "-Cquiet"
      options.client[:groovyserver_opt] << "-q"
      options.client[:quiet] = true
    when "-Cenv-all"
      options.client[:env_all] = true
    when "-Cenv"
      val = args.delete_at(i + 1)
      unless val
        raise "Invalid mask string #{val} for #{arg}"
      end
      options.client[:env_include_mask] << val
    when "-Cenv-exclude"
      val = args.delete_at(i + 1)
      unless val
        raise "Invalid mask string #{val} for #{arg}"
      end
      options.client[:env_exclude_mask] << val
    when "-Ch", "-Chelp"
      options.client[:help] = true
    when "--help", "-help", "-h"
      options.server[:help] = true
      options.server[:args] << arg
    when "-Cv", "-Cversion"
      options.client[:version] = true
    when "--version", /^-v/
      options.server[:version] = true
      options.server[:args] << arg
    when /-C.*/
      raise "Unknown option #{arg}"
    else
      options.server[:args] << arg
    end
  end
  if options.server[:args].empty?
    # display additionally client's usage at the end of session
    options.server[:help] = true
  end
  options
end

#-------------------------------------------
# Main
#-------------------------------------------

# Parsing options
begin
  $options = parse_option(ARGV)
rescue => e
  STDERR.puts "ERROR: #{e.message}"
  usage()
  exit 1
end
#puts "Original ARGV: #{ARGV.inspect}"
#puts "Parsed options: #{$options.inspect}"

# Only show usage (highest priority)
if $options.client[:help]
  usage()
  exit 0
end

# Only show version (highest priority)
if $options.client[:version]
    puts VERSION_MESSAGE
  exit 0
end

# Start or stop server when specified
if $options.need_to_invoke_server?
  start_server($options.client[:groovyserver_opt].uniq)
  if $options.client[:groovyserver_opt].include?("-k")
    exit 0
  end
end

# Invoke script (before, start server if down)
failCount = 0
begin
  TCPSocket.open($options.client[:host], $options.client[:port]) { |socket|
    Signal.trap(:INT) {
      send_interrupt(socket)
      exit 8
    }
    session(socket, $options.server[:args])
  }
rescue Errno::ECONNREFUSED
  if $options.client[:without_invoking_server]
    STDERR.puts "ERROR: groovyserver isn't running"
    exit 9
  end
  if failCount >= 3
    STDERR.puts "ERROR: Failed to start up groovyserver: #{GROOVYSERVER_CMD}"
    exit 1
  end
  start_server([])
  sleep 3
  failCount += 1
  retry
rescue Errno::ECONNRESET, Errno::EPIPE
  # normally exit if reset by peer or broken pipe
  exit 0
rescue => e
  STDERR.puts "ERROR: #{e.message}"
  STDERR.puts e.backtrace
  exit 1
end

