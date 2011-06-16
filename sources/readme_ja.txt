.. _ref-readme_ja:

はじめにお読みください
======================

はじめに
--------

GroovyServは、Groovy処理系をサーバとして動作させることでgroovyコマンドの起動を見た目上高速化するものです。
groovyclientコマンドはTCP/IP 通信を使ってすでに起動しているGroovyランタイムと通信し、結果を出力します。
もし、Emacsのgnuserv(gnudoit)やemacsserver/emacsclientをご存知であれば理解が早いでしょう。

Groovyスクリプトを開発する場合、起動速度がとても重要です。
Groovyは動的言語であるため、コンパイル時にあらかじめチェックできないエラーについて、実行して初めて判明する場合が多いからです。
そのため、実行を繰り返しながら開発をしていくことになります。
たとえその起動が1..2秒しかかからなくても、体感としてはとても大きく感じられるのではないでしょうか。

GroovyServを使うことで、Groovyコマンドの起動時間を短縮し、さくさくと開発を進めていくことができます。
以下は、Windows XP Core(TM) 2 Duo 2.53GHzのマシンにおけるGroovy 1.8.0の起動時間の参考値です(5回測定した平均値)。

    ==================  ===========
    コマンド            結果(sec)
    ==================  ===========
    Groovy              1.1058
    GroovyServ          0.0412
    ==================  ===========

動作環境
--------

動作確認を行っているOSは以下のとおりです。他の環境で動作した場合レポートをいただけますと幸いです。

  - Mac OS X 10.5/6 (Intel Mac)
  - Ubuntu Linux 9.10
  - Ubuntu Linux 10.04
  - Windows XP + Cygwin 1.7.x
  - Windows XP (Cygwin無し)
  - Windows 7 64bit + Cygwin 1.7.x
  - Windows 7 64bit (Cygwin無し)

JDKのバージョンは以下のとおりです。

  - JDK 6u24以降 (Mac OS X)
  - JDK 6u21以降 (Windows, Linux)

Groovyのバージョンは以下のとおりです。

  - Groovy 1.7以降

実装言語
--------

サーバサイドはJavaとGroovy、クライアントはC言語クライアントおよびRubyの
クライアントが現在動作しています。また、サーバサイドではJNA(Java Native Access)
を使用しています。

セキュリティ
------------

GroovyServサーバへの接続は、localhostからのみに制限されており、他のマシ
ンからはgroovyclientコマンドを使って接続することはできません。また、同
マシン上でもgroovyserverを起動したユーザーと同じユーザが実行した
groovyclient の接続しか受け付けないように制約をかけています。
なお、この制約は、サーバが実行ごとに生成する秘密のクッキー情報ファイル
~/.groovy/groovyserv/cookie-<port>の内容を自ユーザのみが読み出せること
に依存しています。このファイルのアクセス制限を、UNIX環境ではowner以外か
らは読めないように設定(chmod 0400)していますが、Windows環境ではこの設定
が機能しないため、必要に応じて他のユーザから読み出せないように設定して
ください。

バイナリパッケージからのインストール
------------------------------------

バイナリパッケージgroovyserv-0.8-<OS>-<arch>-bin.zipを適当なフォルダ
に展開します。例えば、~/optに展開するとします。::

  $ mkdir ~/opt
  $ cd ~/opt
  $ unzip groovyserv-0.8-macosx-bin.zip

上記により~/opt/groovyserv-0.8が展開されます。次に環境変数PATHに上記フォ
ルダ配下のbinを追加します。仮に、~/opt/groovyservに展開した場合、以下の
ように設定します(bashなどの環境変数設定)。::

  export PATH=~/opt/groovyserv-0.8/bin:$PATH

