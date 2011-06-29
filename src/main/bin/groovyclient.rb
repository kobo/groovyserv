#!/usr/bin/ruby
#
# Copyright 2009-2011 the original author or authors.
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

DESTHOST = "localhost" # for security
DESTPORT = ENV.fetch("GROOVYSERVER_PORT", 1961)
IS_WINDOWS = RUBY_PLATFORM.downcase =~ /mswin(?!ce)|mingw|cygwin|bccwin/
HOME_DIR = IS_WINDOWS ? ENV['USERPROFILE'] : ENV['HOME']
COOKIE_FILE_BASE = HOME_DIR + "/.groovy/groovyserv/cookie"
GROOVYSERVER_CMD = File.expand_path(ENV.fetch("GROOVYSERV_HOME", File.dirname($0)+"/..") + "/bin/groovyserver")

CLIENT_OPTION_PREFIX="-C"

#-------------------------------------------
# Classes
#-------------------------------------------

class ClientOption
  def initialize
    @without_invoking_server
    @port = DESTPORT
    @quiet = false
    @env_all
    @env_include_mask = []
    @env_exclude_mask = []
    @help
  end

  attr_accessor :without_invoking_server, :port, :env_all, :env_include_mask, :env_exclude_mask, :help, :quiet

end

class OptionInfo

  attr_accessor :take_value

  class OptionInfoPort < OptionInfo
    def eval(value)
      $client_option.port = value
    end
  end

  class OptionInfoWithoutInvokingServer < OptionInfo
    def eval
      $client_option.without_invoking_server = true
    end
  end

  class OptionInfoEnvExclude < OptionInfo
    def eval(value)
      $client_option.env_exclude_mask.push(value)
    end
  end

  class OptionInfoHelp < OptionInfo
    def eval
      usage()
      exit(0)
    end
  end

  class OptionInfoKillServer < OptionInfo
    def eval
      start_server(["-k"])
      exit(0)
    end
  end

  class OptionInfoRestartServer < OptionInfo
    def eval
      start_server(["-r"])
      exit(0)
    end
  end

  class OptionInfoQuiet < OptionInfo
    def eval
      $client_option.quiet = true
    end
  end

  class OptionInfoEnv < OptionInfo
    def eval(value)
      $client_option.env_include_mask.push(value)
    end
  end

  class OptionInfoEnvAll < OptionInfo
    def eval
      $client_option.env_all = true
    end
  end

  def initialize(take_value)
    @take_value = take_value
  end

  @@options = {
    "without-invoking-server" => OptionInfoWithoutInvokingServer.new(false),
    "p" => OptionInfoPort.new(true),
    "port" => OptionInfoPort.new(true),
    "k" => OptionInfoKillServer.new(false),
    "kill-server" => OptionInfoKillServer.new(false),
    "r" => OptionInfoRestartServer.new(false),
    "restart-server" => OptionInfoRestartServer.new(false),
    "q" => OptionInfoQuiet.new(false),
    "quiet" => OptionInfoQuiet.new(false),
    "env" => OptionInfoEnv.new(true),
    "env-all" => OptionInfoEnvAll.new(false),
    "env-exclude" => OptionInfoEnvExclude.new(true),
    "help" => OptionInfoHelp.new(false),
    "h" => OptionInfoHelp.new(false),
    "" => OptionInfoHelp.new(false)
  }

  def OptionInfo.options
    @@options
  end

end

#-------------------------------------------
# Global Vriables
#-------------------------------------------

$client_option = ClientOption.new()

#-------------------------------------------
# Functions
#-------------------------------------------

def usage()
  print(("\n"+
         "usage: groovyclient.rb %s[option for groovyclient] [args/options for groovy]\n"+
         "options:\n"+
         "  %sh,%shelp                       show this usage\n" \
         "  %sp,%sport <port>                specify the port to connect to groovyserver\n" \
         "  %sk,%skill-server                kill the running groovyserver\n" \
         "  %sr,%srestart-server             restart the running groovyserver\n" \
         "  %sq,%squiet                      suppress statring messages\n" \
         "  %senv <substr>                   pass environment variables of which a name\n" \
         "                                   includes specified substr\n" \
         "  %senv-all                        pass all environment variables\n" \
         "  %senv-exclude <substr>           don't pass environment variables of which a\n" \
         "                                   name includes specified substr\n" \
         "").gsub("%s", CLIENT_OPTION_PREFIX))
end

def is_groovy_help_option(s)
  ["--help", "-help",  "-h"].include?(s)
end

def process_opt(item, arg)
  opt = OptionInfo.options[item[2..-1]]

  if opt == nil
    STDERR.puts "ERROR: unknown option #{item}"
    usage()
    exit(1)
  end

  if opt.take_value
    if arg == []
      STDERR.puts "ERROR: option #{item} require param"
      usage()
      exit(1)
    end
    opt.eval(arg.shift)
  else
    opt.eval()
  end
end

def process_opts(arg)
  if arg == []
    $client_option.help = true
    return arg
  end

  result = []

  while item = arg.shift do
    if is_groovy_help_option(item)
      $client_option.help = true
    end
    if item.start_with?(CLIENT_OPTION_PREFIX)
      process_opt(item, arg)
    else
      result.push(item)
    end
  end
  return result
end

def start_server(arg)
  unless FileTest.executable? GROOVYSERVER_CMD
    STDERR.puts "ERROR: Command not found: #{GROOVYSERVER_CMD}"
    exit 1
  end
  if $client_option.quiet
    arg << "-q"
  else
    command_str = "'#{GROOVYSERVER_CMD}' -p #{$client_option.port.to_s} #{arg.join}"
    STDERR.printf "Invoking server: %s\n", command_str
  end
  system(GROOVYSERVER_CMD, "-p", $client_option.port.to_s, *arg)
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
    if $client_option.env_all || $client_option.env_include_mask.any?{|item| key.include?(item) }
      if !$client_option.env_exclude_mask.any?{|item| key.include?(item) }
        socket.puts "Env: #{key}=#{value}"
      end
    end
  }
end

def send_command(socket, args)
  socket.puts "Cwd: #{current_dir}"
  args.each do |arg|
    # why using gsub? because Base64.encode64 happens to break lines automatically.
    # TODO using default encoding.
    socket.puts "Arg: #{Base64.encode64(arg).gsub(/\s/, '')}"
  end
  File.open(COOKIE_FILE_BASE+"-"+$client_option.port.to_s) { |f|
    socket.puts "Cookie: #{f.read}"
  }
  send_envvars(socket)
  if ENV['CLASSPATH']
    socket.puts "Cp: #{ENV['CLASSPATH']}"
  end
  socket.puts ""
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
    usage() if $client_option.help
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
    socket.write "Size: 0\n\n"
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

args = process_opts(ARGV)

failCount = 0
begin
  TCPSocket.open(DESTHOST, $client_option.port) { |socket|
    Signal.trap(:INT) {
      send_interrupt(socket)
      socket.close()
      exit 8
    }
    session(socket, args)
    if $client_option.help
      usage()
      exit(0)
    end
  }
rescue Errno::ECONNREFUSED
  if $client_option.without_invoking_server
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

