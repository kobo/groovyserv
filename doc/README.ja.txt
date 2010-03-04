GroovyServer 0.1 README
2010年03月02日

==========
はじめに
==========

GroovyServerは、Groovy処理系をサーバーとして動作させることでgroovyコマ
ンドの起動を見た目高速化するものです。groovyコマンドはTCP/IP 通信を使っ
てすでに起動しているGroovyランタイムと通信し、結果を出力します。

もし、Emacsのemacsserver/Emacsclient(もしくはgnuserver/gnuclient)をご存
知ならば理解が早いでしょう。

Groovyスクリプトを開発する場合、起動速度がとても重要です。Groovyは動的
言語であるため、コンパイル時にあらかじめチェックできないエラーについて、
実行して初めて判明する場合が多いからです。そのため、実行を繰り返しなが
ら開発をしていくことになります。たとえその起動が1..2秒しかかからなくて
も、体感としてはとても大きく感じられるのではないでしょうか。

GroovyServerを使うことで、Groovyコマンドの起動時間を短縮し、さくさくと
開発を進めていくことができます。以下は、Windows XP Core(TM) 2 Duo 2GHz
のマシンでの起動時間の例です(三回測定した平均値）。

  Groovyコマンド(非native版) 2.32 (sec)
  Groovyコマンド(native版)   0.90 (sec)
  groovyclient               0.10 (sec)

非nateve版と比べると約20倍程度、起動が高速化しています。

向上の度合いは実際にはケースバイケースですが、native版があるということ
と、JQS(Java Quick Start)の存在により、Windows環境は比較的Groovyの実行
は迅速で差が少なく（といっても10倍はありますが）、UNIX環境の方が差が大
きくなる傾向はあるようです。

==========
動作環境
==========

動作確認を行っている環境は以下のとおり。

- Windows XP+cygwin
- MacOS X 10.5/6
- Ubuntu Linux 9.10

他の環境で動作した場合レポートをいただけますと幸いです。

==========
言語
==========

サーバサイドはJava、クライアントはC言語クライアントおよびRubyのクライア
ントが現在動作しています。サーバサイドではJNA(Java Native Access)を使用
しています。

================
セキュリティ
================

GroovyServerへの接続は、localhostからのみに制限されており、他のマシンか
らはgroovyclientコマンドを使って接続することはできません。また、同マシ
ン上でもgroovyserverを起動したユーザーと同じユーザ実行したgroovyclient
の接続しか受け付けないように制約をかけています。なお、こ制約は、サーバ
が実行後とに生成する秘密のクッキー情報~/.groovy/groovyserv/key ファイル
の内容を同じユーザのみが読み出せることに依存しています。このファイルの
アクセス制限を、UNIX環境ではowner以外からは読めないように設定(chmod
400)していますが、Windows環境ではこの設定が機能しないため、必要に応じて
他のユーザから読み出せないように設定してください。

================
Groovyのバージョン
================

 Groovy 1.6以降で動きます。

============================================
バイナリパッケージからのインストール
============================================

バイナリパッケージgroovyserv-0.1-SNAPSHOT-win32-bin.zipを適当なフォルダ
に展開します。例えば、~/optに展開するとします。

 > mkdir ~/opt
 > cd ~/opt
 > unzip -l groovyserv-0.1-SNAPSHOT-win32-bin.zip

次に環境変数PATHに上記フォルダ配下のbinを追加します。仮に、
/opt/groovyservに展開した場合、以下のように設定します(bashなどの設定)。

 export PATH=~/opt/groovyserv-0.1-SNAPSHOT/bin:$PATH

設定は以上です。groovyclientを実行するとgroovyserverが起動します。

 > groovyclient -v
 starting server..
 Groovy Version: 1.7.0 JVM: 1.6.0_13


============================================
ソースコードからのビルド
============================================

まず、GroovyServerのソースコード配布パッケージ
groovyserv-0.1-SNAPSHOT-src.zip*を展開します。

 > mkdir -p ~/opt/src
 > cd ~/opt/src
 > unzip -l groovyserv-0.1-SNAPSHOT-src.zip

Maven2を使ってコンパイルします。

 > cd ~/opt/src/groovyserv-0.1-SNAPSHOT/
 > mvn clean compile

コンパイルした結果、Linux,Mac OS X環境では

  ~/opt/src/groovyserv-0.1-SNAPSHOT/bin/groovyclient

が、cygwin/Windows環境では

  ~/opt/src/groovyserv-0.1-SNAPSHOT/bin/groovyclient.exe

ができれていればコンパイルに成功しています。テストで失敗する場合以下の
ようにしてください。

 > mvn -Dmaven.test.skip=true clean compile

バイナリパッケージからのインストールの場合と同じように環境変数PATHを設
定してください。

===============
その他の設定
===============

groovyコマンドを実行するとgroovyclientコマンドが呼び出されるように、以
下のようにエイリアス(別名)指定を行っておくと便利です。以下はbash用のエ
イリアスの設定です。

  alias groovy=groovyclient
  alias groovyc="groovyclient -e 'org.codehaus.groovy.tools.FileSystemCompiler.main(args)'"
  alias groovysh="groovyclient -e 'groovy.ui.InteractiveShell.main(args)'"
  alias groovyConsole="groovyclient -e 'groovy.ui.Console.main(args)'"

================
使い方
================

groovyコマンドの代わりにgroovyclientコマンドを実行します。groovyclient
を実行したとき、groovyserverが起動されていなければ、バックグラウンドで
groovyserverが起動されます。起動されていない場合、起動のために数秒の待
ち時間の後、サーバが起動し、実行が行われます。

明示的にgroovyserverを起動しておくこともできます。

 > groovyserver

起動オプションに-vを指定すると詳細メッセージが表示されます。

 > groovyserver

起動オプションについては後述します。

================
制限・機能の違い
================

* 異なるカレントディレクトリを同時に使うことはできません。たとえば、パ
  イプでつないで２つのGroovyコマンドを実行し、それぞれが異なるカレント
  ディレクトリであるように実行することはできません。

   >  groovy -e "..."   | (cd /tmp; groovy -e "......") 

  この場合例外が発生します。

  org.jggug.kobo.groovyserv.GroovyServerException: Can't change
  current directory because of another session running on different
  dir: ....

  複数のコンソールから実行した場合で、それぞれのコンソールで異なるカレ
  ントディレクトリで実行した場合も同じです。同時に実行中になることがな
  ければ、異なるカレントディレクトリであっても、複数のコンソールから利
  用しても問題ありません。

  必要であれば別ポートで複数のGroovyServerを起動することもできます。

* 静的変数はgroovyプログラム間の実行で共有されます。たとえば、システム
  プロパティが共有されます。

  > groovyclient -e "System.setProperty('a','abc')"
  > groovyclient -e "println System.getProperty('a')"
  abc

* 環境変数について、groovyclientコマンドを実行したときのシェルの状態で
  はなく､groovyserverが実行されたときの環境変数の値が使用されます。環境
  変数の変更をgroovyコマンドに影響を及ぼすようにするためには、
  groovyserverを再起動する必要があります。ただし環境変数CLASSPATHの指定
  はクライアントサイドのものが動的に設定されます。

* groovyclient実行時の環境変数CLASSPATH値、および-cpオプションで設定し
  たクラスバス情報は動的にサーバ側のクラスパスに追加されます。ただし、
  このような動的なクラスパス情報は追加のみがなされ、削除されることはあ
  りません。


================================
groovyserverのオプション
================================

GroovyServerの起動オプションは以下のとおり。

   -v 冗長表示。デバッグ情報などを表示します。
   -q メッセージを表示しない。デフォルト。
   -k 起動しているGroovyServerを終了させます。
   -r 起動しているGroovyServerを再起動します(停止+起動)。
   -p <port> GroovyServerがクライアントとの通信に使用する
             ポート番号を指定します。

================
ポート番号
================

通信に使用するポート番号を変更するには、環境変数GROOVYSERVER_PORTを設定
してください。

 > export GROOVYSERVER_PORT=1963

クライアントでは-pオプションで設定することもできます。-p オプションの設
定は環境変数の設定に優先されます。


================
ログファイル
================

~/.groovy/groovyserver/<プロセスID>-<ポート番号>.log

にgroovyserverのログファイルが出力されます。

================
連絡先
================

- githubのURL

================
クレジット
================

- Kobo Project.
- NTT Software Corp.