設定は以上です。groovyclientを実行するとgroovyserverが起動します。::

  $ groovyclient -v
  Invoking server: '/xxx/groovyserv-0.8/bin/groovyserver' -p 1961 
  Groovy home directory: (none)
  Groovy command path: /usr/local/bin/groovy (found at PATH)
  GroovyServ home directory: /xxx/groovyserv-0.8
  GroovyServ work directory: /Users/ynak/.groovy/groovyserv
  Original classpath: (none)
  GroovyServ default classpath: /xxx/groovyserv-0.8/lib/*
  Starting...
  groovyserver 75808(1961) is successfully started
  Groovy Version: 1.8.0 JVM: 1.6.0_24

ソースコードからのビルド
------------------------

まず、GroovyServのソースコード配布パッケージgroovyserv-0.8-src.zipを展開します。::

  $ mkdir -p ~/opt/src
  $ cd ~/opt/src
  $ unzip groovyserv-0.8-src.zip

Mavenを使ってコンパイルします。v0.6からはMaven3が推奨です。::

  $ cd ~/opt/src/groovyserv-0.8/
  $ mvn clean verify

コンパイルした結果、バイナリパッケージが::

  ~/opt/src/groovyserv-0.8/target/groovyserv-0.8-<OS>-<arch>-bin.zip

という形式で作成されますので、これをバイナリパッケージからのインストー
ルの場合と同じようにインストールしてください。テストで失敗する場合は以
下をお試しください。

  文字エンコードをUTF-8に設定する::

    $ export _JAVA_OPTIONS=-Dfile.encoding=UTF-8

  結合テストをスキップする::

    $ mvn clean package

  すべてのテストをスキップする::

    $ mvn -Dmaven.test.skip=true clean package

Windows上でビルドするためにはgcc-3とMinGWが必要です(Cygwin上でのビルドを推奨)。
ビルドを実行する前にインストールしてください。

環境変数
--------

GroovyServは、実行時に以下の環境変数を使用します。

  HOME (LinuxまたはMac OS Xの場合のみ)
    クッキーやPID、ログファイルを格納する~/.groovy/groovyserv ディレ
    クトリを決定するために使用します。Unix系のOSであれば標準で設定され
    ています。

  USERPROFILE (Windowsの場合のみ)
    クッキーやPID、ログファイルを格納する~/.groovy/groovyserv ディレ
    クトリを決定するために使用します。Windows標準で設定されています。
    BATファイルで起動された場合は、PIDファイルは作成されません。

  JAVA_HOME
    Groovyを実行するために必要です。
    通常はGroovyのインストール作業の一貫で設定されています。

  GROOVY_HOME (オプション)
    groovyコマンドのパスを特定するために使用します。
    groovyコマンドが環境変数PATHに設定されている場合はパス探索で見つけることができるため、環境変数GROOVY_HOMEは必要ありません。

  PATH中のgroovyコマンドパス (オプション)
    groovyコマンドのパスを特定するために使用します。
    環境変数GROOVY_HOMEが設定されている場合はそちらが優先して使用されるため、環境変数PATHへのgroovyコマンドの設定は必要ありません。

  GROOVYSERVER_PORT (オプション)
    サーバやクライアントでポート番号を指定する場合に使用します。
    コマンド引数で代替することもできます。

  CLASSPATH (オプション)
    groovyserverを起動したときの環境変数CLASSPATHは、そのままgroovyserver
    プロセスに引き継がれて、GroovyServとして必要なクラスパスが追加された
    ものがデフォルトクラスパスとして使われます。これは全てのスクリプト処理
    に影響します。

    groovyclientを起動したときの環境変数CLASSPATHは、毎回groovyserverへと
    転送され、サーバ上のスクリプトのコンパイル設定に動的に反映されます。
    (サーバ側の環境変数CLASSPATHに追加設定されるわけではない。)
    このクラスパスはセッションごとにクリアされるため、次回のスクリプトの
    実行に影響することはありません。スクリプト実行におけるクラスパスの探索
    では、groovyserver上の環境変数CLASSPATHが優先的に探索されます。
    なおこれらの振る舞いはgroovyclientの-cpオプションと全く同一です。

使い方
------

groovyコマンドの代わりにgroovyclientコマンドを実行します。groovyclient
を実行したとき、groovyserverが起動されていなければ、バックグラウンドで
groovyserverが起動されます。起動されていない場合、起動のために数秒の待
ち時間の後、サーバが起動し、実行が行われます。

明示的にgroovyserverを起動しておくこともできます。::

  $ groovyserver

起動オプションに-vを指定するとログファイルに詳細メッセージが出力されま
す。起動トラブルなどの解析に便利です。::

  $ groovyserver -v

その他の起動オプションについては後述します。

制限・機能の違い
----------------

* 異なるカレントディレクトリを同時に使うことはできません。複数のコン
  ソールから実行した場合で、それぞれのコンソールで異なるカレントディ
  レクトリで実行した場合も同じです。同時に実行中になることがなければ、
  異なるカレントディレクトリであっても、複数のコンソールから利用して
  も問題ありません。

  必要であれば別ポートで複数のGroovyServサーバを起動することもできます。
  別のサーバプロセスであれば、同時に異なるカレントディレクトリに対して
  処理を実行することができます。

* 静的変数はGroovyプログラム間の実行で共有されます。たとえば、システム
  プロパティが共有されます。::

    $ groovyclient -e "System.setProperty('a','abc')"
    $ groovyclient -e "println System.getProperty('a')"
    abc

  ただし、System.out／System.in／System.errはそれぞれのセッション毎に
  区別され、それぞれの標準入力／出力／エラー出力に接続されます。

* 環境変数は、通常、groovyclientコマンドを実行したときの値ではなく､
  groovyserverが起動されたときの値が使用されます。しかし、-Cenv、
  -Cenv-allオプションを指定することで、groovyclient実行時の環境変数の
  値をgroovyserver側に反映させることもできます。

  ただし環境変数CLASSPATHについては、これらのオプションを指定しなくて
  も、毎回実行ごとにクライアント側の値が動的にサーバ側に反映されます。
  このクラスパスはセッションごとにクリアされて次回のスクリプトの実行に
  影響することはありません。

groovyclientのオプション
------------------------

groovyclientでは-Cで始まる以下の起動オプションが指定可能です。これらの
オプションはgroovyclientで解釈され、groovyコマンドには渡されません。::

  -Ch,-Chelp               このメッセージを表示する
  -Cp,-Cport <port>        接続するgroovyserverのポート番号を指定する
  -Ck,-Ckill-server        起動中のgroovyserverを停止する
  -Cr,-Crestart-server     起動中のgroovyserverを再起動する
  -Cq,-Cquiet              起動時のメッセージを表示しない
  -Cenv <substr>           substrを変数名に含む環境変数をサーバに転送する
  -Cenv-all                すべての環境変数の値をサーバに転送する
  -Cenv-exclude <substr>   substrを変数名に含む環境変数をサーバへの転送から除外する

groovyserverのオプション
------------------------

groovyserverの起動オプションは以下の通りです。::

  -v         デバッグ情報などをログファイルに出力する
  -q         起動時のメッセージを表示しない
  -k         起動中のgroovyserverを終了する (groovyserver.batでは使えません)
  -r         起動中のgroovyserverを再起動する (groovyserver.batでは使えません)
  -p <port>  LISTENするポート番号を指定する

groovyserverの起動と停止
------------------------

groovyserverの起動方法には、groovyserverまたはgroovyserver.batを使って
明示的に起動する方法と、groovyclientから透過的にバックグラウンドで起動
する方法があります。

groovyserverを明示的に起動するためのコマンドは以下の通りです。

 - groovyserver      (Mac OS X, Linux, Windows(Cygwin))
 - groovyserver.bat  (Windows(Cygwinなし))

これらが利用可能な環境を以下に整理します。(OK: 利用可, N/A: 利用不可)

    =================  =================  ==================  ===============
    Script             Windows Cygwin版   Windows Cygwinなし  Mac OS X, Linux
    =================  =================  ==================  ===============
    groovyserver       OK                 N/A                 OK
    groovyserver.bat   OK                 OK                  N/A
    =================  =================  ==================  ===============

バッチファイル版groovyserver.batでは、技術的な理由により、-rオプション
と-kオプションは利用できません。このため、コマンドラインからは
groovyserverの終了と再起動を行うことができません。その代わり、起動した
groovyserverは別ウィンドウで最小化されて実行されるため、Windowsの操作で
ウィンドウを閉じることでgroovyserverを終了することができます。その後、
groovyclientを実行することで、全体としてgroovyserverの再起動が可能です。

注意が必要なのは、Cygwin環境下でも、groovyclient.exeを経由した
groovyserverの透過的な起動の際には、内部的にgroovyserver.batが使用され
るということです。このため、Cygwin環境下では以下のような状況となります。

 - シェルスクリプト版groovyserverで明示的に起動した場合、同じくシェルス
   クリプト版groovyserverを-k,-rオプションを指定して実行することで、
   起動中のgroovyserverの終了と再起動を行うことができる。

 - バッチファイル版groovyserver.batで明示的に起動した場合、groovyserver
   の終了は、ウィンドウを閉じることで行う。

 - groovyclient.exeを通じてgroovyserver.batの透過的起動を行った場合、
   groovyserverの終了は、ウィンドウを閉じることで行う。

ややこしいですが、将来的には、バッチファイル版でも-r,-kオプションの実装
ができるように検討しています。

なお、シェルスクリプト版とバッチファイル版を問わず、透過的起動では、内部
で起動するサーバにオプション(例えば-vオプション)を設定することはできませ
ん。デフォルト以外のオプションが必要な場合は、明示的な起動を行ってくださ
い。

環境変数の伝搬
--------------

groovyclientの-Cenvオプションを使うことで、指定した部分文字列が名前に
含まれている環境変数をgroovyserverに転送することができます。
groovyclientプロセスにおけるこれらの環境変数の値はサーバプロセスに転送
され、サーバプロセス上の同名の環境変数の値が上書きされます。この機能は
Groovyで書かれた外部コマンドを起動する際にパラメータを環境変数で受けわ
たすような仕様の既存ツール(IDE、TextMateなど)において特に有用です。

-Cenv-allオプションを指定すると、groovyclientプロセスのすべての環境変
数がサーバ側に渡されます。また-Cenv-excludeを併用することで、指定した
部分文字列を変数名に含む環境変数を転送から除外することができます。

例えば、::

  -Cenv SUBSTRING

という指定をした場合、転送される環境変数の集合は以下のような疑似コード
で決定されます。::

  allEnvironmentVariables.entrySet().findAll {
    it.name.contains("SUBSTRING")
  }

-Cenv／-Cenv-all／-Cenv-excludeを組み合わせたときのルールについては、
例えば、::

  -Cenv SUBSTRING
  -Cenv-all
  -Cenv-exclude EXCLUDE_SUBSTRING

のとき、以下の疑似コードの結果がgroovyserverプロセスに送られることに
なります。::

  allEnvironmentVariables.entrySet().findAll {
    if (isSpecifiedEnvAll || it.name.contains("SUBSTRING")) {
      if (!it.name.contains("EXCLUDE_SUBSTRING")) {
        return true
      }
    }
    return false
  }

groovyserverプロセスに設定された環境変数は、groovyclientの終了後も値
が残り続けることに注意してください。また、groovyserverプロセスにおけ
る環境変数の操作はスレッドセーフではありません。複数のgroovyclientが
同時に実行された場合、環境変数の値は後に起動されたgroovyclientによっ
て上書きされるため、予期せぬ結果となる可能性があります。

ポート番号
----------

groovyserverとgroovyclientが通信するTCPポートとして、デフォルトでは、
1961番ポートを使用します。サーバが通信に使用するポート番号を変更する
には、環境変数GROOVYSERVER_PORTを設定するか、-pオプションを指定して
ください。環境変数と-pオプション両方が指定された場合は、-pオプション
の値が優先されます。::

  $ export GROOVYSERVER_PORT=1963
  $ groovyserver

または::

  $ groovyserver -p 1963

クライアント側では環境変数GROOVYSERVER_PORT指定にくわえて-Cpオプション
でポート番号を指定可能です。透過的起動を行う場合にはgroovyserverに-pオ
プションが指定されて起動されます。::

  $ groovyclient -Cp 1963 -e '...'

ログファイル
------------

groovyserverのログは以下のファイルに出力されます。::

  ~/.groovy/groovyserv/groovyserver-<port>.log

Tips
----

groovyコマンドを実行すると代わりにgroovyclientが呼び出されるように、以
下のようにエイリアス(別名)指定を行っておくと便利です。以下はbash用のエ
イリアスの設定です。::

  alias groovy=groovyclient

Windowsではdoskeyコマンドで以下のように設定することができます。::

  doskey groovy=groovyclient $*

