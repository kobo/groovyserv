GroovyServ 0.1 README
2010年03月02日

==========
はじめに
==========

GroovyServは、Groovy処理系をサーバーとして動作させることでgroovyコマン
ドの起動を見た目高速化するものです。groovyコマンドはTCP/IP 通信を使って
すでに起動しているGroovyランタイムと通信し、結果を出力します。

もし、Emacsのemacsserver/Emacsclient(もしくはgnuserver/gnuclient)をご存
知ならば理解が早いでしょう。

Groovyスクリプトを開発する場合、起動速度がとても重要です。Groovyは動的
言語であるため、コンパイル時にあらかじめチェックできないエラーについて、
実行して初めて判明する場合が多いからです。そのため、実行を繰り返しなが
ら開発をしていくことになります。たとえその起動が1..2秒しかかからなくて
も、体感としてはとても大きく感じられるのではないでしょうか。

GroovyServを使うことで、Groovyコマンドの起動時間を短縮し、さくさくと開
発を進めていくことができます。以下は、Windows XP Core(TM) 2 Duo 2GHz の
マシンでの起動時間の例です(三回測定した平均値）。

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
言語と構成
==========

サーバサイドはJava、クライアントはC言語クライアントおよびRubyのクライア
ントが現在動作しています。サーバサイドではJNA(Java Native Access)を使用
しています。使用するコマンドは以下の通りです。

 groovyserver     GroovyServサーバを起動するコマンド
 groovyclient     ネイティブバイナリ版GroovyServクライアント。
 groovyclient.rb  Ruby版GroovyServクライアント。

================
セキュリティ
================

GroovyServサーバへの接続は、localhostからのみに制限されており、他のマシ
ンからはgroovyclientコマンドを使って接続することはできません。また、同
マシン上でもgroovyserverを起動したユーザーと同じユーザが実行した
groovyclient の接続しか受け付けないように制約をかけています。なお、こ制
約は、サーバが実行後とに生成する秘密のクッキー情報~/.groovy/groovyserver/key
ファイルの内容を自ユーザのみが読み出せることに依存しています。このファ
イルのアクセス制限を、UNIX環境ではowner以外からは読めないように設定
(chmod 0400)していますが、Windows環境ではこの設定が機能しないため、必要
に応じて他のユーザから読み出せないように設定してください。

================
Groovyのバージョン
================

 Groovy 1.6以降で動きます。

============================================
バイナリパッケージからのインストール
============================================

バイナリパッケージgroovyserv-0.1-<OS>-<arch>-bin.zipを適当なフォルダ
に展開します。例えば、~/optに展開するとします。

 > mkdir ~/opt
 > cd ~/opt
 > unzip groovyserv-0.1-win32-bin.zip

上記により~/opt/groovyserv-0.1が展開されます。次に環境変数PATHに上記フォ
ルダ配下のbinを追加します。仮に、~/opt/groovyservに展開した場合、以下の
ように設定します(bashなどの環境変数設定)。

 export PATH=~/opt/groovyserv-0.1/bin:$PATH

設定は以上です。groovyclientを実行するとgroovyserverが起動します。

 > groovyclient -v
 starting server..
 Groovy Version: 1.7.0 JVM: 1.6.0_13


============================================
ソースコードからのビルド
============================================

まず、GroovyServerのソースコード配布パッケージ
groovyserv-0.1-src.zipを展開します。

 > mkdir -p ~/opt/src
 > cd ~/opt/src
 > unzip -l groovyserv-0.1-src.zip

Maven2を使ってコンパイルします。

 > cd ~/opt/src/groovyserv-0.1/
 > mvn clean compile assembly:assembly

コンパイルした結果、バイナリパッケージが

  ~/opt/src/groovyserv-0.1/target/groovyserv-0.1-<OS>-<arch>-bin.zip

という形式で作成されますので、これをバイナリパッケージからのインストー
ルの場合と同じようにインストールしてください。テストで失敗する場合以下
のようにテストをスキップする事もできます。

 > mvn -Dmaven.test.skip=true clean compile assembly:assembly

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

 > groovyserver -v

その他の起動オプションについては後述します。

================
制限・機能の違い
================

* 異なるカレントディレクトリを同時に使うことはできません。たとえば、パ
  イプでつないで２つのGroovyコマンドを実行し、それぞれが異なるカレント
  ディレクトリであるように実行することはできません。

   >  groovyclient -e "..."   | (cd /tmp; groovyclient -e "......") 

  この場合例外が発生します。

  org.jggug.kobo.groovyserv.GroovyServerException: Can't change
  current directory because of another session running on different
  dir: ....

  複数のコンソールから実行した場合で、それぞれのコンソールで異なるカレ
  ントディレクトリで実行した場合も同じです。同時に実行中になることがな
  ければ、異なるカレントディレクトリであっても、複数のコンソールから利
  用しても問題ありません。

  必要であれば別ポートで複数のGroovyServサーバを起動することもできます。

* 静的変数はgroovyプログラム間の実行で共有されます。たとえば、システム
  プロパティが共有されます。

  > groovyclient -e "System.setProperty('a','abc')"
  > groovyclient -e "println System.getProperty('a')"
  abc

  ただし、System.out/System.in/System.errはそれぞれのセッション毎に区別
  され、それぞれの標準入力／出力／エラー出力に接続されます。

* 環境変数について、groovyclientコマンドを実行したときの値ではなく､
  groovyserverが実行されたときの値が使用されます。環境変数の変更を
  groovyコマンドに影響を及ぼすようにするためには、groovyserverを再起動
  する必要があります。ただし環境変数CLASSPATHの指定はクライアントサイド
  のものが動的に設定されます（後述）。

* 前述のように、groovyclient実行時の環境変数CLASSPATH値、および-cpオプ
  ションで設定したクラスバス情報は動的にサーバ側のクラスパスに追加され
  ます。ただし、このような動的なクラスパス情報は追加のみがなされ、削除
  されることはありません。

================================
groovyserverのオプション
================================

groovyserverの起動オプションは以下のとおり。

   -v 冗長表示。デバッグ情報などを表示します。
   -q メッセージを表示しない。デフォルト。
   -k 起動しているgroovyserverを終了させます。
   -r 起動しているgroovyserverを再起動します(停止+起動)。
   -p <port> groovyserverがクライアントとの通信に使用する
             ポート番号を指定します。

================
ポート番号
================

通信に使用するポート番号を変更するには、環境変数GROOVYSERVER_PORTを設定
してください。

 > export GROOVYSERVER_PORT=1963

クライアントでもGROOVYSERVER_PORTの値が通信するポート番号として使用され
ますが、-pオプションで設定することもできます。環境変数と-pオプション両
方が指定された場合、-pオプションの値が優先されます。

================
ログファイル
================

~/.groovy/groovyserver/<プロセスID>-<ポート番号>.log

にgroovyserverのログファイルが出力されます。

================
連絡先
================

- http://kobo.github.com/groovyserv/

================
クレジット
================

- Kobo Project.
- NTT Software Corp.
