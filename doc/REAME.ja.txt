GroovyServer README

==========
はじめに
==========

GroovyServerは、Groovyインタプリタを常駐したサーバーとして動作させるこ
とでgroovyコマンドの起動を高速化するものです。

もし、Emacsのemacsserver/Emacsclient(もしくはgnuserver/gnuclient)をご存
知ならば理解が早いでしょう。

groovyでスクリプトを使用する場合、起動のレスポンスが使い勝手に大きな影
響を及ぼします。なぜなら、Groovyは実行時の型チェックを行う言語ですが、
コンパイル時の型チェックができないので、試行錯誤を伴う実行を繰り返すこ
とで型エラーがはじめて検出できるからです。必然的に、なんども実行を繰り
返すことになりますが、Javaであるため、起動時間が長くかかります。

groovyserverを使うことで、起動時間の遅延を最小限にすることができ効率よ
く開発を行うことができるでしょう。


==========
動作環境
==========

UNIX環境およびWindowsのcygwin環境で動作します。UNIX環境についてはMacOS
Xで動作確認を行っています。

================
Groovyのバージョン
================

1.7-RC-1、1.6.6で動作確認を行っています。

============================================
ソースコードでの配布パッケージからのインストール
============================================

まず、GroovyServer配布パッケージを展開します。以降では、GroovyServer配
布パッケージを展開したディレクトリを「$GROOVYSERV_HOME」と表記します。

 $ unzip groovyserv-1.x.x.zip

mavenを使ってコンパイルします。

 $ cd $GROOVYSERV_HOME
 $ mvn compile

コンパイルしたっけか、

 $GROOVYSERV_HOME/bin/groovyclient.exe

ができれていればコンパイルに成功しています。なお、Mac OSX環境やUNIX環境
でも生成されるのは拡張子.exeがあるgroovyclient.exeです。

次に、環境変数PATHに「$GROOVYSERV_HOME/bin」が含まれるように変更します。

  export PATH=$GROOVYSERV_HOME/bin:$PATH

これで以下のように実行できます。

 $ groovyclient -v

groovyコマンドを実行するとgroovyclientコマンドが呼び出されるように、以
下のようにエイリアス(別名)指定を行っておくと良いでしょう。以下はbash用
のエイリアス指定です。

  alias groovy=groovyclient
  alias groovyc="groovyclient -e 'org.codehaus.groovy.tools.FileSystemCompiler.main(args)'"
  alias groovysh="groovyclient -e 'groovy.ui.InteractiveShell.main(args)'"
  alias groovyConsole="groovyclient -e 'groovy.ui.Console.main(args)'"


======================
バイナリパッケージ
======================

 [TBD]

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

* Java環境はカレントディレクトリを指定する機能を本来は持っていません。
  システムプロパティ「user.dir」を用いて、カレントディレクトリが設定さ
  れているのと類似を設定していますが、不完全であることがわかっています。
  たとえば[TBD]

* 上記と同じ理由により、異なるカレントディレクトリを同時に使うことはで
  きません。たとえば、パイプでつないで２つのGroovyコマンドを実行し、そ
  れぞれが異なるカレントディレクトリであるように実行することはできませ
  ん。

   $  groovy -e "..."   | (cd /tmp; groovy -e "......") 


  のようにサブシェルを起動して、カレントディレクトリの異なるgroovyコマ
  ンド実行を一度に処理したり、長時間処理するようなgroovyプログラムを背
  後で実行させているときに、ことなるカレントディレクトリ持ったプログラ
  ムを実行できないということです。この場合、groovyserverはエラーを発生
  させず、後勝ちとなります。

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

* groovyclient実行時に-cpオプションによるclasspathを指定しても効果があ
  りません。

* 新規スレッドから標準出力・標準入力を扱えません。

* exit statusの201は予約されています。このstatusでExitするGroovyスクリ
  プトを実行した場合、exit status は1として扱われます。

================
バグ
================

* groovyプログラム中でスレッドを生成した場合、そのスレッドからの標準・
  標準エラー出力(System.out/err)への出力は出力されません。また、標準入
  力からの入力も、生成されたスレッド中のコードでは正しく受け取ることが
  できません。

* groovyclientコマンドをControl-Cなどで強制終了した場合でも、Server上で
  実行されるGroovyコードは終了しません。特に、標準入力から入力町になっ
  ているようなGroovyプログラムを強制終了したとき、無限の入力待ちとなり
  ます。groovyserverを終了さセル必要があります。

================
オプション
================

tcpのポート番号はデフォルトでは1961を使用します。変更するには[TBD]
GroovyServerの起動オプションは以下のとおり。

   -v  冗長表示。デバッグ情報などを表示します。
   -k 起動しているGroovyServerを終了させます。

================
ログファイル
================

~/.groovy/groovyserver/<プロセスID>.log

にgroovyserverのログファイルが出力されます。

   -v  冗長表示。デバッグ情報などを表示します。
   -k  起動しているGroovyServerを終了させます。

================
セキュリティ
================

groovyserverには、他のマシンからはgroovyclientコマンドを使って接続する
ことはできません。ただし、認証はかけていないので、同じマシンを使ってい
る他のユーザーからgroovyserverに接続されて実行されてしまう可能性があり
ます。この際に実行が行われるのは、groovyserverを起動したユーザーの権限
で実行されます。


