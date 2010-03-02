GroovyServer README

==========
はじめに
==========

GroovyServerは、Groovyインタプリタを常駐サーバーとして動作させることで
groovyコマンドの起動を見た目高速化するものです。groovyコマンドはTCP/IP
通信を使ってすでに起動しているGroovyランタイムと通信し、結果を出力しま
す。

もし、Emacsのemacsserver/Emacsclient(もしくはgnuserver/gnuclient)をご存
知ならば理解が早いでしょう。

groovyのスクリプトを開発する場合、起動速度がとても重要です。Groovyは動
的言語であるため、コンパイル時にあらかじめチェックできないエラーについ
て、実行して初めて判明する場合が多いからです。そのため、実行を繰り返し
ながら開発をしていくことになります。たとえその起動が1..2秒しかかからな
くても、体感としてはとても大きく感じられるのではないでしょうか。

GroovyServerを使うことで、起動時間を短縮し、さくさくと開発を進めていく
ことができます。Windows XP Core(TM) 2 Duo 2GHzのマシンでは


==========
動作環境
==========

UNIX環境およびWindowsのcygwin環境で動作します。UNIX環境についてはMacOS
Xで動作確認を行っています。

================
Groovyのバージョン
================

1.7-RC-1、1.6.6以上で動作確認を行っています。



============================================
ソースコードでの配布パッケージからのインストール
============================================

まず、GroovyServer配布パッケージを展開します。以降では、GroovyServer配
布パッケージを展開したディレクトリを「$GROOVYSERV_HOME」と表記します。

 $ unzip groovyserv-1.x.x.zip

mavenを使ってコンパイルします。

 $ cd $GROOVYSERV_HOME
 $ mvn compile

コンパイルした結果、Linux,Mac OS X環境では

 $GROOVYSERV_HOME/bin/groovyclient

ができれていればコンパイルに成功しています。
cygwin Windows環境では生成されるのは

 $GROOVYSERV_HOME/bin/groovyclient.exe

です。次に、環境変数PATHに「$GROOVYSERV_HOME/bin」が含まれるように変更します。

  export PATH=$GROOVYSERV_HOME/bin:$PATH

以下のようにコマンドラインから打ち込んで確認してみてください。

 $ groovyclient -v

また、groovyコマンドを実行するとgroovyclientコマンドが呼び出されるように、
以下のようにエイリアス(別名)指定を行っておくと便利です。
以下はbash用のエイリアスの設定です。

  alias groovy=groovyclient
  alias groovyc="groovyclient -e 'org.codehaus.groovy.tools.FileSystemCompiler.main(args)'"
  alias groovysh="groovyclient -e 'groovy.ui.InteractiveShell.main(args)'"
  alias groovyConsole="groovyclient -e 'groovy.ui.Console.main(args)'"


============================================
設定
============================================
環境変数HOMEをホームディレクトリに設定してください。

================
使い方
================

groovyコマンドの代わりにgroovyclientコマンドを実行します。前述のように
エイリアス指定を行っていれば、エイリアスであるgroovyコマンドを、groovy
コマンドと思って実行すれば良いです。

groovyclientを実行したとき、groovyserverが起動されていなければ、バック
グラウンドでgroovyserverが起動されます。起動されていない場合、起動のた
めに数秒の待ち時間の後、サーバが起動し、実行が行われます。

 $ groovyclient -v

================
制限・機能の違い
================

* 異なるカレントディレクトリを同時に使うことはできません。たとえば、パ
  イプでつないで２つのGroovyコマンドを実行し、それぞれが異なるカレント
  ディレクトリであるように実行することはできません。

   $  groovy -e "..."   | (cd /tmp; groovy -e "......") 

  例外が発生します。

* 静的変数はgroovyプログラム間の実行で共有されます。たとえば、システム
  プロパティが共有されます。

  $ groovy -e "System.setProperty('a','abc')"
  $ groovy -e "println System.getProperty('a')"
  abc

* -l(listen)オプションは使用できません。

* groovyの動作に影響を与える環境変数について、groovyclientコマンドを実
  行したときのシェルの状態ではなく､groovyserverが実行されたときの環境変
  数の値が使用されます。環境変数の変更をgroovyコマンドに影響を及ぼすよ
  うにするためには、groovyserverを再起動する必要があります。特に、
  CLASSPATHの指定はgroovyserverを起動するときに指定する必要があります。

================
バグ
================

* groovyclientコマンドをControl-Cなどで強制終了した場合でも、Server上で
  実行されるGroovyコードは終了しません。特に、標準入力から入力待ちになっ
  ているようなGroovyプログラムを強制終了したとき、無限の入力待ちとなり
  ます。groovyserverを終了させる必要があります。

================
オプション
================

tcpのポート番号はデフォルトでは1961を使用します。変更するには[TBD]
GroovyServerの起動オプションは以下のとおり。

   -v  冗長表示。デバッグ情報などを表示します。
   -k 起動しているGroovyServerを終了させます。
   -r 起動しているGroovyServerを再起動します(停止+起動)。

================
ログファイル
================

~/.groovy/groovyserver/<プロセスID>.log

にgroovyserverのログファイルが出力されます。

================
セキュリティ
================

groovyserverには、他のマシンからはgroovyclientコマンドを使って接続する
ことはできません。ただし、認証はかけていないので、同じマシンを使ってい
る他のユーザーからgroovyserverに接続されて実行されてしまう可能性があり
ます。この際に実行が行われるのは、groovyserverを起動したユーザーの権限
で実行されます。


